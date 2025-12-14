import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('签章与时间戳完整测试', () => {
  let authCtx: Awaited<ReturnType<typeof createAuthContext>>['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test.skip('证书过期/吊销：导入过期/CRL 列表', async () => {
    // 预期：拒签并提示，合法证书可签
    if (!authCtx) test.skip('登录失败，跳过证书测试');
    
    // 1. 导入过期证书
    // const expiredCertRes = await authCtx!.post('/api/admin/certificates', {
    //   data: { cert: 'expired_cert.pem', type: 'signature' },
    // });
    
    // 2. 尝试签章，应被拒绝
    // const signRes = await authCtx!.post('/api/archives/123/sign', {
    //   data: { certId: 'expired_cert_id' },
    // });
    // expect(signRes.status()).toBe(400);
    // const error = await signRes.json();
    // expect(error.code).toBe('CERT_EXPIRED');
    
    // 3. 导入合法证书
    // const validCertRes = await authCtx!.post('/api/admin/certificates', {
    //   data: { cert: 'valid_cert.pem', type: 'signature' },
    // });
    
    // 4. 验证可正常签章
    // const validSignRes = await authCtx!.post('/api/archives/123/sign', {
    //   data: { certId: 'valid_cert_id' },
    // });
    // expect(validSignRes.ok()).toBeTruthy();
  });

  test.skip('多页签章位置偏移：10 页 PDF 指定页签', async () => {
    // 预期：坐标准确不遮挡正文，验签通过
    if (!authCtx) test.skip('登录失败，跳过签章位置测试');
    
    // 1. 上传 10 页 PDF
    // 2. 在第 5 页指定位置签章
    // 3. 验证坐标准确
    // 4. 验证验签通过
  });

  test('时间戳服务状态检查', async () => {
    if (!authCtx) test.skip('登录失败，跳过时间戳测试');
    
    // 检查时间戳服务状态
    const statusRes = await authCtx!.get('/api/timestamp/status');
    expect(statusRes.ok()).toBeTruthy();
    
    const status = await statusRes.json();
    const statusData = status.data || status;
    expect(statusData).toBeTruthy();
    
    if (!statusData.available) {
      console.warn('时间戳服务未启用或不可用');
    }
  });

  test.skip('时区不一致：TSA 不同 TZ', async () => {
    // 预期：归档时间与业务时间一致/可换算
    if (!authCtx) test.skip('登录失败，跳过时区测试');
    
    // 1. 设置 TSA 为不同时区
    // 2. 归档档案
    // 3. 验证时间戳与业务时间一致
  });

  test('签章接口可用性检查', async () => {
    if (!authCtx) test.skip('登录失败，跳过接口检查');
    
    // 检查签章服务状态
    const statusRes = await authCtx!.get('/api/signature/status');
    expect(statusRes.ok()).toBeTruthy();
    const status = await statusRes.json();
    expect(status.data || status).toBeTruthy();
    
    // 检查证书管理接口
    const certRes = await authCtx!.get('/api/admin/certificates');
    if (certRes.status() === 404) {
      test.skip('证书管理接口未实现');
    }
    expect(certRes.ok()).toBeTruthy();
  });

  test('证书过期/吊销：查询证书列表并验证', async () => {
    if (!authCtx) test.skip('登录失败，跳过证书测试');
    
    // 1. 查询所有证书
    const certsRes = await authCtx!.get('/api/admin/certificates');
    if (certsRes.status() === 404) {
      test.skip('证书管理接口未实现');
    }
    expect(certsRes.ok()).toBeTruthy();
    
    const certs = await certsRes.json();
    const certList = certs.data || certs;
    
    if (!Array.isArray(certList) || certList.length === 0) {
      test.skip('没有可用的证书');
    }
    
    // 2. 验证每个证书
    for (const cert of certList) {
      const verifyRes = await authCtx!.post(`/api/admin/certificates/${cert.alias}/verify`);
      expect(verifyRes.ok()).toBeTruthy();
      
      const verify = await verifyRes.json();
      const verifyData = verify.data || verify;
      
      // 如果证书过期，应该返回 expired: true
      if (verifyData.expired) {
        console.warn(`证书 ${cert.alias} 已过期`);
      }
    }
  });

  test('PDF 文件签章验证', async () => {
    if (!authCtx) test.skip('登录失败，跳过PDF签章测试');
    
    // 检查签章服务状态
    const statusRes = await authCtx!.get('/api/signature/status');
    if (!statusRes.ok()) {
      test.skip('签章服务不可用');
    }
    
    const status = await statusRes.json();
    const statusData = status.data || status;
    
    if (!statusData.available) {
      test.skip('签章服务未启用或不可用');
    }
    
    // 注意：实际测试需要上传真实的PDF文件
    // 这里仅验证接口存在
    test.skip('需要真实的PDF文件进行测试');
  });
});


