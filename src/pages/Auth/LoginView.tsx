// Input: React、react-router-dom 路由、本地模块 api/auth、utils/audit、store
// Output: React 组件 LoginView
// Pos: src/pages/Auth/LoginView.tsx

import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Shield, Lock, User, ArrowRight } from 'lucide-react';
import { authApi } from '../../api/auth';
import { triggerAuditRefresh } from '../../utils/audit';
import { useAuthStore } from '../../store';

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
            console.log('[LoginView] Attempting login for:', username.trim());
            const res = await authApi.login({ username: username.trim(), password: password.trim() });
            console.log('[LoginView] Login response:', res);
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
                console.error('[LoginView] Login failed with code:', res.code, res.message);
                setError(res.message || '登录失败');
            }
        } catch (err: any) {
            console.error('[LoginView] Login error exception:', err);
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
        if (onVisitLanding) {
            onVisitLanding();
        }
        // 产品首页现在在应用内的 '/' 路径
        navigate('/');
    };

    return (
        <div className="min-h-screen flex bg-[#0B1120] relative overflow-hidden">
            {/* 背景装饰 */}
            <div className="absolute inset-0 z-0">
                <div className="absolute inset-0 bg-gradient-to-br from-[#0B1120] via-[#0f172a] to-[#1e293b]" />
                <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-cyan-500/10 rounded-full blur-[120px]" />
                <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-[120px]" />
            </div>

            <div className="relative z-10 flex w-full max-w-6xl mx-auto px-6 items-center justify-center min-h-screen">
                {/* 左侧 - 品牌信息 */}
                <div className="hidden lg:flex flex-col justify-center w-1/2 pr-12">
                    <div className="flex items-center gap-3 mb-8">
                        <div className="w-12 h-12 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl flex items-center justify-center shadow-[0_0_20px_rgba(6,182,212,0.5)]">
                            <Shield className="w-7 h-7 text-white" />
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-white">DigiVoucher</h1>
                            <p className="text-sm text-slate-400">电子会计档案管理系统</p>
                        </div>
                    </div>

                    <h2 className="text-4xl font-bold text-white mb-6 leading-tight">
                        让每一张凭证都成为<br />
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">
                            合法的数字资产
                        </span>
                    </h2>

                    <div className="space-y-4">
                        <div className="flex items-center gap-4 p-4 bg-slate-800/30 border border-slate-700/50 rounded-xl">
                            <Shield className="w-6 h-6 text-cyan-400" />
                            <p className="text-sm text-slate-400">符合 DA/T 94-2022</p>
                        </div>
                        <div className="flex items-center gap-4 p-4 bg-slate-800/30 border border-slate-700/50 rounded-xl">
                            <Lock className="w-6 h-6 text-emerald-400" />
                            <p className="text-sm text-slate-400">国密 SM2/SM3/SM4 加密</p>
                        </div>
                        <div className="flex items-center gap-4 p-4 bg-slate-800/30 border border-slate-700/50 rounded-xl">
                            <Shield className="w-6 h-6 text-purple-400" />
                            <p className="text-sm text-slate-400">四性检测保障</p>
                        </div>
                    </div>

                    <p className="mt-12 text-slate-500 text-sm">© 2025 DigiVoucher. 符合 DA/T 94-2022 标准</p>
                </div>

                {/* 右侧 - 登录表单 */}
                <div className="w-full lg:w-1/2 lg:pl-12">
                    <div className="w-full max-w-md mx-auto">
                        {/* 移动端 Logo */}
                        <div className="flex lg:hidden items-center gap-3 mb-8 justify-center">
                            <div className="w-10 h-10 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl flex items-center justify-center">
                                <Shield className="w-6 h-6 text-white" />
                            </div>
                            <span className="text-xl font-bold text-white">DigiVoucher</span>
                        </div>

                        <div className="bg-slate-900/50 backdrop-blur border border-slate-800 rounded-2xl p-8 shadow-2xl">
                            <h2 className="text-2xl font-bold text-white mb-2">欢迎回来</h2>
                            <p className="text-slate-400 mb-8">登录您的账户以继续</p>

                            {from !== '/system' && (
                                <p className="text-xs text-cyan-400/80 mb-6 p-3 bg-cyan-500/10 border border-cyan-500/20 rounded-lg">
                                    登录后将跳转到: {from}
                                </p>
                            )}

                            {error && (
                                <p className="text-red-400 text-sm mb-4 p-3 bg-red-500/10 border border-red-500/20 rounded-lg" data-testid="login-error">{error}</p>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-5">
                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">用户名</label>
                                    <div className="relative">
                                        <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                                        <input
                                            type="text"
                                            value={username}
                                            onChange={e => setUsername(e.target.value)}
                                            className="w-full pl-12 pr-4 py-3 bg-slate-800/50 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 focus:ring-2 focus:ring-cyan-500/20 transition-all"
                                            placeholder="请输入用户名"
                                            required
                                            data-testid="login-username"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">密码</label>
                                    <div className="relative">
                                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                                        <input
                                            type="password"
                                            value={password}
                                            onChange={e => setPassword(e.target.value)}
                                            className="w-full pl-12 pr-4 py-3 bg-slate-800/50 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 focus:ring-2 focus:ring-cyan-500/20 transition-all"
                                            placeholder="请输入密码"
                                            required
                                            data-testid="login-password"
                                        />
                                    </div>
                                </div>

                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="w-full py-3 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-bold rounded-xl shadow-[0_0_20px_rgba(6,182,212,0.3)] hover:shadow-[0_0_30px_rgba(6,182,212,0.5)] disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
                                    data-testid="login-submit"
                                >
                                    {loading ? '登录中...' : (
                                        <>
                                            登录
                                            <ArrowRight className="w-4 h-4" />
                                        </>
                                    )}
                                </button>
                            </form>

                            <div className="mt-6 pt-6 border-t border-slate-800 flex items-center justify-between">
                                <button
                                    type="button"
                                    onClick={handleVisitLanding}
                                    className="text-sm text-slate-400 hover:text-cyan-400 transition-colors flex items-center gap-1"
                                >
                                    <ArrowRight className="w-3 h-3 rotate-180" />
                                    访问产品官网
                                </button>
                                <span className="text-slate-600">|</span>
                                <button className="text-sm text-slate-400 hover:text-cyan-400 transition-colors">
                                    忘记密码？
                                </button>
                            </div>
                        </div>

                        <p className="mt-6 text-center text-slate-500 text-xs">
                            登录即表示您同意我们的<a href="#" className="text-cyan-400 hover:underline">服务条款</a>和<a href="#" className="text-cyan-400 hover:underline">隐私政策</a>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LoginView;
