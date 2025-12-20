-- Créer la table pour stocker tous les favoris utilisateur
CREATE TABLE IF NOT EXISTS favoris (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,           -- Ex: "CARD", "CATEGORY_COLOR", etc.
    target_entity BIGINT NOT NULL,       -- ID de l'entité ciblée (ex: ID de la carte, ID de la catégorie)
    value TEXT,                           -- Ex: "position=1" pour les cartes, "#FF0000" pour les couleurs
    CONSTRAINT fk_favorite_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_type_target UNIQUE (user_id, type, target_entity)
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_favoris_user_type ON favoris(user_id, type);
CREATE INDEX IF NOT EXISTS idx_favoris_user_type_target ON favoris(user_id, type, target_entity);

