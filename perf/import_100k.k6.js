import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';
const TOKEN = __ENV.TOKEN || '';

// 自定义指标：成功率
const successRate = new Rate('success_rate');

export const options = {
  vus: 10,
  iterations: 100000, // 10 万条数据，分 10 个虚拟用户执行
  thresholds: {
    http_req_duration: ['p(95)<5000'], // TP95 < 5s
    success_rate: ['rate>0.99'], // 成功率 > 99%
    http_req_failed: ['rate<0.01'], // 失败率 < 1%
  },
};

export default function () {
  const payload = JSON.stringify({
    fondsNo: 'FN-001',
    title: `批量导入测试-${__ITER}`,
    fiscalYear: '2024',
    retentionPeriod: 'permanent',
    orgName: 'QA',
    uniqueBizId: `BATCH-IMPORT-${__ITER}`,
    categoryCode: 'CAT-001',
    status: 'draft',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      ...(TOKEN && { Authorization: `Bearer ${TOKEN}` }),
    },
  };

  const res = http.post(`${BASE_URL}/api/archives`, payload, params);
  const success = check(res, {
    'status 200 or 201': r => r.status === 200 || r.status === 201,
  });
  successRate.add(success);

  // 每 1000 条记录休息一下，避免过载
  if (__ITER % 1000 === 0) {
    sleep(1);
  }
}




