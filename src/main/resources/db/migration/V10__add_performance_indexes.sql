-- ============================================================================
-- Migration V10: Ajout d'index composites pour optimiser les performances
-- ============================================================================
-- 
-- Cette migration ajoute des index composites sur les colonnes fréquemment
-- utilisées dans les requêtes de filtrage et de tri, notamment pour :
-- - Les requêtes de transactions (expenses + incomes) avec ORDER BY date DESC
-- - Les requêtes de budgets avec filtres par période
-- - Les requêtes de statistiques avec filtres par date
-- - Les requêtes de paiements planifiés avec filtres par due_date
--
-- Impact attendu : Réduction significative du temps d'exécution des requêtes
-- (de 100-300ms à 5-20ms pour les requêtes de transactions avec LIMIT)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Index pour la table EXPENSES
-- ----------------------------------------------------------------------------

-- Index pour les requêtes de transactions avec ORDER BY creation_date DESC
-- Utilisé par : /transactions?limit, HomeService.getTransactions
-- Optimise : WHERE user_id = ? AND deleted = false ORDER BY creation_date DESC LIMIT ?
CREATE INDEX IF NOT EXISTS idx_expenses_user_deleted_date_desc
    ON expenses (user_id, deleted, creation_date DESC);

-- Index pour les requêtes de filtrage avec catégorie
-- Utilisé par : BudgetService.calculateSpent, StatisticsService.getBudgetStatisticsData
-- Optimise : WHERE user_id = ? AND deleted = false AND category_id = ? AND creation_date BETWEEN ? AND ?
-- IMPORTANT : Ce index est utilisé par StatisticsService pour les jointures avec categories
CREATE INDEX IF NOT EXISTS idx_expenses_user_deleted_category_date
    ON expenses (user_id, deleted, category_id, creation_date);

-- Index pour les requêtes de statistiques avec plage de dates
-- Utilisé par : StatisticsService.getPeriodSummary, getExpensesByCategory
-- Optimise : WHERE user_id = ? AND deleted = false AND creation_date >= ? AND creation_date <= ?
-- IMPORTANT : Ce index est utilisé par StatisticsService après optimisation (remplacement de DATE() par creation_date)
CREATE INDEX IF NOT EXISTS idx_expenses_user_deleted_date_range
    ON expenses (user_id, deleted, creation_date);

-- ----------------------------------------------------------------------------
-- Index pour la table INCOMES
-- ----------------------------------------------------------------------------

-- Index pour les requêtes de transactions avec ORDER BY creation_date DESC
-- Utilisé par : /transactions?limit, HomeService.getTransactions
-- Optimise : WHERE user_id = ? AND deleted = false ORDER BY creation_date DESC LIMIT ?
CREATE INDEX IF NOT EXISTS idx_incomes_user_deleted_date_desc
    ON incomes (user_id, deleted, creation_date DESC);

-- Index pour les requêtes de statistiques avec plage de dates
-- Utilisé par : StatisticsService.getPeriodSummary
-- Optimise : WHERE user_id = ? AND deleted = false AND creation_date >= ? AND creation_date <= ?
-- IMPORTANT : Ce index est utilisé par StatisticsService après optimisation (remplacement de DATE() par creation_date)
CREATE INDEX IF NOT EXISTS idx_incomes_user_deleted_date_range
    ON incomes (user_id, deleted, creation_date);

-- ----------------------------------------------------------------------------
-- Index pour la table BUDGETS
-- ----------------------------------------------------------------------------

-- Index pour les requêtes de budgets avec filtres par période
-- Utilisé par : BudgetRepository.findBudgetsWithSpentByUserAndMonth
-- Optimise : WHERE user_id = ? AND deleted = false AND start_date <= ? AND end_date >= ?
CREATE INDEX IF NOT EXISTS idx_budgets_user_deleted_dates
    ON budgets (user_id, deleted, start_date, end_date);

-- Index pour les requêtes de budgets récurrents
-- Utilisé par : BudgetRepository.findByIsRecurringTrueOrderByIdDesc
-- Optimise : WHERE is_recurring = true AND deleted = false ORDER BY id DESC
CREATE INDEX IF NOT EXISTS idx_budgets_recurring_deleted
    ON budgets (is_recurring, deleted, id DESC);

-- ----------------------------------------------------------------------------
-- Index pour la table SCHEDULED_PAYMENTS
-- ----------------------------------------------------------------------------

-- Index pour les requêtes de paiements planifiés non payés
-- Utilisé par : ScheduledPaymentRepository.findUnpaidByUserId
-- Optimise : WHERE user_id = ? AND is_paid = false AND deleted = false ORDER BY id DESC
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_user_paid_deleted
    ON scheduled_payments (user_id, is_paid, deleted, id DESC);

-- Index pour les requêtes de notifications de paiements
-- Utilisé par : ScheduledPaymentRepository.findPaymentsToNotify
-- Optimise : WHERE is_paid = false AND deleted = false AND due_date < ? AND due_date IS NOT NULL
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_notify
    ON scheduled_payments (is_paid, deleted, due_date)
    WHERE due_date IS NOT NULL;

-- ----------------------------------------------------------------------------
-- Index pour la table NOTIFICATIONS
-- ----------------------------------------------------------------------------

-- Index pour les requêtes de notifications non lues
-- Utilisé par : NotificationRepository.findUnreadByUserId
-- Optimise : WHERE user_id = ? AND is_read = false AND deleted = false ORDER BY creation_date DESC
CREATE INDEX IF NOT EXISTS idx_notifications_user_read_deleted_date
    ON notifications (user_id, is_read, deleted, creation_date DESC);

-- ----------------------------------------------------------------------------
-- Index pour les tables de récurrence (optionnel, mais utile)
-- ----------------------------------------------------------------------------

-- Index pour les sous-requêtes STRING_AGG dans les transactions
-- Utilisé par : HomeService.getTransactions (sous-requêtes de récurrence)
-- Optimise : WHERE expense_id = ? et WHERE income_id = ?
CREATE INDEX IF NOT EXISTS idx_expense_recurrence_days_expense_id
    ON expense_recurrence_days (expense_id);

CREATE INDEX IF NOT EXISTS idx_income_recurrence_days_income_id
    ON income_recurrence_days (income_id);

-- ============================================================================
-- Notes sur les index créés :
-- ============================================================================
-- 
-- 1. Ordre des colonnes dans les index composites :
--    - user_id en premier (sélectivité élevée)
--    - deleted en second (filtre binaire)
--    - date/category en dernier (pour le tri et les plages)
--
-- 2. Index avec DESC :
--    - Permet à PostgreSQL d'utiliser l'index directement pour ORDER BY DESC
--    - Évite un tri supplémentaire en mémoire
--
-- 3. Index partiels (WHERE clause) :
--    - idx_scheduled_payments_notify utilise un index partiel
--    - Réduit la taille de l'index en ne gardant que les lignes pertinentes
--
-- 4. Impact sur les écritures :
--    - Les index ralentissent légèrement les INSERT/UPDATE/DELETE
--    - Mais le gain en lecture (x5 à x20) compense largement
--
-- 5. Maintenance :
--    - PostgreSQL maintient automatiquement ces index
--    - VACUUM ANALYZE recommandé après migration pour optimiser les statistiques
-- ============================================================================

