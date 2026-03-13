// Input: React、react-router-dom 与官网公开路由
// Output: PublicWebsiteApp 官网公开入口组件
// Pos: 官网公开路由挂载入口

import React from 'react';
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';
import { publicRoutes } from './public-site/routes';

const router = createBrowserRouter([
  ...publicRoutes,
  { path: '*', element: <Navigate to="/" replace /> },
]);

const PublicWebsiteApp: React.FC = () => {
  return <RouterProvider router={router} />;
};

export default PublicWebsiteApp;
