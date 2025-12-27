// Input: 本地合规功能模块
// Output: 功能模块业务逻辑聚合导出（仅 hooks + 类型）
// Pos: 功能模块导出
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 合规管理功能模块 - 业务逻辑层
 *
 * @description 仅导出业务逻辑 hooks，不导出 UI 组件（遵循架构边界规则 C）
 * @see /docs/architecture/frontend-boundaries.md
 */

// TODO: 将合规报告相关业务逻辑抽取为 hooks
// 当前暂无业务逻辑 hooks 可导出

// ❌ 已移除：视图组件导出（违反架构边界规则 C）
// 页面组件应从以下路径直接导入，不经过 features 层：
// - 'pages/archives/ComplianceReportView'
// - 'pages/collection/FourNatureReportView'
