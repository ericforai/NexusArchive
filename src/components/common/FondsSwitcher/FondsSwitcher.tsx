// Input: React、lucide-react
// Output: 全宗切换下拉组件 FondsSwitcher（自适应：单个全宗显示纯文本，多个全宗显示下拉）
// Pos: 通用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { LoadingState } from './LoadingState';
import { EmptyState } from './EmptyState';
import { SingleFondsDisplay } from './SingleFondsDisplay';
import { FondsDropdown } from './FondsDropdown';
import type { Fonds } from './types';

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
    // Initial load of fonds list
    useEffect(() => {
        if (hasHydrated) {
            onLoadFondsList();
        }
    }, [hasHydrated, onLoadFondsList]);

    const [isOpen, setIsOpen] = useState(false);

    const handleToggle = () => setIsOpen((prev) => !prev);

    // Loading state
    if (isLoading && fondsList.length === 0) {
        return <LoadingState />;
    }

    // No fonds permission
    if (fondsList.length === 0 && hasHydrated) {
        return <EmptyState />;
    }

    // Single fonds: plain text display
    if (fondsList.length === 1) {
        return <SingleFondsDisplay fonds={fondsList[0]} />;
    }

    // Multiple fonds: dropdown
    return (
        <FondsDropdown
            isOpen={isOpen}
            currentFonds={currentFonds}
            fondsList={fondsList}
            isLoading={isLoading}
            onToggle={handleToggle}
            onSelect={onSetCurrentFonds}
        />
    );
};

export default FondsSwitcher;
