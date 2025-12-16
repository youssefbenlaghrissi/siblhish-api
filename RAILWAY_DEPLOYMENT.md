# Guide de Déploiement sur Railway

Ce guide vous explique comment déployer votre application Siblhish API sur Railway avec PostgreSQL.

## 📋 Prérequis

1. Un compte GitHub (gratuit)
2. Un compte Railway (gratuit) : https://railway.app
3. Votre projet déjà poussé sur GitHub

## 🚀 Étapes de Déploiement

### 1. Préparer le projet

Les fichiers suivants ont déjà été créés :
- `railway.json` - Configuration Railway
- `Procfile` - Commande de démarrage
- `nixpacks.toml` - Configuration de build
- `application-railway.properties` - Configuration pour Railway

### 2. Créer un compte Railway

1. Allez sur https://railway.app
2. Cliquez sur "Login" et connectez-vous avec GitHub
3. Autorisez Railway à accéder à votre compte GitHub

### 3. Créer un nouveau projet

1. Dans le dashboard Railway, cliquez sur "New Project"
2. Sélectionnez "Deploy from GitHub repo"
3. Choisissez votre repository `siblhish-api`
4. Railway va détecter automatiquement votre projet

### 4. Ajouter PostgreSQL

1. Dans votre projet Railway, cliquez sur "+ New"
2. Sélectionnez "Database" → "Add PostgreSQL"
3. Railway va créer une base de données PostgreSQL automatiquement
4. Notez les variables d'environnement qui seront créées :
   - `DATABASE_URL`
   - `PGHOST`
   - `PGPORT`
   - `PGUSER`
   - `PGPASSWORD`
   - `PGDATABASE`

### 5. Configurer les variables d'environnement

1. Dans votre service Spring Boot, allez dans l'onglet "Variables"
2. Ajoutez les variables suivantes :

#### Variables de base de données (automatiquement ajoutées par Railway PostgreSQL)
- `DATABASE_URL` - URL complète de la base de données (ajoutée automatiquement)
- `DATABASE_USER` - Utilisateur PostgreSQL (généralement `postgres`)
- `DATABASE_PASSWORD` - Mot de passe PostgreSQL
- `DATABASE_HOST` - Host de la base de données
- `DATABASE_PORT` - Port (généralement 5432)
- `DATABASE_NAME` - Nom de la base de données

#### Variables d'application
- `SPRING_PROFILES_ACTIVE=railway` - Active le profil Railway
- `PORT` - Port sur lequel l'application écoute (Railway définit automatiquement)

#### Comment obtenir les variables PostgreSQL

1. Cliquez sur votre service PostgreSQL dans Railway
2. Allez dans l'onglet "Variables"
3. Vous verrez toutes les variables disponibles
4. Pour `DATABASE_URL`, Railway fournit généralement une URL au format :
   ```
   postgresql://postgres:password@host:port/database
   ```

5. Vous pouvez extraire les composants ou utiliser directement `DATABASE_URL`

### 6. Configurer Spring Boot pour Railway

Railway injecte automatiquement `DATABASE_URL` au format :
```
postgresql://user:password@host:port/database
```

Spring Boot peut utiliser cette URL directement, mais il faut la convertir au format JDBC.

#### Option 1 : Utiliser DATABASE_URL directement (Recommandé)

Mettez à jour `application-railway.properties` :

```properties
# Railway fournit DATABASE_URL au format postgresql://user:pass@host:port/db
# Spring Boot a besoin du format JDBC
spring.datasource.url=${DATABASE_URL}
```

Si Railway ne fournit pas `DATABASE_URL` au bon format, créez un script de conversion ou utilisez les variables individuelles.

#### Option 2 : Utiliser les variables individuelles

Si Railway fournit des variables séparées, utilisez :

```properties
spring.datasource.url=jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
```

### 7. Mettre à jour l'application pour Railway

Nous devons créer un DataSourceConfig pour convertir l'URL Railway en format JDBC.

### 8. Déployer

1. Railway va automatiquement détecter votre code depuis GitHub
2. Il va builder votre application avec Gradle
3. Une fois le build terminé, votre application sera accessible

### 9. Obtenir l'URL de votre API

1. Dans votre service Spring Boot, allez dans l'onglet "Settings"
2. Activez "Generate Domain" pour obtenir une URL publique
3. Votre API sera accessible à : `https://votre-app.railway.app/api/v1`

## 🔧 Configuration Avancée

### Variables d'environnement recommandées

```env
SPRING_PROFILES_ACTIVE=railway
PORT=8081
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

### Monitoring

Railway fournit des logs en temps réel :
1. Allez dans votre service
2. Cliquez sur l'onglet "Deployments"
3. Cliquez sur le dernier déploiement pour voir les logs

## 🐛 Dépannage

### Problème : L'application ne démarre pas

1. Vérifiez les logs dans Railway
2. Assurez-vous que toutes les variables d'environnement sont définies
3. Vérifiez que `SPRING_PROFILES_ACTIVE=railway` est défini

### Problème : Erreur de connexion à la base de données

1. Vérifiez que le service PostgreSQL est démarré
2. Vérifiez que les variables `DATABASE_*` sont correctement définies
3. Assurez-vous que votre service Spring Boot est dans le même projet que PostgreSQL

### Problème : Port déjà utilisé

Railway définit automatiquement la variable `PORT`. Assurez-vous que votre application utilise :
```properties
server.port=${PORT:8081}
```

## 📝 Notes Importantes

1. **Sécurité** : Ne commitez jamais vos `application.properties` avec des mots de passe
2. **Base de données** : Railway crée automatiquement les tables si `ddl-auto=update`
3. **Logs** : Les logs sont disponibles en temps réel dans Railway
4. **Redéploiement** : Chaque push sur GitHub déclenche un nouveau déploiement

## 🔗 Liens Utiles

- Documentation Railway : https://docs.railway.app
- Support Railway : https://railway.app/help

