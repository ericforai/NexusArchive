// Input: search function
// Output: search query state hook
// Pos: src/components/hooks/ 复用 hooks

import { useState, useEffect } from 'react';
import { GlobalSearchDTO } from '../../types';

/**
 * Hook to manage search query state with debouncing
 */
export const useSearchQuery = (search: (query: string) => Promise<GlobalSearchDTO[]>) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<GlobalSearchDTO[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isOpen, setIsOpen] = useState(false);

    // Debounce search
    useEffect(() => {
        const timer = setTimeout(async () => {
            if (query.trim().length > 1) {
                setIsLoading(true);
                try {
                    const data = await search(query);
                    setResults(data);
                    setIsOpen(true);
                } catch (error) {
                    // Silently handle search errors
                } finally {
                    setIsLoading(false);
                }
            } else {
                setResults([]);
                setIsOpen(false);
            }
        }, 300);

        return () => clearTimeout(timer);
    }, [query, search]);

    return {
        query,
        setQuery,
        results,
        isLoading,
        isOpen,
        setIsOpen,
    };
};
