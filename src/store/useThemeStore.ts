import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { safeStorage } from '../utils/storage';

/**
 * 主题类型
 */
export type Theme = 'light' | 'dark' | 'system';

/**
 * 主题状态接口
 */
interface ThemeState {
    theme: Theme;
    resolvedTheme: 'light' | 'dark';

    setTheme: (theme: Theme) => void;
    toggleTheme: () => void;
}

/**
 * 获取系统主题偏好
 */
const getSystemTheme = (): 'light' | 'dark' => {
    if (typeof window !== 'undefined') {
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    return 'light';
};

/**
 * 解析主题
 */
const resolveTheme = (theme: Theme): 'light' | 'dark' => {
    if (theme === 'system') {
        return getSystemTheme();
    }
    return theme;
};

/**
 * 应用主题到 DOM
 */
const applyTheme = (resolvedTheme: 'light' | 'dark') => {
    if (typeof document !== 'undefined') {
        const root = document.documentElement;
        root.classList.remove('light', 'dark');
        root.classList.add(resolvedTheme);
        root.setAttribute('data-theme', resolvedTheme);
    }
};

/**
 * 主题状态 Store
 * 
 * 支持 light/dark/system 三种模式
 * 使用 persist 中间件持久化用户偏好
 */
export const useThemeStore = create<ThemeState>()(
    persist(
        (set, get) => ({
            theme: 'light',
            resolvedTheme: 'light',

            setTheme: (theme) => {
                const resolvedTheme = resolveTheme(theme);
                applyTheme(resolvedTheme);
                set({ theme, resolvedTheme });
            },

            toggleTheme: () => {
                const { theme } = get();
                const nextTheme: Theme = theme === 'light' ? 'dark' : 'light';
                const resolvedTheme = resolveTheme(nextTheme);
                applyTheme(resolvedTheme);
                set({ theme: nextTheme, resolvedTheme });
            },
        }),
        {
            name: 'nexus-theme',
            onRehydrateStorage: () => (state) => {
                // 重新加载后应用主题
                if (state) {
                    const resolvedTheme = resolveTheme(state.theme);
                    applyTheme(resolvedTheme);
                    state.resolvedTheme = resolvedTheme;
                }
            },
            // 使用 safeStorage 统一管理存储访问
            storage: createJSONStorage(() => safeStorage),
        }
    )
);

// 监听系统主题变化
if (typeof window !== 'undefined') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        const state = useThemeStore.getState();
        if (state.theme === 'system') {
            const resolvedTheme = e.matches ? 'dark' : 'light';
            applyTheme(resolvedTheme);
            useThemeStore.setState({ resolvedTheme });
        }
    });
}

// 监听系统主题变化
if (typeof window !== 'undefined') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        const state = useThemeStore.getState();
        if (state.theme === 'system') {
            const resolvedTheme = e.matches ? 'dark' : 'light';
            applyTheme(resolvedTheme);
            useThemeStore.setState({ resolvedTheme });
        }
    });
}
