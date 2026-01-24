# 凭证详情页 Bug 修复任务清单

- [x] Bug 定位：分析 `ArchiveDetailPage.tsx` 源码，确认 Mock 数据导致 ID 错误
- [x] Bug 修复：移除 `ArchiveDetailPage.tsx` 的 Mock 逻辑，接入真实 API
- [x] 数据统一：将详情页附件加载逻辑切换至 `useVoucherData`
- [x] 兼容性增强：修复 `OriginalDocumentPreview.tsx` 的 MIME 类型硬编码问题
- [x] 格式支持：在预览组件中增加对图片格式（JPG/PNG等）的原生支持
- [x] 文档归档：编写修复报告并存入 `docs/plans/`
