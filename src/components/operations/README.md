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
├── manifest.config.ts            # 模块清单
├── README.md                     # 本文件
├── useBatchSelection.ts          # 批量选择 Hook ✅
├── BatchOperationBar.tsx         # 批量操作工具栏 ✅
├── BatchApprovalDialog.tsx       # 批量审批弹窗（待实现）
└── BatchResultModal.tsx          # 批量结果报告（待实现）
```

## 组件说明

### useBatchSelection

**位置**: `useBatchSelection.ts`
**类型**: Custom Hook
**功能**: 批量选择状态管理

#### API

```typescript
interface UseBatchSelectionReturn {
  // 状态
  selectedIds: Set<number>;              // 已选中的 ID 集合
  selectAllMode: boolean;                // 是否全选模式
  lastError?: SelectionResult;           // 最后一次操作错误（如果有）

  // Ant Design Table rowSelection 配置
  rowSelection: RowSelectionConfig;

  // 操作方法
  clearSelection: () => void;            // 清空选择
  toggleSelection: (id: number) => SelectionResult;  // 切换单条选中状态
  setSelectedIds: (ids: Set<number> | number[]) => SelectionResult;  // 设置选中 ID 集合
  selectAll: (allIds: number[]) => SelectionResult;  // 全选所有记录
  getSelectedCount: () => number;        // 获取选中数量
  isSelected: (id: number) => boolean;   // 检查是否选中
}

interface SelectionResult {
  success: boolean;
  reason?: string;  // 失败原因
}
```

#### 功能特性

- 跨页选择状态维护
- 全选/反选控制
- 选中项数量统计
- 选择限制（最多 100 条）
- 错误状态跟踪（通过 `lastError`）
- `isSelected` 辅助方法（检查特定 ID 是否选中）

### BatchOperationBar

**位置**: `BatchOperationBar.tsx`
**类型**: React.FC
**功能**: 批量操作工具栏组件（已实现 ✅）

#### Props

```typescript
interface BatchOperationBarProps {
  selectedCount: number;           // 已选中项数量
  totalCount?: number;             // 筛选结果总数（用于全选）
  onBatchApprove: () => void;      // 批量批准回调
  onBatchReject: () => void;       // 批量拒绝回调
  onSelectAll: () => void;         // 全选所有回调
  onClear: () => void;             // 清空选择回调
  loading?: boolean;               // 加载状态
}
```

#### 功能特性

- 显示选中数量和筛选结果总数
- 全选所有按钮（当 `totalCount` 存在且 ≤ 100 时显示）
- 批量批准按钮（主要样式 - emerald）
- 批量拒绝按钮（危险样式 - rose）
- 清空选择按钮
- 超过 100 条时禁用操作并内联提示（不使用 toast）
- 加载状态支持（`loading` prop）
- 响应式设计，支持暗色模式

#### 边界约束

- **最多选择 100 条记录**：超过此限制时禁用批量批准/拒绝按钮
- **全选按钮限制**：仅在 `totalCount ≤ 100` 时显示全选按钮
- **内联提示**：超过限制时在工具栏内显示警告信息，不使用 toast 通知

#### 样式规范

- 使用 Tailwind CSS 工具类
- 左侧显示选中信息（图标 + 文本）
- 右侧显示操作按钮组
- 主色调：primary-50/600（选中栏背景）
- 成功操作：emerald-600（批准）
- 危险操作：rose-600（拒绝）
- 分隔线：slate-300（按钮组分隔）

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

### 基础用法

```typescript
import { useBatchSelection } from '@components/operations';

function MyTable() {
  const {
    selectedIds,          // 已选中的 ID 集合
    selectAllMode,        // 是否全选模式
    lastError,            // 最后一次操作错误
    rowSelection,         // Ant Design Table rowSelection 配置
    clearSelection,       // 清空选择
    toggleSelection,      // 切换单条选中状态
    setSelectedIds,       // 设置选中 ID 集合
    selectAll,            // 全选所有记录
    getSelectedCount,     // 获取选中数量
    isSelected            // 检查是否选中
  } = useBatchSelection();

  // 示例：手动切换选中状态
  const handleToggle = (id: number) => {
    const result = toggleSelection(id);
    if (!result.success) {
      message.warning(result.reason);  // "Cannot select more than 100 items"
    }
  };

  // 示例：全选当前筛选结果
  const handleSelectAll = () => {
    const result = selectAll(currentPageIds);
    if (!result.success) {
      message.warning(result.reason);
    }
  };

  // 示例：设置选中 ID（从外部来源）
  const handleSetIds = () => {
    const result = setSelectedIds([1, 2, 3, 4, 5]);
    if (!result.success) {
      message.error(result.reason);
    }
  };

  // 监听错误状态（可选）
  useEffect(() => {
    if (lastError && !lastError.success) {
      message.error(lastError.reason);
    }
  }, [lastError]);

  return (
    <>
      {/* 使用 rowSelection 配置 Ant Design Table */}
      <Table
        rowSelection={rowSelection}
        dataSource={data}
      />

      {/* 显示选中数量 */}
      <div>已选择 {getSelectedCount()} 条</div>

      {/* 检查特定项是否选中 */}
      {isSelected(1) && <div>ID 1 已选中</div>}
    </>
  );
}
```

### 批量操作工具栏集成

```typescript
import {
  useBatchSelection,
  BatchOperationBar,
  BatchApprovalDialog,
  BatchResultModal
} from '@components/operations';

function MyBatchView() {
  const {
    selectedIds,
    rowSelection,
    clearSelection,
    getSelectedCount,
    lastError
  } = useBatchSelection();

  const [approvalOpen, setApprovalOpen] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);

  const handleBatchApprove = () => {
    // 检查错误
    if (lastError && !lastError.success) {
      message.error(lastError.reason);
      return;
    }

    setApprovalOpen(true);
  };

  return (
    <>
      <BatchOperationBar
        selectedCount={getSelectedCount()}
        totalCount={filteredData.length}
        onBatchApprove={handleBatchApprove}
        onBatchReject={handleBatchReject}
        onSelectAll={() => selectAll(filteredData.map(d => d.id))}
        onClear={clearSelection}
        loading={isProcessing}
      />

      <Table rowSelection={rowSelection} dataSource={data} />

      {/* 批量审批弹窗 */}
      <BatchApprovalDialog
        open={approvalOpen}
        items={Array.from(selectedIds)}
        operationType="approve"
        onConfirm={(comment) => handleBatchApprove(selectedIds, comment)}
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

- ✅ 有 `manifest.config.ts` 模块清单
- ✅ 有 `README.md` 目录说明
- ✅ 有 `index.ts` 公共 API 入口
- ✅ 使用路径别名 `@components/operations` 导入
- ✅ 符合架构防御规范
- ✅ 组件可被 `src/pages/operations/` 引用

## 相关文档

- [批量操作设计文档](../../../docs/plans/batch-operations.md)（待创建）
- [业务操作视图](../../pages/operations/README.md)
