// Input: Node.js 标准库、Vite、Vite React 插件
// Output: Vite 构建配置
// Pos: 构建/测试配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import path from 'path';
import fs from 'fs';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// J4: Runtime Introspection - Load module manifests for architecture debugging
function loadModuleManifests() {
  const manifests: any[] = [];
  const manifestDirs = [
    'src/features',
    'src/components',
    'src/pages',
    'src/utils',
    'src/hooks',
    'src/store'
  ];

  for (const dir of manifestDirs) {
    const dirPath = path.resolve(__dirname, dir);
    if (!fs.existsSync(dirPath)) continue;

    const entries = fs.readdirSync(dirPath, { withFileTypes: true });
    for (const entry of entries) {
      if (entry.isDirectory()) {
        const manifestPath = path.join(dirPath, entry.name, 'manifest.config.ts');
        if (fs.existsSync(manifestPath)) {
          try {
            const content = fs.readFileSync(manifestPath, 'utf-8');
            // Extract module ID from the manifest
            const idMatch = content.match(/id:\s*['"`]([^'"`]+)['"`]/);
            const ownerMatch = content.match(/owner:\s*['"`]([^'"`]+)['"`]/);
            const publicApiMatch = content.match(/publicApi:\s*['"`]([^'"`]+)['"`]/);

            if (idMatch) {
              manifests.push({
                id: idMatch[1],
                owner: ownerMatch?.[1] || 'unknown',
                publicApi: publicApiMatch?.[1] || './index.ts',
                path: `${dir}/${entry.name}`,
                manifestFile: manifestPath
              });
            }
          } catch (e) {
            // Skip invalid manifests
          }
        }
      }
    }
  }

  return manifests;
}

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
        // host: '127.0.0.1', // Removed to allow auto-detection from browser URL
        // port: 15175,
      },
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        }
      }
    },
    preview: {
      port: 4173,
      host: '0.0.0.0',
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
      // Architecture Defense: Runtime introspection (J4)
      {
        name: 'arch-introspection',
        apply: 'serve', // Only in development mode
        transformIndexHtml(html: string) {
          const manifests = loadModuleManifests();
          const archScript = `
<script>
  (function() {
    'use strict';

    // J4: Reflex - Runtime architecture introspection for debugging
    window.__ARCH__ = {
      modules: ${JSON.stringify(manifests)},

      // Get module by ID
      getModule(id) {
        return this.modules.find(m => m.id === id);
      },

      // Get owner of a module
      getOwner(id) {
        const mod = this.getModule(id);
        return mod ? mod.owner : null;
      },

      // Get all modules owned by a team
      getByOwner(owner) {
        return this.modules.filter(m => m.owner === owner);
      },

      // Validate architecture (basic check)
      validate() {
        const issues = [];
        this.modules.forEach(m => {
          if (!m.id) issues.push(\`Missing ID: \${m.path}\`);
          if (!m.owner) issues.push(\`Missing owner: \${m.id}\`);
          if (!m.publicApi) issues.push(\`Missing public API: \${m.id}\`);
        });
        return {
          valid: issues.length === 0,
          issues,
          totalModules: this.modules.length
        };
      },

      // Search modules by pattern
      search(pattern) {
        const regex = new RegExp(pattern, 'i');
        return this.modules.filter(m =>
          regex.test(m.id) || regex.test(m.path)
        );
      },

      // Show architecture info
      info() {
        console.group('%c🏗️ Architecture Defense - J4: Reflex', 'color: #4CAF50; font-size: 14px; font-weight: bold');
        console.log('%cModules loaded:', 'font-weight: bold', this.modules.length);
        console.table(this.modules.map(m => ({
          ID: m.id,
          Owner: m.owner,
          Path: m.path
        })));
        console.log('%cAvailable methods:', 'font-weight: bold');
        console.log('  __ARCH__.getModule("id") - Get module by ID');
        console.log('  __ARCH__.getOwner("id") - Get module owner');
        console.log('  __ARCH__.getByOwner("team") - Get all modules by owner');
        console.log('  __ARCH__.validate() - Validate architecture');
        console.log('  __ARCH__.search("pattern") - Search modules');
        console.log('  __ARCH__.info() - Show this help');
        console.groupEnd();
      }
    };

    // Auto-show info on load
    window.__ARCH__.info();
  })();
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
        // 强制所有 React 导入指向根目录 node_modules
        react: path.resolve(__dirname, 'node_modules/react'),
        'react-dom': path.resolve(__dirname, 'node_modules/react-dom'),
        // 'react-router': path.resolve(__dirname, 'node_modules/react-router'), // Removed to fix subpath resolution
        // 'react-router-dom': path.resolve(__dirname, 'node_modules/react-router-dom'), // Removed to fix subpath resolution
      },
      // 强制使用单一版本的 React、React Router 和 Zustand，解决 "Cannot read properties of null (reading 'useContext')" 问题
      dedupe: ['react', 'react-dom', 'react-router', 'react-router-dom', 'zustand'],
    },
    optimizeDeps: {
      include: [
        'react',
        'react-dom',
        'react-dom/client',
        'zustand',
        'zustand/middleware',
        'react-router-dom',
        'react-router',
        '@tanstack/react-query',
        'antd',
        '@ant-design/icons',
        'lucide-react',
        // 防止运行时重新优化导致页面 reload
        'react-hot-toast',
        'axios',
        'recharts',
        'dayjs',
        'clsx',
        'qrcode.react',
        '@xyflow/react',
      ],
      // 关闭 force，避免每次启动都强制重构建
      // 如果遇到依赖问题，可临时设为 true 并重启
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
