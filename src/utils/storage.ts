// Input: 浏览器存储与 cookie 接口
// Output: safeStorage 实例与存储封装
// Pos: 前端存储兼容层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。


/**
 * Safe Storage Utility
 * Wraps localStorage to handle "Access to storage is not allowed" errors
 * occurring in restricted environments (e.g., iframes, sandboxes).
 * Falls back to sessionStorage, then in-memory storage if localStorage is inaccessible.
 */

class SafeStorage {
    private memoryStorage: Map<string, string>;
    private storageType: 'localStorage' | 'sessionStorage' | 'memory' | 'cookie' = 'memory';
    private checkedStorage = false;

    constructor() {
        this.memoryStorage = new Map<string, string>();
        // 提前初始化，确保在任何访问前确定可用的存储类型
        try {
            this.initStorage();
        } catch (e) {
            console.warn('[SafeStorage] Init failed, using memory storage:', e);
            this.storageType = 'memory';
            this.checkedStorage = true;
        }
        console.log('[SafeStorage] Initialized with:', this.storageType);
    }

    private initStorage(): void {
        if (this.checkedStorage) return;
        this.checkedStorage = true;

        // Try localStorage first
        try {
            if (typeof window !== 'undefined' && window.localStorage) {
                const testKey = '__test_local__';
                window.localStorage.setItem(testKey, testKey);
                window.localStorage.removeItem(testKey);
                this.storageType = 'localStorage';
                console.log('[SafeStorage] Using localStorage');
                return;
            }
        } catch (e) {
            console.warn('[SafeStorage] localStorage not available:', e);
        }

        // Try sessionStorage as fallback
        try {
            if (typeof window !== 'undefined' && window.sessionStorage) {
                const testKey = '__test_session__';
                window.sessionStorage.setItem(testKey, testKey);
                window.sessionStorage.removeItem(testKey);
                this.storageType = 'sessionStorage';
                console.log('[SafeStorage] Using sessionStorage (localStorage unavailable)');
                return;
            }
        } catch (e) {
            console.warn('[SafeStorage] sessionStorage not available:', e);
        }

        // Try Cookie as fallback (for persistence when Storage API is blocked)
        if (typeof document !== 'undefined') {
            try {
                document.cookie = "test_cookie=1; SameSite=Strict";
                if (document.cookie.indexOf('test_cookie=') !== -1) {
                    document.cookie = "test_cookie=1; expires=Thu, 01 Jan 1970 00:00:00 UTC; SameSite=Strict";
                    this.storageType = 'cookie';
                    console.log('[SafeStorage] Using document.cookie (Storage API unavailable)');
                    return;
                }
            } catch (e) {
                console.warn('[SafeStorage] Cookie not available:', e);
            }
        }

        // Fall back to memory
        this.storageType = 'memory';
        console.warn('[SafeStorage] Using memory storage (all browser storage unavailable)');
    }

    private getStorage(): Storage | null {
        // initStorage logic is just for probe. We will try to access it dynamically.
        this.initStorage();
        if (this.storageType === 'localStorage' && typeof window !== 'undefined') {
            try {
                return window.localStorage;
            } catch (e) {
                console.warn('[SafeStorage] localStorage access blocked:', e);
                return null;
            }
        } else if (this.storageType === 'sessionStorage' && typeof window !== 'undefined') {
            try {
                return window.sessionStorage;
            } catch (e) {
                console.warn('[SafeStorage] sessionStorage access blocked:', e);
                return null;
            }
        }
        return null; // For memory or cookie type
    }

    // Cookie Helpers
    private getCookie(name: string): string | null {
        if (typeof document === 'undefined') return null;
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
        return null;
    }

    private setCookie(name: string, value: string, days = 7): void {
        if (typeof document === 'undefined') return;
        const d = new Date();
        d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));
        const expires = "expires=" + d.toUTCString();
        document.cookie = name + "=" + value + ";" + expires + ";path=/;SameSite=Strict";
    }

    private removeCookie(name: string): void {
        if (typeof document === 'undefined') return;
        document.cookie = name + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;SameSite=Strict";
    }

    getItem(key: string): string | null {
        try {
            const storage = this.getStorage();
            if (storage) {
                return storage.getItem(key);
            }
        } catch (e) {
            console.warn(`[SafeStorage] getItem('${key}') failed, forcing fallback to Cookie:`, e);
            if (this.storageType === 'localStorage' || this.storageType === 'sessionStorage') {
                this.storageType = 'cookie';
            }
        }

        if (this.storageType === 'cookie') {
            return this.getCookie(key);
        }

        return this.memoryStorage.get(key) || null;
    }

    setItem(key: string, value: string): void {
        try {
            const storage = this.getStorage();
            if (storage) {
                storage.setItem(key, value);
                return;
            }
        } catch (e) {
            console.warn(`[SafeStorage] setItem('${key}') failed, forcing fallback to Cookie:`, e);
            if (this.storageType === 'localStorage' || this.storageType === 'sessionStorage') {
                this.storageType = 'cookie';
            }
        }

        if (this.storageType === 'cookie') {
            this.setCookie(key, value);
            // Also update memory for consistency/perf? optional.
            // But let's just use cookie.
            return;
        }

        this.memoryStorage.set(key, value);
    }

    removeItem(key: string): void {
        try {
            const storage = this.getStorage();
            if (storage) {
                storage.removeItem(key);
                return;
            }
        } catch (e) {
            console.warn(`[SafeStorage] removeItem('${key}') failed:`, e);
            if (this.storageType === 'localStorage' || this.storageType === 'sessionStorage') {
                this.storageType = 'cookie';
            }
        }

        if (this.storageType === 'cookie') {
            this.removeCookie(key);
            return;
        }

        this.memoryStorage.delete(key);
    }

    clear(): void {
        try {
            const storage = this.getStorage();
            if (storage) {
                storage.clear();
            }
        } catch (e) {
            console.warn('[SafeStorage] clear failed:', e);
            this.initStorage();
        }
        // Easy clear for cookies is hard (need to know all keys). 
        // We might just leave them or implement a prefix scan if needed.
        // For now, clear memory.
        this.memoryStorage.clear();
    }
}

export const safeStorage = new SafeStorage();

