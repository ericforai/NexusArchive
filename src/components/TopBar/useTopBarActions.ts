// Input: React hooks
// Output: TopBar 操作处理 Hook
// Pos: TopBar 自定义 Hook

import { useCallback } from 'react';
import { toast } from '../../utils/notificationService';

interface UseTopBarActionsOptions {
    onLogout?: () => void;
    onProfileOpen: () => void;
}

/**
 * TopBar 操作处理 Hook
 * 处理用户菜单点击事件
 */
export const useTopBarActions = ({
    onLogout,
    onProfileOpen,
}: UseTopBarActionsOptions) => {
    const handleClick = useCallback(
        (item: string) => {
            if (item === '退出登录' && onLogout) {
                onLogout();
                return;
            }
            if (item === '用户个人资料') {
                onProfileOpen();
                return;
            }
            toast.info(`${item} 功能开发中`);
        },
        [onLogout, onProfileOpen]
    );

    return { handleClick };
};
