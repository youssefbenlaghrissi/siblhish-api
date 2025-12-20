-- Créer la table pour stocker les cartes statistiques disponibles
CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,      -- Identifiant unique: "bar_chart", "pie_chart", etc.
    title VARCHAR(200) NOT NULL,           -- Titre de la carte
    description TEXT                       -- Description de la carte
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_cards_code ON cards(code);

-- Insérer les cartes disponibles
INSERT INTO cards (code, title, description) VALUES
    ('bar_chart', 'Graphique Revenus vs Dépenses', 'Comparaison des revenus et dépenses par mois'),
    ('pie_chart', 'Répartition par Catégorie', 'Visualisation de la répartition des dépenses par catégorie'),
    ('balance_card', 'Solde Actuel', 'Solde actuel de votre compte'),
    ('savings_card', 'Économies du Mois', 'Économies réalisées ce mois'),
    ('average_expense_card', 'Moyenne Mensuelle Dépenses', 'Dépense moyenne par mois'),
    ('top_expense_card', 'Dépense la Plus Élevée', 'La dépense la plus importante'),
    ('average_income_card', 'Moyenne Mensuelle Revenus', 'Revenu moyen par mois'),
    ('transaction_count_card', 'Nombre de Transactions', 'Nombre total de transactions'),
    ('top_category_card', 'Top Catégorie', 'Catégorie avec le plus de dépenses'),
    ('scheduled_payments_card', 'Paiements Planifiés', 'Statistiques sur les paiements planifiés')
ON CONFLICT (code) DO NOTHING;

