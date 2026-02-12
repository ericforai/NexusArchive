// Input: React
// Output: 全宗切换器区域组件
// Pos: TopBar 子组件

import React from 'react';
import { FondsSwitcher } from '../common/FondsSwitcher';
import { useFondsStore, useAuthStore } from '../../store';

export const FondsSection: React.FC = () => {
    const {
        currentFonds,
        fondsList,
        isLoading,
        _hasHydrated,
        loadFondsList,
        setCurrentFonds,
    } = useFondsStore();

    const { isAuthenticated } = useAuthStore();

    return (
        <div className="flex items-center gap-4">
            <FondsSwitcher
                currentFonds={currentFonds}
                fondsList={fondsList}
                isLoading={isLoading}
                hasHydrated={_hasHydrated}
                isAuthenticated={isAuthenticated}
                onLoadFondsList={loadFondsList}
                onSetCurrentFonds={setCurrentFonds}
            />
        </div>
    );
};
