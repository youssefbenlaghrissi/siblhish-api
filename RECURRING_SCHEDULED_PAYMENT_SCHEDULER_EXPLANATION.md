# 💳 Explication de `RecurringScheduledPaymentScheduler`

## 🎯 Objectif de la Classe

La classe `RecurringScheduledPaymentScheduler` est un **scheduler Spring** qui crée automatiquement les **prochains paiements planifiés récurrents** après qu'un paiement a été marqué comme payé ou que sa date d'échéance est passée. Elle s'exécute **tous les jours à 04:00**.

## 🔄 Concept de Paiement Planifié Récurrent

Un **paiement planifié récurrent** est un paiement qui se répète automatiquement selon une fréquence (quotidien, hebdomadaire, mensuel, annuel).

### Caractéristiques d'un Paiement Récurrent :
- ✅ `isRecurring = true`
- ✅ `recurrenceFrequency` = DAILY, WEEKLY, MONTHLY, ou YEARLY
- ✅ `isPaid = true` OU `dueDate < maintenant` (date d'échéance passée)
- ✅ `recurrenceEndDate` (optionnel) = date limite de récurrence
- ✅ Même `amount`, `name`, `category`, `user` pour chaque occurrence

## 📋 Structure de la Classe

```java
@Component                    // Bean Spring
@RequiredArgsConstructor      // Injection de dépendances via constructeur
public class RecurringScheduledPaymentScheduler {
    
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final NotificationService notificationService;
    
    @Scheduled(cron = "0 0 4 * * ?")  // Tous les jours à 04:00
    @Transactional
    public void createNextRecurringPayments() {
        createNextRecurringPaymentsInternal();
    }
}
```

## ⏰ Expression Cron

```java
@Scheduled(cron = "0 0 4 * * ?")
```

**Décomposition** :
- `0` = seconde (0)
- `0` = minute (0)
- `4` = heure (04:00)
- `*` = jour du mois (tous les jours)
- `*` = mois (tous les mois)
- `?` = jour de la semaine (ignoré)

**Résultat** : Exécution **tous les jours à 04:00**

## 🔍 Flux d'Exécution Détaillé

### Étape 1 : Récupération des Paiements à Traiter

```java
LocalDateTime now = LocalDateTime.now();
List<ScheduledPayment> recurringPayments = scheduledPaymentRepository
    .findRecurringPaymentsToProcess(now);
```

**Requête SQL générée** :
```sql
SELECT DISTINCT sp.* 
FROM scheduled_payments sp
LEFT JOIN categories c ON sp.category_id = c.id
LEFT JOIN users u ON sp.user_id = u.id
WHERE sp.is_recurring = true
  AND sp.recurrence_frequency IS NOT NULL
  AND sp.deleted = false
  AND (
      sp.is_paid = true
      OR (sp.due_date IS NOT NULL AND sp.due_date < :now)
  )
```

**Critères de sélection** :
- ✅ `isRecurring = true`
- ✅ `recurrenceFrequency IS NOT NULL`
- ✅ `deleted = false`
- ✅ **ET** (`isPaid = true` **OU** `dueDate < maintenant`)

**Exemple de résultats** :
| id | name | amount | due_date | is_paid | recurrence_frequency | user_id |
|----|------|--------|----------|---------|---------------------|---------|
| 10 | Loyer | 3000.0 | 2026-01-31 | true | MONTHLY | 1 |
| 11 | Abonnement | 200.0 | 2026-02-01 | false | MONTHLY | 1 |
| 12 | Facture | 500.0 | 2026-01-15 | true | WEEKLY | 2 |

### Étape 2 : Pour Chaque Paiement, Calculer la Date du Prochain Paiement

```java
LocalDateTime nextDueDate = calculateNextDueDate(
    payment.getDueDate(),
    payment.getRecurrenceFrequency()
);
```

**Logique de calcul** :
```java
private LocalDateTime calculateNextDueDate(LocalDateTime currentDueDate, RecurrenceFrequency frequency) {
    return switch (frequency) {
        case DAILY -> currentDueDate.plusDays(1);      // +1 jour
        case WEEKLY -> currentDueDate.plusWeeks(1);     // +1 semaine
        case MONTHLY -> currentDueDate.plusMonths(1);  // +1 mois
        case YEARLY -> currentDueDate.plusYears(1);    // +1 an
    };
}
```

**Exemples** :
- **DAILY** : `2026-02-01` → `2026-02-02`
- **WEEKLY** : `2026-02-01` → `2026-02-08`
- **MONTHLY** : `2026-02-01` → `2026-03-01`
- **YEARLY** : `2026-02-01` → `2027-02-01`

### Étape 3 : Vérifier la Date Limite (recurrenceEndDate)

```java
if (payment.getRecurrenceEndDate() != null 
        && nextDueDate.isAfter(payment.getRecurrenceEndDate())) {
    logger.debug("⏭️  Paiement récurrent {} a atteint sa date limite", payment.getId());
    continue; // Ignorer ce paiement
}
```

**Exemple** :
- `recurrenceEndDate` = `2026-12-31`
- `nextDueDate` = `2027-01-01`
- **Résultat** : Paiement ignoré (date limite dépassée)

### Étape 4 : Vérifier si le Prochain Paiement Existe Déjà

```java
if (nextPaymentExists(payment, nextDueDate)) {
    logger.debug("⏭️  Prochain paiement existe déjà");
    continue; // Ignorer ce paiement
}
```

**Requête SQL générée** :
```sql
SELECT COUNT(sp) > 0 
FROM scheduled_payments sp
WHERE sp.user_id = :userId
  AND sp.name = :name
  AND sp.amount = :amount
  AND sp.payment_method = :paymentMethod
  AND DATE(sp.due_date) = DATE(:dueDate)
  AND sp.is_paid = false
  AND sp.is_recurring = true
  AND sp.deleted = false
  AND sp.category_id = :categoryId
```

**Critères de vérification** :
- Même `user`, `name`, `amount`, `paymentMethod`
- Même `dueDate` (même jour)
- `isPaid = false` (pas encore payé)
- `isRecurring = true`
- Même `category`

### Étape 5 : Créer le Prochain Paiement

```java
ScheduledPayment nextPayment = createNextPayment(payment, nextDueDate);
scheduledPaymentRepository.save(nextPayment);
```

**Champs copiés du paiement précédent** :
- ✅ `name`, `amount`, `paymentMethod`, `beneficiary`
- ✅ `isRecurring = true`
- ✅ `recurrenceFrequency`, `recurrenceEndDate`
- ✅ `recurrenceDaysOfWeek`, `recurrenceDayOfMonth`, `recurrenceDayOfYear`
- ✅ `notificationOption`
- ✅ `user`, `category`

**Champs modifiés** :
- ✅ `dueDate` = `nextDueDate` (calculé selon la fréquence)
- ✅ `isPaid = false` (nouveau paiement non payé)
- ✅ `creationDate` = `LocalDateTime.now()`

## 📊 Exemples Concrets

### Exemple 1 : Paiement Mensuel "Loyer" (MONTHLY)

**Scénario** :
- **Paiement initial** (créé en janvier) :
  - `id` = 10
  - `name` = "Loyer"
  - `amount` = 3000.0 MAD
  - `dueDate` = 2026-01-31
  - `isRecurring` = true
  - `recurrenceFrequency` = MONTHLY
  - `recurrenceEndDate` = null (pas de limite)
  - `isPaid` = true (marqué comme payé le 31/01)

**Le 1er février 2026 à 04:00** :

1. **Récupération des paiements** :
   ```sql
   SELECT * FROM scheduled_payments 
   WHERE is_recurring = true 
     AND (is_paid = true OR due_date < '2026-02-01 04:00:00')
   ```
   - Trouve le paiement `id = 10` ✅

2. **Calcul de la prochaine date** :
   - `currentDueDate` = `2026-01-31`
   - `frequency` = MONTHLY
   - `nextDueDate` = `2026-01-31.plusMonths(1)` = `2026-02-28`

3. **Vérification de la date limite** :
   - `recurrenceEndDate` = null → Pas de limite ✅

4. **Vérification d'existence** :
   ```sql
   SELECT COUNT(*) > 0 FROM scheduled_payments 
   WHERE user_id = 1 
     AND name = 'Loyer'
     AND amount = 3000.0
     AND DATE(due_date) = '2026-02-28'
     AND is_paid = false
   ```
   - **Résultat** : Aucun paiement trouvé ✅

5. **Création du nouveau paiement** :
   - `id` = 15 (nouveau)
   - `name` = "Loyer"
   - `amount` = 3000.0 MAD
   - `dueDate` = 2026-02-28
   - `isRecurring` = true
   - `recurrenceFrequency` = MONTHLY
   - `isPaid` = false
   - `user_id` = 1
   - `category_id` = 3 (Logement)

6. **Notification créée** :
   - Titre : "Paiement planifié récurrent créé"
   - Description : "Un paiement planifié récurrent de 3000.00 MAD (Loyer) a été créé automatiquement avec une date d'échéance le 2026-02-28. (Logement)"

### Exemple 2 : Paiement Hebdomadaire "Facture" (WEEKLY)

**Scénario** :
- **Paiement initial** :
  - `id` = 12
  - `name` = "Facture Internet"
  - `amount` = 200.0 MAD
  - `dueDate` = 2026-02-01
  - `isRecurring` = true
  - `recurrenceFrequency` = WEEKLY
  - `recurrenceEndDate` = 2026-12-31
  - `isPaid` = false (pas encore payé, mais date passée)

**Le 2 février 2026 à 04:00** :

1. **Récupération des paiements** :
   - Trouve le paiement `id = 12` (car `dueDate < maintenant`) ✅

2. **Calcul de la prochaine date** :
   - `currentDueDate` = `2026-02-01`
   - `frequency` = WEEKLY
   - `nextDueDate` = `2026-02-01.plusWeeks(1)` = `2026-02-08`

3. **Vérification de la date limite** :
   - `recurrenceEndDate` = `2026-12-31`
   - `nextDueDate` = `2026-02-08` < `2026-12-31` ✅

4. **Vérification d'existence** :
   - Aucun paiement trouvé pour le 2026-02-08 ✅

5. **Création du nouveau paiement** :
   - `id` = 16 (nouveau)
   - `dueDate` = 2026-02-08
   - `isPaid` = false

### Exemple 3 : Paiement avec Date Limite Atteinte

**Scénario** :
- **Paiement** :
  - `id` = 13
  - `name` = "Abonnement temporaire"
  - `dueDate` = 2026-12-31
  - `recurrenceFrequency` = MONTHLY
  - `recurrenceEndDate` = 2026-12-31
  - `isPaid` = true

**Le 1er janvier 2027 à 04:00** :

1. **Récupération des paiements** :
   - Trouve le paiement `id = 13` ✅

2. **Calcul de la prochaine date** :
   - `nextDueDate` = `2026-12-31.plusMonths(1)` = `2027-01-31`

3. **Vérification de la date limite** :
   - `recurrenceEndDate` = `2026-12-31`
   - `nextDueDate` = `2027-01-31` > `2026-12-31` ❌
   - **Action** : Paiement ignoré (date limite dépassée)

### Exemple 4 : Paiement Déjà Existant (Pas de Duplication)

**Scénario** :
- **Paiement payé** :
  - `id` = 14
  - `name` = "Loyer"
  - `dueDate` = 2026-01-31
  - `isPaid` = true

- **Paiement déjà créé manuellement** :
  - `id` = 15
  - `name` = "Loyer"
  - `dueDate` = 2026-02-28
  - `isPaid` = false

**Le 1er février 2026 à 04:00** :

1. **Récupération des paiements** :
   - Trouve le paiement `id = 14` ✅

2. **Calcul de la prochaine date** :
   - `nextDueDate` = `2026-02-28`

3. **Vérification d'existence** :
   ```sql
   SELECT COUNT(*) > 0 FROM scheduled_payments 
   WHERE user_id = 1 
     AND name = 'Loyer'
     AND DATE(due_date) = '2026-02-28'
   ```
   - **Résultat** : Paiement trouvé (id = 15) ❌

4. **Action** : Aucun nouveau paiement créé (évite la duplication)

## 🔐 Gestion des Erreurs

### Try-Catch Global

```java
try {
    // Logique de création
} catch (Exception e) {
    logger.error("❌ Erreur lors de la création automatique...");
}
```

**Avantage** : Si une erreur survient pour un paiement, les autres paiements continuent d'être créés.

### Try-Catch par Paiement

```java
for (ScheduledPayment payment : recurringPayments) {
    try {
        // Traitement du paiement
    } catch (Exception e) {
        logger.error("❌ Erreur pour le paiement ID: {}", payment.getId(), e);
    }
}
```

**Avantage** : Une erreur sur un paiement n'empêche pas les autres d'être traités.

### Try-Catch pour les Notifications

```java
private void createRecurringScheduledPaymentNotification(...) {
    try {
        notificationService.createNotification(...);
    } catch (Exception e) {
        logger.error("❌ Erreur lors de la création de la notification...");
        // Ne pas bloquer la création du paiement si la notification échoue
    }
}
```

**Avantage** : Si l'envoi de notification échoue, le paiement est quand même créé.

## ⚙️ Annotations Importantes

### `@Component`
- Rend la classe un **bean Spring**
- Permet l'injection de dépendances (`@RequiredArgsConstructor`)

### `@Scheduled(cron = "0 0 4 * * ?")`
- Active l'exécution automatique tous les jours à 04:00
- Nécessite `@EnableScheduling` dans la classe principale

### `@Transactional`
- Garantit que toutes les opérations DB sont dans une transaction
- En cas d'erreur, rollback automatique

## 🔄 Différence avec `ScheduledPaymentService.createNextRecurringPayment()`

### `ScheduledPaymentService.createNextRecurringPayment()`
- **Déclenchement** : **Immédiat** après `markAsPaid()`
- **Avantage** : Création instantanée du prochain paiement
- **Cas d'usage** : Quand l'utilisateur marque un paiement comme payé

### `RecurringScheduledPaymentScheduler.createNextRecurringPaymentsInternal()`
- **Déclenchement** : **Tous les jours à 04:00**
- **Avantage** : Récupère les paiements oubliés (date passée mais pas payés)
- **Cas d'usage** : 
  - Paiements payés mais prochain paiement non créé (si `markAsPaid()` a échoué)
  - Paiements avec date d'échéance passée mais non payés

## 📈 Avantages de cette Approche

### ✅ Automatisation
- Pas besoin d'intervention manuelle
- Création systématique tous les jours

### ✅ Récupération des Oublis
- Récupère les paiements avec date passée
- Garantit qu'aucun paiement récurrent n'est oublié

### ✅ Évite les Doublons
- Vérification avant création
- Pas de paiements dupliqués pour la même date

### ✅ Respect des Limites
- Vérifie `recurrenceEndDate`
- Arrête automatiquement après la date limite

### ✅ Notification Utilisateur
- L'utilisateur est informé de la création automatique
- Meilleure transparence

### ✅ Robustesse
- Gestion d'erreurs à plusieurs niveaux
- Une erreur n'empêche pas les autres paiements d'être créés

## ⚠️ Points d'Attention

### 1. **Paiement Payé vs Date Passée**
- Le scheduler traite **2 cas** :
  - `isPaid = true` (paiement marqué comme payé)
  - `dueDate < maintenant` (date d'échéance passée, même si non payé)

### 2. **Date d'Exécution**
- Le scheduler s'exécute **tous les jours à 04:00**
- Si l'application est arrêtée à ce moment, les paiements ne seront pas créés
- **Solution** : Exécution manuelle possible via `createNextRecurringPaymentsInternal()`

### 3. **Performance**
- Si beaucoup de paiements récurrents (ex: 1000+), la boucle peut être lente
- **Optimisation** : Déjà optimisé avec `COUNT()` au lieu de `findAll()`

### 4. **Transactions**
- Tous les paiements sont créés dans la même transaction
- Si une erreur survient, rollback complet
- **Avantage** : Cohérence des données
- **Inconvénient** : Si un paiement échoue, aucun n'est créé

### 5. **Calcul de Date**
- Utilise `plusMonths()`, `plusWeeks()`, etc.
- Gère automatiquement les mois avec 28/29/30/31 jours
- **Exemple** : `2026-01-31.plusMonths(1)` = `2026-02-28` (pas `2026-03-03`)

## 🔧 Améliorations Possibles

### 1. **Batch Processing**
```java
// Au lieu de créer un par un
List<ScheduledPayment> paymentsToCreate = new ArrayList<>();
for (ScheduledPayment payment : recurringPayments) {
    if (!exists) {
        paymentsToCreate.add(nextPayment);
    }
}
scheduledPaymentRepository.saveAll(paymentsToCreate); // Insertion en batch
```

### 2. **Logging Détaillé**
```java
logger.info("📊 Paiements récurrents trouvés: {}", recurringPayments.size());
logger.info("✅ Paiements créés: {}", paymentsCreated);
logger.info("⏭️ Paiements ignorés (déjà existants): {}", paymentsSkipped);
logger.info("⏹️ Paiements ignorés (date limite): {}", paymentsExpired);
```

### 3. **Métriques**
- Compter le nombre de paiements créés
- Compter le nombre de paiements ignorés (doublons, date limite)
- Temps d'exécution

## 📝 Résumé

La classe `RecurringScheduledPaymentScheduler` :
1. ✅ S'exécute automatiquement **tous les jours à 04:00**
2. ✅ Récupère tous les **paiements récurrents payés ou avec date passée**
3. ✅ Pour chaque paiement, **calcule la date du prochain paiement** selon la fréquence
4. ✅ **Vérifie la date limite** (`recurrenceEndDate`)
5. ✅ **Vérifie si le prochain paiement existe déjà** (évite les doublons)
6. ✅ Si n'existe pas, **crée un nouveau paiement** avec la date calculée
7. ✅ **Envoie une notification** à l'utilisateur
8. ✅ **Gère les erreurs** sans bloquer les autres paiements

**Résultat** : Les utilisateurs ont automatiquement leurs prochains paiements récurrents créés, sans intervention manuelle ! 🎉

