// Input: useLocation、AppStore、PATH_TO_VIEW、PATH_TO_SUBITEM
// Output: URL 同步 Hook
// Pos: src/pages/system/ 路由同步逻辑
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAppStore } from '../../store';
import { PATH_TO_VIEW, PATH_TO_SUBITEM } from './viewConstants';

/**
 * URL 同步 Hook
 * 监听 URL 变化，同步到 AppStore
 */
export const useUrlSync = () => {
    const location = useLocation();
    const { activeView, activeSubItem, navigate: appNavigate } = useAppStore();

    useEffect(() => {
        const path = location.pathname;

        // 精确匹配子菜单
        if (PATH_TO_SUBITEM[path]) {
            const subItem = PATH_TO_SUBITEM[path];
            // 找到父级 ViewState
            for (const [prefix, view] of Object.entries(PATH_TO_VIEW)) {
                if (path.startsWith(prefix)) {
                    if (activeView !== view || activeSubItem !== subItem) {
                        appNavigate(view, subItem, '');
                    }
                    return;
                }
            }
        }

        // 匹配顶级路由
        for (const [prefix, view] of Object.entries(PATH_TO_VIEW)) {
            if (path.startsWith(prefix)) {
                if (activeView !== view) {
                    appNavigate(view, '', '');
                }
                return;
            }
        }
    }, [location.pathname, activeView, activeSubItem, appNavigate]);
};
