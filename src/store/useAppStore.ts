// Input: zustand 与 ViewState 类型
// Output: useAppStore 与 AppNotification
// Pos: 前端全局 UI 状态管理
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { create } from 'zustand';
import { ViewState } from '../types';

/**
 * 通知接口
 */
export interface AppNotification {
    id: string;
    title: string;
    message?: string;
    type: 'info' | 'success' | 'warning' | 'error';
    time: string;
    read?: boolean;
}

/**
 * 应用全局状态接口
 */
interface AppState {
    // 侧边栏状态
    sidebarCollapsed: boolean;
    toggleSidebar: () => void;
    setSidebarCollapsed: (collapsed: boolean) => void;

    // 当前视图
    activeView: ViewState;
    activeSubItem: string;
    activeResourceId: string;
    setActiveView: (view: ViewState) => void;
    setActiveSubItem: (subItem: string) => void;
    setActiveResourceId: (resourceId: string) => void;
    navigate: (view: ViewState, subItem?: string, resourceId?: string) => void;

    // 全局加载状态
    globalLoading: boolean;
    setGlobalLoading: (loading: boolean) => void;

    // 通知
    notifications: AppNotification[];
    addNotification: (notification: Omit<AppNotification, 'id' | 'time'>) => void;
    removeNotification: (id: string) => void;
    markNotificationRead: (id: string) => void;
    clearNotifications: () => void;
}

/**
 * 生成唯一 ID
 */
const generateId = () => `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

/**
 * 应用状态 Store
 * 
 * 管理视图导航、侧边栏、全局加载状态和通知
 */
export const useAppStore = create<AppState>((set, get) => ({
    // 侧边栏
    sidebarCollapsed: false,
    toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
    setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),

    // 视图导航
    activeView: ViewState.PORTAL,
    activeSubItem: '',
    activeResourceId: '',

    setActiveView: (view) => set({ activeView: view }),
    setActiveSubItem: (subItem) => set({ activeSubItem: subItem }),
    setActiveResourceId: (resourceId) => set({ activeResourceId: resourceId }),

    navigate: (view, subItem = '', resourceId = '') => {
        set({
            activeView: view,
            activeSubItem: subItem,
            activeResourceId: resourceId,
        });
    },

    // 全局加载
    globalLoading: false,
    setGlobalLoading: (loading) => set({ globalLoading: loading }),

    // 通知
    notifications: [],

    addNotification: (notification) => {
        const newNotification: AppNotification = {
            ...notification,
            id: generateId(),
            time: new Date().toISOString(),
            read: false,
        };
        set((s) => ({
            notifications: [newNotification, ...s.notifications].slice(0, 50), // 最多保留50条
        }));
    },

    removeNotification: (id) => {
        set((s) => ({
            notifications: s.notifications.filter((n) => n.id !== id),
        }));
    },

    markNotificationRead: (id) => {
        set((s) => ({
            notifications: s.notifications.map((n) =>
                n.id === id ? { ...n, read: true } : n
            ),
        }));
    },

    clearNotifications: () => set({ notifications: [] }),
}));
