-- Marque les dépenses / revenus créés par le batch récurrent (vs saisie manuelle)
ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS auto_generated BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE incomes
    ADD COLUMN IF NOT EXISTS auto_generated BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN expenses.auto_generated IS
    'true si la ligne a été créée par RecurringTransactionScheduler ; false pour saisie utilisateur.';

COMMENT ON COLUMN incomes.auto_generated IS
    'true si la ligne a été créée par RecurringTransactionScheduler ; false pour saisie utilisateur.';
