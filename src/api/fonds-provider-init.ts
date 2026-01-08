// Input: useFondsStore, client.types
// Output: Fonds 状态提供器注册
// Pos: API 层 - 初始化全宗状态提供器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 全宗状态提供器注册
 *
 * 此文件负责将 FondsStore 注册为 HTTP 客户端的状态提供器。
 * 放在单独的文件中是为了避免循环依赖：
 * - client.types.ts → useFondsStore.ts → fonds.ts → client.ts
 *
 * 通过将注册逻辑独立出来，打破了循环依赖链。
 */

import { useFondsStore } from '../store/useFondsStore';
import { registerFondsProvider, registerClearFondsCallback } from './client.types';

/**
 * 注册全宗状态提供器
 *
 * 应该在应用初始化时调用（例如在 index.tsx 或 App.tsx 中）
 */
export function registerFondsStateProvider(): void {
  // 注册状态获取器
  registerFondsProvider({
    getState: () => {
      const { currentFonds } = useFondsStore.getState();
      return {
        fondsCode: currentFonds?.fondsCode || null,
      };
    },
    clear: () => {
      const store = useFondsStore.getState();
      store.setFondsList([]);
      store.setCurrentFonds(null);
      console.log('[FondsProvider] Cleared fonds state');
    },
  });

  // 注册清除回调（用于登出时）
  registerClearFondsCallback(() => {
    const store = useFondsStore.getState();
    store.setFondsList([]);
    store.setCurrentFonds(null);
  });

  console.log('[FondsProvider] Registered');
}
