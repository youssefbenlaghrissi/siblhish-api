# Tests de charge et de performance – Siblhish API

Ce document décrit comment lancer les tests de charge et de performance sur l’API.

## Prérequis

- **L’API doit être démarrée** (même machine ou URL accessible).
- Exemple : `./gradlew bootRun` (écoute par défaut sur `http://localhost:8082`).
- **Gradle doit tourner avec JDK 21** pour les tâches Gatling (voir Dépannage ci‑dessous).

## Dépannage : « Unsupported class file major version 69 »

Si vous voyez cette erreur en lançant `./gradlew gatlingRun`, c’est que **Gradle s’exécute avec Java 25** (ou une version plus récente). Le compilateur Groovy utilisé par Gradle ne gère pas encore ce bytecode.

**Solution : faire tourner Gradle avec JDK 21.**

- **Option A – Définir `JAVA_HOME`** (recommandé)  
  Installez JDK 21, puis avant d’exécuter Gradle :
  - Windows (PowerShell) : `$env:JAVA_HOME = "C:\chemin\vers\jdk-21"`
  - Windows (CMD) : `set JAVA_HOME=C:\chemin\vers\jdk-21`
  - Linux/macOS : `export JAVA_HOME=/chemin/vers/jdk-21`
  Puis relancez : `./gradlew gatlingRun`

- **Option B – Forcer le JDK dans le projet**  
  Créez ou éditez `gradle.properties` à la racine du projet et ajoutez (en adaptant le chemin) :
  ```properties
  org.gradle.java.home=C:\\Program Files\\Java\\jdk-21
  ```
  Sous Windows, utilisez des doubles backslashes ou des slashes. Puis : `./gradlew --stop` et `./gradlew gatlingRun`.

**Alternative sans changer de JDK :** utilisez **k6** pour les tests de charge (voir Option 2 ci‑dessous) ; k6 ne dépend pas du JVM pour l’exécution des scénarios.

### Dépannage : « Port 8081 was already in use »

L’API est déjà démarrée (ou un autre processus utilise le port). Soit vous gardez cette instance et vous lancez uniquement `./gradlew gatlingRun`, soit vous libérez le port :

- **Voir quel processus utilise le port (Windows)** : `netstat -ano | findstr :8082` (dernier nombre = PID).
- **Arrêter le processus** : `taskkill /PID <PID> /F`.
- **Script fourni** : `.\scripts\kill-port-8081.ps1` (éditer `$port = 8082` si besoin).

## Option 1 : Gatling (intégré au projet)

Gatling est intégré au build Gradle et génère des rapports HTML.

### Scénarios disponibles

| Scénario | Rôle | Commande |
|----------|------|----------|
| **Fumée (smoke)** | Vérification rapide après déploiement, faible charge | `./gradlew gatlingRun-ma.siblhish.SiblhishApiSmokeSimulation` |
| **Charge (load)** | Charge réaliste, objectifs de perf (temps de réponse, débit) | `./gradlew gatlingRun-ma.siblhish.SiblhishApiLoadSimulation` |
| **Stress** | Montée jusqu’à saturation pour trouver la limite du système | `./gradlew gatlingRun-ma.siblhish.SiblhishApiStressSimulation` |
| **Défaut** | Scénario mixte (court) | `./gradlew gatlingRun-ma.siblhish.SiblhishApiSimulation` |

### Lancer tous les scénarios

```bash
./gradlew gatlingRun
```
(Lance toutes les simulations une après l’autre.)

### Lancer une simulation précise

```bash
./gradlew gatlingRun-ma.siblhish.SiblhishApiSmokeSimulation
./gradlew gatlingRun-ma.siblhish.SiblhishApiLoadSimulation
./gradlew gatlingRun-ma.siblhish.SiblhishApiStressSimulation
./gradlew gatlingRun-ma.siblhish.SiblhishApiSimulation
```

### Changer l’URL de l’API

```bash
./gradlew gatlingRun -DloadTest.baseUrl=http://staging.example.com/api/v1
```

### Consulter les rapports

Après l’exécution, ouvrir :

- **Windows** : `build\reports\gatling\<nom-simulation>-<timestamp>\index.html`
- **Linux/macOS** : `build/reports/gatling/<nom-simulation>-<timestamp>/index.html`

Ou exécuter : `.\scripts\open-gatling-report.ps1` pour ouvrir le dernier rapport.

Les rapports contiennent notamment :

- Temps de réponse (moyenne, percentiles 95/99)
- Débit (requêtes/s)
- Taux d’erreur
- Courbes en fonction du temps

### Adapter les scénarios

- **Fumée** : `src/gatling/scala/ma/siblhish/SiblhishApiSmokeSimulation.scala`
- **Charge** : `src/gatling/scala/ma/siblhish/SiblhishApiLoadSimulation.scala`
- **Stress** : `src/gatling/scala/ma/siblhish/SiblhishApiStressSimulation.scala`
- **Défaut** : `src/gatling/scala/ma/siblhish/SiblhishApiSimulation.scala`

Dans chaque fichier vous pouvez modifier :
  - Les URLs et paramètres des requêtes
  - Le `userId` (ex. remplacer `1` par une feuille de données)
  - Le profil de charge : `rampUsersPerSec`, `constantUsersPerSec`, durées
  - Les assertions : `responseTime.percentile4.lt(...)`, `successfulRequests.percent.gt(...)`

---

## Option 2 : k6 (script externe)

[k6](https://k6.io/) permet des scénarios en JavaScript, sans modifier le build Java.

### Installation

- **Windows** : `winget install k6` ou [télécharger k6](https://k6.io/docs/getting-started/installation/)
- **macOS** : `brew install k6`

### Exemple de script

Créer `scripts/load-test.js` :

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081/api/v1';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '20s', target: 10 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  let res = http.get(`${BASE_URL}/home/balance/1`);
  check(res, { 'balance status 200': (r) => r.status === 200 });
  sleep(1);

  res = http.get(`${BASE_URL}/home/transactions/1?dateRange=week`);
  check(res, { 'transactions status 200': (r) => r.status === 200 });
  sleep(1);

  res = http.get(`${BASE_URL}/statistics/all-statistics/1`);
  check(res, { 'statistics status 200': (r) => r.status === 200 });
  sleep(1);
}
```

### Lancer le test

```bash
k6 run scripts/load-test.js
```

Avec une URL personnalisée :

```bash
k6 run -e BASE_URL=https://staging.example.com/api/v1 scripts/load-test.js
```

---

## Bonnes pratiques

1. **Environnement dédié** : faire les tests de charge sur un environnement de préprod/staging, pas sur la prod.
2. **Base de données** : utiliser des données réalistes et un volume proche de la prod.
3. **Montée en charge** : utiliser une rampe (comme dans les exemples) pour éviter un pic brutal.
4. **Seuils** : définir des objectifs (ex. p95 < 2 s, taux d’erreur < 1 %) et les mettre dans les assertions (Gatling) ou `thresholds` (k6).
5. **CI** : vous pouvez appeler `./gradlew gatlingRun` ou `k6 run scripts/load-test.js` dans un pipeline (après déploiement sur un environnement de test).
