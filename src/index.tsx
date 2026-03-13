// Input: ReactDOM 与按路径分流的启动引导
// Output: 将官网公开站点与后台系统按路径动态挂载到根节点
// Pos: 前端启动入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import ReactDOM from 'react-dom/client';
import type { Root } from 'react-dom/client';
import './index.css';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error("Could not find root element to mount to");
}

const root = ReactDOM.createRoot(rootElement);
const SYSTEM_ROUTE_PREFIX = '/system';
type BootstrapModule = {
  mountPublicApp?: (root: Root) => void;
  mountSystemApp?: (root: Root) => void;
};

const bootstrapLoaders = import.meta.glob<BootstrapModule>('./bootstrap/*.tsx');

async function bootstrap() {
  const isSystemRoute = window.location.pathname.startsWith(SYSTEM_ROUTE_PREFIX);
  const targetModule = isSystemRoute ? './bootstrap/system.tsx' : './bootstrap/public.tsx';
  const loader = bootstrapLoaders[targetModule];

  if (!loader) {
    throw new Error(`Missing bootstrap module: ${targetModule}`);
  }

  const module = await loader();
  if (isSystemRoute) {
    module.mountSystemApp?.(root);
    return;
  }

  module.mountPublicApp?.(root);
}

void bootstrap();
