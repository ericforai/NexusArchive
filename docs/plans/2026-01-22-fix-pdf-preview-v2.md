# 修复凭证池（Ledger/Report/Other）PDF 预览失效计划

## 问题分析

在电子凭证池的“账簿”、“报表”和“其他资料”页面中，点击“查看”按钮后无法看到 PDF。

**根本原因**：
1.  **Drawer 组件不统一**：凭证页使用 `ArchiveDetailDrawer`，而账簿/报表等使用新开发的 `FinancialReportDetailDrawer`。
2.  **预览逻辑缺失 isPool 状态**：`FinancialReportDetailDrawer` 内部使用了 `SmartFilePreview`，但未正确识别当前是“凭证池（未归档）”还是“档案库（已归档）”模式。
3.  **API 调用错误**：在凭证池模式下，应该调用 `/pool/preview/{fileId}`，但组件默认调用了 `/archive/preview`，导致后端返回 404 或 500（因为档案 ID 不存在）。

## 修改计划

### 1. 完善 ArchiveListView (容器层)
确保在打开账簿/报表抽屉时，将当前的 `isPoolView` 状态传递下去。

- **文件**: [ArchiveListView.tsx](file:///Users/user/nexusarchive/src/pages/archives/ArchiveListView.tsx)
- **改动**: 在渲染 `FinancialReportDetailDrawer` 时，显式传递 `isPool={mode.isPoolView}`。

### 2. 增强 FinancialReportDetailDrawer (UI 层)
接收 `isPool` 参数，并根据该状态指导 `SmartFilePreview` 选择正确的 API 分支。

- **文件**: [FinancialReportDetailDrawer.tsx](file:///Users/user/nexusarchive/src/pages/archives/FinancialReportDetailDrawer.tsx)
- **改动**:
    - 更新 `FinancialReportDetailDrawerProps` 接口，添加 `isPool?: boolean`。
    - 在调用 `SmartFilePreview` 时：
        - 传递 `isPool={isPool}`。
        - 修正 ID 传递：如果 `isPool` 为 true，将 `row.id` 作为 `fileId` 传递给预览组件（因为 `useFilePreview` 对 pool 使用 `fileId`）。

### 3. 验证 SmartFilePreview 与 useFilePreview (核心逻辑层)
确认底层组件能够处理这些参数（已分析代码，具备此能力）。

## 验证方案

### 自动化测试建议
- 模拟 `isPool=true` 且 `fileId="xxx"` 的场景，检查 `useFilePreview` 是否发起对 `/pool/preview/xxx` 的请求。

### 手动验证步骤
1. 进入“电子凭证池” -> “会计账簿”。
2. 选择一条带附件的记录（如：总账）。
3. 点击“查看”，验证右侧抽屉是否正常渲染 PDF。
4. 切换到“总账库”（已归档），验证“查看”功能依然正常（走归档 API）。

## 预期结果
所有门类的“查看”功能（包括未归档的池数据）均能正常通过 `SmartFilePreview` 看到 PDF 内容。
