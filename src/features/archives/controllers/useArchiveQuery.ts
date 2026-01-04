/**
 * useArchiveQuery - Query State Management Hook
 *
 * Handles search, filter, and organization selection state
 */
import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { adminApi } from '../../../api/admin';
import { ControllerQuery } from './types';

export function useArchiveQuery(): ControllerQuery {
    const location = useLocation();

    // Query state
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [orgFilter, setOrgFilter] = useState('');
    const [subTypeFilter, setSubTypeFilter] = useState(
        new URLSearchParams(location.search).get('type') || ''
    );
    const [orgOptions, setOrgOptions] = useState<{ label: string; value: string }[]>([]);

    // Sync URL query parameters
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        setSubTypeFilter(params.get('type') || '');
    }, [location.search]);

    // Load organization list
    useEffect(() => {
        const loadOrgs = async () => {
            try {
                const res = await adminApi.listOrg();
                if (res.code === 200 && res.data) {
                    setOrgOptions(
                        (res.data as any[]).map((o) => ({
                            label: o.name,
                            value: o.id
                        }))
                    );
                }
            } catch {
                // ignore
            }
        };
        loadOrgs();
    }, []);

    return {
        searchTerm,
        setSearchTerm,
        statusFilter,
        setStatusFilter,
        orgFilter,
        setOrgFilter,
        orgOptions,
        subTypeFilter,
        setSubTypeFilter,
    };
}
