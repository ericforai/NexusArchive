
/**
 * Safe Storage Utility
 * Wraps localStorage to handle "Access to storage is not allowed" errors
 * occurring in restricted environments (e.g., iframes, sandboxes).
 * Falls back to sessionStorage, then in-memory storage if localStorage is inaccessible.
 */

class SafeStorage {
    private memoryStorage: Map<string, string>;
    private storageType: 'localStorage' | 'sessionStorage' | 'memory' = 'memory';
    private checkedStorage = false;

    constructor() {
        this.memoryStorage = new Map<string, string>();
        console.log('[SafeStorage] Initialized');
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

        // Fall back to memory
        this.storageType = 'memory';
        console.warn('[SafeStorage] Using memory storage (all browser storage unavailable)');
    }

    private getStorage(): Storage | null {
        try {
            this.initStorage();
            if (this.storageType === 'localStorage' && typeof window !== 'undefined') {
                return window.localStorage;
            } else if (this.storageType === 'sessionStorage' && typeof window !== 'undefined') {
                return window.sessionStorage;
            }
        } catch (e) {
            console.warn('[SafeStorage] getStorage failed, using memory:', e);
            this.storageType = 'memory';
        }
        return null;
    }

    getItem(key: string): string | null {
        const storage = this.getStorage();
        if (storage) {
            try {
                return storage.getItem(key);
            } catch (e) {
                console.warn(`[SafeStorage] getItem('${key}') failed:`, e);
            }
        }
        return this.memoryStorage.get(key) || null;
    }

    setItem(key: string, value: string): void {
        const storage = this.getStorage();
        if (storage) {
            try {
                storage.setItem(key, value);
                return;
            } catch (e) {
                console.warn(`[SafeStorage] setItem('${key}') failed:`, e);
            }
        }
        this.memoryStorage.set(key, value);
    }

    removeItem(key: string): void {
        const storage = this.getStorage();
        if (storage) {
            try {
                storage.removeItem(key);
            } catch (e) {
                // Ignore
            }
        }
        this.memoryStorage.delete(key);
    }

    clear(): void {
        const storage = this.getStorage();
        if (storage) {
            try {
                storage.clear();
            } catch (e) {
                // Ignore
            }
        }
        this.memoryStorage.clear();
    }
}

export const safeStorage = new SafeStorage();

