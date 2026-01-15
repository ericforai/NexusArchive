/**
 * useArchiveToast - Toast UI Management Hook
 *
 * Handles toast notification state
 * 修复：使用 useMemo 稳定返回值引用
 */
import { useState, useCallback, useMemo } from 'react';
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

    // 使用 useMemo 稳定返回值引用
    return useMemo(() => ({
        toast,
        showToast,
    }), [toast, showToast]);
}
