import { create } from 'zustand';
import { persist } from 'zustand/middleware';

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
            // 使用自定义 storage 适配器，避免 "Access to storage is not allowed" 错误
            storage: {
                getItem: (name) => {
                    try {
                        if (typeof window !== 'undefined' && window.localStorage) {
                            const value = window.localStorage.getItem(name);
                            return value ? JSON.parse(value) : null;
                        }
                    } catch (e) {
                        console.warn('[ThemeStore] Storage getItem failed:', e);
                    }
                    return null;
                },
                setItem: (name, value) => {
                    try {
                        if (typeof window !== 'undefined' && window.localStorage) {
                            window.localStorage.setItem(name, JSON.stringify(value));
                        }
                    } catch (e) {
                        console.warn('[ThemeStore] Storage setItem failed:', e);
                    }
                },
                removeItem: (name) => {
                    try {
                        if (typeof window !== 'undefined' && window.localStorage) {
                            window.localStorage.removeItem(name);
                        }
                    } catch (e) {
                        console.warn('[ThemeStore] Storage removeItem failed:', e);
                    }
                },
            },
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
