import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 50,
  duration: '2h',
  thresholds: { http_req_duration: ['p(95)<2000'] },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';

export default function () {
  const res = http.get(`${BASE_URL}/api/search?q=发票`);
  check(res, { 'status 200': r => r.status === 200 });
  sleep(1);
}
