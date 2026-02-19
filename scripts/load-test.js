/**
 * Test de charge k6 pour l’API Siblhish.
 * Prérequis : API démarrée (ex. http://localhost:8081).
 * Lancer : k6 run scripts/load-test.js
 * Avec URL : k6 run -e BASE_URL=https://staging.example.com/api/v1 scripts/load-test.js
 */
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
