// Input: zustand、TypeScript 类型
// Output: useDrawerStore 与 DrawerTab 类型
// Pos: 前端状态管理
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { create } from 'zustand';

/**
 * 抽屉标签页类型
 */
export type DrawerTab = 'metadata' | 'voucher' | 'attachments';

/**
 * 抽屉状态接口
 */
interface DrawerState {
  // 状态
  isOpen: boolean;
  activeTab: DrawerTab;
  archiveId: string | null;
  expandedMode: boolean;

  // Actions
  open: (id: string) => void;
  close: () => void;
  setActiveTab: (tab: DrawerTab) => void;
  setExpandedMode: (expanded: boolean) => void;
}

/**
 * 抽屉状态 Store
 *
 * 管理档案详情抽屉的全局状态，包括：
 * - 打开/关闭状态
 * - 当前查看的档案ID
 * - 活动标签页
 * - 扩展模式（全屏/半屏）
 */
export const useDrawerStore = create<DrawerState>((set) => ({
  // 初始状态
  isOpen: false,
  activeTab: 'metadata',
  archiveId: null,
  expandedMode: false,

  // 打开抽屉并设置档案ID
  open: (id) => set({ isOpen: true, archiveId: id }),

  // 关闭抽屉并重置状态
  close: () => set({ isOpen: false, archiveId: null, activeTab: 'metadata' }),

  // 设置活动标签页
  setActiveTab: (tab) => set({ activeTab: tab }),

  // 设置扩展模式
  setExpandedMode: (expanded) => set({ expandedMode: expanded }),
}));
