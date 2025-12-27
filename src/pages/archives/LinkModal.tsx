// Input: React, Lucide Icons, Pool API
// Output: LinkModal 组件
// Pos: src/pages/archives/LinkModal.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import { X, Search, Filter, Calendar, DollarSign, Loader2, Link as LinkIcon, FileText, User } from 'lucide-react';
import { poolApi, PoolItem } from '../../api/pool';

interface LinkModalProps {
    isOpen: boolean;
    onClose: () => void;
    selectedIds: string[];
    onToggleSelection: (id: string) => void;
    onConfirm: () => void;
    /** 当前正在关联的凭证对象，用于智能推荐 */
    linkingRow?: any;
}

const DEFAULT_SEARCH_FILTERS = {
    keyword: '',
    minAmount: '',
    maxAmount: '',
    startDate: '',
    endDate: '',
    invoiceNumber: ''
};

/**
 * 高级手动凭证关联模态框
 * 支持按金额、日期、发票号、供应商等维度进行精准搜索
 */
export const LinkModal: React.FC<LinkModalProps> = ({
    isOpen,
    onClose,
    selectedIds,
    onToggleSelection,
    onConfirm,
    linkingRow
}) => {
    const [candidates, setCandidates] = useState<PoolItem[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [showFilters, setShowFilters] = useState(false);

    // 搜索表单状态
    const [searchFilters, setSearchFilters] = useState(DEFAULT_SEARCH_FILTERS);

    const handleSearch = useCallback(async (filters: typeof DEFAULT_SEARCH_FILTERS) => {
        setIsLoading(true);
        // 清理空值，防止后端 Jackson 反序列化失败（特别是日期和金额）
        const sanitizedFilters = Object.keys(filters).reduce((acc: any, key) => {
            const val = filters[key as keyof typeof filters];
            if (val === '' || val === undefined || val === null) {
                acc[key] = null;
            } else if (key === 'minAmount' || key === 'maxAmount') {
                const parsed = parseFloat(val as string);
                acc[key] = isNaN(parsed) ? null : parsed;
            } else {
                acc[key] = val;
            }
            return acc;
        }, {});

        try {
            const results = await poolApi.searchCandidates(sanitizedFilters);
            setCandidates(results);
        } catch (error) {
            console.error('Search failed', error);
        } finally {
            setIsLoading(false);
        }
    }, []);

    // 自动根据 linkingRow 初始化搜索条件 (智能推荐逻辑)
    useEffect(() => {
        if (!isOpen) return;
        if (linkingRow) {
            const rawAmount = typeof linkingRow.amount === 'string'
                ? parseFloat(linkingRow.amount.replace(/[¥,\s]/g, ''))
                : linkingRow.amount;

            const rawDate = linkingRow.date ? linkingRow.date.split(' ')[0] : '';

            const nextFilters = {
                ...DEFAULT_SEARCH_FILTERS,
                minAmount: !isNaN(rawAmount) ? (rawAmount - 1).toFixed(2) : '',
                maxAmount: !isNaN(rawAmount) ? (rawAmount + 1).toFixed(2) : '',
                startDate: rawDate, // 默认当天或范围
                endDate: rawDate
            };

            setSearchFilters(nextFilters);
            handleSearch(nextFilters);
        } else if (isOpen) {
            // 没有任何上下文时，加载最近 20 条
            setSearchFilters(DEFAULT_SEARCH_FILTERS);
            handleSearch(DEFAULT_SEARCH_FILTERS);
        }
    }, [isOpen, linkingRow, handleSearch]);

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-[100] flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-3xl shadow-2xl w-full max-w-4xl border border-slate-100 flex flex-col max-h-[90vh] overflow-hidden">
                {/* Header */}
                <div className="px-8 py-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                    <div>
                        <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                            <LinkIcon className="text-primary-600" size={24} />
                            手动凭证关联
                        </h3>
                        <p className="text-sm text-slate-400 mt-1">查找并关联原始电子凭证到当前会计记录</p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 hover:bg-slate-200 rounded-full transition-colors"
                    >
                        <X size={24} className="text-slate-400" />
                    </button>
                </div>

                {/* Search Bar & Filters */}
                <div className="px-8 py-4 border-b border-slate-50 space-y-4">
                    <div className="flex gap-3">
                        <div className="relative flex-1">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                            <input
                                type="text"
                                placeholder="搜索文件名、发票号或供应商..."
                                className="w-full pl-10 pr-4 py-3 bg-slate-100 border-none rounded-xl focus:ring-2 focus:ring-primary-500 transition-all text-sm"
                                value={searchFilters.keyword}
                                onChange={(e) => setSearchFilters({ ...searchFilters, keyword: e.target.value })}
                                onKeyDown={(e) => e.key === 'Enter' && handleSearch(searchFilters)}
                            />
                        </div>
                        <button
                            onClick={() => setShowFilters(!showFilters)}
                            className={`px-4 py-3 rounded-xl border flex items-center gap-2 transition-all text-sm font-medium ${showFilters ? 'bg-primary-50 border-primary-200 text-primary-600' : 'bg-white border-slate-200 text-slate-600'}`}
                        >
                            <Filter size={18} />
                            高级过滤
                        </button>
                        <button
                            onClick={() => handleSearch(searchFilters)}
                            className="px-6 py-3 bg-primary-600 text-white rounded-xl hover:bg-primary-700 shadow-lg shadow-primary-500/20 transition-all active:scale-95 font-medium"
                        >
                            查询
                        </button>
                    </div>

                    {showFilters && (
                        <div className="grid grid-cols-4 gap-4 p-4 bg-slate-50 rounded-2xl animate-in slide-in-from-top-2 duration-200">
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-500 flex items-center gap-1"><DollarSign size={12} /> 最低金额</label>
                                <input
                                    type="number"
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm"
                                    value={searchFilters.minAmount}
                                    onChange={(e) => setSearchFilters({ ...searchFilters, minAmount: e.target.value })}
                                />
                            </div>
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-500 flex items-center gap-1"><DollarSign size={12} /> 最高金额</label>
                                <input
                                    type="number"
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm"
                                    value={searchFilters.maxAmount}
                                    onChange={(e) => setSearchFilters({ ...searchFilters, maxAmount: e.target.value })}
                                />
                            </div>
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-500 flex items-center gap-1"><Calendar size={12} /> 开始日期</label>
                                <input
                                    type="date"
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm"
                                    value={searchFilters.startDate}
                                    onChange={(e) => setSearchFilters({ ...searchFilters, startDate: e.target.value })}
                                />
                            </div>
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-500 flex items-center gap-1"><Calendar size={12} /> 结束日期</label>
                                <input
                                    type="date"
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm"
                                    value={searchFilters.endDate}
                                    onChange={(e) => setSearchFilters({ ...searchFilters, endDate: e.target.value })}
                                />
                            </div>
                        </div>
                    )}
                </div>

                {/* Candidate List */}
                <div className="flex-1 overflow-y-auto px-8 py-4 bg-slate-50/30">
                    {isLoading ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400 gap-3">
                            <Loader2 size={40} className="animate-spin text-primary-500" />
                            <p className="text-sm font-medium">正在检索匹配的凭证...</p>
                        </div>
                    ) : candidates.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400 gap-4 opacity-60">
                            <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center">
                                <Search size={32} />
                            </div>
                            <div className="text-center">
                                <p className="font-bold text-slate-600">未找到候选凭证</p>
                                <p className="text-xs mt-1">请尝试调整金额、日期或关键字重新搜索</p>
                            </div>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-xs font-bold text-slate-400 tracking-wider uppercase">检索到 {candidates.length} 条记录</span>
                                <span className="text-xs text-slate-400">已选择 {selectedIds.length} 项</span>
                            </div>
                            {candidates.map(c => {
                                const isSelected = selectedIds.includes(c.id);
                                return (
                                    <div
                                        key={c.id}
                                        onClick={() => onToggleSelection(c.id)}
                                        className={`group relative flex items-center p-4 rounded-2xl border transition-all cursor-pointer ${isSelected
                                            ? 'bg-primary-50 border-primary-500 shadow-md ring-1 ring-primary-200'
                                            : 'bg-white border-slate-100 hover:border-primary-300 hover:shadow-sm'
                                            }`}
                                    >
                                        <div className={`p-3 rounded-xl mr-4 transition-colors ${isSelected ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-500 group-hover:bg-primary-100 group-hover:text-primary-600'}`}>
                                            <FileText size={20} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2">
                                                <p className={`font-bold truncate ${isSelected ? 'text-primary-900' : 'text-slate-700'}`}>{c.fileName || c.code}</p>
                                                <span className="text-[10px] px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded font-mono uppercase tracking-tighter">{c.type}</span>
                                            </div>
                                            <div className="flex items-center gap-4 mt-1 text-xs text-slate-400">
                                                <span className="flex items-center gap-1"><Calendar size={12} /> {c.date}</span>
                                                <span className="flex items-center gap-1"><User size={12} /> {c.source || '未知来源'}</span>
                                            </div>
                                        </div>
                                        <div className="text-right">
                                            <p className={`font-mono font-bold text-lg ${isSelected ? 'text-primary-700' : 'text-slate-800'}`}>
                                                ¥ {parseFloat(c.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                                            </p>
                                            <p className="text-[10px] text-slate-400 mt-0.5 font-mono">{c.code}</p>
                                        </div>

                                        {isSelected && (
                                            <div className="absolute -top-2 -right-2 w-6 h-6 bg-primary-600 text-white rounded-full flex items-center justify-center shadow-lg animate-in zoom-in duration-200">
                                                <LinkIcon size={12} />
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="p-8 border-t border-slate-100 bg-slate-50/80 flex justify-between items-center">
                    <div className="text-sm text-slate-500">
                        {selectedIds.length > 0 ? (
                            <span className="flex items-center gap-1 animate-in slide-in-from-left-2 transition-all">
                                <span className="w-2 h-2 bg-primary-500 rounded-full animate-pulse" />
                                准备关联 {selectedIds.length} 个原始凭证
                            </span>
                        ) : '请从列表中选择要关联的项目'}
                    </div>
                    <div className="flex gap-3">
                        <button
                            onClick={onClose}
                            className="px-6 py-3 text-slate-600 font-bold hover:bg-slate-200 rounded-xl transition-colors"
                        >
                            取消
                        </button>
                        <button
                            onClick={onConfirm}
                            disabled={selectedIds.length === 0 || isLoading}
                            className="px-10 py-3 bg-primary-600 text-white font-bold rounded-xl disabled:opacity-50 disabled:cursor-not-allowed shadow-xl shadow-primary-500/30 hover:bg-primary-700 transition-all active:scale-95 flex items-center gap-2"
                        >
                            {isLoading ? <Loader2 size={18} className="animate-spin" /> : <LinkIcon size={18} />}
                            确认关联
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LinkModal;
