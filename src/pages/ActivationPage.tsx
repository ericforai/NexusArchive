// Input: React、LicenseSettings、authApi 与路由导航
// Output: ActivationPage 组件
// Pos: src/pages/ActivationPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { LicenseSettings } from '../components/settings/LicenseSettings';
import { LogOut } from 'lucide-react';
import { authApi } from '../api/auth';
import { useNavigate } from 'react-router-dom';

export const ActivationPage: React.FC = () => {
    const navigate = useNavigate();

    const handleLogout = async () => {
        await authApi.logout();
        navigate('/system/login');
    };

    return (
        <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
            <div className="w-full max-w-4xl space-y-4">
                <div className="bg-white p-8 rounded-2xl shadow-lg border border-red-100">
                    <div className="text-center mb-8">
                        <h1 className="text-2xl font-bold text-slate-800 mb-2">系统需授权激活</h1>
                        <p className="text-slate-500">检测到当前环境未包含有效的 License 许可，或许可已过期/超限。</p>
                        <p className="text-slate-500">请使用管理员账号导入新的 License 证书以继续使用。</p>
                    </div>

                    <LicenseSettings />

                    <div className="mt-8 text-center">
                        <button
                            onClick={handleLogout}
                            className="text-slate-500 hover:text-slate-700 text-sm flex items-center justify-center gap-2 mx-auto"
                        >
                            <LogOut size={14} />
                            退出登录
                        </button>
                    </div>
                </div>

                <div className="text-center text-xs text-slate-400">
                    NexusArchive Electronic Accounting Archive System
                </div>
            </div>
        </div>
    );
};
