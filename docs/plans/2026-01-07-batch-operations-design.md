// Input: 头脑风暴讨论结果
// Output: 批量操作功能设计文档
// Pos: docs/plans/2026-01-07-batch-operations-design.md

# 批量操作功能设计

> 设计日期: 2026-01-07
> 设计目标: 提供通用的批量审批能力，支持所有审批场景

## 1. 背景

当前系统已支持批量上传，但审批类操作仍需逐条处理，效率较低。本设计旨在提供统一的批量操作能力，提升用户处理效率。

### 现状分析

| 功能 | 现状 | 批量支持 |
|------|------|----------|
| 档案归档审批 (`ArchiveApprovalView`) | ✅ 存在 | ❌ 仅单个 |
| 归档批次审批 (`ArchiveBatchView`) | ✅ 存在 | ❌ 仅单个 |
| 销毁审批 (`DestructionApprovalPage`) | ✅ 存在 | ❌ 仅单个 |
| 批量上传 (`BatchUploadView`) | ✅ 存在 | ✅ 已支持 |

---

## 2. 整体架构

### 2.1 核心组件设计

创建通用批量操作层，位于 `src/components/operations/`：

```
src/components/operations/
├── BatchOperationBar.tsx      # 批量操作工具栏组件
├── BatchApprovalDialog.tsx    # 批量审批弹窗
├── BatchResultModal.tsx       # 批量结果报告
└── useBatchOperation.ts       # 批量操作 Hook
```

### 2.2 集成方式

各审批视图通过引入 `BatchOperationBar` 组件获得批量能力：

```tsx
// ArchiveApprovalView.tsx
import { BatchOperationBar, useBatchSelection } from '@/components/operations';

const ArchiveApprovalView = () => {
  const { selectedIds, rowSelection, clearSelection } = useBatchSelection();

  return (
    <>
      <BatchOperationBar
        selectedCount={selectedIds.length}
        onBatchApprove={() => handleBatchApprove(selectedIds)}
        onBatchReject={() => handleBatchReject(selectedIds)}
        onClear={clearSelection}
      />
      <Table rowSelection={rowSelection} />
    </>
  );
};
```

### 2.3 API 层设计

后端新增通用批量审批接口：

```
POST /api/{entity}/batch-approve
Body: {
  ids: number[],
  comment?: string,
  skipIds?: number[]
}

POST /api/{entity}/batch-reject
Body: {
  ids: number[],
  comment: string,
  skipIds?: number[]
}
```

返回值：

```json
{
  "code": 200,
  "data": {
    "success": 12,
    "failed": 3,
    "errors": [
      { "id": 123, "reason": "状态已变更" },
      { "id": 145, "reason": "权限不足" },
      { "id": 178, "reason": "四性检测未通过" }
    ]
  }
}
```

---

## 3. 交互流程

### 3.1 批量选择交互

1. **表格行选择**
   - 在审批列表表格增加 `rowSelection` 配置
   - 复选框列固定在左侧
   - 支持全选/取消全选当前页
   - 选中行高亮显示

2. **快捷全选**
   - 工具栏提供「全选当前筛选结果」按钮
   - 调用 API 获取当前筛选条件下的所有 ID
   - 选中状态显示「已选 500 条（当前页 10 条）」

3. **工具栏显隐**
   - 只有选中 >0 条时才显示批量操作栏
   - 固定在表格上方
   - 显示「已选择 X 条」+ 操作按钮组

### 3.2 批量审批流程图

```
用户选择记录
    ↓
点击批量审批
    ↓
弹出审批对话框
    ↓
填写统一意见 + 选择是否跳过部分记录
    ↓
确认
    ↓
数量检查
    ├─ ≤10 条 → 直接执行
    └─ >10 条 → 确认弹窗（显示清单）
         ↓
       执行
         ↓
       判断结果数量
         ├─ ≤50 条 → 立即弹窗详情
         └─ >50 条 → 转后台任务
```

---

## 4. UI 组件设计

### 4.1 BatchOperationBar 组件

```tsx
interface BatchOperationBarProps {
  selectedCount: number;
  totalCount?: number;  // 筛选结果总数（用于全选）
  onBatchApprove: () => void;
  onBatchReject: () => void;
  onSelectAll: () => void;
  onClear: () => void;
}

// 渲染：
// [已选择 {selectedCount} 条] [全选所有] [批量批准] [批量拒绝] [清空]
```

### 4.2 批量审批弹窗

```
┌─────────────────────────────────────┐
│ 批量审批 (已选 15 条)                │
├─────────────────────────────────────┤
│ 统一审批意见                         │
│ ┌─────────────────────────────────┐ │
│ │ [文本框，可选填]                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ □ 跳过部分记录单独处理              │
│   → 展开后显示已选记录列表，可取消勾 │
│                                     │
│        [取消]  [确认审批]            │
└─────────────────────────────────────┘
```

### 4.3 批量结果报告

```
┌─────────────────────────────────────┐
│ 批量审批完成                         │
├─────────────────────────────────────┤
│ ✓ 成功 12 条  ✗ 失败 3 条           │
│                                     │
│ 失败详情：                           │
│ • 档案 #00123: 状态已变更           │
│ • 档案 #00145: 权限不足             │
│ • 档案 #00178: 四性检测未通过       │
│                                     │
│                    [关闭] [重试失败] │
└─────────────────────────────────────┘
```

---

## 5. 边界约束

| 阈值 | 行为 |
|------|------|
| **单次上限** | 100 条，超出时 toast 提示「单次最多 100 条，请分批操作」 |
| **确认阈值** | >10 条时，弹窗确认清单：「即将批准以下 15 条记录...」 |
| **结果展示** | ≤50 条立即弹窗详情；>50 条转后台任务，提示「已在任务中心创建」 |

---

## 6. 错误处理

| 场景 | 处理方式 |
|------|----------|
| **部分失败** | 已成功的保持不变，失败的列出原因 |
| **全部失败** | 回滚操作，提示用户检查后重试 |
| **网络中断** | 提示操作状态未知，引导查看任务中心 |

---

## 7. 后台任务集成

大批量操作（>50 条）时创建异步任务：

```tsx
if (selectedIds.length > 50) {
  const taskId = await createBatchTask({
    type: 'BATCH_APPROVE',
    entity: 'archive_approval',
    ids: selectedIds,
    comment: unifiedComment
  });
  toast.success(`已在任务中心创建批量审批任务 #${taskId}`);
}
```

复用现有任务中心，用户可查看进度和结果。

---

## 8. 实施范围

### Phase 1: 核心组件
- [ ] `BatchOperationBar.tsx`
- [ ] `BatchApprovalDialog.tsx`
- [ ] `BatchResultModal.tsx`
- [ ] `useBatchOperation.ts`

### Phase 2: 档案归档审批集成
- [ ] 更新 `ArchiveApprovalView.tsx`
- [ ] 后端 API 实现

### Phase 3: 其他场景集成
- [ ] `ArchiveBatchView.tsx` 批量审批
- [ ] `DestructionApprovalPage.tsx` 批量审批
- [ ] 其他审批场景

### Phase 4: 后台任务集成
- [ ] 大批量任务创建
- [ ] 任务中心进度展示

---

## 9. 测试要点

- [ ] 边界测试：0 条、1 条、10 条、50 条、100 条、101 条
- [ ] 全选测试：当前页、筛选结果
- [ ] 跳过记录测试
- [ ] 部分失败场景
- [ ] 后台任务创建和查询
