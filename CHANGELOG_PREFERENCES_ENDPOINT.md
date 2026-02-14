# Changelog - Endpoint Préférences Utilisateur

## 📅 Date : 2026-02-14

## ✅ Modifications apportées

### 1. Nouveau DTO : `UserPreferencesRequest`
**Fichier** : `src/main/java/ma/siblhish/dto/UserPreferencesRequest.java`

DTO pour mettre à jour uniquement les préférences utilisateur :
- `notificationsEnabled` (Boolean, optionnel)
- `language` (String, optionnel)

### 2. Nouvelle méthode dans `UserService`
**Fichier** : `src/main/java/ma/siblhish/service/UserService.java`

Méthode ajoutée :
```java
@Transactional
public UserProfileDto updatePreferences(Long userId, Boolean notificationsEnabled, String language)
```

**Fonctionnalités** :
- Met à jour uniquement les champs fournis (non-null)
- Ne permet PAS de modifier firstName, lastName ou email
- Retourne le UserProfileDto mis à jour

### 3. Nouvel endpoint dans `UserController`
**Fichier** : `src/main/java/ma/siblhish/controller/UserController.java`

Endpoint ajouté :
```
PATCH /api/v1/users/{userId}/preferences
```

**Fonctionnalités** :
- Accepte un `UserPreferencesRequest` (champs optionnels)
- Retourne un `ApiResponse<UserProfileDto>`
- Gestion d'erreurs complète

## 🔗 Endpoint

### URL complète
```
PATCH https://siblhish-api-production-53ca.up.railway.app/api/v1/users/{userId}/preferences
```

### Exemple de requête
```json
{
  "notificationsEnabled": false
}
```

### Exemple de réponse
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "user@example.com",
    "language": "fr",
    "notificationsEnabled": false
  },
  "message": "Préférences mises à jour avec succès",
  "errors": null
}
```

## 🔒 Sécurité

- ✅ L'utilisateur ne peut PAS modifier son nom, prénom ou email via cet endpoint
- ✅ Seuls `notificationsEnabled` et `language` peuvent être modifiés
- ✅ Les champs sont optionnels (on peut modifier l'un ou l'autre)

## 📱 Frontend

Le frontend a été mis à jour pour utiliser ce nouvel endpoint :
- `UserService.updatePreferences()` utilise `PATCH /users/{userId}/preferences`
- `BudgetProvider.updateNotificationsEnabled()` utilise le nouvel endpoint
- Logs ajoutés pour le debugging

## ✅ Tests

Pour tester l'endpoint :

```bash
# Désactiver les notifications
curl -X PATCH http://localhost:8081/api/v1/users/1/preferences \
  -H "Content-Type: application/json" \
  -d '{"notificationsEnabled": false}'

# Changer la langue
curl -X PATCH http://localhost:8081/api/v1/users/1/preferences \
  -H "Content-Type: application/json" \
  -d '{"language": "en"}'

# Modifier les deux
curl -X PATCH http://localhost:8081/api/v1/users/1/preferences \
  -H "Content-Type: application/json" \
  -d '{"notificationsEnabled": true, "language": "fr"}'
```

