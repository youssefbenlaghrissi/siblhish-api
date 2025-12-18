-- Migration: Ajouter le champ transaction_type à la table notifications
-- Date: 2024-01-15
-- Description: Ajoute le champ transactionType pour différencier les notifications de revenus (INCOME) et dépenses (EXPENSE)

-- Ajouter la colonne transaction_type si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'notifications' 
        AND column_name = 'transaction_type'
    ) THEN
        ALTER TABLE notifications 
        ADD COLUMN transaction_type VARCHAR(20);
        
        COMMENT ON COLUMN notifications.transaction_type IS 'Type de transaction: INCOME pour revenus, EXPENSE pour dépenses, NULL pour autres types';
    END IF;
END $$;

