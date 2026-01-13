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

// 业务组件 - 按需导入（仅保留展示组件）
export { LoginCard } from './auth/LoginCard';
export { SettingsLayout } from './settings/SettingsLayout';
export { BasicSettings } from './settings/BasicSettings';
export { UserSettings } from './settings/UserSettings';
export { RoleSettings } from './settings/RoleSettings';
export { SecuritySettings } from './settings/SecuritySettings';
export { IntegrationSettingsPage } from './settings/integration';
export { LicenseSettings } from './settings/LicenseSettings';
