import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';
const TOKEN = __ENV.TOKEN || '';

// 自定义指标
const successRate = new Rate('success_rate');
const retryRate = new Rate('retry_rate');

export const options = {
  vus: 20,
  duration: '30m', // 30 分钟压测
  thresholds: {
    http_req_duration: ['p(95)<10000'], // TP95 < 10s（考虑重试）
    success_rate: ['rate>0.95'], // 成功率 > 95%（允许部分重试失败）
    http_req_failed: ['rate<0.05'], // 失败率 < 5%
  },
};

// 模拟网络抖动：随机延迟和失败
function simulateNetworkJitter() {
  const delay = Math.random() * 2000; // 0-2s 随机延迟
  sleep(delay / 1000);
  return Math.random() > 0.2; // 80% 成功率，模拟 20% 网络抖动
}

export default function () {
  const scenarioId = __ENV.SCENARIO_ID || '1';
  const payload = JSON.stringify({
    voucherNo: `ERP-FLAKY-${__ITER}`,
    amount: Math.random() * 10000,
    taxRate: 0.13,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      ...(TOKEN && { Authorization: `Bearer ${TOKEN}` }),
    },
    timeout: '30s', // 30s 超时
  };

  // 模拟网络抖动
  if (!simulateNetworkJitter()) {
    // 20% 的情况下模拟网络失败，触发重试
    retryRate.add(true);
    return;
  }

  // 发送同步请求
  const res = http.post(`${BASE_URL}/api/erp/scenario/${scenarioId}/sync`, payload, params);
  
  const success = check(res, {
    'status 200 or 201': r => r.status === 200 || r.status === 201,
    'response time < 10s': r => r.timings.duration < 10000,
  });
  successRate.add(success);

  // 验证幂等性：重复发送相同请求
  if (__ITER % 10 === 0) {
    const duplicateRes = http.post(`${BASE_URL}/api/erp/scenario/${scenarioId}/sync`, payload, params);
    check(duplicateRes, {
      'duplicate request handled': r => r.status === 200 || r.status === 409, // 409 表示已存在
    });
  }

  sleep(0.5);
}












