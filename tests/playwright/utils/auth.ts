// Input: Playwright
// Output: 脚本模块
// Pos: 测试工具
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { request, APIRequestContext } from '@playwright/test';

const DEFAULT_USER = process.env.PW_USER ?? 'admin';
const DEFAULT_PASS = process.env.PW_PASS ?? 'admin123';

// Token 缓存，避免频繁登录触发限流
const tokenCache = new Map<string, { token: string; expiresAt: number }>();
const TOKEN_CACHE_TTL = 5 * 60 * 1000; // 5 分钟缓存

export interface AuthContext {
  token: string;
  context: APIRequestContext;
}

export async function createAuthContext(
  baseURL: string,
  username = DEFAULT_USER,
  password = DEFAULT_PASS,
): Promise<AuthContext | null> {
  // 检查缓存
  const cacheKey = `${baseURL}:${username}`;
  const cached = tokenCache.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) {
    const authedCtx = await request.newContext({
      baseURL,
      extraHTTPHeaders: { Authorization: `Bearer ${cached.token}` },
    });
    return { token: cached.token, context: authedCtx };
  }

  // 添加延迟，避免限流
  await new Promise(resolve => setTimeout(resolve, 500));

  const loginCtx = await request.newContext({ baseURL });
  const loginRes = await loginCtx.post('/api/auth/login', {
    data: { username, password },
  });
  
  if (!loginRes.ok()) {
    if (loginRes.status() === 429) {
      console.warn('登录限流，等待后重试...');
      await new Promise(resolve => setTimeout(resolve, 2000));
      // 重试一次
      const retryRes = await loginCtx.post('/api/auth/login', {
        data: { username, password },
      });
      if (!retryRes.ok()) {
        console.warn('登录重试失败', retryRes.status());
        return null;
      }
      const loginJson = await retryRes.json();
      const token = loginJson.data?.token ?? loginJson.token ?? loginJson?.access_token;
      if (!token) {
        console.warn('登录重试后缺少 token', loginJson);
        return null;
      }
      // 缓存 token
      tokenCache.set(cacheKey, {
        token,
        expiresAt: Date.now() + TOKEN_CACHE_TTL,
      });
      const authedCtx = await request.newContext({
        baseURL,
        extraHTTPHeaders: { Authorization: `Bearer ${token}` },
      });
      return { token, context: authedCtx };
    }
    console.warn('login failed', loginRes.status());
    return null;
  }
  
  const loginJson = await loginRes.json();
  const token = loginJson.data?.token ?? loginJson.token ?? loginJson?.access_token;
  if (!token) {
    console.warn('login missing token', loginJson);
    return null;
  }

  // 缓存 token
  tokenCache.set(cacheKey, {
    token,
    expiresAt: Date.now() + TOKEN_CACHE_TTL,
  });

  const authedCtx = await request.newContext({
    baseURL,
    extraHTTPHeaders: { Authorization: `Bearer ${token}` },
  });

  return { token, context: authedCtx };
}
