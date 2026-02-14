# Optimisations de Performance - Clean Code

## 📊 Résumé des Optimisations

Ce document décrit les optimisations de performance effectuées pour améliorer les performances de l'application.

---

## 🔴 Problèmes Identifiés (AVANT)

### 1. **ExpenseService.checkAndNotifyBudgetStatus()**
- ❌ `budgetRepository.findAll()` → Charge TOUS les budgets de TOUS les utilisateurs en mémoire
- ❌ `expenseRepository.findAll()` dans `calculateSpentForBudget()` → Charge TOUTES les dépenses en mémoire
- ❌ Problème N+1 : Pour chaque budget, on fait un findAll() sur toutes les dépenses
- **Impact** : Très lent avec beaucoup d'utilisateurs et de données

### 2. **ScheduledPaymentReminderScheduler**
- ❌ `scheduledPaymentRepository.findAll()` → Charge TOUS les paiements planifiés
- ❌ `notificationRepository.findAll()` dans `hasRecentNotification()` → Charge TOUTES les notifications
- **Impact** : Scheduler très lent, surtout avec beaucoup de paiements

### 3. **RecurringScheduledPaymentScheduler**
- ❌ `scheduledPaymentRepository.findAll()` → Charge TOUS les paiements
- ❌ `nextPaymentExists()` fait aussi un findAll() pour chaque paiement
- **Impact** : Scheduler très lent, problème N+1

---

## ✅ Solutions Implémentées (APRÈS)

### 1. **BudgetRepository - Nouvelles Requêtes Optimisées**

#### `findActiveBudgetsForExpense()`
```java
// AVANT : budgetRepository.findAll().stream().filter(...)
// APRÈS : Requête spécifique avec JOIN FETCH
@Query("""
    SELECT b FROM Budget b 
    LEFT JOIN FETCH b.category 
    LEFT JOIN FETCH b.user
    WHERE b.user.id = :userId 
    AND b.deleted = false
    AND b.startDate <= :expenseDate 
    AND b.endDate >= :expenseDate
    AND (b.category IS NULL OR b.category.id = :categoryId)
""")
```
**Gain** : Filtrage en base de données au lieu de charger tous les budgets

#### `ExpenseRepository.calculateSpentForBudget()`
```java
// AVANT : expenseRepository.findAll().stream().filter(...).sum()
// APRÈS : SUM() directement en SQL
@Query("""
    SELECT COALESCE(SUM(e.amount), 0.0) 
    FROM Expense e 
    WHERE e.user.id = :userId 
    AND e.deleted = false
    AND e.creationDate >= :startDate 
    AND e.creationDate <= :endDate
    AND (:categoryId IS NULL OR e.category.id = :categoryId)
""")
```
**Gain** : Calcul direct en base de données, pas de chargement en mémoire

---

### 2. **ScheduledPaymentRepository - Nouvelles Requêtes Optimisées**

#### `findPaymentsToNotify()`
```java
// AVANT : scheduledPaymentRepository.findAll().stream().filter(...)
// APRÈS : Requête spécifique avec JOIN FETCH
@Query("""
    SELECT DISTINCT sp FROM ScheduledPayment sp
    LEFT JOIN FETCH sp.category
    LEFT JOIN FETCH sp.user
    WHERE (sp.isPaid = false OR sp.isPaid IS NULL)
    AND sp.dueDate IS NOT NULL
    AND sp.deleted = false
    AND (
        (sp.notificationOption IS NOT NULL AND sp.notificationOption != 'NONE')
        OR sp.dueDate < :today
    )
""")
```
**Gain** : Filtrage en base de données, chargement uniquement des paiements nécessaires

#### `findRecurringPaymentsToProcess()`
```java
// AVANT : scheduledPaymentRepository.findAll().stream().filter(...)
// APRÈS : Requête spécifique avec JOIN FETCH
@Query("""
    SELECT DISTINCT sp FROM ScheduledPayment sp
    LEFT JOIN FETCH sp.category
    LEFT JOIN FETCH sp.user
    WHERE sp.isRecurring = true
    AND sp.recurrenceFrequency IS NOT NULL
    AND sp.deleted = false
    AND (
        sp.isPaid = true
        OR (sp.dueDate IS NOT NULL AND sp.dueDate < :now)
    )
""")
```
**Gain** : Filtrage en base de données

#### `existsSimilarPayment()`
```java
// AVANT : scheduledPaymentRepository.findAll().stream().filter(...).anyMatch()
// APRÈS : COUNT() directement en SQL
@Query("""
    SELECT COUNT(sp) > 0 FROM ScheduledPayment sp
    WHERE sp.user.id = :userId
    AND sp.name = :name
    AND sp.amount = :amount
    AND sp.paymentMethod = :paymentMethod
    AND FUNCTION('DATE', sp.dueDate) = FUNCTION('DATE', :dueDate)
    AND sp.isPaid = false
    AND sp.isRecurring = true
    AND sp.deleted = false
    AND (:categoryId IS NULL AND sp.category IS NULL OR sp.category.id = :categoryId)
""")
```
**Gain** : Vérification directe en base de données, pas de chargement

---

### 3. **NotificationRepository - Nouvelle Requête Optimisée**

#### `hasRecentNotificationForPayment()`
```java
// AVANT : notificationRepository.findAll().stream().filter(...).count()
// APRÈS : COUNT() directement en SQL
@Query("""
    SELECT COUNT(n) > 0 FROM Notification n
    WHERE n.user.id = :userId
    AND n.type = :type
    AND n.creationDate > :since
    AND n.description LIKE CONCAT('%ID: ', :paymentIdStr, '%')
""")
```
**Gain** : Vérification directe en base de données, pas de chargement

---

## 📈 Gains de Performance Estimés

### Scénario : 1000 utilisateurs, 10 000 budgets, 50 000 dépenses, 5000 paiements planifiés

| Opération | AVANT | APRÈS | Gain |
|-----------|-------|-------|------|
| **Création d'une dépense** (vérification budgets) | ~5-10s | ~0.1-0.5s | **10-50x plus rapide** |
| **Scheduler rappels paiements** | ~10-30s | ~0.5-2s | **10-30x plus rapide** |
| **Scheduler paiements récurrents** | ~5-15s | ~0.3-1s | **10-20x plus rapide** |
| **Vérification doublons** | ~1-3s | ~0.01-0.05s | **50-100x plus rapide** |

---

## 🎯 Principes d'Optimisation Appliqués

### 1. **Éviter findAll() dans les boucles**
- ✅ Utiliser des requêtes spécifiques avec filtres en base de données
- ✅ Utiliser JOIN FETCH pour éviter les problèmes N+1

### 2. **Utiliser des agrégations SQL**
- ✅ `SUM()`, `COUNT()` directement en SQL au lieu de calculer en Java
- ✅ Réduire le transfert de données entre la base et l'application

### 3. **Filtrage en base de données**
- ✅ Appliquer les filtres dans la requête SQL/JPQL
- ✅ Éviter de charger des données inutiles en mémoire

### 4. **JOIN FETCH pour éviter N+1**
- ✅ Charger les relations nécessaires en une seule requête
- ✅ Réduire le nombre de requêtes à la base de données

---

## 🔍 Points d'Attention pour le Futur

### 1. **Index de Base de Données**
Assurez-vous d'avoir des index sur :
- `budgets(user_id, deleted, start_date, end_date)`
- `expenses(user_id, deleted, creation_date, category_id)`
- `scheduled_payments(user_id, deleted, is_paid, due_date, is_recurring)`
- `notifications(user_id, type, creation_date)`

### 2. **Cache**
Pour améliorer encore les performances :
- Cache des budgets actifs par utilisateur
- Cache des statistiques de dépenses par budget
- Cache des paiements planifiés à notifier

### 3. **Pagination**
Pour les grandes listes :
- Utiliser `Pageable` pour les requêtes de listes
- Limiter le nombre de résultats retournés

### 4. **Batch Processing**
Pour les schedulers :
- Traiter par lots (batch) au lieu de traiter un par un
- Utiliser `@Transactional` avec `readOnly = true` pour les lectures

---

## 📝 Checklist de Code Propre

- ✅ Pas de `findAll()` dans les boucles
- ✅ Requêtes spécifiques avec filtres en base de données
- ✅ Utilisation de `SUM()`, `COUNT()` en SQL
- ✅ JOIN FETCH pour éviter N+1
- ✅ Gestion d'erreurs non bloquante
- ✅ Logs appropriés pour le debugging
- ✅ Code commenté pour expliquer les optimisations

---

## 🚀 Prochaines Optimisations Possibles

1. **Cache Redis** : Pour les données fréquemment consultées
2. **Requêtes natives optimisées** : Pour les calculs complexes
3. **Batch inserts** : Pour les créations multiples
4. **Lazy loading optimisé** : Pour les relations optionnelles
5. **Connection pooling** : Configuration optimale du pool de connexions

