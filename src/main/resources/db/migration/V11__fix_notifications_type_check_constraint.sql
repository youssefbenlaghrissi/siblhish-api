-- Migration V11: Correction de la contrainte CHECK pour le champ type de la table notifications
-- La contrainte doit inclure toutes les valeurs de l'enum TypeNotification

-- Supprimer l'ancienne contrainte si elle existe
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

-- Recréer la contrainte avec toutes les valeurs de TypeNotification
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check 
    CHECK (type IN (
        'DAILY_REPORT',
        'MONTHLY_REPORT',
        'RECURRING_TRANSACTION',
        'RECURRING_BUDGET',
        'RECURRING_SCHEDULED_PAYMENT',
        'PAYMENT_REMINDER',
        'PAYMENT_MARKED_AS_PAID',
        'PAYMENT_DUE_TODAY',
        'PAYMENT_OVERDUE',
        'BUDGET_WARNING',
        'BUDGET_EXCEEDED'
    ));

