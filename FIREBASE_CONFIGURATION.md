# Configuration Firebase pour les Notifications Push

## 📋 Configuration requise

Pour que le backend puisse envoyer des notifications push, vous devez configurer la clé serveur Firebase.

## 🔑 Obtenir la clé serveur Firebase

1. Aller sur [Firebase Console](https://console.firebase.google.com/)
2. Sélectionner votre projet : `siblhish-app`
3. Aller dans **Paramètres du projet** (⚙️) → **Cloud Messaging**
4. Dans la section **"Clés de l'API Cloud Messaging"**, copier la **"Clé serveur"** (Server key)

## ⚙️ Configurer dans le backend

### Option 1 : Dans `application.properties` (pour développement)

**Fichier :** `src/main/resources/application.properties`

```properties
firebase.server-key=VOTRE_CLE_SERVEUR_ICI
firebase.project-id=siblhish-app
```

### Option 2 : Variables d'environnement (pour production)

Sur Railway ou votre plateforme de déploiement, ajouter la variable d'environnement :

```
FIREBASE_SERVER_KEY=VOTRE_CLE_SERVEUR_ICI
```

Puis modifier `application.properties` pour utiliser la variable d'environnement :

```properties
firebase.server-key=${FIREBASE_SERVER_KEY:}
```

## ✅ Comment ça fonctionne

1. **Création d'une notification** : Quand `NotificationService.createNotification()` est appelé
2. **Sauvegarde en base** : La notification est sauvegardée dans la table `notifications`
3. **Envoi push automatique** : Le service envoie automatiquement une notification push à l'utilisateur
4. **Vérifications** :
   - L'utilisateur doit avoir un `fcmToken` enregistré
   - L'utilisateur doit avoir `notificationsEnabled = true`
   - La clé serveur Firebase doit être configurée

## 🧪 Tester

Une fois configuré, chaque fois qu'une notification est créée dans la table `notifications`, l'utilisateur recevra automatiquement une notification push sur son téléphone.

## 📝 Exemple d'utilisation

```java
// Dans n'importe quel service
notificationService.createNotification(
    userId,
    "Rappel de paiement",
    "Votre paiement de 500 MAD est dû aujourd'hui",
    TypeNotification.PAYMENT_REMINDER
);
```

L'utilisateur recevra automatiquement une notification push avec ce titre et ce message.

