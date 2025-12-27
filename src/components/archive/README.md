// Input: archive 组件目录
// Output: 极简架构说明
// Pos: src/components/archive/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 档案组件 (Archive Components)

本目录包含档案管理相关的 UI 组件。

## 文件清单

| 文件 | 功能 |
| --- | --- |
| `ArchiveDetailModal.tsx` | 档案详情弹窗（业务元数据 + 文件预览 + 附件列表）|

## 架构约束

- 组件应为**纯展示组件**，通过 Props 接收数据和回调
- 禁止直接调用 API
- 复用 `components/common/*` 中的原子组件
