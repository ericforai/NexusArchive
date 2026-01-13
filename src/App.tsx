// Input: React、react-router-dom 路由创建器、routes 配置、ToastContainer
// Output: 默认导出的 App 路由入口组件（包装 Toast 容器）
// Pos: 前端路由挂载入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 应用根组件
 *
 * 使用 React Router v7 的 createBrowserRouter 实现企业级路由
 */
import React, { FC, useEffect } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { message as antdMessage, notification as antdNotification } from 'antd';
import { routes } from './routes';

// Architecture Defense: Initialize runtime introspection in development
if (import.meta.env.DEV) {
  import('./lib/architectureIntrospection').then(() => {
    // Architecture introspection initialized
  });
}

// 创建路由实例
const router = createBrowserRouter(routes);

const App: FC = () => {
  // 将原 ToastContainer 的 logic 移入 App
  useEffect(() => {
    try {
      // Configure antd global feedback components
      if (antdMessage && typeof antdMessage.config === 'function') {
        antdMessage.config({
          duration: 3,
          maxCount: 3,
        });
      }
      if (antdNotification && typeof antdNotification.config === 'function') {
        antdNotification.config({
          placement: 'topRight',
          maxCount: 3,
        });
      }
    } catch (e) {
      console.warn('[App] Antd config failed:', e);
    }
  }, []);

  return (
    <>
      {/* 文档守卫：开发环境自动监控代码变更，提醒更新文档 */}
      {/* <DocumentationGuardProvider /> */}
      <RouterProvider router={router} />
    </>
  );
};

export default App;