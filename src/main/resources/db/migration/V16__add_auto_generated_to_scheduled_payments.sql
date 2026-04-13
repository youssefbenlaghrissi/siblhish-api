-- Marque les paiements planifiés créés par le batch récurrent (vs saisie utilisateur)
ALTER TABLE scheduled_payments
    ADD COLUMN IF NOT EXISTS auto_generated BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN scheduled_payments.auto_generated IS
    'true si la ligne a été créée par RecurringScheduledPaymentScheduler ; false pour saisie utilisateur.';
