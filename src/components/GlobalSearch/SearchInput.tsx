// Input: input props
// Output: SearchInput component
// Pos: src/components/GlobalSearch/ 子组件

import React, { useEffect, useState } from 'react';
import { Search, Loader2 } from 'lucide-react';

interface SearchInputProps {
    inputRef: React.RefObject<HTMLInputElement>;
    query: string;
    setQuery: (query: string) => void;
    isLoading: boolean;
    hasResults: boolean;
    onFocus: () => void;
}

export const SearchInput: React.FC<SearchInputProps> = ({
    inputRef,
    query,
    setQuery,
    isLoading,
    onFocus,
}) => {
    const [shortcutLabel, setShortcutLabel] = useState('⌘K');

    useEffect(() => {
        const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
        setShortcutLabel(isMac ? '⌘K' : 'Ctrl+K');
    }, []);

    return (
        <>
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Search size={18} className="text-slate-400 group-focus-within:text-primary-500 transition-colors" />
            </div>
            <input
                ref={inputRef}
                type="text"
                className="block w-full pl-10 pr-3 py-2 border border-slate-200 rounded-xl leading-5 bg-slate-50 text-slate-900 placeholder-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all sm:text-sm"
                placeholder="全库检索: 凭证号、摘要、金额、关联单据..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onFocus={onFocus}
            />
            <div className="absolute inset-y-0 right-0 pr-2 flex items-center">
                {isLoading ? (
                    <Loader2 size={16} className="text-slate-400 animate-spin" />
                ) : (
                    <kbd className="inline-flex items-center border border-slate-200 rounded px-2 text-xs font-sans font-medium text-slate-400">
                        {shortcutLabel}
                    </kbd>
                )}
            </div>
        </>
    );
};
