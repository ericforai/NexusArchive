// Input: React、lucide-react
// Output: 全宗切换下拉组件 FondsSwitcher
// Pos: 通用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect } from 'react';
import { Building2, ChevronDown, Check, Loader2 } from 'lucide-react';

/**
 * 全宗类型（本地定义，避免导入 API 层）
 */
interface Fonds {
    id: string;
    fondsCode: string;
    fondsName: string;
    fondsNo?: string;
    companyName?: string;
    description?: string;
}

/**
 * 全宗切换器组件属性
 */
export interface FondsSwitcherProps {
    /** 当前选中的全宗 */
    currentFonds: Fonds | null;
    /** 全宗列表 */
    fondsList: Fonds[];
    /** 是否正在加载 */
    isLoading: boolean;
    /** 是否已从持久化存储中恢复 */
    hasHydrated: boolean;
    /** 加载全宗列表的回调 */
    onLoadFondsList: () => void;
    /** 设置当前全宗的回调 */
    onSetCurrentFonds: (fonds: Fonds) => void;
}

/**
 * 全宗切换器组件
 *
 * 用于顶部导航栏，支持在多个全宗间快速切换
 *
 * 【架构说明】共享组件不导入 store，通过 props 接收数据和回调
 */
export const FondsSwitcher: React.FC<FondsSwitcherProps> = ({
    currentFonds,
    fondsList,
    isLoading,
    hasHydrated,
    onLoadFondsList,
    onSetCurrentFonds,
}) => {
    // 初始加载全宗列表
    useEffect(() => {
        if (hasHydrated) {
            onLoadFondsList();
        }
    }, [hasHydrated, onLoadFondsList]);

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
                                        onSetCurrentFonds(fonds);
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
