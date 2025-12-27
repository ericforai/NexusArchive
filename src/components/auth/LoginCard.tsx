// Input: React、Props 数据
// Output: 登录表单 UI (纯展示组件)
// Pos: src/components/auth/LoginCard.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * LoginCard Presenter Component
 * 
 * 职责：纯 UI 渲染登录表单，接收回调并触发事件。
 */
import React, { useState } from 'react';

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

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onLogin(username, password);
    };

    return (
        <div className="flex items-center justify-center h-screen bg-gray-100">
            <form onSubmit={handleSubmit} className="bg-white p-6 rounded shadow-md w-80">
                <h2 className="text-xl mb-4 text-center">登录</h2>

                {redirectHint && (
                    <p className="text-xs text-slate-500 mb-3 text-center">
                        登录后将跳转到: {redirectHint}
                    </p>
                )}

                {error && <p className="text-red-500 text-sm mb-2" data-testid="login-error">{error}</p>}
                <div className="mb-3">
                    <label className="block text-sm mb-1">用户名</label>
                    <input
                        type="text"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        className="w-full border rounded px-2 py-1"
                        required
                        data-testid="login-username"
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
                        data-testid="login-password"
                    />
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 mb-4 disabled:opacity-50"
                    data-testid="login-submit"
                >
                    {loading ? '登录中...' : '登录'}
                </button>
                {onVisitLanding && (
                    <button
                        type="button"
                        onClick={onVisitLanding}
                        className="w-full border border-blue-600 text-blue-600 py-2 rounded hover:bg-blue-50"
                    >
                        访问产品官网
                    </button>
                )}
            </form>
        </div>
    );
};

export default LoginCard;
