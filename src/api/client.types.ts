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
  clear?(): void;
  logout?(): void;
}

// 全局状态提供器列表（支持多个提供者）
let authProvider: HttpClientStateProvider | null = null;
let fundsProvider: HttpClientStateProvider | null = null;

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
  fundsProvider = provider;
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
  const fundsState = fundsProvider?.getState() || {};

  return {
    token: authState.token || null,
    fondsCode: fundsState.fondsCode || null,
  };
}

/**
 * 清除全宗状态
 * <p>
 * 用于切换用户时清除上一个用户的全宗选择
 * 通过已注册的提供器调用，避免循环依赖
 * </p>
 */
export function clearFondsState(): void {
  if (fundsProvider) {
    // 添加一个特殊的 clear 方法到提供器
    const provider = fundsProvider as any;
    if (provider.clear) {
      provider.clear();
      console.log('[HttpClient] Fonds state cleared via provider');
    }
  }
}

/**
 * 注册清除全宗状态的回调
 * <p>
 * 由 FondsStore 初始化时调用，用于 logout 时清除状态
 * </p>
 */
let clearFondsCallback: (() => void) | null = null;
export function registerClearFondsCallback(callback: () => void): void {
  clearFondsCallback = callback;
  void clearFondsCallback; // 已注册，通过 provider.clear 调用
  // 同时更新 provider 以支持 clear 方法
  fundsProvider = {
    ...fundsProvider,
    clear: callback,
  } as any;
}

/**
 * 执行登出操作
 * <p>
 * 通过注册的 auth provider 执行登出，同时清除全宗状态
 * </p>
 */
export function performLogout(): void {
  clearFondsState();  // 清除全宗状态（防止缓存了其他用户的全宗选择）
  if (authProvider?.logout) {
    authProvider.logout();
  }
}
