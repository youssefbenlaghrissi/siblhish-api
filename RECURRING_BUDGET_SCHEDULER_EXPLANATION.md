# 📅 Explication de `RecurringBudgetScheduler`

## 🎯 Objectif de la Classe

La classe `RecurringBudgetScheduler` est un **scheduler Spring** qui crée automatiquement des budgets récurrents chaque mois. Elle s'exécute le **1er de chaque mois à 00:01:00** pour créer les budgets du mois en cours.

## 🔄 Concept de Budget Récurrent

Un **budget récurrent** est un **template** qui sert de modèle pour créer automatiquement un nouveau budget chaque mois.

### Caractéristiques d'un Budget Récurrent :
- ✅ `isRecurring = true`
- ✅ `startDate` = 1er jour d'un mois (ex: 2026-02-01)
- ✅ `endDate` = dernier jour du même mois (ex: 2026-02-28)
- ✅ Même `amount` (montant) chaque mois
- ✅ Même `category` (obligatoire - les budgets globaux ne sont pas supportés)
- ✅ Même `user`

## 📋 Structure de la Classe

```java
@Component                    // Bean Spring
@RequiredArgsConstructor      // Injection de dépendances via constructeur
public class RecurringBudgetScheduler {
    
    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;
    
    @Scheduled(cron = "0 1 0 1 * ?")  // Le 1er de chaque mois à 00:01:00
    @Transactional
    public void createRecurringBudgetsForCurrentMonth() {
        // Logique de création
    }
}
```

## ⏰ Expression Cron

```java
@Scheduled(cron = "0 1 0 1 * ?")
```

**Décomposition** :
- `0` = seconde (0)
- `1` = minute (1)
- `0` = heure (00:00)
- `1` = jour du mois (1er)
- `*` = mois (tous les mois)
- `?` = jour de la semaine (ignoré)

**Résultat** : Exécution le **1er de chaque mois à 00:01:00**

## 🔍 Flux d'Exécution Détaillé

### Étape 1 : Récupération du Mois en Cours

```java
YearMonth currentMonth = YearMonth.now();  // Ex: 2026-02
LocalDate firstDayOfMonth = currentMonth.atDay(1);      // 2026-02-01
LocalDate lastDayOfMonth = currentMonth.atEndOfMonth(); // 2026-02-28
```

**Exemple** :
- Si on est le **1er février 2026** :
  - `currentMonth` = `2026-02`
  - `firstDayOfMonth` = `2026-02-01`
  - `lastDayOfMonth` = `2026-02-28`

### Étape 2 : Récupération des Templates de Budgets Récurrents

```java
List<Budget> recurringBudgets = budgetRepository.findByIsRecurringTrueOrderByIdDesc();
```

**Requête SQL générée** :
```sql
SELECT * FROM budgets 
WHERE is_recurring = true 
  AND deleted = false 
ORDER BY id DESC
```

**Exemple de résultats** :
| id | user_id | amount | start_date | end_date | category_id | is_recurring |
|----|---------|--------|------------|----------|-------------|--------------|
| 47 | 1 | 2000.0 | 2026-01-01 | 2026-01-31 | 1 (Alimentation) | true |
| 48 | 1 | 500.0 | 2026-01-01 | 2026-01-31 | 3 (Café) | true |
| 49 | 1 | 300.0 | 2026-01-01 | 2026-01-31 | 5 (Transport) | true |

**Note** : Les budgets sans catégorie (category_id = NULL) seront ignorés avec un warning.

### Étape 3 : Pour Chaque Template, Vérifier si le Budget du Mois Existe Déjà

**Note** : Seuls les budgets avec catégorie sont supportés. Les budgets globaux (sans catégorie) sont ignorés.

```java
// Ignorer les budgets sans catégorie
if (category == null) {
    logger.warn("⚠️ Budget récurrent ignoré car sans catégorie");
    continue;
}

// Vérifier si un budget pour ce mois existe déjà
List<Budget> existingBudgets = budgetRepository
    .findByUserIdAndCategoryIdAndStartDateAndEndDateOrderByIdDesc(
        userId, 
        category.getId(), 
        firstDayOfMonth,  // 2026-02-01
        lastDayOfMonth   // 2026-02-28
    );
boolean exists = !existingBudgets.isEmpty();
```

**Requête SQL générée** :
```sql
SELECT * FROM budgets 
WHERE user_id = 1 
  AND category_id = 1 
  AND start_date = '2026-02-01' 
  AND end_date = '2026-02-28' 
  AND deleted = false
```

### Étape 4 : Créer le Nouveau Budget si N'Existe Pas

```java
if (!exists) {
    Budget newBudget = new Budget();
    newBudget.setUser(templateBudget.getUser());
    newBudget.setAmount(templateBudget.getAmount());
    newBudget.setStartDate(firstDayOfMonth);      // 2026-02-01
    newBudget.setEndDate(lastDayOfMonth);         // 2026-02-28
    newBudget.setIsRecurring(true);
    newBudget.setCategory(category);
    newBudget.setCreationDate(LocalDateTime.now());
    
    Budget savedBudget = budgetRepository.save(newBudget);
    
    // Créer une notification
    createRecurringBudgetNotification(...);
}
```

## 📊 Exemples Concrets

### Exemple 1 : Budget Récurrent "Alimentation" (avec catégorie)

**Scénario** :
- **Template** (créé en janvier) :
  - `id` = 47
  - `user_id` = 1
  - `amount` = 2000.0 MAD
  - `category_id` = 1 (Alimentation)
  - `start_date` = 2026-01-01
  - `end_date` = 2026-01-31
  - `is_recurring` = true

**Le 1er février 2026 à 00:01:00** :

1. **Récupération du mois** :
   - `currentMonth` = `2026-02`
   - `firstDayOfMonth` = `2026-02-01`
   - `lastDayOfMonth` = `2026-02-28`

2. **Récupération des templates** :
   - Trouve le template `id = 47`

3. **Vérification d'existence** :
   ```sql
   SELECT * FROM budgets 
   WHERE user_id = 1 
     AND category_id = 1 
     AND start_date = '2026-02-01' 
     AND end_date = '2026-02-28'
   ```
   - **Résultat** : Aucun budget trouvé ✅

4. **Création du nouveau budget** :
   - `id` = 50 (nouveau)
   - `user_id` = 1
   - `amount` = 2000.0 MAD (copié du template)
   - `category_id` = 1 (Alimentation)
   - `start_date` = 2026-02-01
   - `end_date` = 2026-02-28
   - `is_recurring` = true
   - `creation_date` = 2026-02-01 00:01:00

5. **Notification créée** :
   - Titre : "Budget récurrent créé"
   - Description : "Un budget récurrent de 2000.00 MAD a été créé automatiquement pour le mois de 2026-02. (Alimentation)"

### Exemple 2 : Budget Déjà Existant (Pas de Duplication)

**Scénario** :
- **Template** : Budget "Alimentation" (id = 47)
- **Budget de février déjà créé manuellement** :
  - `id` = 50
  - `user_id` = 1
  - `category_id` = 1
  - `start_date` = 2026-02-01
  - `end_date` = 2026-02-28

**Le 1er février 2026 à 00:01:00** :

1. **Vérification d'existence** :
   ```sql
   SELECT * FROM budgets 
   WHERE user_id = 1 
     AND category_id = 1 
     AND start_date = '2026-02-01' 
     AND end_date = '2026-02-28'
   ```
   - **Résultat** : Budget trouvé (id = 50) ❌

2. **Action** : Aucun nouveau budget créé (évite la duplication)

## 🔐 Gestion des Erreurs

### Try-Catch Global

```java
try {
    // Logique de création
} catch (Exception e) {
    logger.error("❌ Erreur lors de la création automatique des budgets récurrents: {}", 
        e.getMessage(), e);
}
```

**Avantage** : Si une erreur survient pour un budget, les autres budgets continuent d'être créés.

### Try-Catch pour les Notifications

```java
private void createRecurringBudgetNotification(...) {
    try {
        notificationService.createNotification(...);
    } catch (Exception e) {
        logger.error("❌ Erreur lors de la création de la notification...");
        // Ne pas bloquer la création du budget si la notification échoue
    }
}
```

**Avantage** : Si l'envoi de notification échoue, le budget est quand même créé.

## ⚙️ Annotations Importantes

### `@Component`
- Rend la classe un **bean Spring**
- Permet l'injection de dépendances (`@RequiredArgsConstructor`)

### `@Scheduled(cron = "0 1 0 1 * ?")`
- Active l'exécution automatique selon l'expression cron
- Nécessite `@EnableScheduling` dans la classe principale

### `@Transactional`
- Garantit que toutes les opérations DB sont dans une transaction
- En cas d'erreur, rollback automatique

## 📈 Avantages de cette Approche

### ✅ Automatisation
- Pas besoin d'intervention manuelle chaque mois
- Création systématique le 1er du mois

### ✅ Évite les Doublons
- Vérification avant création
- Pas de budgets dupliqués pour le même mois

### ✅ Simplicité
- Supporte uniquement les budgets avec catégorie
- Code simplifié sans gestion des budgets globaux

### ✅ Notification Utilisateur
- L'utilisateur est informé de la création automatique
- Meilleure transparence

### ✅ Robustesse
- Gestion d'erreurs à plusieurs niveaux
- Une erreur n'empêche pas les autres budgets d'être créés

## ⚠️ Points d'Attention

### 1. **Catégorie Obligatoire**
- **Seuls les budgets avec catégorie sont supportés**
- Les budgets globaux (category = null) sont **ignorés** avec un warning
- Assurez-vous que tous vos budgets récurrents ont une catégorie

### 2. **Dépendance au Template**
- Le template doit exister et avoir `isRecurring = true`
- Si le template est supprimé (`deleted = true`), il ne sera plus utilisé

### 3. **Date d'Exécution**
- Le scheduler s'exécute le **1er à 00:01:00**
- Si l'application est arrêtée à ce moment, les budgets ne seront pas créés
- **Solution** : Exécution manuelle possible via une méthode publique

### 4. **Performance**
- Si beaucoup de budgets récurrents (ex: 1000+), la boucle peut être lente
- **Optimisation possible** : Batch processing ou requête SQL optimisée

### 5. **Transactions**
- Tous les budgets sont créés dans la même transaction
- Si une erreur survient, rollback complet
- **Avantage** : Cohérence des données
- **Inconvénient** : Si un budget échoue, aucun n'est créé

## 🔧 Améliorations Possibles

### 1. **Batch Processing**
```java
// Au lieu de créer un par un
List<Budget> budgetsToCreate = new ArrayList<>();
for (Budget template : recurringBudgets) {
    if (!exists) {
        budgetsToCreate.add(newBudget);
    }
}
budgetRepository.saveAll(budgetsToCreate); // Insertion en batch
```

### 2. **Logging Détaillé**
```java
logger.info("📊 Budgets récurrents trouvés: {}", recurringBudgets.size());
logger.info("✅ Budgets créés: {}", budgetsCreated);
logger.info("⏭️ Budgets ignorés (déjà existants): {}", budgetsSkipped);
```

### 3. **Métriques**
- Compter le nombre de budgets créés
- Compter le nombre de budgets ignorés
- Temps d'exécution

## 📝 Résumé

La classe `RecurringBudgetScheduler` :
1. ✅ S'exécute automatiquement le **1er de chaque mois à 00:01:00**
2. ✅ Récupère tous les **templates de budgets récurrents**
3. ✅ Pour chaque template, **vérifie si le budget du mois existe déjà**
4. ✅ Si n'existe pas, **crée un nouveau budget** avec les dates du mois en cours
5. ✅ **Envoie une notification** à l'utilisateur
6. ✅ **Gère les erreurs** sans bloquer les autres budgets

**Résultat** : Les utilisateurs ont automatiquement leurs budgets récurrents créés chaque mois, sans intervention manuelle ! 🎉

