-- ============================================================================
-- Insert JDD scheduled_payments (creation_date = 08/03/2026, due_date = 12/03/2026)
-- À exécuter manuellement (ou via outil) après adaptation des id si conflit.
-- ============================================================================

-- Nettoyer les anciennes données de test (optionnel, décommenter si besoin)
-- DELETE FROM scheduled_payment_recurrence_days WHERE scheduled_payment_id BETWEEN 5311 AND 5324;
-- DELETE FROM scheduled_payments WHERE id BETWEEN 5311 AND 5324;

INSERT INTO scheduled_payments (
    id, creation_date, deleted, name, amount, payment_method, beneficiary,
    due_date, is_recurring, recurrence_frequency, recurrence_end_date,
    recurrence_day_of_month, recurrence_day_of_year, notification_option,
    is_paid, paid_date, user_id, category_id, parent_scheduled_payment_id
) VALUES
-- 5311 - quotidien sans limite
(5311, '2026-03-08 00:00:00', false, 'paiement quotidien sans limite', 5, 'CASH', 'Shell',
 '2026-03-12 08:00:00', true, 'DAILY', NULL, NULL, NULL, 'NONE', false, NULL, 14, 5, NULL),
-- 5312 - quotidien avec limite 8/3
(5312, '2026-03-08 00:00:00', false, 'paiement quotidien avec limite', 35, 'CASH', 'shell',
 '2026-03-12 08:30:00', true, 'DAILY', '2026-03-08 00:00:00', NULL, NULL, 'NONE', false, NULL, 14, 5, NULL),
-- 5313 - hebdo sans limite (vendredi = 5)
(5313, '2026-03-08 00:00:00', false, 'paiement hebdo sans limite', 45, 'CASH', 'indrive',
 '2026-03-12 09:00:00', true, 'WEEKLY', NULL, NULL, NULL, 'NONE', false, NULL, 14, 4, NULL),
-- 5314 - hebdo avec limite 8/3 (lundi = 1)
(5314, '2026-03-08 00:00:00', false, 'paiement hebdo avec limite', 50, 'CASH', 'train',
 '2026-03-12 09:20:00', true, 'WEEKLY', '2026-03-08 00:00:00', NULL, NULL, 'NONE', false, NULL, 14, 4, NULL),
-- 5315 - mensuel 7 sans limite
(5315, '2026-03-08 00:00:00', false, 'paiement mensuel sans limite', 3000, 'CASH', 'mol dar',
 '2026-03-12 12:00:00', true, 'MONTHLY', NULL, 7, NULL, 'NONE', false, NULL, 14, 13, NULL),
-- 5316 - mensuel 5 avec limite 31/3
(5316, '2026-03-08 00:00:00', false, 'paiement mensuel avec limite', 3200, 'CASH', 'haja maad',
 '2026-03-12 13:00:00', true, 'MONTHLY', '2026-03-31 00:00:00', 5, NULL, 'NONE', false, NULL, 14, 13, NULL),
-- 5323 - annuel jour 66 sans limite
(5323, '2026-03-08 00:00:00', false, 'paiement annuel sans limite', 1500, 'CASH', NULL,
 '2026-03-12 14:00:00', true, 'YEARLY', NULL, NULL, 66, 'NONE', false, NULL, 14, 7, NULL),
-- 5324 - annuel jour 66 avec limite 31/3
(5324, '2026-03-08 00:00:00', false, 'paiement annuel avec limite', 1500, 'CASH', NULL,
 '2026-03-12 16:00:00', true, 'YEARLY', '2026-03-31 00:00:00', NULL, 66, 'NONE', false, NULL, 14, 7, NULL);

-- Jours de la semaine pour les hebdo (1=lundi .. 7=dimanche)
-- 5313 : vendredi = 5 (12/3/2026 est un vendredi)
INSERT INTO scheduled_payment_recurrence_days (scheduled_payment_id, day_of_week) VALUES (5313, 5);
-- 5314 : lundi = 1
INSERT INTO scheduled_payment_recurrence_days (scheduled_payment_id, day_of_week) VALUES (5314, 1);

-- Réinitialiser la séquence id si tu utilises des id explicites (PostgreSQL)
SELECT setval(pg_get_serial_sequence('scheduled_payments', 'id'), (SELECT COALESCE(MAX(id), 1) FROM scheduled_payments));
