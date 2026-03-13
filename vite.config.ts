// Input: Node.js 标准库、Vite、Vite React 插件
// Output: Vite 构建配置
// Pos: 构建/测试配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import path from 'path';
import { defineConfig, loadEnv, splitVendorChunkPlugin } from 'vite';
import react from '@vitejs/plugin-react';
import autoprefixer from 'autoprefixer';
import tailwindcss from 'tailwindcss';
import viteCompression from 'vite-plugin-compression';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  const apiBaseUrl = env.VITE_API_BASE_URL || 'http://localhost:19090/api';
  const apiTarget = apiBaseUrl.replace(/\/api\/?$/, '');

  return {
    server: {
      port: 15175,
      host: '0.0.0.0',
      strictPort: true,
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
      splitVendorChunkPlugin(),
      // 移除任何外部注入的 importmap（如 AI Studio 插件），防止 React 版本冲突
      {
        name: 'remove-external-importmap',
        transformIndexHtml(html: string) {
          // 移除指向 aistudiocdn 的 importmap
          return html.replace(/<script type="importmap">[\s\S]*?aistudiocdn[\s\S]*?<\/script>/gi, '');
        },
      },
      viteCompression({
        algorithm: 'gzip',
        ext: '.gz',
        threshold: 10 * 1024,
        deleteOriginFile: false,
      }),
    ],
    build: {
      cssCodeSplit: true,
      reportCompressedSize: true,
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined;
            }

            if (id.includes('/antd/') || id.includes('/@ant-design/')) {
              return 'vendor-antd';
            }
            if (id.includes('/@xyflow/') || id.includes('/d3-force/')) {
              return 'vendor-graph';
            }
            if (id.includes('/recharts/')) {
              return 'vendor-charts';
            }
            if (id.includes('/pdfjs-dist/') || id.includes('/react-pdf/')) {
              return 'vendor-pdf';
            }
            return undefined;
          },
        },
      },
    },
    css: {
      postcss: {
        plugins: [
          tailwindcss(),
          autoprefixer(),
        ],
      },
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
        'react': path.resolve(__dirname, 'node_modules/react'),
        'react-dom': path.resolve(__dirname, 'node_modules/react-dom'),
      },
      dedupe: ['react', 'react-dom', 'react-router', 'react-router-dom', 'zustand']
    },
    optimizeDeps: {
      include: [
        'react',
        'react-dom',
        'react-dom/client',
        'react/jsx-runtime',
        'react/jsx-dev-runtime',
        'react-router-dom',
        'zustand',
        'antd',
        '@ant-design/icons',
        'lucide-react',
        'axios'
      ],
      force: true
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/__tests__/setup.ts'],
      exclude: [
        '**/node_modules/**',
        '**/dist/**',
        '**/.{idea,git,cache,output,temp}/**',
        '**/playwright/**',
        '**/e2e/**',
        '**/.worktrees/**', // 排除工作树目录
        '**/*.spec.ts' // 排除 Playwright 风格的 spec 文件
      ]
    }
  };
});
