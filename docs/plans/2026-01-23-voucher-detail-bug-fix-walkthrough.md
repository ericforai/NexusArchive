# 凭证详情页 Bug 修复报告 (Archive Detail Page Fixes)

我们已成功修复了用户反馈的凭证详情页 ID 不一致、附件数量冲突以及预览失败的问题。

## 修复项摘要

### 1. 凭证 ID 与编号不一致 
- **问题根源**：`ArchiveDetailPage.tsx` 之前使用了一个 Mock 函数 `createMockRowFromId`，它会根据 ID 的后 6 位硬编码生成假凭证号（例如 `seed-voucher-001` 变为 `凭证-er-001`）。
- **修复方案**：移除了 Mock 逻辑。现在组件初始化时会调用 `archivesApi.getArchiveById(id)` 获取真实的档案记录数据。
- **效果**：页面标题、面包屑和元数据区域现在正确显示数据库中的 `archiveCode`（如 `JZ-202311-0052`）。

### 2. 附件数量不一致
- **问题根源**：全屏详情页之前独立调用了 `/archives/:id/files`，而列表抽屉使用的是 `useVoucherData`（内部调用 `/archive/:id/voucher-data`）。两个接口返回的附件过滤逻辑不同，导致数量冲突（1 vs 2）。
- **修复方案**：统一使用 `useVoucherData` 作为单一数据源，确保详情页展示的附件与抽屉视图完全一致。
- **效果**：详情页现在正确显示 1 个关联附件（符合业务预期）。

### 3. 附件预览失败（白屏）
- **问题根源**：`OriginalDocumentPreview.tsx` 将预览 Blob 类型硬编码为 `application/pdf`，导致图片或其他非 PDF 格式文件无法显示。
- **修复方案**：
    - 动态获取响应头中的 `Content-Type`。
    - 针对图片文件（`image/*`）自动切换为 `<img>` 标签展示。
    - 优化了预览区域的滚动和阴影视觉效果。
- **效果**：不仅修复了打不开的问题，还提升了图片类附件的预览体验。

## 涉及文件

- [ArchiveDetailPage.tsx](file:///Users/user/nexusarchive/src/pages/archives/ArchiveDetailPage.tsx): 移除 Mock，统一数据流。
- [OriginalDocumentPreview.tsx](file:///Users/user/nexusarchive/src/components/voucher/OriginalDocumentPreview.tsx): 增强文件预览兼容性。
- [useVoucherData.ts](file:///Users/user/nexusarchive/src/pages/archives/hooks/useVoucherData.ts): 元数据解析逻辑校准。

## 验证建议
1. 在管理后台点击任意凭证。
2. 在右侧抽屉确认凭证号（如 `JZ-202311-0052`）及附件数。
3. 点击"展开到新页"箭头。
4. 验证新页面的标题、面包屑以及附件数量是否与抽屉内一致，并检查附件是否能正常预览。
