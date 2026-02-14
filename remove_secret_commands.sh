#!/bin/bash
# Script pour retirer le fichier Firebase de l'historique Git

echo "=== Étape 1 : Vérification de l'historique ==="
git log --all --full-history --oneline -- src/main/resources/firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json

echo ""
echo "=== Étape 2 : Retrait du fichier de l'historique Git ==="
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch src/main/resources/firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json" \
  --prune-empty --tag-name-filter cat -- --all

echo ""
echo "=== Étape 3 : Nettoyage des références ==="
git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
git reflog expire --expire=now --all
git gc --prune=now --aggressive

echo ""
echo "=== Étape 4 : Vérification ==="
git log --all --full-history --oneline -- src/main/resources/firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json

echo ""
echo "✅ Terminé ! Vous pouvez maintenant faire : git push --force-with-lease"

