// Input: React、ReactDOM、QueryClientProvider、queryClient 与 App
// Output: 将应用挂载到 #root 的启动逻辑
// Pos: 前端启动入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './queryClient';
import App from './App';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error("Could not find root element to mount to");
}

// 强制刷新标识 - 2026-01-02 15:30
console.log('%c[APP] NexusArchive Frontend v2.0.3 - BUILD 20260102-1530', 'color: #0ea5e9; font-weight: bold; font-size: 14px;');
console.log('%c[APP] ArchiveDetailDrawer should have 3 tabs: 档案详情 | 关联附件 | 凭证预览', 'color: #10b981; font-weight: bold;');

const root = ReactDOM.createRoot(rootElement);
root.render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>
);