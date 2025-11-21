import React, { useState } from 'react';
import { authApi } from '../api/auth';

interface LoginViewProps {
    onLoginSuccess: (user: any) => void;
}

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess }) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const res = await authApi.login({ username, password });
            if (res.code === 200 && res.data?.token) {
                localStorage.setItem('token', res.data.token);
                localStorage.setItem('user', JSON.stringify(res.data.user));
                onLoginSuccess(res.data.user);
            } else {
                setError('登录失败');
            }
        } catch (err) {
            setError('网络错误');
        }
    };

    return (
        <div className="flex items-center justify-center h-screen bg-gray-100">
            <form onSubmit={handleSubmit} className="bg-white p-6 rounded shadow-md w-80">
                <h2 className="text-xl mb-4 text-center">登录</h2>
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
                    className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
                >
                    登录
                </button>
            </form>
        </div>
    );
};
