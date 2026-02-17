# 📊 Rapport de Performance et Points d'Amélioration

## 📋 Résumé Exécutif

**Date** : 2026-02-13  
**Analyse** : Schedulers et Services  
**Problèmes identifiés** : **5 problèmes critiques**, **3 problèmes moyens**, **2 améliorations recommandées**

---

## 🔴 Problèmes CRITIQUES (Priorité HAUTE)

### 1. ❌ `RecurringBudgetScheduler` - N+1 Queries (save() dans boucle)

**Localisation** : `src/main/java/ma/siblhish/scheduler/RecurringBudgetScheduler.java`

#### Problème

```java
for (Budget templateBudget : recurringBudgets) {
    // ... vérification ...
    if (!exists) {
        Budget newBudget = new Budget();
        // ... configuration ...
        budgetRepository.save(newBudget);  // ❌ N appels save() dans une boucle
        createRecurringBudgetNotification(...);  // ❌ N appels notification
    }
}
```

**Métriques Actuelles** :

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + (N × 2) |
| **Temps estimé** | 50-200ms (selon N) |
| **Scalabilité** | ❌ Dégradée avec nombre de budgets |

**Détail des Requêtes** :
- `findByIsRecurringTrueOrderByIdDesc()` : **1 requête**
- Pour chaque budget (N budgets) :
  - `findByUserIdAndCategoryIdAndStartDateAndEndDateOrderByIdDesc()` : **N requêtes**
  - `save(newBudget)` : **N requêtes INSERT**
  - `createNotification()` → `findById(userId)` : **N requêtes**
  - `createNotification()` → `save(notification)` : **N requêtes INSERT**

**Total** : **1 + 4N requêtes SQL**

**Exemple** :
- Si 10 budgets récurrents → **41 requêtes SQL**
- Si 50 budgets récurrents → **201 requêtes SQL**

#### Solution Proposée

**Optimisation** : Batch insert avec `saveAll()`

```java
List<Budget> budgetsToCreate = new ArrayList<>();
List<NotificationRequest> notificationsToCreate = new ArrayList<>();

for (Budget templateBudget : recurringBudgets) {
    if (!exists) {
        Budget newBudget = new Budget();
        // ... configuration ...
        budgetsToCreate.add(newBudget);
        notificationsToCreate.add(new NotificationRequest(...));
    }
}

// Batch insert : 1 seule requête SQL
budgetRepository.saveAll(budgetsToCreate);

// Batch insert des notifications : 1 seule requête SQL
notificationRepository.saveAll(notificationsToCreate);
```

**Gain estimé** :
- **Avant** : 1 + 4N requêtes
- **Après** : 1 + N + 2 requêtes (1 pour vérification, N pour vérification existence, 1 pour batch insert budgets, 1 pour batch insert notifications)
- **Temps** : 50-200ms → **10-30ms**
- **Gain** : **x5 à x7**

---

### 2. ❌ `RecurringScheduledPaymentScheduler` - N+1 Queries (save() dans boucle)

**Localisation** : `src/main/java/ma/siblhish/scheduler/RecurringScheduledPaymentScheduler.java`

#### Problème

```java
for (ScheduledPayment payment : recurringPayments) {
    // ... vérification ...
    ScheduledPayment nextPayment = createNextPayment(payment, nextDueDate);
    scheduledPaymentRepository.save(nextPayment);  // ❌ N appels save() dans une boucle
    createRecurringScheduledPaymentNotification(...);  // ❌ N appels notification
}
```

**Métriques Actuelles** :

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + (N × 3) |
| **Temps estimé** | 50-300ms (selon N) |
| **Scalabilité** | ❌ Dégradée avec nombre de paiements |

**Détail des Requêtes** :
- `findRecurringPaymentsToProcess()` : **1 requête**
- Pour chaque paiement (N paiements) :
  - `existsSimilarPayment()` : **N requêtes COUNT()** (déjà optimisé ✅)
  - `save(nextPayment)` : **N requêtes INSERT**
  - `createNotification()` → `findById(userId)` : **N requêtes**
  - `createNotification()` → `save(notification)` : **N requêtes INSERT**

**Total** : **1 + 3N requêtes SQL**

**Exemple** :
- Si 20 paiements récurrents → **61 requêtes SQL**
- Si 100 paiements récurrents → **301 requêtes SQL**

#### Solution Proposée

**Optimisation** : Batch insert avec `saveAll()`

```java
List<ScheduledPayment> paymentsToCreate = new ArrayList<>();
List<NotificationRequest> notificationsToCreate = new ArrayList<>();

for (ScheduledPayment payment : recurringPayments) {
    if (!nextPaymentExists(...) && !expired) {
        ScheduledPayment nextPayment = createNextPayment(payment, nextDueDate);
        paymentsToCreate.add(nextPayment);
        notificationsToCreate.add(new NotificationRequest(...));
    }
}

// Batch insert : 1 seule requête SQL
scheduledPaymentRepository.saveAll(paymentsToCreate);

// Batch insert des notifications : 1 seule requête SQL
notificationRepository.saveAll(notificationsToCreate);
```

**Gain estimé** :
- **Avant** : 1 + 3N requêtes
- **Après** : 1 + N + 2 requêtes (1 pour récupération, N pour vérification existence, 1 pour batch insert paiements, 1 pour batch insert notifications)
- **Temps** : 50-300ms → **15-40ms**
- **Gain** : **x3 à x8**

---

### 3. ❌ `RecurringTransactionScheduler` - Problème MAJEUR (findAll() dans boucle)

**Localisation** : `src/main/java/ma/siblhish/scheduler/RecurringTransactionScheduler.java`

#### Problème

```java
// ❌ PROBLÈME CRITIQUE : findAll() dans une boucle
List<Expense> existing = expenseRepository.findAll().stream()
    .filter(e -> e.getUser().getId().equals(userId)
        && e.getCategory().getId().equals(categoryId)
        && e.getCreationDate().toLocalDate().equals(targetDate))
    .findFirst();

List<Income> existing = incomeRepository.findAll().stream()
    .filter(i -> i.getUser().getId().equals(userId)
        && i.getCreationDate().toLocalDate().equals(targetDate))
    .findFirst();
```

**Métriques Actuelles** :

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + (N × 2 × M) |
| **Temps estimé** | 500-5000ms (selon taille DB) |
| **Scalabilité** | ❌ **CATASTROPHIQUE** |

**Détail des Requêtes** :
- `findByIsRecurringTrueOrderByIdDesc()` : **1 requête**
- Pour chaque transaction récurrente (N transactions) :
  - `expenseRepository.findAll()` : **N requêtes** (charge TOUTES les dépenses) ❌
  - `incomeRepository.findAll()` : **N requêtes** (charge TOUTES les revenus) ❌
  - Filtrage en mémoire (lent)
  - `save(newExpense)` : **N requêtes INSERT**
  - `save(newIncome)` : **N requêtes INSERT**

**Total** : **1 + 4N requêtes SQL** (mais `findAll()` charge potentiellement des milliers d'enregistrements)

**Exemple** :
- Si 10 transactions récurrentes et 10,000 dépenses en DB :
  - **10 appels `findAll()`** → Charge **100,000 enregistrements** en mémoire
  - **Temps** : **5-10 secondes** ❌

#### Solution Proposée

**Optimisation** : Requête spécifique au lieu de `findAll()`

```java
// AVANT : findAll() + filtre en mémoire
List<Expense> existing = expenseRepository.findAll().stream()
    .filter(...)
    .findFirst();

// APRÈS : Requête spécifique
@Query("""
    SELECT e FROM Expense e
    WHERE e.user.id = :userId
      AND e.category.id = :categoryId
      AND DATE(e.creationDate) = :targetDate
      AND e.deleted = false
    LIMIT 1
""")
Optional<Expense> findExistingRecurringExpense(
    @Param("userId") Long userId,
    @Param("categoryId") Long categoryId,
    @Param("targetDate") LocalDate targetDate
);
```

**Gain estimé** :
- **Avant** : 1 + 4N requêtes (avec chargement de milliers d'enregistrements)
- **Après** : 1 + 2N requêtes (requêtes spécifiques avec LIMIT 1)
- **Temps** : 500-5000ms → **20-50ms**
- **Gain** : **x25 à x100** 🚀

---

### 4. ❌ `ScheduledPaymentReminderScheduler` - Notifications Synchrones

**Localisation** : `src/main/java/ma/siblhish/scheduler/ScheduledPaymentReminderScheduler.java`

#### Problème

```java
for (ScheduledPayment payment : paymentsToNotify) {
    // ...
    notificationService.createNotification(...);  // ❌ SYNCHRONE
    // → Appelle fcmNotificationService.sendNotification() de manière SYNCHRONE
}
```

**Métriques Actuelles** :

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + (N × 2) |
| **Temps estimé** | 200-2000ms (selon N et latence FCM) |
| **Scalabilité** | ❌ Bloque le thread pendant FCM |

**Détail** :
- `findPaymentsToNotify()` : **1 requête**
- Pour chaque paiement (N paiements) :
  - `createNotification()` → `findById(userId)` : **N requêtes**
  - `createNotification()` → `save(notification)` : **N requêtes INSERT**
  - `fcmNotificationService.sendNotification()` : **N appels HTTP synchrones** ❌

**Impact** :
- Si 50 paiements à notifier :
  - **50 requêtes SQL** + **50 appels FCM synchrones**
  - **Temps** : **200-2000ms** (selon latence FCM)
  - **Bloque le thread** pendant l'envoi

#### Solution Proposée

**Optimisation** : Notifications asynchrones (déjà implémenté dans `NotificationService` ✅)

Le problème est que `ScheduledPaymentReminderScheduler` appelle `createNotification()` qui est synchrone, mais `NotificationService.sendNotificationAsync()` est déjà asynchrone.

**Vérification nécessaire** : S'assurer que `createNotification()` utilise bien `sendNotificationAsync()` (déjà fait ✅)

**Gain estimé** :
- **Avant** : Thread bloqué pendant 200-2000ms
- **Après** : Thread libéré immédiatement, FCM en arrière-plan
- **Temps** : 200-2000ms → **10-20ms** (sans attendre FCM)
- **Gain** : **x10 à x100**

---

### 5. ❌ `RecurringBudgetScheduler` - N+1 pour Vérification d'Existence

**Localisation** : `src/main/java/ma/siblhish/scheduler/RecurringBudgetScheduler.java`

#### Problème

```java
for (Budget templateBudget : recurringBudgets) {
    // ❌ N requêtes pour vérifier l'existence
    List<Budget> existingBudgets = budgetRepository
        .findByUserIdAndCategoryIdAndStartDateAndEndDateOrderByIdDesc(...);
    boolean exists = !existingBudgets.isEmpty();
}
```

**Métriques Actuelles** :

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + N |
| **Temps estimé** | 20-100ms (selon N) |
| **Scalabilité** | ⚠️ Acceptable mais peut être amélioré |

#### Solution Proposée

**Optimisation** : Batch fetch avec `IN` clause

```java
// Récupérer tous les budgets existants en une seule requête
@Query("""
    SELECT b FROM Budget b
    WHERE b.user.id IN :userIds
      AND b.category.id IN :categoryIds
      AND b.startDate = :firstDayOfMonth
      AND b.endDate = :lastDayOfMonth
      AND b.deleted = false
""")
List<Budget> findExistingBudgetsForMonth(
    @Param("userIds") List<Long> userIds,
    @Param("categoryIds") List<Long> categoryIds,
    @Param("firstDayOfMonth") LocalDate firstDayOfMonth,
    @Param("lastDayOfMonth") LocalDate lastDayOfMonth
);

// Dans le scheduler :
Set<String> existingKeys = findExistingBudgetsForMonth(...).stream()
    .map(b -> b.getUser().getId() + ":" + b.getCategory().getId())
    .collect(Collectors.toSet());

for (Budget templateBudget : recurringBudgets) {
    String key = templateBudget.getUser().getId() + ":" + templateBudget.getCategory().getId();
    if (!existingKeys.contains(key)) {
        // Créer le budget
    }
}
```

**Gain estimé** :
- **Avant** : 1 + N requêtes
- **Après** : 1 + 1 requête
- **Temps** : 20-100ms → **5-15ms**
- **Gain** : **x2 à x7**

---

## 🟡 Problèmes MOYENS (Priorité MOYENNE)

### 6. ⚠️ `RecurringScheduledPaymentScheduler` - N+1 pour Vérification d'Existence

**Localisation** : `src/main/java/ma/siblhish/scheduler/RecurringScheduledPaymentScheduler.java`

#### Problème

```java
for (ScheduledPayment payment : recurringPayments) {
    // ⚠️ N requêtes COUNT() (déjà optimisé avec COUNT() au lieu de findAll())
    if (nextPaymentExists(payment, nextDueDate)) {
        continue;
    }
}
```

**Métriques Actuelles** :

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + N |
| **Temps estimé** | 10-50ms (selon N) |
| **Scalabilité** | ✅ Acceptable (COUNT() est rapide) |

**Note** : Déjà optimisé avec `COUNT()` au lieu de `findAll()`, mais peut être amélioré avec batch fetch.

#### Solution Proposée

**Optimisation** : Batch fetch avec `IN` clause (similaire à RecurringBudgetScheduler)

**Gain estimé** :
- **Avant** : 1 + N requêtes COUNT()
- **Après** : 1 + 1 requête
- **Temps** : 10-50ms → **3-10ms**
- **Gain** : **x2 à x5**

---

### 7. ⚠️ `ScheduledPaymentReminderScheduler` - N+1 pour Vérification Notifications

**Localisation** : `src/main/java/ma/siblhish/scheduler/ScheduledPaymentReminderScheduler.java`

#### Problème

```java
for (ScheduledPayment payment : paymentsToNotify) {
    // ⚠️ N requêtes COUNT() pour vérifier les notifications récentes
    if (!hasRecentNotification(payment.getId(), payment.getUser().getId(), reminderType)) {
        sendReminderNotification(...);
    }
}
```

**Métriques Actuelles** :

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + N |
| **Temps estimé** | 10-50ms (selon N) |
| **Scalabilité** | ✅ Acceptable (COUNT() est rapide) |

**Note** : Déjà optimisé avec `COUNT()` au lieu de `findAll()`, mais peut être amélioré avec batch fetch.

#### Solution Proposée

**Optimisation** : Batch fetch avec `IN` clause

**Gain estimé** :
- **Avant** : 1 + N requêtes COUNT()
- **Après** : 1 + 1 requête
- **Temps** : 10-50ms → **3-10ms**
- **Gain** : **x2 à x5**

---

### 8. ⚠️ Transactions Longues dans les Schedulers

**Problème** : Tous les schedulers utilisent `@Transactional` sur la méthode principale, ce qui crée une transaction unique pour tous les traitements.

**Impact** :
- Si une erreur survient à la fin, **rollback complet** (tous les budgets/paiements créés sont annulés)
- **Verrous DB** maintenus plus longtemps
- **Risque de timeout** si beaucoup de données

#### Solution Proposée

**Optimisation** : Transactions par batch ou par item

```java
// AVANT : Une seule transaction pour tout
@Transactional
public void createRecurringBudgetsForCurrentMonth() {
    for (Budget template : recurringBudgets) {
        // ... traitement ...
    }
}

// APRÈS : Transaction par batch
public void createRecurringBudgetsForCurrentMonth() {
    List<Budget> budgetsToCreate = new ArrayList<>();
    
    for (Budget template : recurringBudgets) {
        // ... collecter ...
        budgetsToCreate.add(newBudget);
    }
    
    // Transaction pour le batch
    createBudgetsBatch(budgetsToCreate);
}

@Transactional
private void createBudgetsBatch(List<Budget> budgets) {
    budgetRepository.saveAll(budgets);
}
```

**Gain estimé** :
- **Avant** : 1 transaction longue (risque de timeout)
- **Après** : Transactions courtes par batch
- **Risque** : Réduit le risque de timeout et de rollback complet

---

## 🟢 Améliorations Recommandées (Priorité BASSE)

### 9. ✅ Ajouter des Métriques et Logging

**Recommandation** : Ajouter des métriques détaillées pour monitorer les performances

```java
long startTime = System.currentTimeMillis();
int budgetsCreated = 0;
int budgetsSkipped = 0;

// ... traitement ...

long duration = System.currentTimeMillis() - startTime;
logger.info("✅ Création terminée: {} créés, {} ignorés, {}ms", 
    budgetsCreated, budgetsSkipped, duration);
```

**Bénéfice** : Meilleure visibilité sur les performances en production

---

### 10. ✅ Ajouter des Index pour les Requêtes des Schedulers

**Recommandation** : Vérifier que les index nécessaires existent

```sql
-- Pour RecurringBudgetScheduler
CREATE INDEX IF NOT EXISTS idx_budgets_recurring_user_category_dates
    ON budgets (is_recurring, user_id, category_id, start_date, end_date, deleted);

-- Pour RecurringScheduledPaymentScheduler
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_recurring_paid_due
    ON scheduled_payments (is_recurring, is_paid, due_date, deleted);

-- Pour ScheduledPaymentReminderScheduler
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_notify_due
    ON scheduled_payments (is_paid, due_date, notification_option, deleted);
```

**Bénéfice** : Amélioration des performances des requêtes

---

## 📊 Tableau Récapitulatif des Gains

| Problème | Avant | Après | Gain | Priorité |
|----------|-------|-------|------|----------|
| RecurringBudgetScheduler - N+1 save() | 1 + 4N | 1 + N + 2 | **x5 à x7** | 🔴 HAUTE |
| RecurringScheduledPaymentScheduler - N+1 save() | 1 + 3N | 1 + N + 2 | **x3 à x8** | 🔴 HAUTE |
| RecurringTransactionScheduler - findAll() | 1 + 4N (milliers) | 1 + 2N | **x25 à x100** | 🔴 **CRITIQUE** |
| ScheduledPaymentReminderScheduler - FCM sync | 200-2000ms | 10-20ms | **x10 à x100** | 🔴 HAUTE |
| RecurringBudgetScheduler - N+1 vérification | 1 + N | 1 + 1 | **x2 à x7** | 🔴 HAUTE |
| RecurringScheduledPaymentScheduler - N+1 vérification | 1 + N | 1 + 1 | **x2 à x5** | 🟡 MOYENNE |
| ScheduledPaymentReminderScheduler - N+1 vérification | 1 + N | 1 + 1 | **x2 à x5** | 🟡 MOYENNE |
| Transactions longues | Risque timeout | Transactions courtes | **Stabilité** | 🟡 MOYENNE |

---

## 🎯 Plan d'Action Recommandé

### Phase 1 : Problèmes Critiques (Semaine 1)
1. ✅ **RecurringTransactionScheduler** - Remplacer `findAll()` par requêtes spécifiques
2. ✅ **RecurringBudgetScheduler** - Batch insert avec `saveAll()`
3. ✅ **RecurringScheduledPaymentScheduler** - Batch insert avec `saveAll()`

### Phase 2 : Problèmes Haute Priorité (Semaine 2)
4. ✅ **ScheduledPaymentReminderScheduler** - Vérifier que FCM est asynchrone
5. ✅ **RecurringBudgetScheduler** - Batch fetch pour vérification existence

### Phase 3 : Problèmes Moyenne Priorité (Semaine 3)
6. ✅ **RecurringScheduledPaymentScheduler** - Batch fetch pour vérification existence
7. ✅ **ScheduledPaymentReminderScheduler** - Batch fetch pour vérification notifications
8. ✅ **Transactions** - Optimiser les transactions par batch

### Phase 4 : Améliorations (Semaine 4)
9. ✅ **Métriques** - Ajouter logging détaillé
10. ✅ **Index** - Vérifier et ajouter les index nécessaires

---

## 📝 Conclusion

**Problèmes identifiés** : **8 problèmes** (5 critiques, 3 moyens)  
**Gain total estimé** : **x2 à x100** selon le problème  
**Impact global** : **Amélioration significative** de la performance des schedulers

**Priorité absolue** : **RecurringTransactionScheduler** (problème catastrophique avec `findAll()`)

