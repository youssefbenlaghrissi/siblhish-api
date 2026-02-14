# Configuration Firebase Service Account pour les Notifications Push

## 📋 Vue d'ensemble

Firebase Cloud Messaging V1 nécessite un **Service Account** au lieu d'une simple "Server Key". Voici comment le configurer.

## 🔧 Étape 1 : Créer/Télécharger le Service Account

1. Aller dans Firebase Console → **Paramètres du projet** (⚙️) → **Comptes de service**
2. Cliquer sur **"Générer une nouvelle clé privée"** ou utiliser un compte existant
3. Télécharger le fichier JSON (ex: `siblhish-firebase-adminsdk-xxxxx.json`)

## 📁 Étape 2 : Placer le fichier dans le projet

### Option A : Dans le classpath (pour développement)

Placer le fichier dans : `src/main/resources/firebase-service-account.json`

### Option B : Chemin absolu (pour production)

Placer le fichier dans un répertoire sécurisé et configurer le chemin dans `application.properties` :

```properties
firebase.service-account-path=/chemin/vers/firebase-service-account.json
```

## ⚙️ Étape 3 : Configurer dans application.properties

**Fichier :** `src/main/resources/application.properties`

```properties
# Option 1 : Fichier depuis classpath (développement)
firebase.service-account-classpath=firebase-service-account.json

# Option 2 : Fichier depuis système de fichiers (production)
# firebase.service-account-path=/chemin/vers/firebase-service-account.json
```

## 🔐 Sécurité

⚠️ **Important :** Ne jamais commiter le fichier JSON dans Git !

Ajouter dans `.gitignore` :
```
firebase-service-account.json
src/main/resources/firebase-service-account.json
```

## ✅ Vérification

Une fois configuré, au démarrage du backend, vous devriez voir dans les logs :
```
Firebase initialized from classpath: firebase-service-account.json
Firebase Messaging initialized successfully
```

## 🚀 Utilisation

Le service est automatiquement utilisé par `NotificationService.createNotification()`.
Chaque notification créée enverra automatiquement une notification push à l'utilisateur.

