// Input: 本地设置功能模块
// Output: 功能模块业务逻辑聚合导出（仅 application + domain）
// Pos: 功能模块导出
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 系统设置功能模块 - 业务逻辑层
 *
 * @description 仅导出 application/domain，不导出 UI 组件（遵循架构边界规则）
 * @see /docs/architecture/frontend-boundaries.md
 */

export * from './application';
export * from './domain';

// ❌ 已移除：视图组件导出（违反架构边界规则）
