// Input: 本地档案功能模块
// Output: 功能模块聚合导出
// Pos: 功能模块导出
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 档案管理功能模块
 */

// 视图组件
export { ArchiveListView } from '../../components/ArchiveListView';
export { ArchiveApprovalView } from '../../components/ArchiveApprovalView';
export { ArchivalPanoramaView } from '../../components/ArchivalPanoramaView';

// Hooks
export { useArchives, useArchive, useCreateArchive, useUpdateArchive, useDeleteArchive, useExportAip } from '../../hooks/useArchives';
