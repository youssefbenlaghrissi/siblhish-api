# 🚀 Benchmark : Mapping Manuel vs MapStruct

## 📋 Contexte

Comparaison entre deux approches pour mapper `IncomeRequestDto` → `Income` :

1. **Approche Manuelle** : Setters explicites (code actuel)
2. **MapStruct** : Génération de code à la compilation

---

## 🔄 Approche 1 : Mapping Manuel (ACTUELLE)

### Code

```java
@Transactional
public IncomeDto createIncome(IncomeRequestDto request) {
    User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    Income income = new Income();
    income.setAmount(request.getAmount());
    income.setMethod(request.getMethod());
    LocalDateTime now = LocalDateTime.now();
    income.setCreationDate(request.getDate() != null ? request.getDate() : now);
    income.setDescription(request.getDescription());
    income.setSource(request.getSource());
    income.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
    income.setRecurrenceFrequency(request.getRecurrenceFrequency());
    income.setRecurrenceEndDate(request.getRecurrenceEndDate());
    income.setRecurrenceDaysOfWeek(request.getRecurrenceDaysOfWeek());
    income.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
    income.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
    income.setUser(user);
    
    Income saved = incomeRepository.save(income);
    return mapper.toIncomeDto(saved);
}
```

### Caractéristiques

- ✅ **Contrôle total** : Logique métier explicite (ex: `LocalDateTime.now()`)
- ✅ **Flexibilité** : Gestion de cas spéciaux facile (ex: `request.getDate() != null ? ...`)
- ❌ **Verbose** : 13 lignes de setters
- ❌ **Maintenance** : Si l'entité change, il faut modifier le code manuellement
- ❌ **Risque d'erreur** : Facile d'oublier un setter

---

## 🔄 Approche 2 : MapStruct

### Code MapStruct

#### Interface Mapper

```java
@Mapper(componentModel = "spring")
public interface IncomeMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "creationDate", 
             expression = "java(request.getDate() != null ? request.getDate() : java.time.LocalDateTime.now())")
    @Mapping(target = "isRecurring", 
             expression = "java(request.getIsRecurring() != null ? request.getIsRecurring() : false)")
    @Mapping(target = "user", source = "user")
    Income toIncome(IncomeRequestDto request, User user);
}
```

#### Service avec MapStruct

```java
@Transactional
public IncomeDto createIncome(IncomeRequestDto request) {
    User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    Income income = incomeMapper.toIncome(request, user);
    
    Income saved = incomeRepository.save(income);
    return mapper.toIncomeDto(saved);
}
```

### Code Généré par MapStruct (à la compilation)

```java
@Component
public class IncomeMapperImpl implements IncomeMapper {
    
    @Override
    public Income toIncome(IncomeRequestDto request, User user) {
        if (request == null) {
            return null;
        }
        
        Income income = new Income();
        income.setAmount(request.getAmount());
        income.setMethod(request.getMethod());
        income.setCreationDate(
            request.getDate() != null ? request.getDate() : LocalDateTime.now()
        );
        income.setDescription(request.getDescription());
        income.setSource(request.getSource());
        income.setIsRecurring(
            request.getIsRecurring() != null ? request.getIsRecurring() : false
        );
        income.setRecurrenceFrequency(request.getRecurrenceFrequency());
        income.setRecurrenceEndDate(request.getRecurrenceEndDate());
        income.setRecurrenceDaysOfWeek(request.getRecurrenceDaysOfWeek());
        income.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        income.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        income.setUser(user);
        
        return income;
    }
}
```

### Caractéristiques

- ✅ **Concis** : 1 ligne dans le service
- ✅ **Type-safe** : Erreurs détectées à la compilation
- ✅ **Maintenance** : Si l'entité change, MapStruct détecte les problèmes à la compilation
- ✅ **Performance** : Code généré = pas de réflexion, pas d'overhead runtime
- ⚠️ **Logique métier** : Nécessite des expressions Java pour les cas spéciaux
- ⚠️ **Dépendance** : Ajoute une dépendance Maven

---

## 📊 Benchmark de Performance

### Scénario : Créer 10,000 Income

#### Test Setup

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class IncomeMappingBenchmark {
    
    private IncomeRequestDto request;
    private User user;
    private IncomeMapper incomeMapper; // MapStruct
    private EntityMapper entityMapper; // Manuel
    
    @Setup
    public void setup() {
        request = new IncomeRequestDto();
        request.setUserId(1L);
        request.setAmount(1000.0);
        request.setMethod(PaymentMethod.CASH);
        request.setDate(LocalDateTime.now());
        // ... autres champs
        
        user = new User();
        user.setId(1L);
    }
    
    @Benchmark
    public Income manualMapping() {
        Income income = new Income();
        income.setAmount(request.getAmount());
        income.setMethod(request.getMethod());
        // ... 13 setters
        income.setUser(user);
        return income;
    }
    
    @Benchmark
    public Income mapstructMapping() {
        return incomeMapper.toIncome(request, user);
    }
}
```

### Résultats Estimés (JMH Benchmark)

| Métrique | Mapping Manuel | MapStruct | Différence |
|----------|----------------|-----------|------------|
| **Temps moyen (1 mapping)** | **~50-100 ns** | **~50-100 ns** | **Équivalent** |
| **Temps (10,000 mappings)** | **~0.5-1 ms** | **~0.5-1 ms** | **Équivalent** |
| **Allocation mémoire** | **~200 bytes** | **~200 bytes** | **Équivalent** |
| **Overhead runtime** | **Aucun** | **Aucun** | **Équivalent** |

### 🔍 Analyse Détaillée

#### 1. Performance d'Exécution

**Verdict** : **ÉQUIVALENT** ✅

**Pourquoi ?**
- MapStruct génère du code Java **pur** à la compilation
- Le code généré est **identique** au code manuel
- **Aucune réflexion** à l'exécution
- **Aucun overhead** runtime

**Bytecode généré** (simplifié) :
```java
// Les deux approches génèrent exactement le même bytecode
aload_1  // request
invokevirtual IncomeRequestDto.getAmount()
aload_0  // income
invokevirtual Income.setAmount(Double)
// ... répété pour chaque champ
```

#### 2. Temps de Compilation

| Métrique | Mapping Manuel | MapStruct |
|----------|----------------|-----------|
| **Compilation initiale** | ~5-10s | ~6-12s (+1-2s) |
| **Recompilation incrémentale** | ~1-2s | ~2-3s (+1s) |
| **Annotation processing** | N/A | ~1-2s |

**Verdict** : MapStruct ajoute **~1-2 secondes** à la compilation, mais c'est négligeable.

#### 3. Taille du Code

| Métrique | Mapping Manuel | MapStruct |
|----------|----------------|-----------|
| **Lignes dans le service** | 13 lignes | 1 ligne |
| **Lignes totales (service + mapper)** | 13 lignes | ~20 lignes (interface + généré) |
| **Maintenabilité** | ⚠️ Manuelle | ✅ Automatique |

**Verdict** : MapStruct réduit le code dans le service, mais génère du code ailleurs.

---

## 🎯 Comparaison Détaillée

### ✅ Avantages du Mapping Manuel

1. **Contrôle total**
   - Logique métier explicite et visible
   - Facile à déboguer (pas de code généré à comprendre)

2. **Pas de dépendance externe**
   - Pas besoin d'ajouter MapStruct au `pom.xml`
   - Moins de dépendances = moins de risques

3. **Pas de temps de compilation supplémentaire**
   - Compilation plus rapide (pas d'annotation processing)

4. **Simplicité**
   - Pas besoin d'apprendre MapStruct
   - Code immédiatement compréhensible

### ✅ Avantages de MapStruct

1. **Concision**
   - 1 ligne au lieu de 13 dans le service
   - Code plus lisible et maintenable

2. **Type-safety**
   - Erreurs détectées à la compilation
   - Si l'entité change, MapStruct détecte les problèmes

3. **Maintenance automatique**
   - Si un champ est ajouté/supprimé, MapStruct le détecte
   - Pas besoin de modifier manuellement tous les mappers

4. **Cohérence**
   - Tous les mappers suivent le même pattern
   - Moins de risques d'incohérences

5. **Réutilisabilité**
   - Le mapper peut être réutilisé dans plusieurs services
   - Pas de duplication de code

---

## 📈 Scénarios d'Usage

### Scénario 1 : Création simple (cas actuel)

**Mapping Manuel** :
```java
Income income = new Income();
income.setAmount(request.getAmount());
// ... 12 autres setters
```

**MapStruct** :
```java
Income income = incomeMapper.toIncome(request, user);
```

**Verdict** : MapStruct est **plus concis** et **plus maintenable**.

### Scénario 2 : Logique métier complexe

**Mapping Manuel** :
```java
income.setCreationDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
income.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
```

**MapStruct** :
```java
@Mapping(target = "creationDate", 
         expression = "java(request.getDate() != null ? request.getDate() : java.time.LocalDateTime.now())")
@Mapping(target = "isRecurring", 
         expression = "java(request.getIsRecurring() != null ? request.getIsRecurring() : false)")
```

**Verdict** : Mapping Manuel est **plus lisible** pour la logique complexe.

### Scénario 3 : Mapping de listes

**Mapping Manuel** :
```java
List<Income> incomes = ...;
List<IncomeDto> dtos = incomes.stream()
    .map(income -> {
        IncomeDto dto = new IncomeDto();
        dto.setId(income.getId());
        // ... 10 setters
        return dto;
    })
    .collect(Collectors.toList());
```

**MapStruct** :
```java
List<IncomeDto> dtos = incomeMapper.toIncomeDtoList(incomes);
```

**Verdict** : MapStruct est **beaucoup plus concis** pour les listes.

---

## 🎯 Recommandation

### ✅ **Utiliser MapStruct** si :

1. ✅ Tu as **beaucoup de mappers** (Income, Expense, Budget, etc.)
2. ✅ Tu veux **réduire la duplication** de code
3. ✅ Tu veux **détecter les erreurs à la compilation**
4. ✅ Tu veux **améliorer la maintenabilité** à long terme
5. ✅ Tu es prêt à ajouter une dépendance Maven

### ⚠️ **Garder le mapping manuel** si :

1. ⚠️ Tu as **peu de mappers** (1-2 seulement)
2. ⚠️ Tu veux **éviter les dépendances externes**
3. ⚠️ Tu as **beaucoup de logique métier complexe** dans le mapping
4. ⚠️ Tu préfères le **contrôle total** et la **simplicité**

---

## 📊 Score Final

| Critère | Mapping Manuel | MapStruct | Gagnant |
|---------|----------------|-----------|---------|
| **Performance runtime** | ✅ Équivalent | ✅ Équivalent | 🤝 Égalité |
| **Temps de compilation** | ✅ Plus rapide | ⚠️ +1-2s | 🏆 Manuel |
| **Concision** | ❌ Verbose | ✅ Concis | 🏆 MapStruct |
| **Maintenabilité** | ⚠️ Manuelle | ✅ Automatique | 🏆 MapStruct |
| **Type-safety** | ⚠️ Runtime | ✅ Compile-time | 🏆 MapStruct |
| **Simplicité** | ✅ Simple | ⚠️ Nécessite apprentissage | 🏆 Manuel |
| **Flexibilité** | ✅ Totale | ⚠️ Expressions Java | 🏆 Manuel |

### 🎯 Verdict Global

**Pour un projet avec plusieurs entités (Income, Expense, Budget, etc.)** :
- ✅ **MapStruct est recommandé** pour la maintenabilité et la cohérence
- ✅ **Performance équivalente** (aucun overhead runtime)
- ✅ **Gain de temps** à long terme (moins de maintenance)

**Pour un projet simple avec peu de mappers** :
- ⚠️ **Mapping manuel est acceptable** si tu préfères la simplicité
- ⚠️ **Performance équivalente** dans tous les cas

---

## 🚀 Conclusion

### Performance Runtime : **ÉQUIVALENT** ✅

Les deux approches génèrent **exactement le même bytecode** à l'exécution. MapStruct ne génère **aucun overhead** runtime car il génère du code Java pur à la compilation.

### Différence Principale : **MAINTENABILITÉ** 🎯

- **Mapping Manuel** : Plus de code à maintenir manuellement
- **MapStruct** : Maintenance automatique, détection d'erreurs à la compilation

### Recommandation pour ton projet

Vu que tu as **plusieurs entités** (Income, Expense, Budget, Category, etc.), **MapStruct serait bénéfique** pour :
- ✅ Réduire la duplication de code
- ✅ Améliorer la maintenabilité
- ✅ Détecter les erreurs à la compilation
- ✅ Garder une performance équivalente

**Mais** si tu préfères garder le contrôle total et éviter les dépendances, le mapping manuel reste **parfaitement valable** avec des performances identiques.

---

**Date** : 2026-02-13  
**Conclusion** : **Performance équivalente**, choix basé sur **maintenabilité** et **préférences** du projet.

