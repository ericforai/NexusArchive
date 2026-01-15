// Input: React
// Output: 无全宗权限提示组件
// Pos: FondsSwitcher 子组件

import React from 'react';

export const EmptyState: React.FC = React.memo(() => {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm text-slate-400">暂无全宗权限</div>
    );
});

EmptyState.displayName = 'EmptyState';
