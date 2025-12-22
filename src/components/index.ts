// Input: 本地组件模块
// Output: 组件聚合导出
// Pos: 业务页面组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 组件统一导出
 */

// 布局组件
export * from './layout';

// 通用组件
export * from './common';

// 视图组件 - 按需导入
export { Dashboard } from './Dashboard';
export { LoginView } from './LoginView';
export { ArchiveListView } from './ArchiveListView';
export { StatsView } from './StatsView';
export { SettingsView } from './SettingsView';
export { BorrowingView } from './BorrowingView';
export { WarehouseView } from './WarehouseView';
export { DestructionView } from './DestructionView';
export { ComplianceReportView } from './ComplianceReportView';
