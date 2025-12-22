-- Ajouter la colonne is_recurring à la table budgets
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN NOT NULL DEFAULT FALSE;

-- Ajouter un index pour améliorer les performances des requêtes sur les budgets récurrents
CREATE INDEX IF NOT EXISTS idx_budgets_recurring ON budgets(is_recurring) WHERE is_recurring = TRUE;

