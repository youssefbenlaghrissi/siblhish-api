# Explication du Filtre de Période - Monthly Summary

## 📊 Vue d'ensemble

L'endpoint `/api/v1/statistics/monthly-summary/{userId}?period={period}` agrège les revenus et dépenses selon la période choisie.

## 🔍 Paramètres

- **period** : `"week"`, `"month"`, `"quarter"`, ou `"year"` (par défaut: `"month"`)

## 📋 Exemples Concrets

### Exemple 1 : `period=month` (Par mois)

**Requête :**
```
GET /api/v1/statistics/monthly-summary/1?period=month
```

**Données en base :**
- 15 janvier 2025 : Revenu 5000 MAD
- 20 janvier 2025 : Dépense 200 MAD
- 5 février 2025 : Revenu 3000 MAD
- 10 février 2025 : Dépense 150 MAD

**Résultat :**
```json
[
  {
    "month": "2025-01",  // Janvier 2025
    "totalIncome": 5000.0,
    "totalExpenses": 200.0,
    "balance": 4800.0
  },
  {
    "month": "2025-02",  // Février 2025
    "totalIncome": 3000.0,
    "totalExpenses": 150.0,
    "balance": 2850.0
  }
]
```

**Explication :** Les transactions sont groupées par mois. Chaque ligne représente un mois avec le total des revenus et dépenses.

---

### Exemple 2 : `period=week` (Par semaine)

**Requête :**
```
GET /api/v1/statistics/monthly-summary/1?period=week
```

**Données en base :**
- Semaine 1 (1-7 janvier) : Revenu 2000 MAD, Dépense 100 MAD
- Semaine 2 (8-14 janvier) : Revenu 1500 MAD, Dépense 50 MAD
- Semaine 3 (15-21 janvier) : Revenu 3000 MAD, Dépense 200 MAD

**Résultat :**
```json
[
  {
    "month": "2025-01",  // Semaine 1 de 2025
    "totalIncome": 2000.0,
    "totalExpenses": 100.0,
    "balance": 1900.0
  },
  {
    "month": "2025-02",  // Semaine 2 de 2025
    "totalIncome": 1500.0,
    "totalExpenses": 50.0,
    "balance": 1450.0
  },
  {
    "month": "2025-03",  // Semaine 3 de 2025
    "totalIncome": 3000.0,
    "totalExpenses": 200.0,
    "balance": 2800.0
  }
]
```

**Explication :** Les transactions sont groupées par semaine ISO. Le format `IYYY-IW` donne l'année et le numéro de semaine (1-53).

---

### Exemple 3 : `period=quarter` (Par trimestre)

**Requête :**
```
GET /api/v1/statistics/monthly-summary/1?period=quarter
```

**Données en base :**
- Janvier-Février-Mars 2025 : Revenus 15000 MAD, Dépenses 5000 MAD
- Avril-Mai-Juin 2025 : Revenus 18000 MAD, Dépenses 6000 MAD

**Résultat :**
```json
[
  {
    "month": "2025-1",  // Trimestre 1 (Q1)
    "totalIncome": 15000.0,
    "totalExpenses": 5000.0,
    "balance": 10000.0
  },
  {
    "month": "2025-2",  // Trimestre 2 (Q2)
    "totalIncome": 18000.0,
    "totalExpenses": 6000.0,
    "balance": 12000.0
  }
]
```

**Explication :** Les transactions sont groupées par trimestre. Format : `YYYY-Q` (ex: "2025-1" = Q1, "2025-2" = Q2).

---

### Exemple 4 : `period=year` (Par année)

**Requête :**
```
GET /api/v1/statistics/monthly-summary/1?period=year
```

**Données en base :**
- 2023 : Revenus 60000 MAD, Dépenses 40000 MAD
- 2024 : Revenus 80000 MAD, Dépenses 50000 MAD
- 2025 : Revenus 10000 MAD, Dépenses 3000 MAD

**Résultat :**
```json
[
  {
    "month": "2023",  // Année 2023
    "totalIncome": 60000.0,
    "totalExpenses": 40000.0,
    "balance": 20000.0
  },
  {
    "month": "2024",  // Année 2024
    "totalIncome": 80000.0,
    "totalExpenses": 50000.0,
    "balance": 30000.0
  },
  {
    "month": "2025",  // Année 2025
    "totalIncome": 10000.0,
    "totalExpenses": 3000.0,
    "balance": 7000.0
  }
]
```

**Explication :** Les transactions sont groupées par année. Retourne les 5 dernières années.

---

## 🎯 Résumé

| Période | Format `month` | Période couverte | Exemple |
|---------|---------------|------------------|---------|
| **week** | `IYYY-IW` | Année en cours | `"2025-01"` = Semaine 1 de 2025 |
| **month** | `YYYY-MM` | Année en cours | `"2025-01"` = Janvier 2025 |
| **quarter** | `YYYY-Q` | Année en cours | `"2025-1"` = Q1 2025 |
| **year** | `YYYY` | 5 dernières années | `"2025"` = Année 2025 |

## ⚠️ Note importante

Le champ `month` dans la réponse contient toujours la période formatée, même si ce n'est pas un mois :
- Pour `period=week` : contient la semaine (ex: "2025-01")
- Pour `period=quarter` : contient le trimestre (ex: "2025-1")
- Pour `period=year` : contient l'année (ex: "2025")

## 🔄 Filtre de date automatique

- **week** : Affiche toutes les semaines de l'année en cours
- **month** : Affiche tous les mois de l'année en cours
- **quarter** : Affiche tous les trimestres de l'année en cours
- **year** : Affiche les 5 dernières années


