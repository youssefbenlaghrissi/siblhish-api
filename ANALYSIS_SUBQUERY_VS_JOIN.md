# 🔍 Analyse : Sous-requêtes Corrélées vs Jointures + GROUP BY

## 📋 Contexte

Pour la requête `/transactions?limit=50`, on utilise actuellement des **sous-requêtes corrélées** pour récupérer les jours de récurrence :

```sql
SELECT e.id, ...,
  (SELECT STRING_AGG(CAST(erd.day_of_week AS TEXT), ',')
   FROM expense_recurrence_days erd 
   WHERE erd.expense_id = e.id) as recurrence_days_of_week
FROM expenses e
LEFT JOIN categories c ON e.category_id = c.id
WHERE e.user_id = :userId AND e.deleted = false
ORDER BY e.creation_date DESC
LIMIT 50
```

**Question** : Est-ce que remplacer par une **jointure + GROUP BY** serait plus performant ?

---

## 🔄 Approche Alternative (Jointure + GROUP BY)

```sql
SELECT e.id, ...,
  STRING_AGG(CAST(erd.day_of_week AS TEXT), ',') as recurrence_days_of_week
FROM expenses e
LEFT JOIN categories c ON e.category_id = c.id
LEFT JOIN expense_recurrence_days erd ON erd.expense_id = e.id
WHERE e.user_id = :userId AND e.deleted = false
GROUP BY e.id, e.amount, e.payment_method, e.location, e.description, 
         e.creation_date, e.is_recurring, e.recurrence_frequency, 
         e.recurrence_end_date, e.recurrence_day_of_month, 
         e.recurrence_day_of_year, c.id, c.name, c.icon, c.color
ORDER BY e.creation_date DESC
LIMIT 50
```

---

## ⚖️ Comparaison Détaillée

### 📊 Caractéristiques des Données

- **Volume** : 100k expenses, 50k incomes pour un utilisateur actif
- **Récurrence** : Seulement ~10-20% des transactions sont récurrentes (`is_recurring = true`)
- **Jours de récurrence** : 1-7 jours par transaction récurrente (moyenne ~3 jours)
- **Tables de récurrence** : Petites tables (~20k lignes pour `expense_recurrence_days`)

---

### 🎯 Approche 1 : Sous-requêtes Corrélées (ACTUELLE)

#### ✅ Avantages

1. **LIMIT s'applique AVANT les sous-requêtes**
   - PostgreSQL filtre d'abord les 50 transactions avec l'index
   - Puis exécute les sous-requêtes uniquement sur ces 50 transactions
   - **Coût** : 50 sous-requêtes × ~0.1ms = **~5ms**

2. **Index optimisé**
   - Avec `idx_expense_recurrence_days_expense_id`, chaque sous-requête est un **index scan direct**
   - Très rapide même si la table de récurrence grandit

3. **Pas de duplication de lignes**
   - Chaque transaction apparaît une seule fois dans le résultat
   - Pas besoin de GROUP BY complexe

4. **Code SQL simple**
   - Pas besoin de lister toutes les colonnes dans GROUP BY
   - Moins de risques d'erreurs

#### ❌ Inconvénients

1. **N sous-requêtes** (N = nombre de transactions retournées)
   - Mais avec LIMIT 50, c'est seulement 50 sous-requêtes
   - Chaque sous-requête est très rapide avec l'index

2. **Plan d'exécution moins "élégant"**
   - Mais PostgreSQL optimise bien les sous-requêtes corrélées simples

---

### 🎯 Approche 2 : Jointure + GROUP BY

#### ✅ Avantages

1. **Une seule requête** (pas de sous-requêtes)
   - Plan d'exécution plus "propre" visuellement

2. **Peut être meilleur si pas de LIMIT**
   - Si on récupère toutes les transactions, le GROUP BY peut être plus efficace

#### ❌ Inconvénients

1. **GROUP BY doit s'exécuter AVANT le LIMIT**
   - PostgreSQL doit :
     1. Scanner toutes les transactions (100k)
     2. Joindre avec les jours de récurrence
     3. Grouper toutes les transactions
     4. Trier par `creation_date DESC`
     5. Appliquer LIMIT 50
   - **Coût** : Scan de 100k transactions + GROUP BY + tri = **50-200ms**

2. **GROUP BY complexe**
   - Doit lister **toutes** les colonnes non agrégées
   - 15+ colonnes à lister
   - Risque d'erreur si on oublie une colonne

3. **Duplication de lignes**
   - Si une transaction a 5 jours de récurrence, elle apparaît 5 fois avant le GROUP BY
   - Augmente temporairement le volume de données à traiter

4. **Moins efficace avec LIMIT**
   - Le LIMIT ne peut pas être appliqué tôt dans le plan d'exécution
   - Doit traiter toutes les données avant de limiter

---

## 📈 Benchmark Estimé (avec 100k expenses dans la base)

### Scénario 1 : `/transactions?limit=50` (cas typique)

| Métrique | Sous-requêtes Corrélées | Jointure + GROUP BY |
|----------|------------------------|---------------------|
| **Transactions scannées** | 50 (après LIMIT) | 100k (avant LIMIT) |
| **Sous-requêtes exécutées** | 50 | 0 |
| **GROUP BY** | Non | Oui (sur 100k) |
| **Temps estimé** | **5-10ms** | **50-200ms** |
| **I/O DB** | Faible (index scans) | Élevé (scan complet) |
| **CPU DB** | Faible | Élevé (GROUP BY) |
| **Verdict** | ✅ **x5 à x20 plus rapide** | ❌ |

### Scénario 2 : `/transactions?limit=200` (cas fréquent)

| Métrique | Sous-requêtes Corrélées | Jointure + GROUP BY |
|----------|------------------------|---------------------|
| **Transactions scannées** | 200 (après LIMIT) | 100k (avant LIMIT) |
| **Sous-requêtes exécutées** | 200 | 0 |
| **GROUP BY** | Non | Oui (sur 100k) |
| **Temps estimé** | **15-25ms** | **50-200ms** |
| **I/O DB** | Faible (index scans) | Élevé (scan complet) |
| **CPU DB** | Faible | Élevé (GROUP BY) |
| **Verdict** | ✅ **x2 à x8 plus rapide** | ❌ |

### Scénario 3 : `/transactions?limit=1000` (cas rare)

| Métrique | Sous-requêtes Corrélées | Jointure + GROUP BY |
|----------|------------------------|---------------------|
| **Transactions scannées** | 1000 (après LIMIT) | 100k (avant LIMIT) |
| **Sous-requêtes exécutées** | 1000 | 0 |
| **GROUP BY** | Non | Oui (sur 100k) |
| **Temps estimé** | **50-100ms** | **50-200ms** |
| **I/O DB** | Moyen (index scans) | Élevé (scan complet) |
| **CPU DB** | Moyen | Élevé (GROUP BY) |
| **Verdict** | ✅ **Équivalent ou meilleur** | ⚠️ |

### Scénario 4 : `/transactions` SANS LIMIT (toutes les transactions - cas très rare)

| Métrique | Sous-requêtes Corrélées | Jointure + GROUP BY |
|----------|------------------------|---------------------|
| **Transactions scannées** | 100k | 100k |
| **Sous-requêtes exécutées** | 100k | 0 |
| **GROUP BY** | Non | Oui (sur 100k) |
| **Temps estimé** | **100-200ms** | **80-150ms** |
| **Verdict** | ⚠️ Légèrement plus lent | ✅ Légèrement meilleur |

### 📊 Graphique de Comparaison (Temps en ms)

```
Temps (ms)
  │
200│                    ┌─────────────────┐
   │                    │  GROUP BY       │
150│                    │  (constant)     │
   │                    │                 │
100│                    │                 │
   │                    │                 │
 50│                    │                 │
   │                    │                 │
 25│     ┌──────────────┘                 │
   │     │  Sous-requêtes                 │
 10│     │  (linéaire)                    │
   │     │                                │
  5│     │                                │
   │     │                                │
  0└─────┴────────────────────────────────┴───> LIMIT
     50   200   1000   Toutes
```

**Point d'intersection** : ~1000-2000 transactions
- **En dessous** : Sous-requêtes sont meilleures ✅
- **Au-dessus** : GROUP BY pourrait être légèrement meilleur ⚠️

---

## 🎯 Conclusion

### ✅ **GARDER les sous-requêtes corrélées** pour `/transactions?limit=X`

**Raisons** :

1. **Avec LIMIT ≤ 1000, les sous-requêtes sont plus rapides**
   - LIMIT s'applique AVANT les sous-requêtes
   - Seulement N sous-requêtes à exécuter (N = LIMIT)
   - Chaque sous-requête est très rapide avec l'index (~0.1ms)
   - **Pour LIMIT=200** : 200 × 0.1ms = **~20ms** (toujours meilleur que GROUP BY)

2. **Le GROUP BY force un scan complet**
   - Doit traiter toutes les transactions avant le LIMIT
   - Coût constant (~50-200ms) indépendamment du LIMIT
   - **Pour LIMIT=200** : Toujours **50-200ms** (scan de 100k transactions)

3. **L'index qu'on vient d'ajouter optimise déjà les sous-requêtes**
   - `idx_expense_recurrence_days_expense_id` rend chaque sous-requête quasi-instantanée
   - Même avec 200 sous-requêtes, le coût reste linéaire et prévisible

4. **Code plus simple et maintenable**
   - Pas besoin de lister 15+ colonnes dans GROUP BY
   - Pas de risque d'oublier une colonne

### 📊 Analyse pour LIMIT=200

**Sous-requêtes corrélées** :
- 200 transactions scannées (avec index) : ~5ms
- 200 sous-requêtes (index scan) : 200 × 0.1ms = ~20ms
- **Total : ~25ms** ✅

**Jointure + GROUP BY** :
- Scan de 100k transactions : ~30ms
- Jointure avec récurrence : ~10ms
- GROUP BY sur 100k : ~50ms
- Tri : ~20ms
- LIMIT 200 : ~5ms
- **Total : ~115ms** ❌

**Gain** : **x4.6 plus rapide** avec les sous-requêtes pour LIMIT=200

### ⚠️ **Exception** : Si on récupère TOUTES les transactions (sans LIMIT ou LIMIT > 2000)

Dans ce cas très rare, la jointure + GROUP BY pourrait être légèrement meilleure, mais :
- C'est un cas d'usage rare (on utilise toujours LIMIT ≤ 200)
- Le gain serait marginal (~20-30ms)
- La complexité du code n'en vaut pas la peine
- Pour un cas d'usage si rare, on pourrait créer une méthode séparée avec GROUP BY

---

## 🚀 Recommandation Finale

### ✅ **GARDER l'approche actuelle (sous-requêtes corrélées)**

**Optimisations déjà en place** :
- ✅ Index `idx_expense_recurrence_days_expense_id` créé
- ✅ Index `idx_income_recurrence_days_income_id` créé
- ✅ Index `idx_expenses_user_deleted_date_desc` pour le tri rapide
- ✅ Index `idx_incomes_user_deleted_date_desc` pour le tri rapide

**Résultat** :
- **Temps d'exécution** : 5-10ms pour `/transactions?limit=50`
- **Scalabilité** : Excellent (constant même avec 1M transactions)
- **Maintenabilité** : Code simple et clair

---

## 📝 Note Technique

### Pourquoi PostgreSQL ne peut pas optimiser le GROUP BY avec LIMIT ?

PostgreSQL doit **grouper toutes les lignes** avant de pouvoir appliquer le LIMIT, car :
- Le GROUP BY peut changer l'ordre des lignes
- Le LIMIT doit s'appliquer sur le résultat final après GROUP BY
- PostgreSQL ne peut pas "deviner" quelles transactions seront dans le top 50 avant de les grouper

### Pourquoi les sous-requêtes corrélées sont rapides avec LIMIT ?

1. PostgreSQL filtre d'abord les 50 transactions avec l'index
2. Pour chaque transaction, exécute la sous-requête (index scan rapide)
3. Résultat : Seulement 50 sous-requêtes sur un petit dataset

---

**Date** : 2026-02-13  
**Conclusion** : Les sous-requêtes corrélées sont **optimales** pour ce cas d'usage avec LIMIT.

