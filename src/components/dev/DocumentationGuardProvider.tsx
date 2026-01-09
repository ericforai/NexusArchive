// Input: useDocumentationGuard hook
// Output: 文档守卫 Provider 组件（开发环境自动生效）
// Pos: 开发辅助组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect } from 'react';
import { useDocumentationGuard } from '../../hooks/useDocumentationGuard';

/**
 * 文档守卫 Provider
 *
 * 开发环境自动监控代码变更，提醒更新对应文档
 * 生产环境无任何副作用
 *
 * @example
 * ```tsx
 * // App.tsx
 * import { DocumentationGuardProvider } from './components/dev/DocumentationGuardProvider';
 *
 * const App = () => (
 *   <>
 *     <DocumentationGuardProvider />
 *     <RouterProvider router={router} />
 *   </>
 * );
 * ```
 */
export function DocumentationGuardProvider() {
  // 环境标识 - 必须在所有 Hook 之前获取
  const isProd = import.meta.env.PROD;

  // Hook 必须在组件顶层无条件调用（React Hooks 规则）
  const { state } = useDocumentationGuard({
    autoCheck: !isProd, // 生产环境禁用自动检查
    checkInterval: 30000, // 每 30 秒检查一次
  });

  useEffect(() => {
    // 生产环境跳过日志输出
    if (isProd) return;

    if (state.hasPendingDocs && state.pendingDocs.length > 0) {
      console.group('%c📄 文档更新提醒', 'color: #f5222d; font-size: 16px; font-weight: bold;');
      console.warn('检测到代码变更，请同步更新以下文档：\n');

      state.pendingDocs.forEach((doc, index) => {
        console.log(
          `%c${index + 1}. ${doc.docPath}`,
          'color: #1890ff; font-weight: bold;'
        );
        console.log(`   描述: ${doc.description}`);
        if (doc.relatedFile) {
          console.log(`   关联文件: ${doc.relatedFile}`);
        }
        console.log('');
      });

      console.log(
        '%c提示: 更新文档后运行 git add 标记为已处理\n',
        'color: #52c41a;'
      );
      console.groupEnd();
    }
  }, [isProd, state.hasPendingDocs, state.pendingDocs]);

  // 生产环境返回 null，开发环境渲染不可见占位符
  if (isProd) {
    return null;
  }

  return <div style={{ display: 'none' }} aria-hidden="true" />;
}

/**
 * 简化版：仅控制台警告
 *
 * @example
 * ```tsx
 * import { DocReminder } from './components/dev/DocumentationGuardProvider';
 *
 * DocReminder('src/hooks/useMyHook.ts'); // 当前文件变更时提醒
 * ```
 */
export function DocReminder(filePath: string) {
  // 环境标识 - 必须在所有 Hook 之前获取
  const isProd = import.meta.env.PROD;

  // Hook 必须在组件顶层无条件调用（React Hooks 规则）
  const { state } = useDocumentationGuard({
    watchedFiles: [filePath],
    autoCheck: !isProd,
  });

  useEffect(() => {
    // 生产环境跳过
    if (isProd) return;

    if (state.hasPendingDocs && state.pendingDocs.length > 0) {
      const doc = state.pendingDocs[0];
      console.warn(
        `%c📄 [${doc.relatedFile}] 需要更新文档: ${doc.docPath}`,
        'color: #faad14; font-weight: bold;'
      );
    }
  }, [isProd, state]);

  return null;
}
