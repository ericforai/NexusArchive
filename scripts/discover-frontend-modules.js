#!/usr/bin/env node
/**
 * 前端模块发现与文档生成工具
 *
 * 功能:
 * 1. 扫描 src/ 目录自动发现新模块
 * 2. 分析组件、store、hooks 等模块结构
 * 3. 自动生成/更新 module-manifest.md
 * 4. 生成模块依赖关系图
 *
 * 使用:
 *   node scripts/discover-frontend-modules.js
 *   node scripts/discover-frontend-modules.js --update-manifest
 *   node scripts/discover-frontend-modules.js --validate
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const SRC_PATH = path.join(ROOT, 'src');
const DOCS_PATH = path.join(ROOT, 'docs/architecture');
const MANIFEST_PATH = path.join(DOCS_PATH, 'module-manifest.md');

// 模块扫描配置
const MODULE_PATTERNS = {
  'FE.PAGES': {
    path: 'pages',
    label: '页面容器层',
    description: '页面级容器组件',
    status: 'ACTIVE'
  },
  'FE.COMPONENTS': {
    path: 'components',
    label: '通用组件层',
    description: '可复用 UI 组件',
    status: 'ACTIVE'
  },
  'FE.STORE': {
    path: 'store',
    label: '状态管理层',
    description: 'Zustand 全局状态',
    status: 'ACTIVE'
  },
  'FE.API': {
    path: 'api',
    label: 'API 客户端层',
    description: '后端 API 调用封装',
    status: 'ACTIVE'
  },
  'FE.HOOKS': {
    path: 'hooks',
    label: '自定义 Hooks',
    description: 'React 自定义 Hooks',
    status: 'ACTIVE'
  },
  'FE.UTILS': {
    path: 'utils',
    label: '工具函数层',
    description: '通用工具函数',
    status: 'ACTIVE'
  }
};

/**
 * 扫描目录获取模块信息
 */
function scanDirectory(dirPath, moduleName) {
  if (!fs.existsSync(dirPath)) {
    return { fileCount: 0, subModules: [] };
  }

  const subModules = [];
  let fileCount = 0;

  const items = fs.readdirSync(dirPath, { withFileTypes: true });

  for (const item of items) {
    const fullPath = path.join(dirPath, item.name);

    if (item.isDirectory()) {
      const subResult = scanDirectory(fullPath, `${moduleName}/${item.name}`);
      fileCount += subResult.fileCount;
      if (subResult.fileCount > 0) {
        subModules.push({
          name: item.name,
          path: fullPath,
          fileCount: subResult.fileCount
        });
      }
    } else if (item.isFile() && /\.(tsx?|jsx?)$/.test(item.name)) {
      fileCount++;
    }
  }

  return { fileCount, subModules };
}

/**
 * 发现所有前端模块
 */
function discoverFrontendModules() {
  const modules = [];

  for (const [moduleId, config] of Object.entries(MODULE_PATTERNS)) {
    const dirPath = path.join(SRC_PATH, config.path);
    const { fileCount, subModules } = scanDirectory(dirPath, config.path);

    if (fileCount > 0 || subModules.length > 0) {
      modules.push({
        id: moduleId,
        name: config.label,
        path: `src/${config.path}`,
        description: config.description,
        fileCount,
        subModules: subModules.map(m => ({
          name: `${config.path}/${m.name}`,
          fileCount: m.fileCount
        })),
        status: config.status
      });
    }
  }

  return modules;
}

/**
 * 验证模块清单与代码的一致性
 */
function validateManifest() {
  const issues = [];

  // 读取现有清单
  if (!fs.existsSync(MANIFEST_PATH)) {
    issues.push('模块清单文件不存在');
    return { valid: false, issues };
  }

  const manifest = fs.readFileSync(MANIFEST_PATH, 'utf-8');
  const actualModules = discoverFrontendModules();

  // 检查新发现的模块
  for (const module of actualModules) {
    const escapedId = module.id.replace('.', '\\.');
    const regex = new RegExp(`\\|\\s*${escapedId}\\s*\\|`);
    if (!regex.test(manifest)) {
      issues.push(`发现未在清单中记录的模块: ${module.id} (${module.name})`);
    }
  }

  // 检查已删除的模块
  const manifestLines = manifest.split('\n');
  for (const line of manifestLines) {
    const match = line.match(/\| FE\.([A-Z_]+)\s+\|/);
    if (match) {
      const moduleId = `FE.${match[1]}`;
      const exists = actualModules.some(m => m.id === moduleId);
      if (!exists && !line.includes('待收敛') && !line.includes('实验性')) {
        issues.push(`清单中记录的模块在代码中不存在: ${moduleId}`);
      }
    }
  }

  return {
    valid: issues.length === 0,
    issues,
    actualModules
  };
}

/**
 * 生成 Markdown 清单内容
 */
function generateManifestMarkdown() {
  const modules = discoverFrontendModules();
  const date = new Date().toISOString().split('T')[0];

  let markdown = `# Module Manifest（模块清单）

> 本清单是模块边界与依赖关系的单一事实来源（SSOT）。
> **版本**: 2.3.0
> **更新日期**: ${date}
> **自动生成**: 通过 scripts/discover-frontend-modules.js

---

## Frontend Modules

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
`;

  // 读取现有清单以保留后端部分
  const existingManifest = fs.existsSync(MANIFEST_PATH)
    ? fs.readFileSync(MANIFEST_PATH, 'utf-8')
    : '';

  // 提取后端部分（如果存在）
  const backendSection = existingManifest.includes('## Backend Modules')
    ? existingManifest.substring(existingManifest.indexOf('## Backend Modules'))
    : '';

  for (const module of modules) {
    const scope = module.subModules.length > 0
      ? module.subModules.map(m => `src/${m.name}`).join(', ')
      : `src/${module.path}`;

    markdown += `| ${module.id} | ${module.name} | ${scope} | ${module.description} | ${module.fileCount} files | ${getStatusEmoji(module.status)} ${module.status} |\n`;
  }

  markdown += '\n### 前端子模块详情\n\n';
  for (const module of modules) {
    if (module.subModules.length > 0) {
      markdown += `#### ${module.id} - ${module.name}\n\n`;
      for (const sub of module.subModules) {
        markdown += `- \`src/${sub.name}\`: ${sub.fileCount} files\n`;
      }
      markdown += '\n';
    }
  }

  markdown += backendSection;

  return markdown;
}

/**
 * 获取状态 emoji
 */
function getStatusEmoji(status) {
  const emojis = {
    'ACTIVE': '✅',
    'LOCKED': '🔒',
    'DEPRECATED': '⚠️',
    'EXPERIMENTAL': '🧪'
  };
  return emojis[status] || 'ℹ️';
}

/**
 * 主函数
 */
function main() {
  const args = process.argv.slice(2);
  const command = args[0];

  switch (command) {
    case '--validate':
    case '-v': {
      console.log('🔍 验证模块清单...\n');
      const result = validateManifest();

      if (result.valid) {
        console.log('✅ 模块清单与代码一致');
        process.exit(0);
      } else {
        console.log('❌ 发现不一致:\n');
        result.issues.forEach(issue => console.log(`  - ${issue}`));
        console.log('\n💡 运行: node scripts/discover-frontend-modules.js --update-manifest');
        process.exit(1);
      }
    }

    case '--update-manifest':
    case '-u': {
      console.log('📝 更新模块清单...\n');
      const markdown = generateManifestMarkdown();
      fs.writeFileSync(MANIFEST_PATH, markdown);
      console.log(`✅ 已更新: ${MANIFEST_PATH}`);
      console.log(`\n📊 发现 ${discoverFrontendModules().length} 个前端模块`);
      process.exit(0);
    }

    case '--discover':
    case '-d': {
      console.log('🔍 发现前端模块...\n');
      const modules = discoverFrontendModules();
      console.log(`发现 ${modules.length} 个模块:\n`);

      for (const module of modules) {
        console.log(`  ${module.id}: ${module.name}`);
        console.log(`    范围: ${module.path}`);
        console.log(`    文件: ${module.fileCount}`);
        if (module.subModules.length > 0) {
          console.log(`    子模块:`);
          for (const sub of module.subModules) {
            console.log(`      - ${sub.name} (${sub.fileCount} files)`);
          }
        }
        console.log('');
      }
      process.exit(0);
    }

    default: {
      console.log(`
前端模块发现工具

用法:
  node scripts/discover-frontend-modules.js [命令]

命令:
  -d, --discover        发现所有前端模块
  -v, --validate        验证模块清单与代码一致性
  -u, --update-manifest  自动更新 module-manifest.md

示例:
  node scripts/discover-frontend-modules.js --discover
  node scripts/discover-frontend-modules.js --validate
  node scripts/discover-frontend-modules.js --update-manifest
      `);
      process.exit(0);
    }
  }
}

main();
