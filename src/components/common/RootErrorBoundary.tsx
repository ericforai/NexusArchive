// Input: React、lucide-react 图标
// Output: React 组件 RootErrorBoundary
// Pos: 根错误边界 - 捕获 React Hooks 和 Context 相关错误

import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertCircle, RefreshCw, Bug } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

/**
 * 根错误边界组件
 *
 * 专门用于捕获 React Hooks 和 Context 相关的错误：
 * - "Cannot read properties of null (reading 'useContext')"
 * - "Invalid hook call" 错误
 * - HMR 导致的 Context 丢失错误
 */
export class RootErrorBoundary extends Component<Props, State> {
  private retryCount = 0;
  private readonly MAX_RETRIES = 3;

  public state: State = {
    hasError: false,
    error: null,
    errorInfo: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error, errorInfo: null };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });

    // 检测特定错误类型
    const isHookError = Boolean(
      error.message?.includes('useContext') ||
      error.message?.includes('Invalid hook call') ||
      error.message?.includes('Hooks can only be called') ||
      error.message?.includes('resolveDispatcher')
    );

    // 诊断信息
    const diagnostics = {
      timestamp: new Date().toISOString(),
      errorMessage: error.message,
      errorStack: error.stack,
      componentStack: errorInfo.componentStack,
      isHookError,
      hmrRefreshCount: (window as any).__HMR_REFRESH_COUNT__,
      reactInstances: (window as any).__REACT_LOG__,
      userAgent: navigator.userAgent,
    };

    // 在控制台输出详细诊断信息
    console.group('%c React Error Caught ', 'background: #ef4444; color: white; font-weight: bold; padding: 2px 5px;');
    console.error('Error:', error);
    console.error('Component Stack:', errorInfo.componentStack);
    console.log('Diagnostics:', diagnostics);
    console.groupEnd();

    // Hooks 错误的特殊处理
    if (isHookError) {
      console.error('%c HOOK ERROR DETECTED ', 'background: #f59e0b; color: white; font-weight: bold;');
      console.error('This is usually caused by:');
      console.error('1. Multiple React instances in the app');
      console.error('2. HMR (Hot Module Replacement) context loss');
      console.error('3. Calling hooks outside of a component');
      console.error('4. antd ConfigProvider compatibility issues');
      console.error('');
      console.error('Please check:');
      console.error('- Browser console for duplicate React warnings');
      console.error('- Network tab for multiple React bundles');
      console.error('- DevTools Profiler for component render issues');
    }
  }

  private handleRetry = () => {
    this.retryCount++;

    if (this.retryCount <= this.MAX_RETRIES) {
      console.log(`[RootErrorBoundary] Retry attempt ${this.retryCount}/${this.MAX_RETRIES}`);
      // 清理可能残留的 antd 状态
      (window as any).__ANTD_MESSAGE_INSTANCE__ = null;
      // 重置状态并重试
      this.setState({ hasError: false, error: null, errorInfo: null });
    } else {
      console.error('[RootErrorBoundary] Max retries reached, forcing page reload');
      window.location.reload();
    }
  };

  private handleReload = () => {
    // 清理所有可能的缓存
    if (typeof window !== 'undefined') {
      (window as any).__ANTD_MESSAGE_INSTANCE__ = null;
      // 清理 sessionStorage 中的 HMR 状态
      try {
        sessionStorage.clear();
      } catch (_e) {
        // Ignore storage errors
      }
    }
    window.location.reload();
  };

  private isHookRelatedError(): boolean {
    const { error } = this.state;
    return Boolean(
      error?.message?.includes('useContext') ||
      error?.message?.includes('Invalid hook call') ||
      error?.message?.includes('Hooks can only be called') ||
      error?.message?.includes('resolveDispatcher') ||
      error?.message?.includes('dispatcher')
    );
  }

  private isMultipleReactError(): boolean {
    const log = (window as any).__REACT_LOG__;
    return log && log.length > 1;
  }

  public render() {
    if (this.state.hasError) {
      const isHookError = this.isHookRelatedError();
      const isMultipleReact = this.isMultipleReactError();
      const { error, errorInfo } = this.state;

      return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900 p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl max-w-2xl w-full p-8 border border-slate-200 dark:border-slate-700">
            {/* 图标 */}
            <div className="flex justify-center mb-6">
              <div className={`w-20 h-20 rounded-full flex items-center justify-center ${isHookError ? 'bg-amber-100 dark:bg-amber-900/30' : 'bg-rose-100 dark:bg-rose-900/30'}`}>
                {isHookError ? (
                  <Bug size={40} className="text-amber-600 dark:text-amber-400" />
                ) : (
                  <AlertCircle size={40} className="text-rose-600 dark:text-rose-400" />
                )}
              </div>
            </div>

            {/* 标题 */}
            <h2 className="text-2xl font-bold text-center text-slate-800 dark:text-white mb-4">
              {isHookError ? 'React Context 错误' : '应用程序错误'}
            </h2>

            {/* 错误描述 */}
            <div className="space-y-4 mb-8">
              {isHookError ? (
                <>
                  <p className="text-center text-slate-600 dark:text-slate-300">
                    检测到 React Hooks 或 Context 相关错误。这通常是由于热更新（HMR）导致的临时状态不一致。
                  </p>
                  {isMultipleReact && (
                    <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg p-4">
                      <p className="text-amber-800 dark:text-amber-200 text-sm font-medium">
                        检测到多个 React 实例！这可能是根本原因。
                      </p>
                      <p className="text-amber-700 dark:text-amber-300 text-sm mt-2">
                        请检查浏览器扩展是否注入了额外的 React 副本。
                      </p>
                    </div>
                  )}
                </>
              ) : (
                <p className="text-center text-slate-600 dark:text-slate-300">
                  应用程序遇到意外错误。
                </p>
              )}

              {/* 开发环境：显示详细错误信息 */}
              {import.meta.env.DEV && error && (
                <details className="mt-4">
                  <summary className="cursor-pointer text-sm font-medium text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white">
                    查看错误详情
                  </summary>
                  <div className="mt-3 p-4 bg-slate-100 dark:bg-slate-900 rounded-lg overflow-auto max-h-60">
                    <pre className="text-xs text-rose-600 dark:text-rose-400 whitespace-pre-wrap">
                      {String(error.message || 'Unknown error')}
                    </pre>
                    {errorInfo?.componentStack && (
                      <pre className="text-xs text-slate-600 dark:text-slate-400 whitespace-pre-wrap mt-2">
                        {errorInfo.componentStack}
                      </pre>
                    )}
                  </div>
                </details>
              )}
            </div>

            {/* 操作按钮 */}
            <div className="flex flex-col sm:flex-row gap-3">
              {isHookError && this.retryCount < this.MAX_RETRIES && (
                <button
                  onClick={this.handleRetry}
                  className="flex-1 py-3 px-4 bg-amber-600 hover:bg-amber-700 text-white rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
                >
                  <RefreshCw size={18} />
                  重试 ({this.MAX_RETRIES - this.retryCount} 次剩余)
                </button>
              )}
              <button
                onClick={this.handleReload}
                className="flex-1 py-3 px-4 bg-primary-600 hover:bg-primary-700 text-white rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
              >
                <RefreshCw size={18} />
                刷新页面
              </button>
            </div>

            {/* 诊断提示 */}
            {import.meta.env.DEV && (
              <div className="mt-6 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                <p className="text-sm text-blue-800 dark:text-blue-200">
                  开发提示：如果此错误频繁出现，请检查控制台中的详细诊断信息。
                </p>
              </div>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
