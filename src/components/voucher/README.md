// Input: 凭证预览组件库
// Output: 极简架构说明
// Pos: src/components/voucher/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 凭证预览组件 (Voucher Preview Components)

本目录包含凭证预览相关的 React 组件。

## 组件清单 (2026-01-02 创建)

| 组件 | 功能 | 状态 |
| --- | --- | --- |
| `VoucherMetadata.tsx` | 业务元数据展示（凭证号/日期/制单人等） | ✅ 活跃 |
| `VoucherPreview.tsx` | 凭证预览主容器组件 | ✅ 活跃 |
| `VoucherPreviewCanvas.tsx` | 会计凭证画布（分录表格渲染） | ✅ 活跃 |
| `VoucherPreviewTabs.tsx` | 标签页导航（业务元数据/会计凭证/关联附件） | ✅ 活跃 |
| `OriginalDocumentPreview.tsx` | 原始凭证预览（发票/单据等） | ✅ 活跃 |
| `index.ts` | 统一导出入口 | ✅ 活跃 |
| `styles.ts` | 共享样式定义 | ✅ 活跃 |

## 使用示例

```tsx
import { VoucherPreview } from '@/components/voucher';

<VoucherPreview
  data={voucherData}
  layout="horizontal"
  size="normal"
/>
```

## 架构约束

- 纯 UI 组件，不包含 API 调用
- 数据通过 props 传入
- 样式使用 Tailwind + 共享 styles.ts

## 测试

- `VoucherMetadata.test.tsx`: 7 个测试用例
- `VoucherPreview.test.tsx`: 5 个测试用例
- `VoucherPreviewCanvas.test.tsx`: 7 个测试用例
- `VoucherPreviewTabs.test.tsx`: 3 个测试用例
