// Input: zustand、persist 中间件与 safeStorage、fonds API
// Output: useFondsStore 全宗状态管理
// Pos: 前端全宗全局状态
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { safeStorage } from '../utils/storage';
import { fondsApi, BasFonds } from '../api/fonds';

/**
 * 全宗状态接口
 */
interface FondsState {
    // 状态
    currentFonds: BasFonds | null;
    fondsList: BasFonds[];
    isLoading: boolean;
    _hasHydrated: boolean;

    // Actions
    setCurrentFonds: (fonds: BasFonds | null) => void;
    setFondsList: (list: BasFonds[]) => void;
    loadFondsList: () => Promise<void>;
    setHasHydrated: (hydrated: boolean) => void;

    // 获取当前全宗号（用于 API 请求）
    getCurrentFondsCode: () => string | null;
}

/**
 * 全宗状态 Store
 * 
 * 管理全局全宗切换状态，支持多法人集团场景
 * 持久化到 localStorage
 */
export const useFondsStore = create<FondsState>()(
    persist(
        (set, get) => ({
            // 初始状态
            currentFonds: null,
            fondsList: [],
            isLoading: false,
            _hasHydrated: false,

            // 设置当前全宗
            setCurrentFonds: (fonds) => {
                console.log('[FondsStore] Switch to:', fonds?.fondsCode || 'none');
                set({ currentFonds: fonds });
            },

            // 设置全宗列表
            setFondsList: (list) => {
                set({ fondsList: list });
                // 如果没有当前全宗，自动选择第一个
                const { currentFonds } = get();
                if (!currentFonds && list.length > 0) {
                    set({ currentFonds: list[0] });
                }
                // 如果当前全宗不在列表中，重新选择
                if (currentFonds && !list.find(f => f.id === currentFonds.id)) {
                    set({ currentFonds: list[0] || null });
                }
            },

            // 加载全宗列表
            loadFondsList: async () => {
                set({ isLoading: true });
                try {
                    const res = await fondsApi.list();
                    if (res.code === 200 && res.data) {
                        get().setFondsList(res.data);
                    }
                } catch (error) {
                    console.error('[FondsStore] Failed to load fonds:', error);
                } finally {
                    set({ isLoading: false });
                }
            },

            // 设置 hydration 完成状态
            setHasHydrated: (hydrated) => set({ _hasHydrated: hydrated }),

            // 获取当前全宗号
            getCurrentFondsCode: () => {
                const { currentFonds } = get();
                return currentFonds?.fondsCode || null;
            },
        }),
        {
            name: 'nexus-fonds', // localStorage key
            version: 2,
            partialize: (state) => ({
                // 只持久化当前选中的全宗（列表每次从 API 加载）
                currentFonds: state.currentFonds,
            }),
            storage: createJSONStorage(() => safeStorage),
            onRehydrateStorage: () => {
                console.log('[FondsStore] Starting hydration...');
                return (hydratedState, error) => {
                    if (error) {
                        console.error('[FondsStore] Hydration failed:', error);
                    } else {
                        console.log('[FondsStore] Hydration completed, current:', hydratedState?.currentFonds?.fondsCode || 'none');
                    }
                };
            },
        }
    )
);

// Hydration 完成后设置标志
if (typeof window !== 'undefined') {
    useFondsStore.persist.onFinishHydration(() => {
        console.log('[FondsStore] onFinishHydration called');
        useFondsStore.setState({ _hasHydrated: true });
    });

    if (useFondsStore.persist.hasHydrated()) {
        console.log('[FondsStore] Already hydrated on init');
        useFondsStore.setState({ _hasHydrated: true });
    }
}
