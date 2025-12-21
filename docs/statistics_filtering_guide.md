# Guide : Filtrage des Statistiques - Backend vs Frontend

## 🎯 Recommandation : **Filtrage côté BACKEND**

Pour les statistiques, **tous les filtres doivent être implémentés côté backend** pour les raisons suivantes :

### ✅ Avantages du filtrage backend

1. **Performance** : Les requêtes SQL avec `GROUP BY` et agrégations sont optimisées par la base de données
2. **Réduction du trafic réseau** : Seules les données filtrées sont envoyées au frontend
3. **Cohérence** : Les calculs d'agrégation (SUM, COUNT, etc.) sont faits une seule fois côté serveur
4. **Sécurité** : Les filtres sont appliqués avant l'envoi des données
5. **Scalabilité** : Même avec des millions de transactions, les requêtes restent rapides

### ❌ Pourquoi pas le frontend ?

- ❌ Charger toutes les transactions puis filtrer côté client = **très lent**
- ❌ Consommation mémoire excessive sur mobile
- ❌ Calculs d'agrégation complexes à faire en JavaScript/Dart
- ❌ Risque d'erreurs dans les calculs

---

## 📋 Endpoints de Statistiques avec Filtres

### 1. Résumé par période (Graphique en barres)
```
GET /api/v1/statistics/summary/{userId}?period={period}
```

**Paramètres :**
- `period` : `"day"`, `"month"`, `"year"` (défaut: `"month"`)

**Exemple :**
```bash
GET /api/v1/statistics/summary/1?period=day
GET /api/v1/statistics/summary/1?period=month
GET /api/v1/statistics/summary/1?period=year
```

**Réponse :**
```json
[
  {
    "period": "2025-01-15",  // ou "2025-01" ou "2025"
    "totalIncome": 1500.0,
    "totalExpenses": 200.0,
    "balance": 1300.0
  }
]
```

---

### 2. Dépenses par catégorie (Graphique en camembert)
```
GET /api/v1/statistics/expenses-by-category-graph/{userId}?period={period}
```

**Paramètres :**
- `period` : `"day"`, `"month"`, `"year"` (défaut: `"month"`)

**Exemple :**
```bash
GET /api/v1/statistics/expenses-by-category-graph/1?period=month
```

**Réponse :**
```json
[
  {
    "categoryId": 5,
    "categoryName": "Carburant",
    "icon": "⛽",
    "color": "#FF8C00",
    "amount": 500.0,
    "percentage": 45.5
  }
]
```

---

### 3. Statistiques détaillées (avec dates personnalisées)
```
GET /api/v1/statistics/detailed/{userId}?startDate={date}&endDate={date}
```

**Paramètres :**
- `startDate` : Date de début (format: `YYYY-MM-DD`)
- `endDate` : Date de fin (format: `YYYY-MM-DD`)

**Exemple :**
```bash
GET /api/v1/statistics/detailed/1?startDate=2025-01-01&endDate=2025-01-31
```

---

### 4. Répartition par catégorie (avec dates et période)
```
GET /api/v1/statistics/expenses-by-category/{userId}?startDate={date}&endDate={date}&period={period}
```

**Paramètres :**
- `startDate` : Date de début (optionnel)
- `endDate` : Date de fin (optionnel)
- `period` : `"day"`, `"month"`, `"year"` (optionnel)

---

## 🎨 Implémentation Frontend (Flutter)

### Exemple de code Flutter

```dart
class StatisticsService {
  final String baseUrl = 'https://your-api.com/api/v1/statistics';
  
  // Récupérer le résumé par période
  Future<List<PeriodSummary>> getPeriodSummary({
    required int userId,
    String period = 'month', // 'day', 'month', 'year'
  }) async {
    final response = await http.get(
      Uri.parse('$baseUrl/summary/$userId?period=$period'),
    );
    
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return (data['data'] as List)
          .map((e) => PeriodSummary.fromJson(e))
          .toList();
    }
    throw Exception('Failed to load statistics');
  }
  
  // Récupérer les dépenses par catégorie
  Future<List<CategoryExpense>> getExpensesByCategory({
    required int userId,
    String period = 'month',
  }) async {
    final response = await http.get(
      Uri.parse('$baseUrl/expenses-by-category-graph/$userId?period=$period'),
    );
    
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return (data['data'] as List)
          .map((e) => CategoryExpense.fromJson(e))
          .toList();
    }
    throw Exception('Failed to load category expenses');
  }
}
```

### Widget de sélection de période

```dart
class PeriodSelector extends StatelessWidget {
  final String selectedPeriod;
  final Function(String) onPeriodChanged;
  
  const PeriodSelector({
    required this.selectedPeriod,
    required this.onPeriodChanged,
  });
  
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _PeriodButton(
          label: 'Jour',
          value: 'day',
          isSelected: selectedPeriod == 'day',
          onTap: () => onPeriodChanged('day'),
        ),
        _PeriodButton(
          label: 'Mois',
          value: 'month',
          isSelected: selectedPeriod == 'month',
          onTap: () => onPeriodChanged('month'),
        ),
        _PeriodButton(
          label: 'Année',
          value: 'year',
          isSelected: selectedPeriod == 'year',
          onTap: () => onPeriodChanged('year'),
        ),
      ],
    );
  }
}
```

---

## 📊 Comportement des Filtres

| Période | Format `period` | Période couverte | Exemple |
|---------|----------------|------------------|---------|
| **day** | `YYYY-MM-DD` | 30 derniers jours | `"2025-01-15"` |
| **month** | `YYYY-MM` | Année en cours | `"2025-01"` |
| **year** | `YYYY` | 5 dernières années | `"2025"` |

---

## ✅ Checklist Frontend

- [ ] Créer un service pour appeler les APIs de statistiques
- [ ] Créer un widget de sélection de période (boutons Jour/Mois/Année)
- [ ] Gérer l'état de la période sélectionnée
- [ ] Appeler l'API avec le paramètre `period` approprié
- [ ] Afficher les données dans les graphiques
- [ ] Gérer les états de chargement et d'erreur

---

## 🚫 Ce qu'il ne faut PAS faire côté frontend

- ❌ Charger toutes les transactions puis filtrer
- ❌ Faire des calculs d'agrégation (SUM, COUNT) côté client
- ❌ Grouper les données par période en Dart
- ❌ Calculer les pourcentages côté client

---

## 💡 Résumé

**Backend** = Filtrage, agrégation, calculs  
**Frontend** = Affichage, sélection de période, visualisation

Le frontend envoie simplement le paramètre `period` au backend, et le backend retourne les données déjà filtrées et agrégées.

