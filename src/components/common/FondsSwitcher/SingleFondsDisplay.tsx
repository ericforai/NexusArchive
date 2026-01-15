// Input: React、类型定义
// Output: 单个全宗纯文本显示组件
// Pos: FondsSwitcher 子组件

import React from 'react';
import type { Fonds } from './types';

interface SingleFondsDisplayProps {
    fonds: Fonds;
}

export const SingleFondsDisplay: React.FC<SingleFondsDisplayProps> = React.memo(({ fonds }) => {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm">
            <span className="text-slate-700 font-medium">{fonds.fondsName}</span>
            <span className="ml-2 text-xs text-slate-400 font-mono">{fonds.fondsCode}</span>
        </div>
    );
});

SingleFondsDisplay.displayName = 'SingleFondsDisplay';
