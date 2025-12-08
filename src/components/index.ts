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
