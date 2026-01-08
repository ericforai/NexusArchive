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
├── BatchApprovalDialog.tsx       # 批量审批弹窗 ✅
└── BatchResultModal.tsx          # 批量结果报告弹窗 ✅
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
**功能**: 批量审批确认弹窗（已实现 ✅）

#### Props

```typescript
interface BatchApprovalDialogProps {
  visible: boolean;                        // 是否显示对话框
  selectedCount: number;                   // 已选中记录数量
  action: 'approve' | 'reject';            // 操作类型：批准或拒绝
  onConfirm: (comment: string, skipIds: number[]) => void | Promise<void>;  // 确认回调
  onCancel: () => void;                    // 取消回调
  selectedRecords?: ApprovalRecord[];      // 已选中记录列表（用于跳过部分记录）
  loading?: boolean;                       // 加载状态
}

interface ApprovalRecord {
  id: number;                              // 记录 ID
  title?: string;                          // 记录标题
  code?: string;                           // 记录编号
}
```

#### 功能特性

- **统一审批意见输入**：
  - 批准时意见可选
  - 驳回时意见必填
  - 500 字符限制，带实时计数器
- **跳过部分记录**：
  - 可展开记录列表，选择跳过部分记录单独处理
  - 显示已跳过数量标签
  - 无记录时的友好提示
- **阈值确认提示**（>= 10 条时触发）：
  - 显示警告提示框
  - 展示前 5 条记录清单
  - 超过 5 条时显示总数
- **实际处理数量提示**：
  - 跳过记录后显示实际将处理的数量
  - 蓝色提示框区分于警告提示
- **加载状态**：
  - 禁用所有交互元素
  - 按钮显示加载动画
- **响应式设计**：
  - 支持亮色/暗色主题
  - 自适应布局

#### UI 状态

- **批准模式**：绿色主题 (emerald)，确认按钮为"确认审批"
- **驳回模式**：红色主题 (rose)，确认按钮为"确认驳回"，意见输入框带必填标记
- **确认阈值**：10 条记录时显示二次确认提示和记录清单
- **跳过记录**：可展开/收起的记录列表，支持复选框切换跳过状态

### BatchResultModal

**位置**: `BatchResultModal.tsx`
**类型**: React.FC
**功能**: 批量操作结果报告弹窗（已实现 ✅）

#### Props

```typescript
interface BatchResultModalProps {
  visible: boolean;                        // 是否显示对话框
  successCount: number;                    // 成功数量
  failedCount: number;                     // 失败数量
  errors?: BatchError[];                   // 错误列表
  onRetry?: (failedIds: number[]) => void | Promise<void>;  // 重试回调（传入失败的 ID 列表）
  onClose: () => void;                     // 关闭回调
  operationType?: 'approval' | 'operation'; // 操作类型（用于标题显示，默认 'operation'）
  onExportReport?: () => void | Promise<void>;  // 导出失败报告回调
  isRetrying?: boolean;                    // 重试中状态（默认 false）
}

interface BatchError {
  id: number;                              // 记录 ID
  reason: string;                          // 失败原因
}
```

#### 功能特性

- **动态标题切换**：根据 `operationType` 显示"批量审批完成"或"批量操作完成"
- **成功/失败统计摘要**：
  - 成功数量统计（emerald 主题）
  - 失败数量统计（rose 主题，失败数量为 0 时置灰）
- **状态图标系统**：
  - 全部成功：CheckCircle（emerald）+ "全部成功"标题
  - 全部失败：XCircle（rose）+ "全部失败"标题
  - 部分成功：CheckCircle（amber）+ "部分成功"标题
- **失败详情列表**：
  - 可滚动列表（最大高度 240px）
  - 显示记录 ID 和失败原因
  - 失败项带 XCircle 图标
  - 友好提示：支持重试或导出报告
- **重试功能**：
  - 仅在有失败项且提供 `onRetry` 回调时显示
  - 重试中状态：按钮显示旋转图标和"重试中..."文本
  - 禁用状态：重试中禁用关闭和导出按钮
- **导出报告功能**：
  - 仅在有失败项且提供 `onExportReport` 回调时显示
  - 带下载图标
- **全部成功提示框**：
  - 绿色背景提示框
  - 显示庆祝消息 "🎉 所有记录均已成功处理完成！"
- **加载状态**：
  - 重试中禁用所有交互
  - 禁用点击背景关闭
  - 隐藏关闭按钮
- **响应式设计**：
  - 支持亮色/暗色主题
  - 自适应布局

#### UI 状态

- **全部成功**（failedCount = 0）：emerald 主题，成功提示框
- **全部失败**（successCount = 0）：rose 主题，失败详情列表
- **部分成功**（两者都 > 0）：amber 主题，失败详情列表

#### 使用示例

```typescript
import { BatchResultModal } from '@components/operations';

function MyBatchView() {
  const [resultOpen, setResultOpen] = useState(false);
  const [isRetrying, setIsRetrying] = useState(false);
  const [batchResult, setBatchResult] = useState({
    successCount: 95,
    failedCount: 5,
    errors: [
      { id: 1, reason: '状态不允许审批' },
      { id: 2, reason: '网络连接失败' }
    ]
  });

  const handleRetry = async (failedIds: number[]) => {
    setIsRetrying(true);
    try {
      // 重试逻辑
      await retryItems(failedIds);
    } finally {
      setIsRetrying(false);
    }
  };

  const handleExportReport = async () => {
    // 导出失败报告
    const report = generateErrorReport(batchResult.errors);
    downloadFile(report, 'batch-errors.xlsx');
  };

  return (
    <BatchResultModal
      visible={resultOpen}
      successCount={batchResult.successCount}
      failedCount={batchResult.failedCount}
      errors={batchResult.errors}
      operationType="approval"
      onRetry={handleRetry}
      onExportReport={handleExportReport}
      isRetrying={isRetrying}
      onClose={() => setResultOpen(false)}
    />
  );
}
```

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
        visible={approvalOpen}
        selectedCount={getSelectedCount()}
        action="approve"
        onConfirm={(comment, skipIds) => handleBatchApprove(selectedIds, comment, skipIds)}
        onCancel={() => setApprovalOpen(false)}
        selectedRecords={selectedRecords}
        loading={isProcessing}
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

- [批量操作设计文档](../../../docs/plans/2026-01-07-batch-operations-design.md)
- [业务操作视图](../../pages/operations/README.md)
