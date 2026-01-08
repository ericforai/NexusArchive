// Input: ref, callback
// Output: click outside detection hook
// Pos: src/components/hooks/ 复用 hooks

import { useEffect } from 'react';

/**
 * Hook to detect clicks outside a component
 */
export const useClickOutside = <T extends HTMLElement>(
    ref: React.RefObject<T | null>,
    callback: () => void
) => {
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (ref.current && !ref.current.contains(event.target as Node)) {
                callback();
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [ref, callback]);
};
