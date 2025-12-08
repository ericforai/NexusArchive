import { safeStorage } from './storage';

export const isDemoMode = (): boolean => {
  try {
    const localFlag = safeStorage.getItem('demoMode');
    if (localFlag !== null) {
      return localFlag === 'true';
    }
  } catch (e) {
    // ignore storage access issues
  }
  return import.meta.env.VITE_DEMO_MODE === 'true';
};
