// Input: useDocumentationGuard hook
// Output: 文档守卫 Provider 组件（开发环境自动生效）
// Pos: 开发辅助组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useEffect } from 'react';
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
  // 生产环境直接返回，无任何副作用
  if (import.meta.env.PROD) {
    return null;
  }

  const { state } = useDocumentationGuard({
    autoCheck: true,
    checkInterval: 30000, // 每 30 秒检查一次
  });

  useEffect(() => {
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
  }, [state.hasPendingDocs, state.pendingDocs]);

  // 渲染不可见的占位符（不影响布局）
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
  if (import.meta.env.PROD) return null;

  const { state } = useDocumentationGuard({
    watchedFiles: [filePath],
    autoCheck: true,
  });

  useEffect(() => {
    if (state.hasPendingDocs && state.pendingDocs.length > 0) {
      const doc = state.pendingDocs[0];
      console.warn(
        `%c📄 [${doc.relatedFile}] 需要更新文档: ${doc.docPath}`,
        'color: #faad14; font-weight: bold;'
      );
    }
  }, [state]);

  return null;
}
