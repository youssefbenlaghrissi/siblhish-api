-- Migration pour ajouter le champ fcm_token à la table users
-- Ce champ permet de stocker le token FCM (Firebase Cloud Messaging) pour envoyer des notifications push

ALTER TABLE users ADD COLUMN fcm_token VARCHAR(500) NULL;

-- Ajouter un index pour améliorer les performances lors des recherches
CREATE INDEX IF NOT EXISTS idx_users_fcm_token ON users(fcm_token);

-- Commentaire sur la colonne (si votre SGBD le supporte)
-- COMMENT ON COLUMN users.fcm_token IS 'Token FCM (Firebase Cloud Messaging) pour envoyer des notifications push à l''utilisateur';

