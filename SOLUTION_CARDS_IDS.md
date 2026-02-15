# 🔧 Solution au Problème des IDs de Cards

## 📋 Problème Identifié

Après un `TRUNCATE` de la table `cards`, PostgreSQL ne réinitialise **pas automatiquement** la séquence d'auto-incrément. 

**Conséquence** :
- Les anciens favoris pointent vers des IDs 1, 2, 3...
- Après TRUNCATE, les nouveaux inserts ont des IDs 19, 20, 21...
- Les favoris deviennent invalides (références cassées)

---

## ✅ Solutions

### **Solution 1 : TRUNCATE avec RESTART IDENTITY (RECOMMANDÉ)**

Utilisez `TRUNCATE RESTART IDENTITY` pour réinitialiser la séquence en même temps :

```sql
TRUNCATE TABLE cards RESTART IDENTITY CASCADE;
```

**Avantages** :
- ✅ Simple et direct
- ✅ Réinitialise automatiquement la séquence
- ✅ Les nouveaux IDs recommencent à 1

**Inconvénient** :
- ⚠️ `CASCADE` supprime aussi les favoris (si vous voulez les garder, utilisez `RESTART IDENTITY` sans `CASCADE`)

---

### **Solution 2 : IDs Fixes dans le Script d'Insertion (MEILLEURE POUR LA PRODUCTION)**

Utilisez des IDs explicites dans votre script d'insertion :

```sql
-- Réinitialiser la séquence
SELECT setval('cards_id_seq', 1, false);

-- Insérer avec des IDs fixes
INSERT INTO cards (id, code, title, description) VALUES
    (1, 'bar_chart', 'Graphique Revenus vs Dépenses', '...'),
    (2, 'pie_chart', 'Répartition par Catégorie', '...'),
    ...
ON CONFLICT (code) DO UPDATE 
    SET title = EXCLUDED.title,
        description = EXCLUDED.description;

-- Réinitialiser la séquence après insertion
SELECT setval('cards_id_seq', (SELECT MAX(id) FROM cards));
```

**Avantages** :
- ✅ Garantit toujours les mêmes IDs
- ✅ Les favoris restent valides
- ✅ Fonctionne même après plusieurs TRUNCATE

**Inconvénient** :
- ⚠️ Nécessite de spécifier les IDs manuellement

---

### **Solution 3 : Utiliser le CODE au lieu de l'ID (SOLUTION LONG TERME)**

**Recommandation** : Modifier la structure pour utiliser le `code` (qui est unique) au lieu de l'ID.

#### Étape 1 : Modifier la table favoris

```sql
-- Ajouter une colonne pour stocker le code de la carte
ALTER TABLE favoris ADD COLUMN target_code VARCHAR(50);

-- Mettre à jour les favoris existants
UPDATE favoris f
SET target_code = c.code
FROM cards c
WHERE f.type = 'CARD' AND f.target_entity = c.id;

-- Créer un index
CREATE INDEX idx_favoris_target_code ON favoris(target_code);

-- Optionnel : Supprimer target_entity si vous ne l'utilisez plus
-- ALTER TABLE favoris DROP COLUMN target_entity;
```

#### Étape 2 : Modifier le code Java

```java
// Au lieu de :
favorite.setTargetEntity(card.getId());

// Utiliser :
favorite.setTargetCode(card.getCode());
```

**Avantages** :
- ✅ Plus robuste : le code ne change jamais
- ✅ Indépendant des IDs
- ✅ Plus lisible (code = "bar_chart" vs id = 1)

---

## 🚀 Script de Correction Immédiate

Si vous avez **déjà** le problème (favoris cassés), utilisez ce script :

```sql
-- 1. Réinitialiser la séquence
SELECT setval('cards_id_seq', 1, false);

-- 2. Supprimer les cards existantes
DELETE FROM cards;

-- 3. Réinsérer avec des IDs fixes
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
    (10, 'scheduled_payments_card', 'Paiements Planifiés', 'Statistiques sur les paiements planifiés');

-- 4. Réinitialiser la séquence après insertion
SELECT setval('cards_id_seq', (SELECT MAX(id) FROM cards));

-- 5. Vérifier les favoris cassés
SELECT f.id, f.user_id, f.type, f.target_entity, f.value
FROM favoris f
LEFT JOIN cards c ON c.id = f.target_entity
WHERE f.type = 'CARD' AND c.id IS NULL;
```

---

## 📝 Recommandations

1. **Pour le développement** : Utilisez `TRUNCATE RESTART IDENTITY`
2. **Pour la production** : Utilisez des **IDs fixes** dans les scripts de migration
3. **Pour le long terme** : Passez au **CODE** au lieu de l'ID pour les références

---

## 🔍 Vérification

Après correction, vérifiez que tout est cohérent :

```sql
-- Vérifier les IDs des cards
SELECT id, code, title FROM cards ORDER BY id;

-- Vérifier les favoris valides
SELECT f.id, f.user_id, f.type, f.target_entity, c.code, c.title
FROM favoris f
LEFT JOIN cards c ON c.id = f.target_entity
WHERE f.type = 'CARD'
ORDER BY f.user_id, f.target_entity;

-- Vérifier les favoris cassés (ne devrait rien retourner)
SELECT f.id, f.user_id, f.type, f.target_entity
FROM favoris f
LEFT JOIN cards c ON c.id = f.target_entity
WHERE f.type = 'CARD' AND c.id IS NULL;
```

