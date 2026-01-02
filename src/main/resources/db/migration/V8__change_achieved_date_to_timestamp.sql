-- Migration: Changer le type de achieved_date de DATE à TIMESTAMP
-- Pour stocker la date ET l'heure d'atteinte de l'objectif

-- Pour PostgreSQL
ALTER TABLE goals ALTER COLUMN achieved_date TYPE TIMESTAMP USING achieved_date::TIMESTAMP;

-- Ajouter un index pour améliorer les performances des requêtes sur les objectifs atteints
CREATE INDEX IF NOT EXISTS idx_goals_achieved_date ON goals(achieved_date) WHERE achieved_date IS NOT NULL;

