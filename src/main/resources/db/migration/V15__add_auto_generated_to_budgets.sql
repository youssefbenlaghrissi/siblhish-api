-- Marque les budgets créés automatiquement par le scheduler (vs saisie manuelle)
ALTER TABLE budgets
    ADD COLUMN IF NOT EXISTS auto_generated BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN budgets.auto_generated IS
    'true si la ligne a été créée par RecurringBudgetScheduler ; false pour saisie utilisateur.';

