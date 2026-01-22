# PDF 预览修复 Walkthrough

## 问题描述

在"电子凭证池"的"会计账簿"、"财务报告"和"其他会计资料"页面中，点击"查看"按钮后，PDF 预览显示"文件不存在或已被删除"错误。

### 根本原因

`FinancialReportDetailDrawer.tsx` 组件在处理凭证池（pool）模式时，错误地将 `archiveCode`（如 `BR-GROUP-2020-30Y-ORG-AC01-000001`）传递给了 `SmartFilePreview` 组件的 `fileId` 参数，而后端 `/api/pool/preview/{fileId}` 接口需要的是数据库中的 UUID（如 `be4a056e6fbf471281c9341d876da5c0`）。

## 修复内容

### 1. `FinancialReportDetailDrawer.tsx`

**修改前：**
```tsx
const archiveId = row.archiveCode || row.code || row.archivalCode || row.id;
// ...
<SmartFilePreview
  archiveId={!isPool ? archiveId : undefined}
  fileId={isPool ? archiveId : `${row.title || 'report'}.pdf`}
  isPool={isPool}
/>
```

**修改后：**
```tsx
// 凭证池 (isPool) 模式：必须使用 row.id (数据库 UUID)
// 档案库模式：优先使用 archiveCode 作为 archiveId
const previewArchiveId = !isPool ? (row.archiveCode || row.code || row.archivalCode || row.id) : undefined;
const previewFileId = isPool ? (row.id as string) : undefined;
// ...
<SmartFilePreview
  archiveId={previewArchiveId}
  fileId={previewFileId}
  fileName={row.fileName}
  isPool={isPool}
/>
```

### 2. `SmartFilePreview.tsx`

添加了 `fileName` 属性支持，用于正确识别文件类型：

```tsx
export interface SmartFilePreviewProps extends Omit<UseFilePreviewParams, 'autoLoad'> {
  // ...existing props...
  /** 文件名（用于识别类型和显示） */
  fileName?: string;
}

// 组件内部使用
const fileName = currentFile?.fileName || propFileName || previewParams.fileId || '预览';
```

## 修改的文件

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| [FinancialReportDetailDrawer.tsx](file:///Users/user/nexusarchive/src/pages/archives/FinancialReportDetailDrawer.tsx) | 修改 | 修复 fileId 传递逻辑，使用 row.id 作为 pool 模式的 fileId |
| [SmartFilePreview.tsx](file:///Users/user/nexusarchive/src/components/preview/SmartFilePreview.tsx) | 修改 | 添加 fileName 属性支持 |

## 验证结果

通过浏览器子代理验证：
1. ✅ API 请求现在正确使用 UUID：`/api/pool/preview/be4a056e6fbf471281c9341d876da5c0`
2. ✅ 后端正确返回 PDF 内容（200 OK，application/pdf，31KB）
3. ⚠️ 前端预览组件需要额外调试（文件类型检测问题已通过 fileName 属性修复）

## 已知限制

1. **会计账簿和财务报告页面无数据**：当前测试环境中这两个页面没有预归档数据，需要通过上传或 ERP 同步添加测试数据。
2. **认证头传递**：PDF 预览 iframe 可能需要额外的认证头处理（此为已知架构问题，参见 `docs/knowledge/2026-01-13-attachment-preview-auth-fix.md`）。

## 后续建议

1. 考虑在 `useFilePreview` hook 中添加更详细的错误日志，便于调试。
2. 为"会计账簿"和"财务报告"类别添加演示数据生成功能，类似于凭证池的 `/generate-demo` 接口。
