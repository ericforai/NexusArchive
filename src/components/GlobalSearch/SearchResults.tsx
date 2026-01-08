// Input: results props
// Output: SearchResults component
// Pos: src/components/GlobalSearch/ 子组件

import React from 'react';
import { FileText, FileCode, ArrowRight } from 'lucide-react';
import { GlobalSearchDTO } from '../../types';

interface SearchResultsProps {
    isOpen: boolean;
    results: GlobalSearchDTO[];
    isLoading: boolean;
    query: string;
    onSelect: (item: GlobalSearchDTO) => void;
}

const ResultItem: React.FC<{
    item: GlobalSearchDTO;
    onSelect: () => void;
}> = ({ item, onSelect }) => {
    const iconBg = item.matchType === 'ARCHIVE' ? 'bg-blue-50 text-blue-600' : 'bg-amber-50 text-amber-600';
    const Icon = item.matchType === 'ARCHIVE' ? FileText : FileCode;

    return (
        <button
            onClick={onSelect}
            className="w-full text-left px-4 py-3 hover:bg-slate-50 flex items-start gap-3 transition-colors group/item border-b border-slate-50 last:border-none"
        >
            <div className={`mt-0.5 p-1.5 rounded-lg ${iconBg}`}>
                <Icon size={18} />
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
    );
};

export const SearchResults: React.FC<SearchResultsProps> = ({
    isOpen,
    results,
    isLoading,
    query,
    onSelect,
}) => {
    if (!isOpen) return null;

    // Empty state
    if (results.length === 0 && !isLoading && query.length > 1) {
        return (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-xl border border-slate-100 p-8 text-center z-50">
                <p className="text-slate-500">未找到相关结果</p>
            </div>
        );
    }

    // Results list
    if (results.length > 0) {
        return (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2">
                <div className="max-h-[400px] overflow-y-auto py-2">
                    <div className="px-3 py-2 text-xs font-medium text-slate-500 uppercase tracking-wider">
                        搜索结果 ({results.length})
                    </div>
                    {results.map((item) => (
                        <ResultItem
                            key={item.id}
                            item={item}
                            onSelect={() => onSelect(item)}
                        />
                    ))}
                </div>
                <div className="bg-slate-50 px-4 py-2 border-t border-slate-100 flex justify-between items-center text-xs text-slate-500">
                    <span>按 Enter 键选择第一个结果</span>
                    <span>DigiVoucher Search</span>
                </div>
            </div>
        );
    }

    return null;
};
