// Input: React、后台系统入口组件、状态提供器与根错误边界
// Output: mountSystemApp 后台系统挂载函数
// Pos: 后台系统启动引导

import React from 'react';
import type { Root } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { RootErrorBoundary } from '../components/common/RootErrorBoundary';
import { queryClient } from '../queryClient';
import App from '../App';
import { registerAuthStateProvider } from '../api/auth-provider-init';
import { registerFondsStateProvider } from '../api/fonds-provider-init';

export function mountSystemApp(root: Root) {
  try {
    registerAuthStateProvider();
    registerFondsStateProvider();
  } catch (err) {
    console.error('[APP] Failed to register providers:', err);
  }

  root.render(
    <RootErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </RootErrorBoundary>,
  );
}
