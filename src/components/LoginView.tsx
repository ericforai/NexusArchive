import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '../api/auth';
import { triggerAuditRefresh } from '../utils/audit';
import { useAuthStore } from '../store';

interface LoginViewProps {
    // 保留可选 props 用于向后兼容（如用于嵌入式登录场景）
    onLoginSuccess?: (user: any) => void;
    onVisitLanding?: () => void;
}

interface LocationState {
    from?: { pathname: string };
}

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess, onVisitLanding }) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    // 使用 AuthStore
    const { login } = useAuthStore();

    // 获取登录后的跳转目标（登录后跳转回原页面）
    const state = location.state as LocationState;
    const from = state?.from?.pathname || '/system';

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            const res = await authApi.login({ username, password });
            if (res.code === 200) {
                // 使用 AuthStore 存储登录状态
                login(res.data.token, {
                    id: res.data.user.id,
                    username: res.data.user.username,
                    realName: res.data.user.fullName,
                    roles: res.data.user.roles || [],
                    permissions: res.data.user.permissions || [],
                });

                // Dispatch login event for other components
                window.dispatchEvent(new Event('login'));

                // 如果有回调则调用（向后兼容）
                if (onLoginSuccess) {
                    onLoginSuccess(res.data.user);
                }
                triggerAuditRefresh();

                // 跳转到原页面或默认门户
                console.log('[LoginView] Login success, navigating to:', from);
                navigate(from, { replace: true });
            } else {
                setError(res.message || '登录失败');
            }
        } catch (err: any) {
            const message = err?.response?.data?.message || '网络错误';
            setError(message);
        }
        setLoading(false);
    };


    const handleVisitLanding = () => {
        if (onVisitLanding) {
            onVisitLanding();
        } else {
            navigate('/');
        }
    };

    return (
        <div className="flex items-center justify-center h-screen bg-gray-100">
            <form onSubmit={handleSubmit} className="bg-white p-6 rounded shadow-md w-80">
                <h2 className="text-xl mb-4 text-center">登录</h2>

                {/* 显示跳转来源提示 */}
                {from !== '/system' && (
                    <p className="text-xs text-slate-500 mb-3 text-center">
                        登录后将跳转到: {from}
                    </p>
                )}

                {error && <p className="text-red-500 text-sm mb-2">{error}</p>}
                <div className="mb-3">
                    <label className="block text-sm mb-1">用户名</label>
                    <input
                        type="text"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        className="w-full border rounded px-2 py-1"
                        required
                    />
                </div>
                <div className="mb-3">
                    <label className="block text-sm mb-1">密码</label>
                    <input
                        type="password"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        className="w-full border rounded px-2 py-1"
                        required
                    />
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 mb-4 disabled:opacity-50"
                >
                    {loading ? '登录中...' : '登录'}
                </button>

                <button
                    type="button"
                    onClick={handleVisitLanding}
                    className="w-full bg-transparent border border-blue-600 text-blue-600 py-2 rounded hover:bg-blue-50"
                >
                    访问产品官网
                </button>
            </form>
        </div>
    );
};

export default LoginView;
