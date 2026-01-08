// Input: AuthStore、AppStore、authApi、navigate
// Output: SystemApp 事件处理 Hook
// Pos: src/pages/system/ 事件处理逻辑
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ViewState } from '../../types';
import { useAuthStore, useAppStore } from '../../store';
import { authApi } from '../../api/auth';
import { triggerAuditRefresh } from '../../utils/audit';

/**
 * SystemApp 事件处理 Hook
 * 封装登录、登出、导航等事件处理逻辑
 */
export const useSystemHandlers = () => {
    const navigate = useNavigate();
    const { login, logout: authLogout } = useAuthStore();
    const { navigate: appNavigate } = useAppStore();

    const handleLoginSuccess = useCallback((user: any) => {
        console.log('[SystemApp] Login success, setting authenticated');
        const token = useAuthStore.getState().token;
        login(token || '', user);
        appNavigate(ViewState.PORTAL);
    }, [login, appNavigate]);

    const handleNavigate = useCallback((view: ViewState, subItem: string = '', resourceId: string = '') => {
        appNavigate(view, subItem, resourceId);
    }, [appNavigate]);

    const handleLogout = useCallback(async () => {
        await authApi.logout();
        triggerAuditRefresh();
        authLogout();
        navigate('/system');
    }, [authLogout, navigate]);

    const handleNavigateToPanorama = useCallback((item: any) => {
        handleNavigate(ViewState.PANORAMA, '', item.id);
    }, [handleNavigate]);

    return {
        handleLoginSuccess,
        handleNavigate,
        handleLogout,
        handleNavigateToPanorama,
    };
};
