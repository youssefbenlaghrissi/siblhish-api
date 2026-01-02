-- Migration: Ajouter la colonne achieved_date à la table goals
-- Cette colonne enregistre la date et l'heure à laquelle l'objectif a été atteint

ALTER TABLE goals ADD COLUMN IF NOT EXISTS achieved_date TIMESTAMP;

-- Ajouter un index pour améliorer les performances des requêtes sur les objectifs atteints
CREATE INDEX IF NOT EXISTS idx_goals_achieved_date ON goals(achieved_date) WHERE achieved_date IS NOT NULL;

