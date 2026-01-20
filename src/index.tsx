// Input: React、ReactDOM、QueryClientProvider、queryClient 与 App
// Output: 将应用挂载到 #root 的启动逻辑 + React 实例诊断
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

// 移除所有自定义运行时检测
console.log('%c[APP] NexusArchive v2.0.7 - CLEAN START', 'color: #0ea5e9; font-weight: bold; font-size: 14px;');

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
