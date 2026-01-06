// Input: LoginCard 展示组件
// Output: Login 页面容器（Page 层）
// Pos: src/pages/Auth/Login/index.tsx

/**
 * Login Page Container
 * 
 * 职责：路由集成、认证状态处理、AuthStore 调用。
 * 渲染委托给 LoginCard 展示组件（Presenter）。
 */
import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '../../../api/auth';
import { triggerAuditRefresh } from '../../../utils/audit';
import { useAuthStore } from '../../../store';
import { LoginCard } from '../../../components/auth';

interface LocationState {
    from?: { pathname: string };
}

const LoginPage: React.FC = () => {
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const { login } = useAuthStore();

    const state = location.state as LocationState;
    const from = state?.from?.pathname || '/system';

    const handleLogin = async (username: string, password: string) => {
        setLoading(true);
        setError(null);
        try {
            const res = await authApi.login({ username: username.trim(), password: password.trim() });
            if (res.code === 200) {
                // 存储完整的用户数据
                const { user } = res.data;
                login(res.data.token, {
                    id: user.id,
                    username: user.username,
                    fullName: user.fullName,
                    email: user.email,
                    avatar: user.avatar,
                    departmentId: user.departmentId,
                    status: user.status,
                    roles: user.roles || [],
                    permissions: user.permissions || [],
                    // 新增字段：个人资料展示
                    phone: user.phone,
                    employeeId: user.employeeId,
                    jobTitle: user.jobTitle,
                    orgCode: user.orgCode,
                    lastLoginAt: user.lastLoginAt,
                    createdTime: user.createdTime,
                    roleNames: user.roleNames,
                });
                window.dispatchEvent(new Event('login'));
                triggerAuditRefresh();
                navigate(from, { replace: true });
            } else {
                setError(res.message || '登录失败');
            }
        } catch (err: any) {
            const serverMessage = err?.response?.data?.message;
            const rawMessage = err?.message;
            let message = serverMessage || rawMessage || '网络错误';
            if (err?.code === 'ERR_NETWORK' || rawMessage === 'Network Error') {
                message = '网络错误';
            }
            setError(message);
        }
        setLoading(false);
    };

    const handleVisitLanding = () => {
        navigate('/');
    };

    return (
        <LoginCard
            error={error}
            loading={loading}
            redirectHint={from !== '/system' ? from : undefined}
            onLogin={handleLogin}
            onVisitLanding={handleVisitLanding}
        />
    );
};

export default LoginPage;
