-- ============================================================================
-- Migration V12: parent_scheduled_payment_id sur scheduled_payments
-- ============================================================================
-- Permet de lier une occurrence générée par le batch à son template récurrent.
-- Null pour les modèles créés par l'utilisateur ; renseigné pour les occurrences.
-- ============================================================================

-- 1. Ajout de la colonne
ALTER TABLE scheduled_payments
    ADD COLUMN IF NOT EXISTS parent_scheduled_payment_id BIGINT NULL;

-- 2. Commentaire
COMMENT ON COLUMN scheduled_payments.parent_scheduled_payment_id IS
    'ID du paiement planifié parent (template). Null pour les modèles, renseigné pour les occurrences créées par le batch.';

-- 3. Index pour les requêtes du batch (max due_date par parent, exists par parent+date)
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_parent_due
    ON scheduled_payments (parent_scheduled_payment_id, due_date)
    WHERE parent_scheduled_payment_id IS NOT NULL;

