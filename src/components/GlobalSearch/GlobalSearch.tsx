// Input: React、lucide-react 图标、本地模块 hooks/useGlobalSearchApi
// Output: React 组件 GlobalSearch
// Pos: src/components/GlobalSearch/ 主组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useGlobalSearchApi } from '../../hooks/useGlobalSearchApi';
import { GlobalSearchDTO } from '../../types';
import { useKeyboardShortcut } from '../hooks/useKeyboardShortcut';
import { useClickOutside } from '../hooks/useClickOutside';
import { useSearchQuery } from '../hooks/useSearchQuery';
import { SearchInput } from './SearchInput';
import { SearchResults } from './SearchResults';

interface GlobalSearchProps {
    onNavigate?: (item: GlobalSearchDTO) => void;
}

export const GlobalSearch: React.FC<GlobalSearchProps> = ({ onNavigate }) => {
    const { search } = useGlobalSearchApi();
    const inputRef = useRef<HTMLInputElement>(null);
    const wrapperRef = useRef<HTMLDivElement>(null);

    const {
        query,
        setQuery,
        results,
        isLoading,
        isOpen,
        setIsOpen,
    } = useSearchQuery(search);

    // Keyboard shortcut (Cmd/Ctrl + K)
    useKeyboardShortcut('k', () => inputRef.current?.focus(), true);

    // Close on click outside
    useClickOutside<HTMLDivElement>(wrapperRef, () => setIsOpen(false));

    const handleSelect = useCallback((item: GlobalSearchDTO) => {
        setIsOpen(false);
        setQuery('');
        if (onNavigate) {
            onNavigate(item);
        }
    }, [onNavigate, setIsOpen, setQuery]);

    return (
        <div className="relative group w-full" ref={wrapperRef}>
            <SearchInput
                inputRef={inputRef}
                query={query}
                setQuery={setQuery}
                isLoading={isLoading}
                hasResults={results.length > 0}
                onFocus={() => { if (results.length > 0) setIsOpen(true); }}
            />
            <SearchResults
                isOpen={isOpen}
                results={results}
                isLoading={isLoading}
                query={query}
                onSelect={handleSelect}
            />
        </div>
    );
};
