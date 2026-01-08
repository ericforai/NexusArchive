// Input: keyboard key, callback
// Output: keyboard shortcut hook
// Pos: src/components/hooks/ 复用 hooks

import { useEffect } from 'react';

/**
 * Hook to register a keyboard shortcut (with Cmd/Ctrl modifier)
 */
export const useKeyboardShortcut = (
    key: string,
    callback: () => void,
    requireCtrlOrMeta: boolean = true
) => {
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            const isModifierKey = e.metaKey || e.ctrlKey;
            if (requireCtrlOrMeta ? isModifierKey && e.key === key : e.key === key) {
                e.preventDefault();
                callback();
            }
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [key, callback, requireCtrlOrMeta]);
};
