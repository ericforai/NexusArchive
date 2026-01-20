一旦我所属的文件夹有所变化，请更新我。

// Input: archives 页面模块
// Output: 极简架构说明
// Pos: src/pages/archives/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 档案管理页面 (Archives Pages)

本目录包含档案管理相关的页面容器组件。

## 文件清单

| 文件 | 功能 |
| --- | --- |
| `__tests__/` | 测试 | 页面层测试用例 |
| `ArchiveListPage.tsx` | 档案列表页面容器 |
| `ArchiveListView.tsx` | 档案列表视图 |
| `ArchiveDetailDrawer.tsx` | 档案详情抽屉（2026-01-02: 响应式 Drawer UI；2026-01-08: Spin tip 嵌套）|
| `ArchiveDetailPage.tsx` | 档案详情全页面（2026-01-02: 展开-to-新页功能）|
| `AddRecordModal.tsx` | 新增档案弹窗 |
| `ComplianceModal.tsx` | 合规检查弹窗 |
| `ComplianceReportView.tsx` | 合规报告页面 |
| `components/` | 子组件 | 页面局部组件 |
| `CreateOriginalVoucherDialog.tsx` | 原始凭证创建弹窗 |
| `FinancialReportDetailDrawer.tsx` | 财务报告预览抽屉（使用预览组件公共 API） |
| `hooks/` | Hooks | 页面级 Hook |
| `index.ts` | 模块入口 | 统一导出 |
| `LinkModal.tsx` | 关联凭证弹窗 |
| `manifest.config.ts` | 模块清单 | 页面模块声明 |
| `MatchPreviewModal.tsx` | 匹配预览弹窗 |
| `OriginalVoucherListView.tsx` | 原始凭证列表视图（支持 URL type 菜单标题；DEV 调试日志） |
| `RuleConfigModal.tsx` | 规则配置弹窗 |
| `utils/` | 工具 | 页面辅助工具 |

## 2026-01-02 更新

- ✅ 模态框 → 抽屉重构（Drawer 替代 Modal）
- ✅ 响应式布局（50vw/70vw/100vw 断点）
- ✅ 路由变更自动关闭
- ✅ 展开-to-新页功能

## 2026-01-08 更新

- ✅ Spin tip 采用嵌套写法，避免 antd 告警

## 架构约束

- Page 层仅负责"胶水"逻辑
- 禁止在此编写底层 HTML/CSS
- 业务逻辑应抽离至 `src/features/archives/`
