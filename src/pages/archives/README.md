// Input: archives 页面模块
// Output: 极简架构说明
// Pos: src/pages/archives/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 档案管理页面 (Archives Pages)

本目录包含档案管理相关的页面容器组件。

## 文件清单

| 文件 | 功能 |
| --- | --- |
| `ArchiveListPage.tsx` | 档案列表页面容器 |
| `ArchiveListView.tsx` | 档案列表视图 |
| `ArchiveDetailDrawer.tsx` | 档案详情抽屉（2026-01-02: 替代 ArchiveDetailModal）|
| `AddRecordModal.tsx` | 新增档案弹窗 |
| `ComplianceModal.tsx` | 合规检查弹窗 |
| `ComplianceReportView.tsx` | 合规报告页面 |
| `CreateOriginalVoucherDialog.tsx` | 原始凭证创建弹窗 |
| `LinkModal.tsx` | 关联凭证弹窗 |
| `MatchPreviewModal.tsx` | 匹配预览弹窗 |
| `OriginalVoucherListView.tsx` | 原始凭证列表视图 |
| `RuleConfigModal.tsx` | 规则配置弹窗 |

## 架构约束

- Page 层仅负责"胶水"逻辑
- 禁止在此编写底层 HTML/CSS
- 业务逻辑应抽离至 `src/features/archives/`
