// Input: useRouteError from react-router-dom, UI components
// Output: Route Error Boundary Component
// Pos: src/components/common/RouteErrorBoundary.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { useRouteError, isRouteErrorResponse, useNavigate } from 'react-router-dom';
import { AlertTriangle, RefreshCw, Home, ArrowLeft } from 'lucide-react';

export const RouteErrorBoundary: React.FC = () => {
    const error = useRouteError();
    const navigate = useNavigate();

    // Parse error details
    let errorMessage: string;
    let errorStack: string | undefined;
    let errorStatus: number | undefined;

    if (isRouteErrorResponse(error)) {
        // Router expected errors (404, 401, etc.)
        errorMessage = error.statusText || error.data?.message || 'Page not found or access denied';
        errorStatus = error.status;
    } else if (error instanceof Error) {
        // Uncaught exceptions
        errorMessage = error.message;
        errorStack = error.stack;
    } else if (typeof error === 'string') {
        errorMessage = error;
    } else {
        errorMessage = 'Unknown application error';
        try {
            errorMessage = JSON.stringify(error);
        } catch (e) {
            // Failed to stringify
        }
    }

    // Detect "Objects are not valid as a React child" specifically to give helpful hint
    const isReactChildError = errorMessage.includes('Objects are not valid as a React child');

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900 p-4">
            <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl max-w-lg w-full p-8 border border-slate-200 dark:border-slate-700">
                {/* Icon */}
                <div className="flex justify-center mb-6">
                    <div className="w-20 h-20 rounded-full flex items-center justify-center bg-rose-100 dark:bg-rose-900/30">
                        <AlertTriangle size={40} className="text-rose-600 dark:text-rose-400" />
                    </div>
                </div>

                {/* Title */}
                <h2 className="text-2xl font-bold text-center text-slate-800 dark:text-white mb-2">
                    {errorStatus === 404 ? '页面未找到' : '遇到了问题'}
                </h2>
                <p className="text-center text-slate-500 mb-6">
                    {errorStatus === 404 ? '您访问的页面不存在或已被移除' : '抱歉，应用程序处理您的请求时出现了意外错误'}
                </p>

                {/* Error Details Box */}
                <div className="bg-slate-100 dark:bg-slate-900 rounded-lg p-4 mb-8 overflow-hidden">
                    <p className="text-sm font-mono text-rose-600 dark:text-rose-400 break-words">
                        {errorMessage}
                    </p>
                    {isReactChildError && (
                        <p className="text-xs text-slate-500 mt-2 border-t border-slate-200 pt-2">
                            提示: 这通常是因为代码试图直接渲染对象或数组，而不是字符串或组件。
                        </p>
                    )}
                    {import.meta.env.DEV && errorStack && (
                        <div className="mt-2 pt-2 border-t border-slate-200/50">
                            <details>
                                <summary className="text-xs text-slate-500 cursor-pointer hover:text-slate-700">查看堆栈</summary>
                                <pre className="text-[10px] text-slate-500 mt-1 whitespace-pre-wrap overflow-x-auto">
                                    {errorStack}
                                </pre>
                            </details>
                        </div>
                    )}
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-3">
                    <button
                        onClick={() => window.location.reload()}
                        className="w-full py-3 px-4 bg-primary-600 hover:bg-primary-700 text-white rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
                    >
                        <RefreshCw size={18} />
                        刷新页面
                    </button>

                    <div className="flex gap-3">
                        <button
                            onClick={() => navigate(-1)}
                            className="flex-1 py-3 px-4 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
                        >
                            <ArrowLeft size={18} />
                            返回上一页
                        </button>
                        <button
                            onClick={() => navigate('/')}
                            className="flex-1 py-3 px-4 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
                        >
                            <Home size={18} />
                            回到首页
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};
