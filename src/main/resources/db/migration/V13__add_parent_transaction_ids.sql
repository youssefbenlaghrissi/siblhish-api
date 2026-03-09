-- ============================================================================
-- Migration V13: parent_expense_id et parent_income_id
-- ============================================================================
-- Permet de lier une dépense/revenu généré par le batch à son template récurrent.
-- Null pour les modèles créés par l'utilisateur ; renseigné pour les occurrences.
-- ============================================================================

-- 1. Ajout des colonnes génériques parent_transaction_id
ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS parent_transaction_id BIGINT NULL;

ALTER TABLE incomes
    ADD COLUMN IF NOT EXISTS parent_transaction_id BIGINT NULL;

-- 2. Commentaires
COMMENT ON COLUMN expenses.parent_transaction_id IS
    'ID de la transaction parent (template) pour une dépense. Null pour les modèles, renseigné pour les occurrences créées par le batch.';

COMMENT ON COLUMN incomes.parent_transaction_id IS
    'ID de la transaction parent (template) pour un revenu. Null pour les modèles, renseigné pour les occurrences créées par le batch.';

-- 3. Index pour les requêtes éventuelles par parent
CREATE INDEX IF NOT EXISTS idx_expenses_parent
    ON expenses (parent_transaction_id)
    WHERE parent_transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_incomes_parent
    ON incomes (parent_transaction_id)
    WHERE parent_transaction_id IS NOT NULL;

