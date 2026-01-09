// Input: React、GlobalSearch 组件、类型定义
// Output: 全局搜索区域组件
// Pos: TopBar 子组件

import React from 'react';
import { GlobalSearch } from '../GlobalSearch';
import type { GlobalSearchDTO } from '../../types';

interface SearchSectionProps {
    onNavigate?: (item: GlobalSearchDTO) => void;
}

export const SearchSection: React.FC<SearchSectionProps> = React.memo(({ onNavigate }) => {
    return (
        <div className="flex-1 max-w-2xl mx-4">
            <GlobalSearch onNavigate={onNavigate} />
        </div>
    );
});
