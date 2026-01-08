// Input: React、ReactDOM、QueryClientProvider、queryClient 与 App
// Output: 将应用挂载到 #root 的启动逻辑
// Pos: 前端启动入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './queryClient';
import App from './App';
import { RootErrorBoundary } from './components/common/RootErrorBoundary';

// 初始化状态提供器注册（必须在应用启动前调用）
// 放在这里避免循环依赖：client.types.ts → useFondsStore.ts → fonds.ts → client.ts
import { registerAuthStateProvider } from './api/auth-provider-init';
import { registerFondsStateProvider } from './api/fonds-provider-init';

if (typeof window !== 'undefined') {
  try {
    registerAuthStateProvider();
    registerFondsStateProvider();
  } catch (err) {
    console.error('[APP] Failed to register providers:', err);
  }
}

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error("Could not find root element to mount to");
}

// 运行时多实例防御监测
if (typeof window !== 'undefined') {
  const win = window as any;
  win.__REACT_LOG__ = win.__REACT_LOG__ || [];
  win.__REACT_LOG__.push({ version: React.version, time: new Date().toISOString() });
  console.log(`%c[Runtime] React v${React.version} | ReactDOM Ready`, 'color: #7c3aed; font-weight: bold; background: #f3e8ff; padding: 2px 5px; border-radius: 4px;');

  if (win.__REACT_LOG__.length > 1) {
    console.error('%c[Runtime] Multiple React instances detected! This will cause Hooks errors.', 'color: #ef4444; font-weight: bold; font-size: 14px;');
  }
}

// HMR 状态清理：防止热更新时 Context 丢失
if (import.meta.env.DEV && typeof window !== 'undefined') {
  const win = window as any;

  // 检测 HMR 循环 - 如果刷新次数过多，强制清理
  const currentRefreshCount = (win.__HMR_REFRESH_COUNT__ || 0) + 1;
  win.__HMR_REFRESH_COUNT__ = currentRefreshCount;

  if (currentRefreshCount > 10) {
    console.warn('%c[HMR] Excessive refresh detected, resetting...', 'color: #ef4444; font-weight: bold;');
    win.__HMR_REFRESH_COUNT__ = 0;
    // 清理所有可能的缓存
    try {
      if (win.__ANTD_MESSAGE_INSTANCE__) {
        win.__ANTD_MESSAGE_INSTANCE__ = null;
      }
      // 清理 React Query 缓存（如果存在）
      const queryCache = win.__REACT_QUERY_CLIENT__;
      if (queryCache && typeof queryCache.clear === 'function') {
        queryCache.clear();
      }
    } catch (e) {
      console.warn('[HMR] Cache cleanup failed:', e);
    }
  }

  // 清理可能残留的 antd message 实例
  win.__ANTD_MESSAGE_INSTANCE__ = null;

  console.log(`%c[HMR] Refresh count: ${currentRefreshCount}`, 'color: #8b5cf6; font-size: 12px;');
}

console.log('%c[APP] NexusArchive v2.0.6 - BUILD 20260108-HMR-FIX', 'color: #0ea5e9; font-weight: bold; font-size: 14px;');
console.log('%c[APP] Context stability fixes applied', 'color: #10b981; font-weight: bold;');

const root = ReactDOM.createRoot(rootElement);

// 注意：已移除 React.StrictMode
// 原因：React 19 + antd 6.x 在 StrictMode 下存在 HMR 兼容性问题
// 会导致 ConfigProvider 的 resolveDispatcher 返回 null
// 详见：https://github.com/ant-design/ant-design/issues/50000+
root.render(
  <RootErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </RootErrorBoundary>
);