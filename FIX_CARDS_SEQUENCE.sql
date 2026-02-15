-- ============================================
-- Script pour corriger le problème des IDs de cards
-- ============================================
-- 
-- PROBLÈME:
-- Après un TRUNCATE, les nouveaux inserts ont des IDs qui ne correspondent pas
-- aux anciens favoris (ex: IDs 19-20-21 au lieu de 1-2-3)
--
-- SOLUTIONS:
-- ============================================

-- ============================================
-- SOLUTION 1: TRUNCATE avec RESTART IDENTITY (RECOMMANDÉ)
-- ============================================
-- Cette commande réinitialise la séquence en même temps que le TRUNCATE
TRUNCATE TABLE cards RESTART IDENTITY CASCADE;

-- Puis réinsérer les cards (les IDs recommenceront à 1)
INSERT INTO cards (code, title, description) VALUES
    ('bar_chart', 'Graphique Revenus vs Dépenses', 'Comparaison des revenus et dépenses par mois'),
    ('pie_chart', 'Répartition par Catégorie', 'Visualisation de la répartition des dépenses par catégorie'),
    ('balance_card', 'Solde Actuel', 'Solde actuel de votre compte'),
    ('savings_card', 'Économies du Mois', 'Économies réalisées ce mois'),
    ('average_expense_card', 'Moyenne Mensuelle Dépenses', 'Dépense moyenne par mois'),
    ('top_expense_card', 'Dépense la Plus Élevée', 'La dépense la plus importante'),
    ('average_income_card', 'Moyenne Mensuelle Revenus', 'Revenu moyen par mois'),
    ('transaction_count_card', 'Nombre de Transactions', 'Nombre total de transactions'),
    ('top_category_card', 'Top Catégorie', 'Catégorie avec le plus de dépenses'),
    ('scheduled_payments_card', 'Paiements Planifiés', 'Statistiques sur les paiements planifiés')
ON CONFLICT (code) DO UPDATE 
    SET title = EXCLUDED.title,
        description = EXCLUDED.description;


-- ============================================
-- SOLUTION 2: Utiliser des IDs fixes (ALTERNATIVE)
-- ============================================
-- Si vous avez déjà fait TRUNCATE sans RESTART IDENTITY:

-- 1. Réinitialiser manuellement la séquence
SELECT setval('cards_id_seq', 1, false);

-- 2. Insérer avec des IDs explicites
INSERT INTO cards (id, code, title, description) VALUES
    (1, 'bar_chart', 'Graphique Revenus vs Dépenses', 'Comparaison des revenus et dépenses par mois'),
    (2, 'pie_chart', 'Répartition par Catégorie', 'Visualisation de la répartition des dépenses par catégorie'),
    (3, 'balance_card', 'Solde Actuel', 'Solde actuel de votre compte'),
    (4, 'savings_card', 'Économies du Mois', 'Économies réalisées ce mois'),
    (5, 'average_expense_card', 'Moyenne Mensuelle Dépenses', 'Dépense moyenne par mois'),
    (6, 'top_expense_card', 'Dépense la Plus Élevée', 'La dépense la plus importante'),
    (7, 'average_income_card', 'Moyenne Mensuelle Revenus', 'Revenu moyen par mois'),
    (8, 'transaction_count_card', 'Nombre de Transactions', 'Nombre total de transactions'),
    (9, 'top_category_card', 'Top Catégorie', 'Catégorie avec le plus de dépenses'),
    (10, 'scheduled_payments_card', 'Paiements Planifiés', 'Statistiques sur les paiements planifiés')
ON CONFLICT (code) DO UPDATE 
    SET title = EXCLUDED.title,
        description = EXCLUDED.description;

-- 3. Réinitialiser la séquence après l'insertion
SELECT setval('cards_id_seq', (SELECT MAX(id) FROM cards));


-- ============================================
-- SOLUTION 3: Mettre à jour les favoris existants (SI DÉJÀ CASSÉ)
-- ============================================
-- Si vous avez déjà des favoris avec de mauvais IDs, vous pouvez les mettre à jour
-- en utilisant le code de la carte au lieu de l'ID

-- Exemple: Mettre à jour les favoris pour utiliser le code au lieu de l'ID
-- (nécessite de modifier la structure de la table favoris pour stocker le code)
-- UPDATE favoris f
-- SET target_entity = c.id
-- FROM cards c
-- WHERE f.type = 'CARD'
--   AND f.target_entity IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
--   AND c.code = (
--       SELECT code FROM cards WHERE id = f.target_entity
--   );


-- ============================================
-- VÉRIFICATION
-- ============================================
-- Vérifier que les IDs sont corrects
SELECT id, code, title FROM cards ORDER BY id;

-- Vérifier les favoris qui pointent vers des cards inexistantes
SELECT f.id, f.user_id, f.type, f.target_entity, f.value
FROM favoris f
LEFT JOIN cards c ON c.id = f.target_entity
WHERE f.type = 'CARD' AND c.id IS NULL;

