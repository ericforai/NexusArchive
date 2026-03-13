// Input: React、官网公开入口组件与根错误边界
// Output: mountPublicApp 官网挂载函数
// Pos: 官网启动引导

import React from 'react';
import type { Root } from 'react-dom/client';
import { RootErrorBoundary } from '../components/common/RootErrorBoundary';
import PublicWebsiteApp from '../PublicWebsiteApp';

export function mountPublicApp(root: Root) {
  root.render(
    <RootErrorBoundary>
      <PublicWebsiteApp />
    </RootErrorBoundary>,
  );
}
