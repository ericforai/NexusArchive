import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 10, duration: '72h' };
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';

export default function () {
  const payload = JSON.stringify({ voucherNo: `SOAK-${__ITER}`, content: '...base64...' });
  const res = http.post(`${BASE_URL}/api/archive`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'status 200': r => r.status === 200 });
  sleep(0.5);
}
