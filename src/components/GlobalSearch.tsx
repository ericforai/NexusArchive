// Input: React、lucide-react 图标、本地模块 hooks/useGlobalSearchApi
// Output: React 组件 GlobalSearch
// Pos: 业务页面组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useRef } from 'react';
import { Search, FileText, FileCode, Loader2, ArrowRight } from 'lucide-react';
import { useGlobalSearchApi } from '../hooks/useGlobalSearchApi';
import { GlobalSearchDTO } from '../types';
import { toast } from './utils/notificationService';

interface GlobalSearchProps {
    onNavigate?: (item: GlobalSearchDTO) => void;
}

export const GlobalSearch: React.FC<GlobalSearchProps> = ({ onNavigate }) => {
    const { search } = useGlobalSearchApi();
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<GlobalSearchDTO[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isOpen, setIsOpen] = useState(false);
    const [shortcutLabel, setShortcutLabel] = useState('⌘K');
    const inputRef = useRef<HTMLInputElement>(null);
    const wrapperRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        // Detect OS for shortcut label
        const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
        setShortcutLabel(isMac ? '⌘K' : 'Ctrl+K');
    }, []);

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
                    console.error('Search error:', error);
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

    // Keyboard shortcut (Cmd/Ctrl + K)
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
                e.preventDefault();
                inputRef.current?.focus();
            }
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, []);

    // Close on click outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSelect = (item: GlobalSearchDTO) => {
        setIsOpen(false);
        setQuery('');
        if (onNavigate) {
            onNavigate(item);
        } else {
            toast.info(`Selected: ${item.title} (${item.archiveCode})`);
        }
    };

    return (
        <div className="relative group w-full" ref={wrapperRef}>
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
                onFocus={() => { if (results.length > 0) setIsOpen(true); }}
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

            {/* Results Dropdown */}
            {isOpen && results.length > 0 && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2">
                    <div className="max-h-[400px] overflow-y-auto py-2">
                        <div className="px-3 py-2 text-xs font-medium text-slate-500 uppercase tracking-wider">
                            搜索结果 ({results.length})
                        </div>
                        {results.map((item) => (
                            <button
                                key={item.id}
                                onClick={() => handleSelect(item)}
                                className="w-full text-left px-4 py-3 hover:bg-slate-50 flex items-start gap-3 transition-colors group/item border-b border-slate-50 last:border-none"
                            >
                                <div className={`mt-0.5 p-1.5 rounded-lg ${item.matchType === 'ARCHIVE' ? 'bg-blue-50 text-blue-600' : 'bg-amber-50 text-amber-600'}`}>
                                    {item.matchType === 'ARCHIVE' ? <FileText size={18} /> : <FileCode size={18} />}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center justify-between">
                                        <h4 className="text-sm font-medium text-slate-900 truncate pr-2">{item.title}</h4>
                                        <span className="text-xs text-slate-400 font-mono">{item.archiveCode}</span>
                                    </div>
                                    <p className="text-xs text-slate-500 mt-0.5 truncate">
                                        {item.matchType === 'METADATA' ? (
                                            <span className="flex items-center gap-1">
                                                <span className="bg-amber-100 text-amber-700 px-1 rounded text-[10px]">元数据匹配</span>
                                                {item.matchDetail}
                                            </span>
                                        ) : (
                                            item.matchDetail
                                        )}
                                    </p>
                                </div>
                                <ArrowRight size={16} className="text-slate-300 group-hover/item:text-primary-500 transition-colors self-center opacity-0 group-hover/item:opacity-100" />
                            </button>
                        ))}
                    </div>
                    <div className="bg-slate-50 px-4 py-2 border-t border-slate-100 flex justify-between items-center text-xs text-slate-500">
                        <span>按 Enter 键选择第一个结果</span>
                        <span>DigiVoucher Search</span>
                    </div>
                </div>
            )}

            {isOpen && results.length === 0 && !isLoading && query.length > 1 && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-xl border border-slate-100 p-8 text-center z-50">
                    <p className="text-slate-500">未找到相关结果</p>
                </div>
            )}
        </div>
    );
};
