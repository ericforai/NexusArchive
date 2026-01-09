// Input: React、lucide-react 图标、类型定义
// Output: 全宗下拉菜单组件
// Pos: FondsSwitcher 子组件

import React from 'react';
import { Check, ChevronDown, Building2, Loader2 } from 'lucide-react';
import type { Fonds } from './types';

interface FondsDropdownProps {
    isOpen: boolean;
    currentFonds: Fonds | null;
    fondsList: Fonds[];
    isLoading: boolean;
    onToggle: () => void;
    onSelect: (fonds: Fonds) => void;
}

export const FondsDropdown: React.FC<FondsDropdownProps> = React.memo(({
    isOpen,
    currentFonds,
    fondsList,
    isLoading,
    onToggle,
    onSelect,
}) => {
    return (
        <div className="relative">
            <button
                onClick={onToggle}
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

            {/* Dropdown Menu */}
            {isOpen && (
                <>
                    {/* Overlay */}
                    <div
                        className="fixed inset-0 z-40"
                        onClick={() => onToggle()}
                    />

                    {/* Menu Content */}
                    <div className="absolute top-full left-0 mt-1 w-64 bg-white rounded-lg shadow-lg border border-slate-200 z-50 py-1 animate-in fade-in slide-in-from-top-2 duration-150">
                        <div className="px-3 py-2 border-b border-slate-100">
                            <p className="text-xs text-slate-500 font-medium">切换全宗</p>
                        </div>
                        <div className="max-h-60 overflow-y-auto">
                            {fondsList.map((fonds) => (
                                <button
                                    key={fonds.id}
                                    onClick={() => {
                                        onSelect(fonds);
                                        onToggle();
                                    }}
                                    className={`w-full flex items-center justify-between px-3 py-2 text-left hover:bg-slate-50 transition-colors ${
                                        currentFonds?.id === fonds.id ? 'bg-primary-50' : ''
                                    }`}
                                >
                                    <div className="flex-1 min-w-0">
                                        <p
                                            className={`text-sm font-medium truncate ${
                                                currentFonds?.id === fonds.id ? 'text-primary-600' : 'text-slate-700'
                                            }`}
                                        >
                                            {fonds.fondsName}
                                        </p>
                                        <p className="text-xs text-slate-400 font-mono">{fonds.fondsCode}</p>
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
});
