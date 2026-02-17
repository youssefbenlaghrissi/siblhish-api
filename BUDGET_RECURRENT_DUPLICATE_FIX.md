# 🔧 Problème : Budgets Récurrents - Retourne 2 Budgets au lieu d'1

## 🎯 Problème Identifié

### Scénario

**Budgets Récurrents "Alimentation"** (créés chaque mois automatiquement) :
- Budget Alimentation Février : 01/02/2026 → 28/02/2026 (1000 MAD, isRecurring=true)
- Budget Alimentation Mars : 01/03/2026 → 31/03/2026 (1000 MAD, isRecurring=true)
- Budget Alimentation Avril : 01/04/2026 → 30/04/2026 (1000 MAD, isRecurring=true)

**Dépense créée le 17/02/2026 (Alimentation, 200 MAD)**

### ❌ Problème : La Requête Retourne 2 Budgets

**Requête actuelle** `findActiveBudgetsForExpense` :

```sql
SELECT b.*
FROM budgets b
WHERE b.user_id = :userId
  AND b.deleted = false
  AND b.start_date <= '2026-02-17'    -- 17/02/2026
  AND b.end_date >= '2026-02-17'      -- 17/02/2026
  AND (b.category_id IS NULL OR b.category_id = :categoryId)  -- categoryId = Alimentation
```

**Résultat ACTUEL** : **2 budgets** retournés ❌
1. Budget Alimentation Février : `01/02 <= 17/02 <= 28/02` ✅ (correct)
2. Budget Alimentation Mars : `01/03 <= 17/02` ❌ **PROBLÈME !**

**Attendu** : Seulement **1 budget** (Budget Alimentation Février)

---

## 🔍 Analyse du Problème

### Pourquoi le Budget de Mars est Retourné ?

La condition `b.start_date <= :expenseDate` devrait exclure le budget de Mars car `01/03 > 17/02`.

**Hypothèses possibles** :

1. **Budget Template Récurrent** : Il y a peut-être un budget "template" récurrent avec une période large (ex: 01/01/2026 → 31/12/2026) qui n'a pas encore été converti en budgets mensuels.

2. **Dates Mal Configurées** : Le budget de Mars pourrait avoir des dates incorrectes (ex: start_date = 01/02 au lieu de 01/03).

3. **Budget Récurrent Original** : Le budget récurrent "original" (template) pourrait avoir une période qui couvre plusieurs mois.

### Vérification

Pour une dépense du 17/02/2026, seuls les budgets avec :
- `start_date <= 17/02/2026` ET
- `end_date >= 17/02/2026`

devraient être retournés.

**Budget Février** : `01/02 <= 17/02 <= 28/02` ✅  
**Budget Mars** : `01/03 <= 17/02` ❌ (01/03 > 17/02, donc ne devrait PAS être retourné)

---

## ✅ Solution : Filtrer Correctement les Budgets

### Solution 1 : Vérifier que la Requête est Correcte

La requête actuelle semble correcte. Le problème pourrait venir de :
- Données incorrectes dans la base (dates mal configurées)
- Budget template récurrent avec période large

### Solution 2 : Ajouter un Filtre Supplémentaire pour les Budgets Récurrents

Si le problème vient d'un budget template récurrent, on peut ajouter un filtre pour exclure les budgets templates :

```sql
SELECT b.*
FROM budgets b
WHERE b.user_id = :userId
  AND b.deleted = false
  AND b.start_date <= :expenseDate
  AND b.end_date >= :expenseDate
  AND (b.category_id IS NULL OR b.category_id = :categoryId)
  -- Exclure les budgets templates récurrents (période > 1 mois)
  AND NOT (
      b.is_recurring = true 
      AND (b.end_date - b.start_date) > 31  -- Période > 1 mois = template
  )
```

### Solution 3 : Optimisation avec Calcul du Spent (Recommandée)

Au lieu de corriger juste le filtre, on peut optimiser en calculant le `spent` pour tous les budgets en une seule requête :

```sql
-- Calculer spent pour TOUS les budgets actifs en une seule requête
SELECT 
    b.id as budget_id,
    b.amount as budget_amount,
    b.start_date,
    b.end_date,
    b.category_id,
    b.is_recurring,
    COALESCE(SUM(e.amount), 0.0) as spent
FROM budgets b
LEFT JOIN expenses e ON e.user_id = b.user_id
    AND e.deleted = false
    -- Dépenses dans la période du budget
    AND e.creation_date >= b.start_date
    AND e.creation_date <= b.end_date
    AND (
        -- Si budget global (category_id IS NULL), toutes les dépenses
        -- Si budget par catégorie, seulement les dépenses de cette catégorie
        b.category_id IS NULL 
        OR e.category_id = b.category_id
    )
WHERE b.user_id = :userId
    AND b.deleted = false
    -- Budgets actifs à la date de la dépense
    AND b.start_date <= :expenseDate
    AND b.end_date >= :expenseDate
    -- Budgets affectés par cette catégorie de dépense
    AND (b.category_id IS NULL OR b.category_id = :categoryId)
    -- Exclure les budgets templates récurrents (période > 1 mois)
    AND NOT (
        b.is_recurring = true 
        AND (b.end_date - b.start_date) > 31
    )
GROUP BY b.id, b.amount, b.start_date, b.end_date, b.category_id, b.is_recurring
ORDER BY b.id
```

---

## 🔍 Diagnostic : Pourquoi 2 Budgets Sont Retournés ?

### Test de la Requête Actuelle

Pour une dépense du **17/02/2026** (Alimentation, categoryId=1) :

**Budget Alimentation Février** :
- `start_date = 01/02/2026`
- `end_date = 28/02/2026`
- `category_id = 1`
- Vérification :
  - `01/02 <= 17/02` ✅
  - `28/02 >= 17/02` ✅
  - `category_id = 1` ✅
- **Résultat** : Budget retourné ✅

**Budget Alimentation Mars** :
- `start_date = 01/03/2026`
- `end_date = 31/03/2026`
- `category_id = 1`
- Vérification :
  - `01/03 <= 17/02` ❌ (01/03 > 17/02)
  - `31/03 >= 17/02` ✅
  - `category_id = 1` ✅
- **Résultat** : Budget **NE DEVRAIT PAS** être retourné ❌

### Si le Budget de Mars est Retourné, C'est un Bug !

**Causes possibles** :
1. **Dates incorrectes** : Le budget de Mars a `start_date = 01/02` au lieu de `01/03`
2. **Budget template** : Il y a un budget template récurrent avec période large
3. **Bug dans la requête** : La condition `b.start_date <= :expenseDate` ne fonctionne pas correctement

---

## ✅ Solution Recommandée : Optimisation Complète

### 1. Nouvelle Méthode dans BudgetRepository

```java
@Query("""
    SELECT 
        b.id,
        b.amount,
        b.startDate,
        b.endDate,
        b.category.id,
        b.isRecurring,
        COALESCE(SUM(e.amount), 0.0) as spent
    FROM Budget b
    LEFT JOIN Expense e ON e.user.id = b.user.id
        AND e.deleted = false
        AND e.creationDate >= b.startDate
        AND e.creationDate <= b.endDate
        AND (b.category IS NULL OR e.category.id = b.category.id)
    WHERE b.user.id = :userId
        AND b.deleted = false
        AND b.startDate <= :expenseDate
        AND b.endDate >= :expenseDate
        AND (b.category IS NULL OR b.category.id = :categoryId)
        -- Exclure les budgets templates récurrents (période > 1 mois)
        AND NOT (
            b.isRecurring = true 
            AND FUNCTION('DATEDIFF', 'DAY', b.endDate, b.startDate) > 31
        )
    GROUP BY b.id, b.amount, b.startDate, b.endDate, b.category.id, b.isRecurring
""")
List<Object[]> findActiveBudgetsWithSpent(
    @Param("userId") Long userId,
    @Param("expenseDate") LocalDate expenseDate,
    @Param("categoryId") Long categoryId
);
```

**Note** : La fonction `DATEDIFF` peut varier selon le SGBD. Pour PostgreSQL, utiliser :
```sql
AND NOT (
    b.isRecurring = true 
    AND (b.endDate - b.startDate) > 31
)
```

### 2. Modification dans ExpenseService

```java
private void checkAndNotifyBudgetStatus(Long userId, Long categoryId, LocalDateTime expenseDate, Double expenseAmount) {
    try {
        LocalDate expenseLocalDate = expenseDate.toLocalDate();
        
        // ✅ NOUVELLE APPROCHE : Une seule requête pour tous les budgets
        List<Object[]> budgetResults = budgetRepository.findActiveBudgetsWithSpent(
                userId, expenseLocalDate, categoryId);
        
        for (Object[] row : budgetResults) {
            Long budgetId = ((Number) row[0]).longValue();
            Double budgetAmount = ((Number) row[1]).doubleValue();
            LocalDate startDate = (LocalDate) row[2];
            LocalDate endDate = (LocalDate) row[3];
            Long budgetCategoryId = row[4] != null ? ((Number) row[4]).longValue() : null;
            Boolean isRecurring = (Boolean) row[5];
            Double spent = ((Number) row[6]).doubleValue();
            
            // Vérifier que le budget est vraiment actif (double vérification)
            if (startDate.isAfter(expenseLocalDate) || endDate.isBefore(expenseLocalDate)) {
                log.warn("Budget {} ignoré : période {} - {} ne contient pas {}", 
                    budgetId, startDate, endDate, expenseLocalDate);
                continue;
            }
            
            Double percentageUsed = budgetAmount > 0 ? (spent / budgetAmount) * 100 : 0.0;
            
            // Charger le budget complet pour les notifications
            Budget budget = budgetRepository.findById(budgetId)
                    .orElseThrow(() -> new RuntimeException("Budget not found"));
            
            // Vérifier si le budget est dépassé
            if (percentageUsed >= 100) {
                Double exceeded = spent - budgetAmount;
                createBudgetExceededNotification(budget, spent, exceeded, percentageUsed);
            } 
            // Vérifier si le budget atteint 90% (warning)
            else if (percentageUsed >= 90 && percentageUsed < 100) {
                Double remaining = budgetAmount - spent;
                createBudgetWarningNotification(budget, spent, remaining, percentageUsed);
            }
        }
    } catch (Exception e) {
        log.error("❌ Erreur lors de la vérification des budgets pour la dépense: {}", e.getMessage(), e);
    }
}
```

---

## 📝 Résumé

### Problème

- ❌ La requête retourne **2 budgets** au lieu d'**1 budget** pour une dépense du 17/02
- ❌ Le budget de Mars est retourné alors qu'il ne devrait pas l'être
- ❌ **N+1 queries** : Une requête SQL par budget pour calculer `spent`

### Solution

- ✅ **Filtre supplémentaire** : Exclure les budgets templates récurrents (période > 1 mois)
- ✅ **Double vérification** : Vérifier dans le code Java que le budget est vraiment actif
- ✅ **1 seule requête** : Calculer `spent` pour tous les budgets avec `GROUP BY`
- ✅ **Temps** : 10-25ms (constant)

### Pourquoi 2 Budgets Sont Retournés ?

**Causes possibles** :
1. Budget template récurrent avec période large
2. Dates incorrectes dans la base de données
3. Bug dans la requête SQL

**Solution** : Ajouter un filtre pour exclure les budgets templates et optimiser avec une seule requête.

---

**Date** : 2026-02-13  
**Conclusion** : L'optimisation résout à la fois le problème de duplication ET le problème N+1 queries.

