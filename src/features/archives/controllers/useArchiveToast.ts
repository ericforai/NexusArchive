/**
 * useArchiveToast - Toast UI Management Hook
 *
 * Handles toast notification state
 */
import { useState, useCallback } from 'react';
import { ControllerToast, ControllerUI } from './types';

export function useArchiveToast(): ControllerUI {
    const [toast, setToast] = useState<ControllerToast>({
        visible: false,
        message: '',
        type: 'success'
    });

    const showToast = useCallback((message: string, type: 'success' | 'error' = 'success') => {
        setToast({ visible: true, message, type });
        setTimeout(() => {
            setToast(prev => ({ ...prev, visible: false }));
        }, 3000);
    }, []);

    return {
        toast,
        showToast,
    };
}
