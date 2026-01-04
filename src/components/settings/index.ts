// Input: 本地设置组件模块
// Output: 组件聚合导出
// Pos: 系统设置组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 系统设置功能模块
 *
 * 提供设置页面的布局和各子页面组件
 */

export { SettingsLayout } from './SettingsLayout';
export { BasicSettings } from './BasicSettings';
export { UserSettings } from './UserSettings';
export { RoleSettings } from './RoleSettings';
export { OrgSettings } from './OrgSettings';
export { SecuritySettings } from './SecuritySettings';
export { default as IntegrationSettings } from './IntegrationSettings';
export { default as LicenseSettings } from './LicenseSettings';
