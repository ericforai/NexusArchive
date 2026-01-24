# 凭证详情页数据显示与布局优化实施计划

针对用户反馈的凭证号不匹配及预览布局未最大化问题，制定以下修复方案。

## 用户审查建议

> [!IMPORTANT]
> **关于最大化布局**：我们将把详情页的内容区域高度固定为 `calc(100vh - 250px)` 并增加内部滚动支持。这不仅解决了预览附件缩小的问题，也能确保元数据较多时页面依然整洁。

## 拟定变更

### 1. 核心逻辑层

#### [MODIFY] [useVoucherData.ts](file:///Users/user/nexusarchive/src/pages/archives/hooks/useVoucherData.ts)
- **变更理由**：目前 Effect 仅监听 `row.id`。在详情页中，`row` 的标识符 `id` 始终不变，但其 `code` 属性会从 Fallback 状态（ID）更新为真实状态（财务编号）。
- **优化建议**：将 `row.code` 加入依赖项，确保数据同步。

### 2. 界面展示层

#### [MODIFY] [ArchiveDetailPage.tsx](file:///Users/user/nexusarchive/src/pages/archives/ArchiveDetailPage.tsx)
- **布局调整**：
    - 将主容器 `max-w-7xl` (1280px) 提升至 `max-w-[1600px]`，提升大屏下的视觉张力。
    - 为每个 Tab 的内容容器设置 `style={{ height: 'calc(100vh - 250px)' }}` 且 `overflow-y-auto`。
- **一致性对齐**：
    - `VoucherMetadata` 增加内部滚动支持。
    - `OriginalDocumentPreview` 现在能在固定高度的容器中全屏展示图片或 PDF。

---

## 专家联合建议

### ⚖️ 专家组审查意见

- **合规专家 (Compliance Authority)**：凭证号是档案唯一性的法定标识。必须确保 `VoucherMetadata` 显示的编号与 `ArchiveDetailPage` 头部显示的 `archiveCode` 严格一致。本次 Hook 依赖项修正解决了该合规风险。
- **信创架构师 (Xinchuang Architect)**：支持高分辨率屏幕下的“最大化”显示符合大屏审阅场景，建议保留响应式处理。
- **交付专家 (Delivery Strategist)**：详情页与抽屉视图的视觉体验对齐有助于降低最终用户的学习成本，支持该重构。

---

## 验证计划

1. **凭证号验证**：进入详情页，检查“业务元数据”列表中的“凭证号”是否已从 ID（如 `seed-voucher-002`）正确更新为财务编号（如 `V-202511-TEST`）。
2. **布局验证**：
    - 切换至“会计凭证”标签，确认渲染区域高度足够，能清晰看到完整凭证或通过滚动查看。
    - 切换至“关联附件”，确认图片/PDF 预览区域占据屏幕大部分空间，不再缩成小块区域。
