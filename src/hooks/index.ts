// Input: 本地 hooks 模块
// Output: hooks 统一导出
// Pos: hooks 聚合入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Hooks 统一导出
 */

// React Query hooks
export { useArchives, useArchive, useRecentArchives, useCreateArchive, useUpdateArchive, useDeleteArchive, useExportAip } from './useArchives';

// 权限相关 hooks
export { usePermissions, usePermissionsQuery } from './usePermissions';
