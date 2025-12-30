// Input: React、lucide-react、useFondsStore
// Output: 全宗切换下拉组件 FondsSwitcher
// Pos: 通用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect } from 'react';
import { Building2, ChevronDown, Check, Loader2 } from 'lucide-react';
import { useFondsStore } from '../../store';

/**
 * 全宗切换器组件
 * 
 * 用于顶部导航栏，支持在多个全宗间快速切换
 */
export const FondsSwitcher: React.FC = () => {
    const {
        currentFonds,
        fondsList,
        isLoading,
        loadFondsList,
        setCurrentFonds,
        _hasHydrated
    } = useFondsStore();

    // 初始加载全宗列表
    useEffect(() => {
        if (_hasHydrated) {
            loadFondsList();
        }
    }, [_hasHydrated, loadFondsList]);

    const [isOpen, setIsOpen] = React.useState(false);

    // 如果只有一个全宗或没有全宗，隐藏切换器
    if (fondsList.length <= 1) {
        return null;
    }

    return (
        <div className="relative">
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 transition-colors text-sm"
            >
                <Building2 size={16} className="text-slate-500" />
                <span className="font-medium text-slate-700 max-w-[120px] truncate">
                    {isLoading ? (
                        <Loader2 size={14} className="animate-spin" />
                    ) : (
                        currentFonds?.fondsName || '选择全宗'
                    )}
                </span>
                <ChevronDown
                    size={14}
                    className={`text-slate-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
                />
            </button>

            {/* 下拉菜单 */}
            {isOpen && (
                <>
                    {/* 遮罩 */}
                    <div
                        className="fixed inset-0 z-40"
                        onClick={() => setIsOpen(false)}
                    />

                    {/* 菜单内容 */}
                    <div className="absolute top-full left-0 mt-1 w-64 bg-white rounded-lg shadow-lg border border-slate-200 z-50 py-1 animate-in fade-in slide-in-from-top-2 duration-150">
                        <div className="px-3 py-2 border-b border-slate-100">
                            <p className="text-xs text-slate-500 font-medium">切换全宗</p>
                        </div>
                        <div className="max-h-60 overflow-y-auto">
                            {fondsList.map((fonds) => (
                                <button
                                    key={fonds.id}
                                    onClick={() => {
                                        setCurrentFonds(fonds);
                                        setIsOpen(false);
                                    }}
                                    className={`w-full flex items-center justify-between px-3 py-2 text-left hover:bg-slate-50 transition-colors ${currentFonds?.id === fonds.id ? 'bg-primary-50' : ''
                                        }`}
                                >
                                    <div className="flex-1 min-w-0">
                                        <p className={`text-sm font-medium truncate ${currentFonds?.id === fonds.id ? 'text-primary-600' : 'text-slate-700'
                                            }`}>
                                            {fonds.fondsName}
                                        </p>
                                        <p className="text-xs text-slate-400 font-mono">
                                            {fonds.fondsCode}
                                        </p>
                                    </div>
                                    {currentFonds?.id === fonds.id && (
                                        <Check size={16} className="text-primary-600 flex-shrink-0 ml-2" />
                                    )}
                                </button>
                            ))}
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default FondsSwitcher;
