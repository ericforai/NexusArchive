// Input: error, loading, redirectHint, onLogin, onVisitLanding
// Output: 登录表单 UI（深色主题 Enterprise 风格）
// Pos: src/components/auth/LoginCard.tsx
// Design System: DigiVoucher - Enterprise Gateway + Dark Mode

import React, { useState } from 'react';
import { Shield, Lock, User, ArrowRight, Eye, EyeOff } from 'lucide-react';

interface LoginCardProps {
    error?: string | null;
    loading?: boolean;
    redirectHint?: string;
    onLogin: (username: string, password: string) => void;
    onVisitLanding?: () => void;
}

export const LoginCard: React.FC<LoginCardProps> = ({
    error,
    loading = false,
    redirectHint,
    onLogin,
    onVisitLanding
}) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onLogin(username, password);
    };

    return (
        <div className="min-h-screen flex bg-[#0B1120] relative overflow-hidden">
            {/* Background Effects */}
            <div className="absolute inset-0 z-0">
                <div className="absolute inset-0 bg-gradient-to-br from-[#0B1120] via-[#0f172a] to-[#1e293b]" />
                <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-cyan-500/10 rounded-full blur-[120px]" />
                <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-[120px]" />
            </div>

            <div className="relative z-10 flex w-full max-w-6xl mx-auto px-6 items-center justify-center min-h-screen">
                {/* Left Side - Brand */}
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

                {/* Right Side - Login Form */}
                <div className="w-full lg:w-1/2 lg:pl-12">
                    <div className="w-full max-w-md mx-auto">
                        {/* Mobile Logo */}
                        <div className="flex lg:hidden items-center gap-3 mb-8 justify-center">
                            <div className="w-10 h-10 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl flex items-center justify-center">
                                <Shield className="w-6 h-6 text-white" />
                            </div>
                            <span className="text-xl font-bold text-white">DigiVoucher</span>
                        </div>

                        {/* Form Card */}
                        <div className="bg-slate-900/50 backdrop-blur border border-slate-800 rounded-2xl p-8 shadow-2xl">
                            <h2 className="text-2xl font-bold text-white mb-2">欢迎回来</h2>
                            <p className="text-slate-400 mb-8">登录您的账户以继续</p>

                            {redirectHint && (
                                <p className="text-xs text-cyan-400/80 mb-6 p-3 bg-cyan-500/10 border border-cyan-500/20 rounded-lg">
                                    登录后将跳转到: {redirectHint}
                                </p>
                            )}

                            {error && (
                                <p className="text-red-400 text-sm mb-4 p-3 bg-red-500/10 border border-red-500/20 rounded-lg" data-testid="login-error">
                                    {error}
                                </p>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-5">
                                {/* Username Input */}
                                <div>
                                    <label htmlFor="username" className="block text-sm font-medium text-slate-300 mb-2">
                                        用户名
                                    </label>
                                    <div className="relative">
                                        <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                                        <input
                                            id="username"
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

                                {/* Password Input */}
                                <div>
                                    <label htmlFor="password" className="block text-sm font-medium text-slate-300 mb-2">
                                        密码
                                    </label>
                                    <div className="relative">
                                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                                        <input
                                            id="password"
                                            type={showPassword ? 'text' : 'password'}
                                            value={password}
                                            onChange={e => setPassword(e.target.value)}
                                            className="w-full pl-12 pr-12 py-3 bg-slate-800/50 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 focus:ring-2 focus:ring-cyan-500/20 transition-all"
                                            placeholder="请输入密码"
                                            required
                                            data-testid="login-password"
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassword(!showPassword)}
                                            className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors"
                                        >
                                            {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                        </button>
                                    </div>
                                </div>

                                {/* Submit Button */}
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

                            {/* Footer Links */}
                            <div className="mt-6 pt-6 border-t border-slate-800 flex items-center justify-between">
                                {onVisitLanding && (
                                    <button
                                        type="button"
                                        onClick={onVisitLanding}
                                        className="text-sm text-slate-400 hover:text-cyan-400 transition-colors flex items-center gap-1 cursor-pointer"
                                    >
                                        <ArrowRight className="w-3 h-3 rotate-180" />
                                        访问产品官网
                                    </button>
                                )}
                                <span className="text-slate-600">|</span>
                                <button className="text-sm text-slate-400 hover:text-cyan-400 transition-colors cursor-pointer">
                                    忘记密码？
                                </button>
                            </div>
                        </div>

                        {/* Legal Notice */}
                        <p className="mt-6 text-center text-slate-500 text-xs">
                            登录即表示您同意我们的
                            <a href="#" className="text-cyan-400 hover:underline cursor-pointer">服务条款</a>
                            和
                            <a href="#" className="text-cyan-400 hover:underline cursor-pointer">隐私政策</a>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LoginCard;
