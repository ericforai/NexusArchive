// Input: React、lucide-react 图标
// Output: 加载状态组件
// Pos: FondsSwitcher 子组件

import React from 'react';
import { Loader2 } from 'lucide-react';

export const LoadingState: React.FC = React.memo(() => {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm">
            <Loader2 size={14} className="animate-spin text-slate-400" />
        </div>
    );
});
