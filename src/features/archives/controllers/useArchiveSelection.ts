/**
 * useArchiveSelection - Selection Management Hook
 *
 * Handles item selection state and operations
 */
import { useState, useCallback } from 'react';
import { ControllerSelection } from './types';
import type { GenericRow } from '../../../types';

export function useArchiveSelection(rows: GenericRow[]): ControllerSelection {
    const [selectedIds, setSelectedIds] = useState<string[]>([]);

    const toggle = useCallback((id: string) => {
        setSelectedIds(prev =>
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
        );
    }, []);

    const toggleAll = useCallback(() => {
        if (selectedIds.length === rows.length) {
            setSelectedIds([]);
        } else {
            setSelectedIds(rows.map(r => r.id));
        }
    }, [selectedIds.length, rows]);

    const clear = useCallback(() => {
        setSelectedIds([]);
    }, []);

    return {
        selectedIds,
        toggle,
        toggleAll,
        clear,
        allSelected: rows.length > 0 && selectedIds.length === rows.length,
    };
}
