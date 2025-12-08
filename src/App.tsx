/**
 * 应用根组件
 * 
 * 使用 React Router v7 的 createBrowserRouter 实现企业级路由
 */
import React from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { routes } from './routes';

// 创建路由实例
const router = createBrowserRouter(routes);

const App: React.FC = () => {
  return <RouterProvider router={router} />;
};

export default App;