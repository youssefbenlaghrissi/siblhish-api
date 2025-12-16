# Siblhish API - Application de Gestion de Budget

API backend pour une application mobile de gestion de budget moderne avec suivi des revenus, dépenses, budgets, objectifs d'épargne et notifications.

## 📋 Table des matières

- [Vue d'ensemble](#vue-densemble)
- [Architecture](#architecture)
- [Entités](#entités)
- [Enums](#enums)
- [Relations entre entités](#relations-entre-entités)
- [Différence entre Income et Budget](#différence-entre-income-et-budget)
- [Exemples d'utilisation](#exemples-dutilisation)
- [Technologies utilisées](#technologies-utilisées)

## 🎯 Vue d'ensemble

Cette application permet aux utilisateurs de :
- **Accueil** : Afficher le solde actuel, les transactions récentes, et ajouter rapidement des revenus ou des dépenses
- **Statistiques** : Visualiser la répartition des dépenses par catégorie et l'évolution mensuelle des revenus et dépenses
- **Objectifs** : Suivre les objectifs d'épargne avec des barres de progression visuelles
- **Profil** : Gérer les informations personnelles, le salaire mensuel, les catégories de dépenses personnalisées, et les paramètres

## 🏗️ Architecture

```
📦 ma.siblhish
├── 📁 entities/
│   ├── AbstractEntity          # Classe abstraite de base
│   ├── User                    # Utilisateur
│   ├── Category                # Catégories de dépenses
│   ├── Expense                 # Dépenses réelles
│   ├── Income                  # Revenus réels
│   ├── Budget                  # Limites de dépenses
│   ├── Goal                    # Objectifs d'épargne
│   └── Notification            # Notifications
└── 📁 enums/
    ├── UserType                # Types d'utilisateurs
    ├── PaymentMethod           # Méthodes de paiement
    ├── PeriodFrequency         # Fréquences de période
    ├── RecurrenceFrequency     # Fréquences de récurrence
    └── TypeNotification        # Types de notifications
```

## 📦 Entités

### AbstractEntity

Classe abstraite de base pour toutes les entités, fournissant :
- `id` (Long) : Identifiant unique généré automatiquement
- `creationDate` (LocalDateTime) : Date de création (gérée automatiquement)
- `updateDate` (LocalDateTime) : Date de dernière modification (gérée automatiquement)

### User

Représente un utilisateur de l'application.

**Champs :**
- `firstName` (String, requis) : Prénom
- `lastName` (String, requis) : Nom
- `email` (String, requis, unique) : Email de l'utilisateur
- `password` (String, requis) : Mot de passe
- `type` (UserType, requis) : Type d'utilisateur (EMPLOYEE, FREELANCER, etc.)
- `language` (String) : Langue préférée (défaut: "fr")
- `monthlySalary` (Double) : Salaire mensuel

**Relations :**
- `categories` (ManyToMany) : Catégories personnalisées de l'utilisateur
- `expenses` (OneToMany) : Dépenses de l'utilisateur
- `incomes` (OneToMany) : Revenus de l'utilisateur
- `budgets` (OneToMany) : Budgets de l'utilisateur
- `goals` (OneToMany) : Objectifs de l'utilisateur
- `notifications` (OneToMany) : Notifications de l'utilisateur

### Category

Représente une catégorie de dépenses.

**Champs :**
- `name` (String, requis) : Nom de la catégorie
- `icon` (String) : Icône de la catégorie
- `color` (String) : Couleur de la catégorie

**Relations :**
- `expenses` (OneToMany) : Dépenses associées à cette catégorie

### Expense

Représente une transaction réelle de sortie d'argent (dépense).

**Champs :**
- `amount` (Double, requis, positif) : Montant de la dépense
- `method` (PaymentMethod, requis) : Méthode de paiement
- `date` (LocalDateTime, requis) : Date et heure de la dépense
- `description` (String) : Description de la dépense
- `location` (String) : Lieu de la dépense
- `isRecurring` (Boolean) : Indique si la dépense est récurrente (défaut: false)
- `recurrenceFrequency` (RecurrenceFrequency) : Fréquence de récurrence si applicable

**Relations :**
- `user` (ManyToOne, requis) : Utilisateur propriétaire
- `category` (ManyToOne, requis) : Catégorie de la dépense

### Income

Représente une transaction réelle d'entrée d'argent (revenu).

**Champs :**
- `amount` (Double, requis, positif) : Montant du revenu
- `method` (PaymentMethod, requis) : Méthode de réception
- `date` (LocalDateTime, requis) : Date et heure du revenu
- `description` (String) : Description du revenu
- `source` (String) : Source du revenu (ex: "Salaire", "Freelance", "Vente")
- `isRecurring` (Boolean) : Indique si le revenu est récurrent (défaut: false)
- `recurrenceFrequency` (RecurrenceFrequency) : Fréquence de récurrence si applicable

**Relations :**
- `user` (ManyToOne, requis) : Utilisateur propriétaire

**Note :** Contrairement à `Budget`, `Income` représente une transaction réelle avec une date précise.

### Budget

Représente une limite de dépenses prévue pour une période donnée (règle/plafond).

**Champs :**
- `amount` (Double, requis, positif) : Montant maximum autorisé pour la période
- `period` (PeriodFrequency, requis) : Fréquence de la période (DAILY, WEEKLY, MONTHLY, YEARLY)
- `startDate` (LocalDate) : Date de début du budget (optionnel)
- `endDate` (LocalDate) : Date de fin du budget (optionnel)
- `isActive` (Boolean) : Indique si le budget est actif (défaut: true)

**Relations :**
- `user` (ManyToOne, requis) : Utilisateur propriétaire
- `category` (ManyToOne, optionnel) : Catégorie associée. Si null, c'est un budget global

**Exemples :**
- Budget mensuel global : 5000 MAD
- Budget mensuel pour "Alimentation" : 2000 MAD
- Budget hebdomadaire pour "Loisirs" : 500 MAD

**Note :** Contrairement à `Income`, `Budget` représente une règle/plafond de dépenses, pas une transaction réelle.

### Goal

Représente un objectif d'épargne.

**Champs :**
- `name` (String, requis) : Nom de l'objectif
- `description` (String) : Description de l'objectif
- `targetAmount` (Double, requis, positif) : Montant cible à atteindre
- `currentAmount` (Double) : Montant actuellement épargné (défaut: 0.0)
- `targetDate` (LocalDate) : Date cible pour atteindre l'objectif
- `isAchieved` (Boolean) : Indique si l'objectif est atteint (défaut: false)

**Relations :**
- `user` (ManyToOne, requis) : Utilisateur propriétaire
- `category` (ManyToOne, optionnel) : Catégorie associée (si objectif lié à une catégorie spécifique)

### Notification

Représente une notification pour l'utilisateur.

**Champs :**
- `title` (String, requis) : Titre de la notification
- `description` (String) : Description de la notification
- `isRead` (Boolean) : Indique si la notification est lue (défaut: false)
- `type` (TypeNotification, requis) : Type de notification

**Relations :**
- `user` (ManyToOne, requis) : Utilisateur destinataire

## 🔢 Enums

### UserType

Types d'utilisateurs supportés :
- `EMPLOYEE` : Employé
- `FREELANCER` : Indépendant sans entreprise
- `ENTREPRENEUR` : Créateur d'entreprise
- `MERCHANT` : Commerçant / Marchand
- `ARTISAN` : Artisan / Activité manuelle locale
- `SELF_EMPLOYED` : Travailleur indépendant
- `STUDENT` : Étudiant
- `UNEMPLOYED` : Sans emploi
- `RETIRED` : Retraité
- `OTHER` : Autre

### PaymentMethod

Méthodes de paiement supportées :
- `CASH` : Paiement en espèces
- `CREDIT_CARD` : Carte bancaire
- `BANK_TRANSFER` : Virement bancaire
- `MOBILE_PAYMENT` : Paiement mobile
- `PAYPAL` : Paiement via PayPal
- `CRYPTOCURRENCY` : Paiement en cryptomonnaie
- `CHECK` : Paiement par chèque
- `DIRECT_DEBIT` : Prélèvement automatique

### PeriodFrequency

Fréquences de période pour les budgets :
- `DAILY` : Quotidien
- `WEEKLY` : Hebdomadaire
- `MONTHLY` : Mensuel
- `YEARLY` : Annuel

### RecurrenceFrequency

Fréquences de récurrence pour les transactions :
- `DAILY` : Quotidien
- `WEEKLY` : Hebdomadaire
- `MONTHLY` : Mensuel
- `YEARLY` : Annuel

### TypeNotification

Types de notifications :
- `DAILY_REPORT` : Rapport quotidien
- `MONTHLY_REPORT` : Rapport mensuel

## 🔗 Relations entre entités

```
User (1) ──→ (N) Income
User (1) ──→ (N) Expense
User (1) ──→ (N) Budget
User (1) ──→ (N) Goal
User (1) ──→ (N) Notification
User (N) ──→ (N) Category (ManyToMany)

Category (1) ──→ (N) Expense
Category (0..1) ──→ (N) Budget (optionnel, null = global)
Category (0..1) ──→ (N) Goal (optionnel)

Expense (N) ──→ (1) User
Expense (N) ──→ (1) Category

Income (N) ──→ (1) User

Budget (N) ──→ (1) User
Budget (N) ──→ (0..1) Category

Goal (N) ──→ (1) User
Goal (N) ──→ (0..1) Category

Notification (N) ──→ (1) User
```

## ⚖️ Différence entre Income et Budget

Ces deux entités sont **complémentaires** et ont des rôles différents :

| Aspect | Income | Budget |
|--------|--------|--------|
| **Type** | Transaction réelle | Règle/limite |
| **Nature** | Historique (argent reçu) | Plafond de dépenses |
| **Date** | Date précise (`LocalDateTime`) | Période (`PeriodFrequency`) |
| **Usage** | Enregistrer les revenus reçus | Définir les limites de dépenses |
| **Exemple** | "Salaire reçu le 1er janvier : 8000 MAD" | "Budget mensuel max : 5000 MAD" |

### Exemple concret :

```java
// 1. Revenu réel (Income) - Transaction historique
Income salaire = new Income();
salaire.setAmount(8000.0);
salaire.setDate(LocalDateTime.of(2024, 1, 1, 0, 0));
salaire.setSource("Salaire");
salaire.setMethod(PaymentMethod.BANK_TRANSFER);

// 2. Budget limite (Budget) - Règle de dépenses
Budget budgetMensuel = new Budget();
budgetMensuel.setAmount(5000.0);
budgetMensuel.setPeriod(PeriodFrequency.MONTHLY);
budgetMensuel.setCategory(null); // Budget global
budgetMensuel.setIsActive(true);

// 3. Dépense réelle (Expense) - Transaction historique
Expense achat = new Expense();
achat.setAmount(200.0);
achat.setDate(LocalDateTime.now());
achat.setCategory(categoryAlimentation);
achat.setMethod(PaymentMethod.CREDIT_CARD);
```

## 💡 Exemples d'utilisation

### Créer un utilisateur avec catégories

```java
User user = new User();
user.setFirstName("Ahmed");
user.setLastName("Benali");
user.setEmail("ahmed@example.com");
user.setPassword("hashedPassword");
user.setType(UserType.EMPLOYEE);
user.setMonthlySalary(8000.0);

Category alimentation = new Category();
alimentation.setName("Alimentation");
alimentation.setIcon("🍔");
alimentation.setColor("#FF5733");

user.getCategories().add(alimentation);
```

### Enregistrer un revenu récurrent

```java
Income salaire = new Income();
salaire.setAmount(8000.0);
salaire.setMethod(PaymentMethod.BANK_TRANSFER);
salaire.setDate(LocalDateTime.now());
salaire.setSource("Salaire");
salaire.setIsRecurring(true);
salaire.setRecurrenceFrequency(RecurrenceFrequency.MONTHLY);
salaire.setUser(user);
```

### Créer un budget par catégorie

```java
Budget budgetAlimentation = new Budget();
budgetAlimentation.setAmount(2000.0);
budgetAlimentation.setPeriod(PeriodFrequency.MONTHLY);
budgetAlimentation.setCategory(alimentation);
budgetAlimentation.setUser(user);
budgetAlimentation.setIsActive(true);
```

### Définir un objectif d'épargne

```java
Goal objectifVacances = new Goal();
objectifVacances.setName("Vacances d'été");
objectifVacances.setDescription("Épargner pour les vacances");
objectifVacances.setTargetAmount(10000.0);
objectifVacances.setCurrentAmount(2500.0);
objectifVacances.setTargetDate(LocalDate.of(2024, 7, 1));
objectifVacances.setUser(user);
objectifVacances.setIsAchieved(false);
```

### Enregistrer une dépense

```java
Expense depense = new Expense();
depense.setAmount(150.0);
depense.setMethod(PaymentMethod.CREDIT_CARD);
depense.setDate(LocalDateTime.now());
depense.setDescription("Courses au supermarché");
depense.setLocation("Carrefour");
depense.setCategory(alimentation);
depense.setUser(user);
```

## 🛠️ Technologies utilisées

- **Java 25** : Langage de programmation
- **Spring Boot 4.0.0** : Framework backend
- **Spring Data JPA** : Persistance des données
- **Hibernate** : ORM
- **Lombok** : Réduction du code boilerplate
- **Jakarta Validation** : Validation des données
- **Gradle** : Gestion des dépendances

## 📝 Notes importantes

1. **AbstractEntity** : Toutes les entités héritent de cette classe pour avoir automatiquement un ID, une date de création et une date de modification.

2. **Relations bidirectionnelles** : Les relations sont configurées avec `mappedBy` pour éviter la duplication et assurer la cohérence.

3. **Cascade et orphanRemoval** : Les relations OneToMany utilisent `cascade = CascadeType.ALL` et `orphanRemoval = true` pour une gestion automatique des entités enfants.

4. **FetchType.LAZY** : Toutes les relations ManyToOne et OneToMany utilisent le chargement paresseux pour optimiser les performances.

5. **Validations** : Les entités utilisent des annotations de validation Jakarta (`@NotNull`, `@NotBlank`, `@Positive`, `@Email`) pour garantir l'intégrité des données.

## 🚀 Prochaines étapes

- [ ] Implémentation des repositories (JPA)
- [ ] Implémentation des services métier
- [ ] Implémentation des controllers REST
- [ ] Configuration de la base de données
- [ ] Tests unitaires et d'intégration
- [ ] Documentation API (Swagger/OpenAPI)
- [ ] Sécurité et authentification (JWT)
- [ ] Gestion des exceptions

---

**Version** : 0.0.1-SNAPSHOT  
**Dernière mise à jour** : 2024

