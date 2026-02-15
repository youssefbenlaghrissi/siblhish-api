-- Créer la table pour stocker les cartes statistiques disponibles
CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,      -- Identifiant unique: "bar_chart", "pie_chart", etc.
    title VARCHAR(200) NOT NULL,           -- Titre de la carte
    description TEXT                       -- Description de la carte
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_cards_code ON cards(code);

-- Insérer les cartes disponibles avec des IDs fixes pour garantir la cohérence
-- IMPORTANT: Utiliser des IDs fixes pour éviter les problèmes après TRUNCATE
INSERT INTO cards (id, code, title, description) VALUES
    (1, 'bar_chart', 'Graphique Revenus vs Dépenses', 'Comparaison des revenus et dépenses par mois'),
    (2, 'pie_chart', 'Répartition par Catégorie', 'Visualisation de la répartition des dépenses par catégorie'),
    (3, 'balance_card', 'Solde Actuel', 'Solde actuel de votre compte'),
    (4, 'savings_card', 'Économies du Mois', 'Économies réalisées ce mois'),
    (5, 'average_expense_card', 'Moyenne Mensuelle Dépenses', 'Dépense moyenne par mois'),
    (6, 'top_expense_card', 'Dépense la Plus Élevée', 'La dépense la plus importante'),
    (7, 'average_income_card', 'Moyenne Mensuelle Revenus', 'Revenu moyen par mois'),
    (8, 'transaction_count_card', 'Nombre de Transactions', 'Nombre total de transactions'),
    (9, 'top_category_card', 'Top Catégorie', 'Catégorie avec le plus de dépenses'),
    (10, 'scheduled_payments_card', 'Paiements Planifiés', 'Statistiques sur les paiements planifiés')
ON CONFLICT (code) DO UPDATE 
    SET title = EXCLUDED.title,
        description = EXCLUDED.description;

-- Réinitialiser la séquence pour qu'elle continue après le dernier ID utilisé
SELECT setval('cards_id_seq', (SELECT MAX(id) FROM cards));
