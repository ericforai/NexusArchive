// Input: React 批量操作组件
// Output: 批量操作通用组件目录说明
// Pos: src/components/operations/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 批量操作组件 (Operations)

本目录包含批量操作相关的通用组件，用于跨模块的批量选择、审批和结果展示。

## 目录结构

```
src/components/operations/
├── index.ts                      # 公共 API 入口
├── README.md                     # 本文件
├── useBatchSelection.ts          # 批量选择 Hook（待实现）
├── BatchOperationBar.tsx         # 批量操作工具栏（待实现）
├── BatchApprovalDialog.tsx       # 批量审批弹窗（待实现）
└── BatchResultModal.tsx          # 批量结果报告（待实现）
```

## 组件说明

### useBatchSelection

**位置**: `useBatchSelection.ts`
**类型**: Custom Hook
**功能**: 批量选择状态管理

#### 功能特性

- 跨页选择状态维护
- 全选/反选控制
- 选中项数量统计
- 选择状态持久化（可选）

### BatchOperationBar

**位置**: `BatchOperationBar.tsx`
**类型**: React.FC
**功能**: 批量操作工具栏组件

#### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| `selectedCount` | `number` | 已选中项数量 |
| `operations` | `Operation[]` | 可用操作列表 |
| `onOperation` | `(type: string) => void` | 操作回调 |
| `onClear` | `() => void` | 清空选择回调 |

#### 功能

- 显示已选中项数量
- 提供批量操作按钮组
- 清空选择功能
- 操作权限控制

### BatchApprovalDialog

**位置**: `BatchApprovalDialog.tsx`
**类型**: React.FC
**功能**: 批量审批确认弹窗

#### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| `open` | `boolean` | 是否显示对话框 |
| `items` | `ApprovalItem[]` | 待审批项列表 |
| `operationType` | `string` | 操作类型 |
| `onConfirm` | `(comment: string) => void` | 确认回调 |
| `onCancel` | `() => void` | 取消回调 |

#### 功能

- 显示待审批项摘要
- 审批意见输入
- 批量确认/取消
- 操作前二次确认

### BatchResultModal

**位置**: `BatchResultModal.tsx`
**类型**: React.FC
**功能**: 批量操作结果报告弹窗

#### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| `open` | `boolean` | 是否显示对话框 |
| `result` | `BatchResult` | 批量操作结果 |
| `onClose` | `() => void` | 关闭回调 |
| `onRetry` | `(failedItems: string[]) => void` | 重试失败项回调 |

#### 功能

- 显示操作成功/失败统计
- 失败项明细列表
- 导出失败报告
- 重试失败项功能

## 使用方式

```typescript
import {
  useBatchSelection,
  BatchOperationBar,
  BatchApprovalDialog,
  BatchResultModal
} from '@components/operations';

function MyBatchView() {
  const {
    selectedRows,
    selectedCount,
    isSelected,
    toggleSelection,
    clearSelection,
    selectAll,
    selectNone
  } = useBatchSelection();

  const [approvalOpen, setApprovalOpen] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);

  return (
    <>
      <BatchOperationBar
        selectedCount={selectedCount}
        operations={[
          { key: 'approve', label: '批量审批', icon: CheckCircle },
          { key: 'reject', label: '批量驳回', icon: XCircle },
          { key: 'export', label: '导出', icon: Download }
        ]}
        onOperation={(type) => {
          if (type === 'approve') setApprovalOpen(true);
        }}
        onClear={clearSelection}
      />

      {/* 批量审批弹窗 */}
      <BatchApprovalDialog
        open={approvalOpen}
        items={selectedRows}
        operationType="approve"
        onConfirm={(comment) => handleBatchApprove(selectedRows, comment)}
        onCancel={() => setApprovalOpen(false)}
      />

      {/* 批量结果弹窗 */}
      <BatchResultModal
        open={resultOpen}
        result={batchResult}
        onClose={() => setResultOpen(false)}
        onRetry={(failedItems) => handleRetry(failedItems)}
      />
    </>
  );
}
```

## 设计原则

1. **通用性**: 组件不依赖具体业务逻辑，通过 props 注入数据和回调
2. **可组合性**: Hook 和组件可以独立使用，也可以组合使用
3. **类型安全**: 完整的 TypeScript 类型定义
4. **可访问性**: 符合 WCAG 2.1 AA 标准
5. **主题适配**: 支持亮色/暗色主题切换

## 依赖

- `lucide-react`: 图标库
- `antd`: UI 组件库（Button, Modal, Table 等）
- `react`: React 核心
- `../../utils`: 工具函数

## 架构合规

- ✅ 有 `README.md` 目录说明
- ✅ 有 `index.ts` 公共 API 入口
- ✅ 使用路径别名 `@components/operations` 导入
- ✅ 符合架构防御规范
- ✅ 组件可被 `src/pages/operations/` 引用

## 相关文档

- [批量操作设计文档](../../../docs/plans/batch-operations.md)（待创建）
- [业务操作视图](../../pages/operations/README.md)
