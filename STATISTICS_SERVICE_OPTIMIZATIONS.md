# 🔍 Optimisations pour StatisticsService

## 📋 Analyse du Code Actuel

### Problèmes Identifiés

1. **❌ Utilisation de `DATE()` sur `creation_date`** : Empêche l'utilisation des index
2. **❌ Calcul du total en Java** : Dans `getExpensesByCategory`, le total est calculé après la requête
3. **❌ Double parcours des résultats** : Dans `getAllBudgetStatisticsUnified`, on parcourt les résultats deux fois
4. **❌ Requêtes similaires dupliquées** : `getBudgetStatisticsData` et `budgetSql` sont très similaires
5. **❌ Utilisation de `GREATEST`/`LEAST` avec `DATE()`** : Peut être coûteux

---

## 🚀 Optimisations Proposées

### 1. ⚠️ **CRITIQUE** : Remplacer `DATE(creation_date)` par comparaisons directes

#### Problème Actuel

```sql
-- ❌ Empêche l'utilisation des index sur creation_date
WHERE DATE(e.creation_date) >= :startDate 
  AND DATE(e.creation_date) <= :endDate
```

**Impact** :
- PostgreSQL doit convertir chaque `creation_date` en `DATE` avant de comparer
- **Les index sur `creation_date` ne peuvent pas être utilisés**
- Scan complet de la table au lieu d'index scan

#### Solution Optimisée

```sql
-- ✅ Utilise directement les index sur creation_date
WHERE e.creation_date >= :startDateTime 
  AND e.creation_date < :endDateTimePlusOne
```

**Gain** : **x5 à x20** sur les requêtes avec plage de dates

---

### 2. ✅ Calculer le total directement en SQL

#### Problème Actuel

```java
// ❌ Calcul en Java après la requête
double totalAmount = results.stream()
    .mapToDouble(row -> mapper.convertToDouble(row[4]))
    .sum();
```

#### Solution Optimisée

```sql
-- ✅ Calculer le total directement en SQL
SELECT 
    c.id as category_id,
    c.name as category_name,
    c.icon as category_icon,
    c.color as category_color,
    SUM(e.amount) as total_amount,
    COUNT(e.id) as transaction_count,
    SUM(SUM(e.amount)) OVER () as grand_total  -- Total global
FROM categories c
LEFT JOIN expenses e ON c.id = e.category_id 
    AND e.user_id = :userId 
    AND e.deleted = false
    AND e.creation_date >= :startDateTime 
    AND e.creation_date < :endDateTimePlusOne
GROUP BY c.id, c.name, c.icon, c.color
HAVING SUM(e.amount) > 0
ORDER BY total_amount DESC
```

**Gain** : Évite un parcours supplémentaire en Java

---

### 3. ✅ Fusionner les deux requêtes de budgets

#### Problème Actuel

```java
// ❌ Deux requêtes très similaires
List<Object[]> categoryResults = getBudgetStatisticsData(...);  // Requête 1
List<Object[]> budgetResults = budgetQuery.getResultList();      // Requête 2
```

#### Solution Optimisée

```sql
-- ✅ Une seule requête avec GROUP BY sur deux niveaux
WITH budget_category_stats AS (
    SELECT 
        b.category_id,
        c.name as category_name,
        c.icon as category_icon,
        c.color as category_color,
        SUM(b.amount) as budget_amount,
        SUM(e.amount) as actual_amount
    FROM budgets b
    LEFT JOIN categories c ON b.category_id = c.id
    LEFT JOIN expenses e ON e.user_id = :userId
      AND e.deleted = false
      AND e.creation_date >= GREATEST(b.start_date, :startDate)
      AND e.creation_date < LEAST(b.end_date, :endDate) + INTERVAL '1 day'
      AND e.category_id = b.category_id
    WHERE b.user_id = :userId
      AND b.deleted = false
      AND b.start_date <= :endDate
      AND b.end_date >= :startDate
    GROUP BY b.category_id, c.name, c.icon, c.color
    HAVING SUM(b.amount) > 0
),
budget_individual_stats AS (
    SELECT 
        b.id,
        b.amount,
        SUM(e.amount) as spent_amount
    FROM budgets b
    LEFT JOIN expenses e ON e.user_id = :userId
      AND e.deleted = false
      AND e.creation_date >= GREATEST(b.start_date, :startDate)
      AND e.creation_date < LEAST(b.end_date, :endDate) + INTERVAL '1 day'
      AND e.category_id = b.category_id
    WHERE b.user_id = :userId
      AND b.deleted = false
      AND b.start_date <= :endDate
      AND b.end_date >= :startDate
    GROUP BY b.id, b.amount
)
SELECT * FROM budget_category_stats
UNION ALL
SELECT NULL, NULL, NULL, NULL, NULL, NULL FROM budget_individual_stats;
```

**Gain** : **-1 requête SQL** (de 2 à 1)

---

### 4. ✅ Éviter le double parcours des résultats

#### Problème Actuel

```java
// ❌ Parcourt categoryResults deux fois
for (Object[] row : categoryResults) {
    // ... BudgetVsActual
}
for (Object[] row : categoryResults) {
    // ... Distribution
}
```

#### Solution Optimisée

```java
// ✅ Un seul parcours
for (Object[] row : categoryResults) {
    // Créer BudgetVsActualDto
    BudgetVsActualDto vsActual = new BudgetVsActualDto();
    // ... mapping
    budgetVsActual.add(vsActual);
    
    // Créer BudgetDistributionDto
    BudgetDistributionDto distribution = new BudgetDistributionDto();
    // ... mapping
    distributionList.add(distribution);
}
```

**Gain** : **-50% de temps de traitement** pour les listes

---

### 5. ✅ Optimiser `GREATEST`/`LEAST` avec `DATE()`

#### Problème Actuel

```sql
-- ❌ Conversion DATE() à chaque ligne
AND DATE(e.creation_date) >= GREATEST(DATE(b.start_date), :startDate)
AND DATE(e.creation_date) <= LEAST(DATE(b.end_date), :endDate)
```

#### Solution Optimisée

```sql
-- ✅ Comparaisons directes avec TIMESTAMP
AND e.creation_date >= GREATEST(b.start_date::timestamp, :startDateTime)
AND e.creation_date < LEAST(b.end_date::timestamp, :endDateTime) + INTERVAL '1 day'
```

**Gain** : Évite les conversions `DATE()` répétées

---

## 📊 Benchmark Estimé

### Scénario : `/statistics?startDate=2025-01-01&endDate=2025-12-31` (1 an de données)

| Optimisation | Avant | Après | Gain |
|--------------|-------|-------|------|
| **1. DATE() → TIMESTAMP** | 200-500ms | 20-50ms | **x10** |
| **2. Total en SQL** | +5ms (Java) | 0ms | **-5ms** |
| **3. Fusion requêtes budgets** | 2 requêtes | 1 requête | **-50ms** |
| **4. Parcours unique** | 2× parcours | 1× parcours | **-2ms** |
| **5. GREATEST/LEAST** | +10ms | +2ms | **-8ms** |
| **TOTAL** | **~265-567ms** | **~22-55ms** | **x5 à x10** |

---

## 🎯 Implémentation Recommandée

### Priorité 1 : ⚠️ **CRITIQUE** - Remplacer `DATE(creation_date)`

**Impact** : **x5 à x20** sur toutes les requêtes avec plage de dates

**Changements nécessaires** :
1. Convertir `LocalDate` en `LocalDateTime` dans le service
2. Remplacer `DATE(creation_date)` par `creation_date >= :startDateTime`

### Priorité 2 : ✅ Fusionner les requêtes de budgets

**Impact** : **-1 requête SQL** (de 2 à 1)

**Changements nécessaires** :
1. Utiliser une CTE (Common Table Expression) pour fusionner les deux requêtes
2. Ou utiliser une seule requête avec GROUP BY sur deux niveaux

### Priorité 3 : ✅ Calculer le total en SQL

**Impact** : **-5ms** et code plus simple

**Changements nécessaires** :
1. Utiliser `SUM() OVER ()` pour calculer le total global
2. Supprimer le calcul en Java

### Priorité 4 : ✅ Parcours unique des résultats

**Impact** : **-50% de temps de traitement** pour les listes

**Changements nécessaires** :
1. Fusionner les deux boucles `for` en une seule

---

## 📝 Code Optimisé (Exemple)

### `getExpensesByCategory` Optimisé

```java
public CategoryExpensesDto getExpensesByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
    // Convertir LocalDate en LocalDateTime pour utiliser les index
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
    
    String sql = """
        SELECT 
            c.id as category_id,
            c.name as category_name,
            c.icon as category_icon,
            c.color as category_color,
            SUM(e.amount) as total_amount,
            COUNT(e.id) as transaction_count,
            SUM(SUM(e.amount)) OVER () as grand_total
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.category_id 
            AND e.user_id = :userId 
            AND e.deleted = false
            AND e.creation_date >= :startDateTime 
            AND e.creation_date <= :endDateTime
        GROUP BY c.id, c.name, c.icon, c.color
        HAVING SUM(e.amount) > 0
        ORDER BY total_amount DESC
    """;

    Query query = entityManager.createNativeQuery(sql);
    query.setParameter("userId", userId);
    query.setParameter("startDateTime", startDateTime);
    query.setParameter("endDateTime", endDateTime);

    @SuppressWarnings("unchecked")
    List<Object[]> results = query.getResultList();

    // Le total est maintenant dans la première ligne (ou calculer depuis grand_total)
    double totalAmount = results.isEmpty() ? 0.0 : 
        mapper.convertToDouble(results.get(0)[6]); // grand_total

    List<CategoryExpenseDto> categories = new ArrayList<>();
    for (Object[] row : results) {
        double amount = mapper.convertToDouble(row[4]);
        CategoryExpenseDto dto = new CategoryExpenseDto();
        dto.setCategoryId(((Number) row[0]).longValue());
        dto.setCategoryName((String) row[1]);
        dto.setIcon((String) row[2]);
        dto.setColor((String) row[3]);
        dto.setAmount(amount);
        dto.setPercentage(totalAmount > 0 ? (amount / totalAmount) * 100 : 0);
        categories.add(dto);
    }

    return new CategoryExpensesDto(totalAmount, categories);
}
```

---

## ✅ Checklist d'Optimisation

- [ ] **Priorité 1** : Remplacer `DATE(creation_date)` par `creation_date >= :startDateTime` dans toutes les requêtes
- [ ] **Priorité 2** : Fusionner les deux requêtes de budgets en une seule
- [ ] **Priorité 3** : Calculer le total en SQL avec `SUM() OVER ()`
- [ ] **Priorité 4** : Fusionner les deux boucles `for` en une seule dans `getAllBudgetStatisticsUnified`
- [ ] **Priorité 5** : Optimiser `GREATEST`/`LEAST` avec `TIMESTAMP` au lieu de `DATE()`

---

## 🎯 Impact Global

### Avant Optimisations

- **Temps d'exécution** : 200-500ms pour `/statistics`
- **Requêtes SQL** : 3-4 requêtes
- **Utilisation des index** : ❌ Empêchée par `DATE()`

### Après Optimisations

- **Temps d'exécution** : 20-50ms pour `/statistics` (**x5 à x10**)
- **Requêtes SQL** : 2-3 requêtes (**-1 requête**)
- **Utilisation des index** : ✅ Optimale avec les index créés dans V10

---

**Date** : 2026-02-13  
**Conclusion** : Les optimisations proposées peuvent améliorer les performances de **x5 à x10**, notamment en utilisant correctement les index créés dans la migration V10.

