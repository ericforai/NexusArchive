// Input: useAuthStore, client.types
// Output: Auth 状态提供器注册
// Pos: API 层 - 初始化认证状态提供器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 认证状态提供器注册
 *
 * 此文件负责将 AuthStore 注册为 HTTP 客户端的状态提供器。
 * 放在单独的文件中是为了避免循环依赖。
 */

import { useAuthStore } from '../store/useAuthStore';
import { registerAuthProvider } from './client.types';

/**
 * 注册认证状态提供器
 *
 * 应该在应用初始化时调用（例如在 index.tsx 或 App.tsx 中）
 */
export function registerAuthStateProvider(): void {
  registerAuthProvider({
    getState: () => {
      const { token } = useAuthStore.getState();
      return { token };
    },
    logout: () => {
      useAuthStore.getState().logout();
    },
  });
}
