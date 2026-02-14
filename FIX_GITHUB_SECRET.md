# Guide pour résoudre le problème de secret Firebase sur GitHub

## Problème
GitHub bloque le push car le fichier `src/main/resources/firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json` contient des credentials Firebase.

## Solution

### Étape 1 : Retirer le fichier du dernier commit (sans le supprimer localement)

```bash
# Retirer le fichier du dernier commit mais le garder sur le disque
git reset HEAD~1

# Ou si vous voulez juste retirer ce fichier spécifique du dernier commit :
git reset HEAD~1 -- src/main/resources/firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json
```

### Étape 2 : Retirer le fichier de l'index Git (mais le garder localement)

```bash
git rm --cached src/main/resources/firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json
```

### Étape 3 : Vérifier que le fichier est bien ignoré

Le fichier `.gitignore` a été mis à jour pour ignorer les fichiers Firebase.

### Étape 4 : Recréer le commit sans le fichier secret

```bash
# Ajouter tous les fichiers sauf celui qui est maintenant ignoré
git add .

# Recréer le commit
git commit -m "Votre message de commit (sans le fichier Firebase)"
```

### Étape 5 : Pousser à nouveau

```bash
git push
```

## Alternative : Si le fichier est déjà dans plusieurs commits

Si le fichier secret est dans plusieurs commits de l'historique, utilisez `git filter-branch` ou `git filter-repo` :

```bash
# Option 1 : Utiliser git filter-branch (plus ancien)
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch src/main/resources/firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json" \
  --prune-empty --tag-name-filter cat -- --all

# Option 2 : Utiliser BFG Repo-Cleaner (recommandé, plus rapide)
# Téléchargez BFG depuis https://rtyley.github.io/bfg-repo-cleaner/
bfg --delete-files siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json
git reflog expire --expire=now --all && git gc --prune=now --aggressive
```

## Important : Après avoir retiré le secret

1. **Régénérez les credentials Firebase** dans la console Firebase (car ils ont été exposés)
2. **Ne commitez JAMAIS** de fichiers contenant des secrets
3. **Utilisez des variables d'environnement** pour les secrets en production

## Fichier d'exemple (optionnel)

Créez un fichier `firebase-adminsdk-example.json` avec une structure similaire mais sans les vraies valeurs :

```json
{
  "type": "service_account",
  "project_id": "YOUR_PROJECT_ID",
  "private_key_id": "YOUR_KEY_ID",
  "private_key": "YOUR_PRIVATE_KEY",
  "client_email": "YOUR_CLIENT_EMAIL",
  ...
}
```

Et documentez dans le README comment obtenir ce fichier.

