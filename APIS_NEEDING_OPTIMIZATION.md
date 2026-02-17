# 🚨 APIs Nécessitant une Optimisation

## 📋 Résumé Exécutif

**Nombre d'APIs nécessitant une optimisation** : **7**

Ces APIs présentent des problèmes de performance identifiés :
- N+1 query problems
- Temps de réponse élevé (>50ms)
- Requêtes SQL multiples non optimisées
- Traitement synchrone lourd

---

## 🔴 Priorité HAUTE

### 1. `POST /expenses` - Création de dépense avec vérification budgets

**Endpoint** : `POST /expenses`  
**Service** : `ExpenseService.createExpense()`  
**Problème** : N+1 queries pour vérification budgets

#### Métriques Actuelles

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 3-5 + N budgets |
| **Temps estimé** | 50-200ms |
| **Scalabilité** | ❌ Dégradée avec nombre de budgets |

#### Problème Détailé

```java
// Dans ExpenseService.createExpense()
checkAndNotifyBudgetStatus(user.getId(), category.getId(), saved.getCreationDate(), saved.getAmount());

// Cette méthode fait :
// 1. findActiveBudgetsForExpense() - 1 requête
// 2. Pour chaque budget : calculateSpentForBudgetOptimized() - N requêtes
// 3. Pour chaque budget dépassé/atteint 90% : createNotification() - M requêtes
```

**Requêtes SQL** :
- `SELECT * FROM users WHERE id = ?` (1)
- `SELECT * FROM categories WHERE id = ?` (1)
- `INSERT INTO expenses ...` (1)
- `SELECT * FROM budgets WHERE ...` (1) - Trouver budgets actifs
- `SELECT SUM(amount) FROM expenses WHERE ...` (N) - **N+1 PROBLEM** : Une requête par budget
- `SELECT * FROM users WHERE id = ?` (M) - Pour notifications
- `INSERT INTO notifications ...` (M) - Pour notifications

**Impact** :
- Si utilisateur a 10 budgets actifs : **10 requêtes supplémentaires**
- Si utilisateur a 20 budgets actifs : **20 requêtes supplémentaires**
- Temps d'exécution : **50-200ms** (selon nombre de budgets)

#### Solution Proposée

**Optimisation** : Calculer `spent` pour tous les budgets en une seule requête

```sql
-- Calculer spent pour tous les budgets en une seule requête
SELECT 
    b.id as budget_id,
    COALESCE(SUM(e.amount), 0) as spent
FROM budgets b
LEFT JOIN expenses e ON e.user_id = b.user_id
    AND e.deleted = false
    AND e.creation_date BETWEEN b.start_date AND b.end_date
    AND (b.category_id IS NULL OR e.category_id = b.category_id)
WHERE b.user_id = :userId
    AND b.deleted = false
    AND b.start_date <= :expenseDate
    AND b.end_date >= :expenseDate
    AND (b.category_id IS NULL OR b.category_id = :categoryId)
GROUP BY b.id
```

**Gain estimé** :
- **Avant** : 3-5 + N requêtes (N = nombre de budgets)
- **Après** : 3-5 + 1 requête
- **Temps** : 50-200ms → **20-40ms**
- **Gain** : **x2.5 à x5**

---

### 2. `POST /scheduled-payments/reminders/send` - Envoi notifications de rappel

**Endpoint** : `POST /scheduled-payments/reminders/send`  
**Service** : `ScheduledPaymentReminderScheduler.sendPaymentRemindersInternal()`  
**Problème** : Envoi FCM synchrone + N requêtes

#### Métriques Actuelles

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 + N notifications |
| **Temps estimé** | 200-1000ms |
| **Scalabilité** | ❌ Dégradée avec nombre de notifications |

#### Problème Détailé

```java
// Pour chaque paiement à notifier :
// 1. createNotification() - 1 requête SQL
// 2. fcmNotificationService.sendNotification() - Appel HTTP synchrone
```

**Requêtes SQL** :
- `SELECT * FROM scheduled_payments WHERE ...` (1)
- `SELECT * FROM users WHERE id = ?` (N) - **N+1 PROBLEM**
- `INSERT INTO notifications ...` (N)
- Appels FCM API (N) - **SYNCHRONE** ❌

**Impact** :
- Si 50 paiements à notifier : **50 requêtes SQL + 50 appels FCM**
- Temps d'exécution : **200-1000ms** (selon latence FCM)
- Bloque le thread pendant l'envoi

#### Solution Proposée

**Optimisation 1** : Batch fetch des users
```sql
-- Récupérer tous les users en une seule requête
SELECT DISTINCT u.* FROM scheduled_payments sp
JOIN users u ON sp.user_id = u.id
WHERE sp.id IN (?, ?, ...)
```

**Optimisation 2** : Envoi FCM asynchrone
```java
@Async
public CompletableFuture<Boolean> sendNotificationAsync(User user, String title, String body, Map<String, String> data) {
    return CompletableFuture.supplyAsync(() -> {
        return fcmNotificationService.sendNotification(user, title, body, data);
    });
}
```

**Gain estimé** :
- **Avant** : 1 + N requêtes SQL + N appels FCM synchrone
- **Après** : 1 + 1 requête SQL + N appels FCM asynchrone
- **Temps** : 200-1000ms → **50-100ms** (sans attendre FCM)
- **Gain** : **x4 à x10**

---

### 3. `POST /scheduled-payments/recurring/generate` - Génération paiements récurrents

**Endpoint** : `POST /scheduled-payments/recurring/generate`  
**Service** : `RecurringScheduledPaymentScheduler.createNextRecurringPaymentsInternal()`  
**Problème** : Batch processing non optimisé

#### Métriques Actuelles

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 2 + N paiements générés |
| **Temps estimé** | 100-500ms |
| **Scalabilité** | ⚠️ Acceptable mais peut être amélioré |

#### Problème Détailé

**Requêtes SQL** :
- `SELECT * FROM scheduled_payments WHERE is_recurring = true AND ...` (1)
- `INSERT INTO scheduled_payments ...` (1, batch insert) ✅ Déjà optimisé
- `INSERT INTO notifications ...` (N) - Si notifications activées

**Impact** :
- Si 100 paiements à générer : **100 requêtes de notifications**
- Temps d'exécution : **100-500ms**

#### Solution Proposée

**Optimisation** : Batch insert des notifications
```java
// Au lieu de créer une notification par paiement
List<Notification> notifications = new ArrayList<>();
for (ScheduledPayment payment : payments) {
    Notification notif = new Notification(...);
    notifications.add(notif);
}
notificationRepository.saveAll(notifications); // Batch insert
```

**Gain estimé** :
- **Avant** : 2 + N requêtes (N = nombre de notifications)
- **Après** : 2 + 1 requête (batch insert)
- **Temps** : 100-500ms → **50-150ms**
- **Gain** : **x2 à x3**

---

## 🟡 Priorité MOYENNE

### 4. `POST /budgets/batch` - Création batch de budgets

**Endpoint** : `POST /budgets/batch`  
**Service** : `BudgetService.createBudgets()`  
**Problème** : N requêtes pour categories

#### Métriques Actuelles

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 2 + N budgets |
| **Temps estimé** | 30-100ms |
| **Scalabilité** | ⚠️ Acceptable mais peut être amélioré |

#### Problème Détailé

```java
// Pour chaque budget avec categoryId :
Category category = categoryRepository.findById(categoryId) // N requêtes
```

**Requêtes SQL** :
- `SELECT * FROM users WHERE id = ?` (1)
- `SELECT * FROM categories WHERE id = ?` (N) - **N+1 PROBLEM** : Une requête par categoryId unique
- `INSERT INTO budgets ...` (1, batch insert) ✅ Déjà optimisé

**Impact** :
- Si 10 budgets avec 5 categories différentes : **5 requêtes** (acceptable)
- Si 10 budgets avec 10 categories différentes : **10 requêtes** (peut être optimisé)

#### Solution Proposée

**Optimisation** : Batch fetch des categories
```java
// Récupérer toutes les categories uniques en une seule requête
Set<Long> uniqueCategoryIds = budgets.stream()
    .map(BudgetRequestDto::getCategoryId)
    .filter(Objects::nonNull)
    .collect(Collectors.toSet());

Map<Long, Category> categoriesMap = categoryRepository.findAllById(uniqueCategoryIds)
    .stream()
    .collect(Collectors.toMap(Category::getId, Function.identity()));
```

**Gain estimé** :
- **Avant** : 2 + N requêtes (N = nombre de categories uniques)
- **Après** : 2 + 1 requête
- **Temps** : 30-100ms → **20-50ms**
- **Gain** : **x1.5 à x2**

---

### 5. `GET /expenses/user/{userId}` - Liste des dépenses

**Endpoint** : `GET /expenses/user/{userId}`  
**Service** : `ExpenseService.getExpensesByUser()`  
**Problème** : Pas de pagination, peut retourner beaucoup de données

#### Métriques Actuelles

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 |
| **Temps estimé** | 10-50ms |
| **Scalabilité** | ⚠️ Dégradée avec volume élevé |

#### Problème Détailé

**Requêtes SQL** :
- `SELECT e.*, c.* FROM expenses e LEFT JOIN categories c ON e.category_id = c.id WHERE e.user_id = ? ORDER BY e.id DESC` (1)

**Impact** :
- Si utilisateur a 1000 dépenses : **10-50ms** ✅ Acceptable
- Si utilisateur a 10000 dépenses : **100-500ms** ❌ Problématique
- Si utilisateur a 100000 dépenses : **1-5s** ❌ Très problématique

#### Solution Proposée

**Optimisation** : Ajouter pagination optionnelle
```java
@GetMapping("/user/{userId}")
public ResponseEntity<ApiResponse<Page<ExpenseDto>>> getExpensesByUser(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
    Page<ExpenseDto> expenses = expenseService.getExpensesByUser(userId, PageRequest.of(page, size));
    return ResponseEntity.ok(ApiResponse.success(expenses));
}
```

**Gain estimé** :
- **Avant** : 1 requête (peut retourner 100k lignes)
- **Après** : 1 requête (retourne max 50 lignes par page)
- **Temps** : 100-500ms → **10-20ms** (pour grandes listes)
- **Gain** : **x5 à x25** (pour grandes listes)

---

### 6. `GET /incomes/user/{userId}` - Liste des revenus

**Endpoint** : `GET /incomes/user/{userId}`  
**Service** : `IncomeService.getIncomesByUser()`  
**Problème** : Pas de pagination, peut retourner beaucoup de données

#### Métriques Actuelles

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 |
| **Temps estimé** | 10-50ms |
| **Scalabilité** | ⚠️ Dégradée avec volume élevé |

#### Solution Proposée

**Même optimisation que pour `/expenses/user/{userId}`** : Ajouter pagination optionnelle

**Gain estimé** :
- **Temps** : 100-500ms → **10-20ms** (pour grandes listes)
- **Gain** : **x5 à x25** (pour grandes listes)

---

### 7. `GET /notifications/{userId}` - Liste des notifications

**Endpoint** : `GET /notifications/{userId}`  
**Service** : `NotificationService.getNotifications()`  
**Problème** : Pas de pagination, peut retourner beaucoup de données

#### Métriques Actuelles

| Métrique | Valeur |
|----------|--------|
| **Requêtes SQL** | 1 |
| **Temps estimé** | 10-50ms |
| **Scalabilité** | ⚠️ Dégradée avec volume élevé |

#### Solution Proposée

**Même optimisation que pour `/expenses/user/{userId}`** : Ajouter pagination optionnelle

**Gain estimé** :
- **Temps** : 100-500ms → **10-20ms** (pour grandes listes)
- **Gain** : **x5 à x25** (pour grandes listes)

---

## 📊 Résumé des Optimisations

| # | API | Problème | Priorité | Gain Estimé | Effort |
|---|-----|----------|----------|-------------|--------|
| 1 | `POST /expenses` | N+1 queries budgets | 🔴 HAUTE | **x2.5 à x5** | Moyen |
| 2 | `POST /scheduled-payments/reminders/send` | FCM synchrone + N+1 | 🔴 HAUTE | **x4 à x10** | Élevé |
| 3 | `POST /scheduled-payments/recurring/generate` | Batch notifications | 🔴 HAUTE | **x2 à x3** | Faible |
| 4 | `POST /budgets/batch` | N+1 categories | 🟡 MOYENNE | **x1.5 à x2** | Faible |
| 5 | `GET /expenses/user/{userId}` | Pas de pagination | 🟡 MOYENNE | **x5 à x25** | Moyen |
| 6 | `GET /incomes/user/{userId}` | Pas de pagination | 🟡 MOYENNE | **x5 à x25** | Moyen |
| 7 | `GET /notifications/{userId}` | Pas de pagination | 🟡 MOYENNE | **x5 à x25** | Moyen |

---

## 🎯 Plan d'Action Recommandé

### Phase 1 : Optimisations Critiques (Priorité HAUTE)

1. ✅ **`POST /expenses`** - Optimiser vérification budgets
   - **Effort** : 2-3 heures
   - **Impact** : Réduction de 50-200ms → 20-40ms
   - **Gain** : x2.5 à x5

2. ✅ **`POST /scheduled-payments/reminders/send`** - FCM asynchrone
   - **Effort** : 3-4 heures
   - **Impact** : Réduction de 200-1000ms → 50-100ms
   - **Gain** : x4 à x10

3. ✅ **`POST /scheduled-payments/recurring/generate`** - Batch notifications
   - **Effort** : 1-2 heures
   - **Impact** : Réduction de 100-500ms → 50-150ms
   - **Gain** : x2 à x3

### Phase 2 : Optimisations Amélioration (Priorité MOYENNE)

4. ✅ **`POST /budgets/batch`** - Batch fetch categories
   - **Effort** : 1 heure
   - **Impact** : Réduction de 30-100ms → 20-50ms
   - **Gain** : x1.5 à x2

5. ✅ **`GET /expenses/user/{userId}`** - Pagination
   - **Effort** : 2-3 heures
   - **Impact** : Réduction de 100-500ms → 10-20ms (pour grandes listes)
   - **Gain** : x5 à x25

6. ✅ **`GET /incomes/user/{userId}`** - Pagination
   - **Effort** : 2-3 heures
   - **Impact** : Réduction de 100-500ms → 10-20ms (pour grandes listes)
   - **Gain** : x5 à x25

7. ✅ **`GET /notifications/{userId}`** - Pagination
   - **Effort** : 2-3 heures
   - **Impact** : Réduction de 100-500ms → 10-20ms (pour grandes listes)
   - **Gain** : x5 à x25

---

## 📝 Notes Techniques

### Critères d'Optimisation

Une API nécessite une optimisation si :
- ✅ Temps de réponse > 50ms
- ✅ Nombre de requêtes SQL > 3
- ✅ Problème N+1 queries identifié
- ✅ Traitement synchrone lourd (FCM, etc.)
- ✅ Pas de pagination pour grandes listes

### APIs Déjà Optimisées

Ces APIs ont déjà été optimisées et ne nécessitent pas d'action :
- ✅ `GET /budgets/user/{userId}` - NamedQuery JPQL optimisée
- ✅ `POST /favorites/{userId}` - Batch query
- ✅ `DELETE /favorites/{userId}` - Batch query
- ✅ `GET /statistics/all-statistics/{userId}` - Index + optimisations SQL
- ✅ `GET /home/transactions/{userId}` - Index + sous-requêtes optimisées

---

**Date** : 2026-02-13  
**Total d'APIs nécessitant optimisation** : 7  
**Priorité HAUTE** : 3  
**Priorité MOYENNE** : 4

