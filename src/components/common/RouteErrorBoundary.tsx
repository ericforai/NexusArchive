// Input: useRouteError from react-router-dom, UI components
// Output: Route Error Boundary Component
// Pos: src/components/common/RouteErrorBoundary.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { useRouteError, isRouteErrorResponse, useNavigate } from 'react-router-dom';
import { AlertTriangle, RefreshCw, Home, ArrowLeft } from 'lucide-react';

export const RouteErrorBoundary: React.FC = () => {
    // const error = useRouteError();
    // const navigate = useNavigate();

    // 临时调试：仅显示静态错误信息，绕过 Hooks
    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
            <div className="bg-white rounded-xl shadow-lg p-8 max-w-md w-full border border-slate-200">
                <h2 className="text-xl font-bold text-rose-600 mb-4">应用遇到严重错误</h2>
                <p className="text-slate-600 mb-6">
                    为了防止崩溃循环，已临时禁用详细错误视图。
                    <br />
                    请检查控制台 (Console) 获取原始报错信息。
                </p>
                <button
                    onClick={() => window.location.href = '/'}
                    className="w-full py-2 px-4 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
                >
                    强制回到首页
                </button>
            </div>
        </div>
    );
};
