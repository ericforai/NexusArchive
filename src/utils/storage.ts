
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
        // initStorage logic is just for probe. We will try to access it dynamically.
        this.initStorage();
        if (this.storageType === 'localStorage' && typeof window !== 'undefined') {
            return window.localStorage;
        } else if (this.storageType === 'sessionStorage' && typeof window !== 'undefined') {
            return window.sessionStorage;
        }
        return null; // For memory type
    }

    getItem(key: string): string | null {
        try {
            const storage = this.getStorage();
            if (storage) {
                return storage.getItem(key);
            }
        } catch (e) {
            console.warn(`[SafeStorage] getItem('${key}') failed, falling back to memory:`, e);
            // Fallback: If storage access fails (e.g. SecurityError), switch to memory for this session
            this.storageType = 'memory';
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
            console.warn(`[SafeStorage] setItem('${key}') failed, falling back to memory:`, e);
            this.storageType = 'memory';
        }
        this.memoryStorage.set(key, value);
    }

    removeItem(key: string): void {
        try {
            const storage = this.getStorage();
            if (storage) {
                storage.removeItem(key);
                return; // Should return, but also clear memory just in case
            }
        } catch (e) {
            console.warn(`[SafeStorage] removeItem('${key}') failed:`, e);
            this.storageType = 'memory';
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
            this.storageType = 'memory';
        }
        this.memoryStorage.clear();
    }
}

export const safeStorage = new SafeStorage();

