import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PHONE_NUMBER = __ENV.PHONE_NUMBER || '010-7777-2214';

export default function () {
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, {
    'health status is 200': (response) => response.status === 200,
  });

  const histories = http.get(
    `${BASE_URL}/api/v1/subscriptions/histories?phoneNumber=${encodeURIComponent(PHONE_NUMBER)}`
  );
  check(histories, {
    'histories status is 200': (response) => response.status === 200,
    'histories response has summary': (response) => response.json('summary') !== undefined,
  });

  sleep(1);
}
