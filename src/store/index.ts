// Input: 各个 Zustand store
// Output: store 统一导出
// Pos: 状态管理入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Zustand Store 统一导出
 */

export { useAuthStore } from './useAuthStore';
export type { User } from './useAuthStore';

export { useAppStore } from './useAppStore';
export type { AppNotification } from './useAppStore';

export { useThemeStore } from './useThemeStore';
export type { Theme } from './useThemeStore';

export { useFondsStore } from './useFondsStore';

