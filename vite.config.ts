// Input: Node.js 标准库、Vite、Vite React 插件
// Output: Vite 构建配置
// Pos: 构建/测试配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  const apiBaseUrl = env.VITE_API_BASE_URL || 'http://localhost:19090/api';
  const apiTarget = apiBaseUrl.replace(/\/api\/?$/, '');
  return {
    server: {
      port: 15175,
      host: '0.0.0.0',
      strictPort: true,
      hmr: {
        protocol: 'ws',
        host: '127.0.0.1',
        port: 15175,
      },
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        }
      }
    },
    plugins: [
      react(),
      // 移除任何外部注入的 importmap（如 AI Studio 插件），防止 React 版本冲突
      {
        name: 'remove-external-importmap',
        transformIndexHtml(html: string) {
          // 移除指向 aistudiocdn 的 importmap
          return html.replace(/<script type="importmap">[\s\S]*?aistudiocdn[\s\S]*?<\/script>/gi, '');
        },
      },
      // Architecture Defense: Runtime introspection
      {
        name: 'arch-introspection',
        apply: 'serve', // Only in development mode
        transformIndexHtml(html: string) {
          const archScript = `
<script>
  // Architecture Defense - Runtime Introspection
  window.__ARCH__ = window.__ARCH__ || {};
  console.group('%c🏗️ Architecture Defense', 'color: #4CAF50; font-size: 14px; font-weight: bold');
  console.log('Module manifests available at: window.__ARCH__');
  console.log('Usage:');
  console.log('  window.__ARCH__.modules - All registered modules');
  console.log('  window.__ARCH__.getModule("id") - Get specific module');
  console.log('  window.__ARCH__.getOwner("id") - Get module owner');
  console.log('  window.__ARCH__.validate() - Validate architecture');
  console.groupEnd();
</script>`;
          return html.replace('</head>', `${archScript}</head>`);
        },
      },
    ],
    define: {
      'process.env.API_KEY': JSON.stringify(env.GEMINI_API_KEY),
      'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY)
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        '@api': path.resolve(__dirname, './src/api'),
        '@components': path.resolve(__dirname, './src/components'),
        '@features': path.resolve(__dirname, './src/features'),
        '@pages': path.resolve(__dirname, './src/pages'),
        '@hooks': path.resolve(__dirname, './src/hooks'),
        '@store': path.resolve(__dirname, './src/store'),
        '@utils': path.resolve(__dirname, './src/utils'),
      },
      // 强制使用单一版本的 React 和 React Router，解决 "Cannot read properties of null (reading 'useContext')" 问题
      dedupe: ['react', 'react-dom', 'react-router', 'react-router-dom'],
    },
    optimizeDeps: {
      include: ['react', 'react-dom', 'zustand', 'react-router-dom'],
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/__tests__/setup.ts'],
      include: ['src/**/*.{test,spec}.{ts,tsx}'],
      coverage: {
        reporter: ['text', 'json', 'html'],
        exclude: ['node_modules/', 'src/__tests__/setup.ts'],
      },
    },
  };
});
