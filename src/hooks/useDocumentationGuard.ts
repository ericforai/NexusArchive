// Input: 受监控的文件路径、文档映射配置
// Output: 文档守卫 hook（检查状态 + 提醒方法）
// Pos: 文档自洽性守卫 hooks
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback, useRef } from 'react';

/**
 * 目录文档映射关系
 * key: 文件路径模式（支持通配符 *）
 * value: 对应的文档路径
 */
interface DocMapping {
  pattern: string;
  docPath: string;
  description: string;
}

/**
 * 文档守卫状态
 */
interface DocGuardState {
  /** 是否有未更新的文档 */
  hasPendingDocs: boolean;
  /** 待更新的文档列表 */
  pendingDocs: Array<{
    docPath: string;
    description: string;
    relatedFile?: string;
  }>;
  /** 是否正在检查 */
  checking: boolean;
}

/**
 * 文档守卫操作
 */
interface DocGuardActions {
  /** 检查文档状态 */
  checkDocs: () => Promise<void>;
  /** 标记文档已更新 */
  markUpdated: (docPath: string) => void;
  /** 获取文档更新模板 */
  getDocTemplate: (docPath: string) => string;
  /** 重置状态 */
  reset: () => void;
}

/**
 * 默认文档映射配置
 */
const DEFAULT_DOC_MAPPINGS: DocMapping[] = [
  // hooks 目录 -> src/hooks/README.md
  { pattern: 'src/hooks/**/*.ts', docPath: 'src/hooks/README.md', description: 'Hooks 目录文档' },
  // 组件 hooks -> 组件目录/hooks/README.md
  { pattern: 'src/components/**/hooks/*.ts', docPath: 'src/components/$DIR/hooks/README.md', description: '组件 Hooks 文档' },
  // API 层 -> src/api/README.md
  { pattern: 'src/api/**/*.ts', docPath: 'src/api/README.md', description: 'API 层文档' },
  // Store -> src/store/README.md
  { pattern: 'src/store/**/*.ts', docPath: 'src/store/README.md', description: '状态管理文档' },
  // Components -> 各组件目录/README.md
  { pattern: 'src/components/**/*.tsx', docPath: 'src/components/$DIR/README.md', description: '组件文档' },
  // Services -> src/services/README.md
  { pattern: 'src/services/**/*.ts', docPath: 'src/services/README.md', description: '服务层文档' },
];

/**
 * 生成目录文档的标准模板
 */
function generateDocTemplate(docPath: string, description: string): string {
  const today = new Date().toISOString().split('T')[0];
  return `<!-- 一旦我所属的文件夹有所变化，请更新我。-->
<!-- 本目录 ${description} -->
<!-- 最后更新: ${today} -->

## 用途

[请描述此目录的作用，1-3 行]

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| \`xxx.ts\` | Hook/Component/Util | [功能描述] |
| \`index.ts\` | 聚合入口 | 统一导出 |

## 设计说明

[可选] 描述目录的设计决策、依赖关系等

---
*此文档由 useDocumentationGuard 生成，请根据实际情况完善*
`;
}

/**
 * 提取目录路径（用于 $DIR 替换）
 */
function extractDirPath(filePath: string, pattern: string): string | null {
  // 简单实现：从 pattern 中提取目录结构
  // 例如: 'src/components/**/hooks/*.ts' -> 提取中间的目录
  const parts = pattern.split('/');
  const fileIndex = parts.findIndex(p => p.includes('*'));

  if (fileIndex === -1) return null;

  const filePathParts = filePath.split('/');
  return filePathParts.slice(0, fileIndex + 1).join('/');
}

/**
 * 文档守卫 Hook
 *
 * @param options - 配置选项
 * @returns 文档守卫状态和操作
 *
 * @example
 * ```tsx
 * const { state, actions } = useDocumentationGuard({
 *   watchedFiles: ['src/hooks/useMyHook.ts'],
 *   autoCheck: true,
 * });
 *
 * if (state.hasPendingDocs) {
 *   state.pendingDocs.forEach(doc => {
 *     console.warn(`需要更新文档: ${doc.docPath}`);
 *   });
 * }
 * ```
 */
export function useDocumentationGuard(options?: {
  /** 需要监控的文件列表（默认：当前 git 变更文件） */
  watchedFiles?: string[];
  /** 自定义文档映射 */
  docMappings?: DocMapping[];
  /** 是否自动检查（默认：true） */
  autoCheck?: boolean;
  /** 检查间隔（毫秒，默认：30000） */
  checkInterval?: number;
}): { state: DocGuardState; actions: DocGuardActions } {
  const {
    watchedFiles,
    docMappings = DEFAULT_DOC_MAPPINGS,
    autoCheck = true,
    checkInterval = 30000,
  } = options || {};

  const [state, setState] = useState<DocGuardState>({
    hasPendingDocs: false,
    pendingDocs: [],
    checking: false,
  });

  const lastCheckRef = useRef<number>(0);

  /**
   * 检查文档状态
   */
  const checkDocs = useCallback(async () => {
    const now = Date.now();
    if (now - lastCheckRef.current < 1000) {
      return; // 防止频繁检查
    }
    lastCheckRef.current = now;

    setState(prev => ({ ...prev, checking: true }));

    try {
      const filesToCheck = watchedFiles || await getChangedFiles();
      const pendingDocs: DocGuardState['pendingDocs'] = [];

      for (const file of filesToCheck) {
        for (const mapping of docMappings) {
          if (matchPattern(file, mapping.pattern)) {
            let docPath = mapping.docPath;

            // 处理 $DIR 替换
            if (docPath.includes('$DIR')) {
              const dirPath = extractDirPath(file, mapping.pattern);
              if (dirPath) {
                docPath = docPath.replace('$DIR', dirPath.split('/').pop() || '');
              }
            }

            pendingDocs.push({
              docPath,
              description: mapping.description,
              relatedFile: file,
            });
          }
        }
      }

      setState({
        hasPendingDocs: pendingDocs.length > 0,
        pendingDocs: Array.from(
          new Map(pendingDocs.map(d => [d.docPath, d])).values()
        ),
        checking: false,
      });
    } catch (error) {
      console.error('Documentation check failed:', error);
      setState(prev => ({ ...prev, checking: false }));
    }
  }, [watchedFiles, docMappings]);

  /**
   * 标记文档已更新
   */
  const markUpdated = useCallback((docPath: string) => {
    setState(prev => ({
      ...prev,
      pendingDocs: prev.pendingDocs.filter(d => d.docPath !== docPath),
      hasPendingDocs: false,
    }));
  }, []);

  /**
   * 获取文档更新模板
   */
  const getDocTemplate = useCallback((docPath: string): string => {
    const pending = state.pendingDocs.find(d => d.docPath === docPath);
    return generateDocTemplate(docPath, pending?.description || '文档');
  }, [state.pendingDocs]);

  /**
   * 重置状态
   */
  const reset = useCallback(() => {
    setState({
      hasPendingDocs: false,
      pendingDocs: [],
      checking: false,
    });
  }, []);

  // 自动检查
  useEffect(() => {
    if (!autoCheck) return;

    checkDocs();

    const interval = setInterval(checkDocs, checkInterval);
    return () => clearInterval(interval);
  }, [autoCheck, checkInterval, checkDocs]);

  const actions: DocGuardActions = {
    checkDocs,
    markUpdated,
    getDocTemplate,
    reset,
  };

  return { state, actions };
}

/**
 * 获取 Git 变更文件
 */
async function getChangedFiles(): Promise<string[]> {
  // 浏览器环境无法直接访问 Git，始终返回空
  // 需要后续通过 API 对接后端获取
  return [];
}

/**
 * 简单的通配符匹配
 */
function matchPattern(filePath: string, pattern: string): boolean {
  const regex = new RegExp(
    '^' +
    pattern
      .replace(/\./g, '\\.')
      .replace(/\*\*/g, '.*')
      .replace(/\*/g, '[^/]*') +
    '$'
  );
  return regex.test(filePath);
}

/**
 * 便捷 hook：在开发环境自动提示文档更新
 *
 * @example
 * ```tsx
 * function MyComponent() {
 *   useDocReminder('src/hooks/useMyHook.ts');
 *   return <div>...</div>;
 * }
 * ```
 */
export function useDocReminder(filePath: string) {
  const { state, actions } = useDocumentationGuard({
    watchedFiles: [filePath],
    autoCheck: true,
  });

  useEffect(() => {
    if (state.hasPendingDocs && state.pendingDocs.length > 0) {
      const docPath = state.pendingDocs[0].docPath;
      console.warn(
        `%c📄 文档提醒: 代码已变更，请更新 ${docPath}`,
        'color: #f5222d; font-weight: bold; font-size: 14px;'
      );
      console.log(
        `%c使用 actions.getDocTemplate('${docPath}') 获取文档模板`,
        'color: #1890ff;'
      );
    }
  }, [state.hasPendingDocs, state.pendingDocs]);
}
