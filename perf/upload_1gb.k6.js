import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';
const file = new SharedArray('file', () => [open('./fixtures/1gb.bin', 'b')]);

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: { http_req_duration: ['p(95)<15000'] },
};

export default function () {
  const res = http.post(
    `${BASE_URL}/api/archive/upload`,
    { file: http.file(file[0], 'bigfile.bin', 'application/octet-stream') },
  );
  check(res, { 'status 200': r => r.status === 200 });
}
