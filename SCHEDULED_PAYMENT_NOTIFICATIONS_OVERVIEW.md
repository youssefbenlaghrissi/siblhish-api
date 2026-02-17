# 📬 Vue d'Ensemble des Notifications pour Paiements Planifiés

## 📋 Résumé

Il y a **3 endroits** dans le code qui créent des notifications pour les paiements planifiés :

1. ✅ **`ScheduledPaymentReminderScheduler`** - Rappels avant échéance
2. ✅ **`RecurringScheduledPaymentScheduler`** - Notification de création automatique
3. ✅ **`ScheduledPaymentService`** - Notification de paiement confirmé

---

## 1. 📅 `ScheduledPaymentReminderScheduler` - Rappels d'Échéance

### 📍 Localisation
`src/main/java/ma/siblhish/scheduler/ScheduledPaymentReminderScheduler.java`

### ⏰ Fréquence d'Exécution
**Tous les jours à 08:00** (`@Scheduled(cron = "0 0 8 * * ?")`)

### 🎯 Objectif
Envoyer des **rappels** aux utilisateurs pour les paiements planifiés qui approchent ou sont en retard.

### 📊 Types de Notifications Créées

| Type de Notification | Quand | Titre | Description |
|---------------------|-------|-------|-------------|
| `PAYMENT_REMINDER` | 3 jours avant (si `THREE_DAYS_BEFORE`) | "Rappel de paiement planifié" | "📅 Rappel : Votre paiement planifié \"X\" d'un montant de Y MAD est dû dans 3 jours" |
| `PAYMENT_REMINDER` | 1 jour avant (si `ONE_DAY_BEFORE`) | "Rappel de paiement planifié" | "📅 Rappel : Votre paiement planifié \"X\" d'un montant de Y MAD est dû demain" |
| `PAYMENT_DUE_TODAY` | Le jour même (si `ON_DUE_DATE`) | "📅 Paiement dû aujourd'hui" | "⚠️ Votre paiement planifié \"X\" d'un montant de Y MAD est dû aujourd'hui" |
| `PAYMENT_OVERDUE` | En retard (date passée) | "⚠️ Paiement en retard" | "⚠️ Votre paiement planifié \"X\" d'un montant de Y MAD était dû il y a N jours" |

### 🔍 Logique de Déclenchement

```java
// Vérifie les paiements non payés avec notificationOption != NONE
List<ScheduledPayment> paymentsToNotify = scheduledPaymentRepository.findPaymentsToNotify(now);

for (ScheduledPayment payment : paymentsToNotify) {
    long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
    
    if (daysUntilDue < 0) {
        // Paiement en retard → PAYMENT_OVERDUE
    } else {
        switch (payment.getNotificationOption()) {
            case THREE_DAYS_BEFORE:
                if (daysUntilDue == 3) → PAYMENT_REMINDER
            case ONE_DAY_BEFORE:
                if (daysUntilDue == 1) → PAYMENT_REMINDER
            case ON_DUE_DATE:
                if (daysUntilDue == 0) → PAYMENT_DUE_TODAY
        }
    }
}
```

### ✅ Protection contre les Doublons
- Vérifie si une notification a déjà été envoyée dans les **24 dernières heures**
- Utilise `notificationRepository.hasRecentNotificationForPayment()`

### 📝 Exemple

**Paiement** :
- `name` = "Loyer"
- `amount` = 3000.0 MAD
- `dueDate` = 2026-02-05
- `notificationOption` = `ONE_DAY_BEFORE`

**Le 4 février 2026 à 08:00** :
- `daysUntilDue` = 1
- **Notification créée** : "Rappel de paiement planifié" - "📅 Rappel : Votre paiement planifié \"Loyer\" d'un montant de 3000.00 MAD est dû demain"

---

## 2. 🔄 `RecurringScheduledPaymentScheduler` - Création Automatique

### 📍 Localisation
`src/main/java/ma/siblhish/scheduler/RecurringScheduledPaymentScheduler.java`

### ⏰ Fréquence d'Exécution
**Tous les jours à 04:00** (`@Scheduled(cron = "0 0 4 * * ?")`)

### 🎯 Objectif
Notifier l'utilisateur lorsqu'un **nouveau paiement récurrent est créé automatiquement**.

### 📊 Type de Notification Créée

| Type de Notification | Quand | Titre | Description |
|---------------------|-------|-------|-------------|
| `RECURRING_SCHEDULED_PAYMENT` | Après création d'un nouveau paiement récurrent | "Paiement planifié récurrent créé" | "Un paiement planifié récurrent de X MAD (Y) a été créé automatiquement avec une date d'échéance le Z. (Catégorie)" |

### 🔍 Logique de Déclenchement

```java
// Récupère les paiements récurrents payés ou avec date passée
List<ScheduledPayment> recurringPayments = scheduledPaymentRepository
    .findRecurringPaymentsToProcess(now);

for (ScheduledPayment payment : recurringPayments) {
    // Calcule la date du prochain paiement
    LocalDateTime nextDueDate = calculateNextDueDate(...);
    
    // Vérifie si le prochain paiement existe déjà
    if (!nextPaymentExists(payment, nextDueDate)) {
        // Crée le nouveau paiement
        ScheduledPayment nextPayment = createNextPayment(payment, nextDueDate);
        scheduledPaymentRepository.save(nextPayment);
        
        // Crée la notification
        createRecurringScheduledPaymentNotification(...);
    }
}
```

### 📝 Exemple

**Paiement récurrent payé** :
- `name` = "Loyer"
- `amount` = 3000.0 MAD
- `dueDate` = 2026-01-31
- `isPaid` = true
- `recurrenceFrequency` = MONTHLY

**Le 1er février 2026 à 04:00** :
- Calcule `nextDueDate` = 2026-02-28
- Crée nouveau paiement avec `dueDate` = 2026-02-28
- **Notification créée** : "Paiement planifié récurrent créé" - "Un paiement planifié récurrent de 3000.00 MAD (Loyer) a été créé automatiquement avec une date d'échéance le 2026-02-28. (Logement)"

---

## 3. ✅ `ScheduledPaymentService` - Paiement Confirmé

### 📍 Localisation
`src/main/java/ma/siblhish/service/ScheduledPaymentService.java`

### ⏰ Fréquence d'Exécution
**Immédiat** (lors de l'appel à `markAsPaid()`)

### 🎯 Objectif
Notifier l'utilisateur lorsqu'un **paiement planifié est marqué comme payé**.

### 📊 Type de Notification Créée

| Type de Notification | Quand | Titre | Description |
|---------------------|-------|-------|-------------|
| `PAYMENT_MARKED_AS_PAID` | Après `markAsPaid()` | "Paiement confirmé" | "✅ Votre paiement planifié \"X\" d'un montant de Y MAD a été marqué comme payé le Z" |

### 🔍 Logique de Déclenchement

```java
@Transactional
public ScheduledPaymentDto markAsPaid(Long paymentId, String paymentDateStr) {
    // Marquer le paiement comme payé
    payment.setIsPaid(true);
    payment.setPaidDate(paymentDate);
    
    ScheduledPayment saved = scheduledPaymentRepository.save(payment);
    
    // Créer la notification (asynchrone)
    createPaymentMarkedAsPaidNotificationAsync(saved);
    
    return mapper.toScheduledPaymentDto(saved);
}
```

### ⚡ Asynchrone
- La notification est créée de manière **asynchrone** (`@Async`)
- Ne bloque pas la réponse HTTP

### 📝 Exemple

**Paiement** :
- `name` = "Loyer"
- `amount` = 3000.0 MAD
- `dueDate` = 2026-02-28

**Lors de `markAsPaid(paymentId, "2026-02-28T10:30:00")`** :
- `isPaid` = true
- `paidDate` = 2026-02-28 10:30:00
- **Notification créée** (asynchrone) : "Paiement confirmé" - "✅ Votre paiement planifié \"Loyer\" d'un montant de 3000.00 MAD a été marqué comme payé le 2026-02-28"

---

## 📊 Tableau Récapitulatif

| Scheduler/Service | Fréquence | Type de Notification | Déclencheur |
|------------------|-----------|---------------------|-------------|
| `ScheduledPaymentReminderScheduler` | Quotidien 08:00 | `PAYMENT_REMINDER`<br>`PAYMENT_DUE_TODAY`<br>`PAYMENT_OVERDUE` | Paiement non payé avec `notificationOption` activé |
| `RecurringScheduledPaymentScheduler` | Quotidien 04:00 | `RECURRING_SCHEDULED_PAYMENT` | Nouveau paiement récurrent créé |
| `ScheduledPaymentService` | Immédiat | `PAYMENT_MARKED_AS_PAID` | Paiement marqué comme payé |

---

## 🔄 Flux Complet d'un Paiement Récurrent

### Scénario : Paiement "Loyer" Mensuel

1. **Création initiale** (par l'utilisateur)
   - Paiement créé avec `dueDate` = 2026-01-31
   - `isRecurring` = true
   - `notificationOption` = `ONE_DAY_BEFORE`

2. **30 janvier 2026 à 08:00** - `ScheduledPaymentReminderScheduler`
   - `daysUntilDue` = 1
   - **Notification** : `PAYMENT_REMINDER` - "Rappel : Votre paiement planifié \"Loyer\" est dû demain"

3. **31 janvier 2026 à 08:00** - `ScheduledPaymentReminderScheduler`
   - `daysUntilDue` = 0
   - **Notification** : `PAYMENT_DUE_TODAY` - "⚠️ Votre paiement planifié \"Loyer\" est dû aujourd'hui"

4. **31 janvier 2026 à 10:30** - Utilisateur marque comme payé
   - `ScheduledPaymentService.markAsPaid()`
   - **Notification** : `PAYMENT_MARKED_AS_PAID` - "✅ Votre paiement planifié \"Loyer\" a été marqué comme payé"

5. **1er février 2026 à 04:00** - `RecurringScheduledPaymentScheduler`
   - Crée nouveau paiement avec `dueDate` = 2026-02-28
   - **Notification** : `RECURRING_SCHEDULED_PAYMENT` - "Un paiement planifié récurrent de 3000.00 MAD (Loyer) a été créé automatiquement"

6. **27 février 2026 à 08:00** - `ScheduledPaymentReminderScheduler`
   - `daysUntilDue` = 1
   - **Notification** : `PAYMENT_REMINDER` - "Rappel : Votre paiement planifié \"Loyer\" est dû demain"

... et ainsi de suite chaque mois.

---

## ⚠️ Points d'Attention

### 1. **Doublons de Notifications**
- ✅ `ScheduledPaymentReminderScheduler` vérifie les notifications récentes (24h)
- ✅ `RecurringScheduledPaymentScheduler` vérifie si le paiement existe déjà
- ⚠️ `ScheduledPaymentService` ne vérifie pas (mais est appelé une seule fois par paiement)

### 2. **Notifications Asynchrones**
- ✅ `ScheduledPaymentService.createPaymentMarkedAsPaidNotificationAsync()` est asynchrone
- ✅ `NotificationService.sendNotificationAsync()` (FCM) est asynchrone
- ⚠️ `ScheduledPaymentReminderScheduler` et `RecurringScheduledPaymentScheduler` sont synchrones

### 3. **Gestion d'Erreurs**
- ✅ Tous les schedulers/services ont des `try-catch` pour gérer les erreurs
- ✅ Une erreur de notification n'empêche pas le traitement principal

### 4. **Performance**
- ✅ `ScheduledPaymentReminderScheduler` utilise `findPaymentsToNotify()` (requête optimisée)
- ✅ `RecurringScheduledPaymentScheduler` utilise `findRecurringPaymentsToProcess()` (requête optimisée)
- ✅ Vérification des doublons avec `COUNT()` au lieu de `findAll()`

---

## 📝 Conclusion

Il y a **3 endroits** qui créent des notifications pour les paiements planifiés :

1. **`ScheduledPaymentReminderScheduler`** (08:00) - Rappels avant échéance
2. **`RecurringScheduledPaymentScheduler`** (04:00) - Création automatique
3. **`ScheduledPaymentService`** (immédiat) - Paiement confirmé

Chaque endroit a un **rôle spécifique** et crée des **types de notifications différents** pour informer l'utilisateur à différents moments du cycle de vie d'un paiement planifié.

