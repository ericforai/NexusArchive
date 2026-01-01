// Input: React、lucide-react 图标
// Output: React 组件 ErrorBoundary
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
        error: null,
    };

    public static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        // 安全地记录错误信息，避免对象转换错误
        try {
            console.error('Uncaught error:', error?.message || error, errorInfo?.componentStack || errorInfo);
        } catch (e) {
            // 如果记录错误本身失败，使用更安全的方式
            console.error('Error occurred:', String(error?.message || 'Unknown error'));
            if (errorInfo?.componentStack) {
                console.error('Component stack:', errorInfo.componentStack);
            }
        }
    }

    private handleReload = () => {
        window.location.reload();
    };

    public render() {
        if (this.state.hasError) {
            const isChunkError = this.state.error?.message?.includes('Dynamically imported module') ||
                this.state.error?.message?.includes('Loading chunk');

            return (
                <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900 p-4">
                    <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl max-w-md w-full p-8 text-center border mr-slate-100 dark:border-slate-700">
                        <div className="w-16 h-16 bg-rose-100 dark:bg-rose-900/30 rounded-full flex items-center justify-center mx-auto mb-6">
                            <AlertCircle size={32} className="text-rose-600 dark:text-rose-400" />
                        </div>

                        <h2 className="text-2xl font-bold text-slate-800 dark:text-white mb-2">
                            {isChunkError ? '版本更新提示' : '出错了'}
                        </h2>

                        <p className="text-slate-600 dark:text-slate-300 mb-8 leading-relaxed">
                            {isChunkError
                                ? '系统检测到新版本发布，请刷新页面以加载最新内容。'
                                : '应用程序遇到意外错误，我们正在努力修复。'}
                            {process.env.NODE_ENV === 'development' && this.state.error && (
                                <code className="block mt-4 p-3 bg-slate-100 dark:bg-slate-900 rounded-lg text-xs text-left overflow-auto max-h-32 text-rose-600">
                                    {(() => {
                                        try {
                                            return this.state.error?.message || String(this.state.error || 'Unknown error');
                                        } catch {
                                            return 'Error details unavailable';
                                        }
                                    })()}
                                </code>
                            )}
                        </p>

                        <button
                            onClick={this.handleReload}
                            className="w-full py-3 px-4 bg-primary-600 hover:bg-primary-700 text-white rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
                        >
                            <RefreshCw size={18} />
                            {isChunkError ? '刷新页面' : '重试'}
                        </button>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}
