// Input: 无外部依赖
// Output: HTTP 客户端状态接口
// Pos: API 层 - 定义状态获取接口

/**
 * HTTP 客户端状态接口
 * <p>
 * 用于解耦 client 和 store，避免循环依赖
 * </p>
 */
export interface HttpClientState {
  /** 认证 Token */
  token: string | null;
  /** 当前全宗号 */
  fondsCode: string | null;
}

/**
 * HTTP 客户端状态获取器接口
 * <p>
 * 运行时注册，用于从 store 获取状态
 * </p>
 */
export interface HttpClientStateProvider {
  getState(): Partial<HttpClientState>;
  logout?(): void;
}

// 全局状态提供器列表（支持多个提供者）
let authProvider: HttpClientStateProvider | null = null;
let fondsProvider: HttpClientStateProvider | null = null;

/**
 * 注册认证状态提供器
 * <p>
 * 由 AuthStore 在初始化时调用
 * </p>
 */
export function registerAuthProvider(provider: HttpClientStateProvider): void {
  authProvider = provider;
  console.log('[HttpClient] Auth provider registered');
}

/**
 * 注册全宗状态提供器
 * <p>
 * 由 FondsStore 在初始化时调用
 * </p>
 */
export function registerFondsProvider(provider: HttpClientStateProvider): void {
  fondsProvider = provider;
  console.log('[HttpClient] Fonds provider registered');
}

/**
 * 获取当前 HTTP 状态
 * <p>
 * 合并 auth 和 fonds 状态，如果未注册则返回空状态
 * </p>
 */
export function getHttpClientState(): HttpClientState {
  const authState = authProvider?.getState() || {};
  const fondsState = fondsProvider?.getState() || {};

  return {
    token: authState.token || null,
    fondsCode: fondsState.fondsCode || null,
  };
}

/**
 * 执行登出操作
 * <p>
 * 通过注册的 auth provider 执行登出
 * </p>
 */
export function performLogout(): void {
  if (authProvider?.logout) {
    authProvider.logout();
  }
}
