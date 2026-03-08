-- ============================================================================
-- Migration V12: parent_scheduled_payment_id sur scheduled_payments
-- ============================================================================
-- Permet de lier une occurrence générée par le batch à son template récurrent,
-- pour créer une occurrence par paiement récurrent (et non une seule par nom/montant/date).
-- ============================================================================

ALTER TABLE scheduled_payments
    ADD COLUMN IF NOT EXISTS parent_scheduled_payment_id BIGINT NULL;

COMMENT ON COLUMN scheduled_payments.parent_scheduled_payment_id IS
    'ID du paiement planifié parent (template) dont ce paiement est la prochaine occurrence récurrente. Null pour les créations manuelles.';

CREATE INDEX IF NOT EXISTS idx_scheduled_payments_parent_due
    ON scheduled_payments (parent_scheduled_payment_id, due_date)
    WHERE parent_scheduled_payment_id IS NOT NULL;
