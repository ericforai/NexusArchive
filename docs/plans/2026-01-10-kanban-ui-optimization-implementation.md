# 看板UI优化实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 优化电子凭证池看板界面，实现清晰视觉层级和减少界面拥挤

**Architecture:**
1. 将列级操作按钮下沉到卡片级，移除 `kanban-column__actions` 区域
2. 子状态标签从 Tabs 改为 Segmented 组件，内嵌到列标题右侧
3. 批量操作按钮集成到顶部工具栏，选中时替换布局控制按钮

**Tech Stack:** React 19.2, TypeScript 5.8, Ant Design 6, Vite 6

**设计文档:** `docs/plans/2026-01-10-kanban-ui-optimization-design.md`

---

## Task 1: 扩展 KanbanCard 组件支持列级操作

**Files:**
- Modify: `src/components/pool-kanban/KanbanCard.tsx`
- Modify: `src/components/pool-kanban/KanbanCard.css`
- Test: `src/components/pool-kanban/__tests__/KanbanCard.test.tsx`

**Step 1: 添加 ColumnAction 类型导入和 props**

编辑 `src/components/pool-kanban/KanbanCard.tsx`，在现有导入后添加：

```tsx
import type { ColumnAction } from '@/config/pool-columns.config';
```

修改 `KanbanCardProps` 接口，添加 `columnActions` prop：

```tsx
export interface KanbanCardProps {
  card: PoolItem;
  selected: boolean;
  onSelect?: (cardId: string) => void;
  onAction?: (cardId: string, action: string) => void;  // 扩展 action 类型为 string
  columnActions?: ColumnAction[];  // 新增：列级操作配置
}
```

**Step 2: 修改操作按钮渲染逻辑**

替换 `kanban-card__actions` 区域的渲染逻辑：

```tsx
// 计算要显示的操作按钮
const displayActions = useMemo(() => {
  const baseActions = [
    { key: 'view', label: '查看' },
    { key: 'edit', label: '编辑' },
  ];

  // 添加列级操作（排除删除，删除单独处理）
  if (columnActions) {
    const extraActions = columnActions
      .filter(a => a.key !== 'delete')
      .map(a => ({ key: a.key, label: a.label, danger: a.danger }));
    baseActions.push(...extraActions);
  }

  return baseActions;
}, [columnActions]);
```

替换操作按钮区域的 JSX：

```tsx
{/* 操作按钮 */}
<div className="kanban-card__actions">
  {displayActions.map(action => (
    <Button
      key={action.key}
      size="small"
      type="link"
      danger={action.danger}
      onClick={() => handleAction(action.key as any)}
    >
      {action.label}
    </Button>
  ))}
  <Button
    size="small"
    type="link"
    danger
    onClick={() => handleAction('delete')}
  >
    删除
  </Button>
</div>
```

修改 `handleAction` 函数：

```tsx
const handleAction = (action: string) => {
  onAction?.(card.id, action);
};
```

**Step 3: 更新 KanbanCard.css 样式**

编辑 `src/components/pool-kanban/KanbanCard.css`，修改 `.kanban-card__actions` 样式：

```css
.kanban-card__actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.kanban-card__actions .ant-btn-link {
  padding: 0 4px;
  height: auto;
  font-size: 12px;
  line-height: 1.5;
}
```

**Step 4: 更新测试文件**

编辑 `src/components/pool-kanban/__tests__/KanbanCard.test.tsx`，添加列级操作测试：

```tsx
it('should render column actions when provided', () => {
  const columnActions = [
    { key: 'recheck', label: '重新检测' },
    { key: 'delete', label: '删除', danger: true },
  ];
  const { getByText } = render(
    <KanbanCard
      card={mockCard}
      selected={false}
      columnActions={columnActions}
      onAction={vi.fn()}
    />
  );
  expect(getByText('重新检测')).toBeInTheDocument();
});

it('should call onAction with correct action key', () => {
  const mockOnAction = vi.fn();
  const columnActions = [{ key: 'recheck', label: '重新检测' }];
  const { getByText } = render(
    <KanbanCard
      card={mockCard}
      selected={false}
      columnActions={columnActions}
      onAction={mockOnAction}
    />
  );
  fireEvent.click(getByText('重新检测'));
  expect(mockOnAction).toHaveBeenCalledWith('test-id-1', 'recheck');
});
```

**Step 5: 运行测试验证**

```bash
cd .worktrees/kanban-ui-opt
npm test -- src/components/pool-kanban/__tests__/KanbanCard.test.tsx
```

预期: 测试通过

**Step 6: 提交**

```bash
cd .worktrees/kanban-ui-opt
git add src/components/pool-kanban/KanbanCard.tsx src/components/pool-kanban/KanbanCard.css src/components/pool-kanban/__tests__/KanbanCard.test.tsx
git commit -m "feat(kanban): extend KanbanCard to support column-level actions"
```

---

## Task 2: 改造 KanbanColumn 使用 Segmented 替代 Tabs

**Files:**
- Modify: `src/components/pool-kanban/KanbanColumn.tsx`
- Modify: `src/components/pool-kanban/KanbanColumn.css`
- Test: `src/components/pool-kanban/__tests__/KanbanColumn.test.tsx`

**Step 1: 替换 Tabs 为 Segmented**

编辑 `src/components/pool-kanban/KanbanColumn.tsx`，修改导入：

```tsx
import { Badge, Button, Dropdown, Segmented } from 'antd';
```

移除 `Tabs` 导入（如果已存在），添加 `Segmented`。

**Step 2: 修改列标题区域 JSX**

替换 `kanban-column__header` 的 JSX 结构：

```tsx
<div className="kanban-column__header">
  <div className="kanban-column__title-row">
    <h3 className="kanban-column__title">{column.title}</h3>

    {/* 内嵌式子状态选择器 */}
    <div className="kanban-column__sub-states">
      <Segmented
        options={column.subStates.map(sub => ({
          label: (
            <span className="kanban-column__sub-state-option">
              {sub.label}
              <Badge count={subStateCounts.get(sub.value) || 0} size="small" />
            </span>
          ),
          value: sub.value,
        }))}
        value={activeTab}
        onChange={setActiveTab}
        size="small"
      />
    </div>

    <Badge count={cards.length} showZero className="kanban-column__total-badge" />
  </div>
</div>
```

**Step 3: 移除列操作按钮区域**

删除整个 `kanban-column__actions` div 及相关代码：

```tsx
{/* 删除以下区域 */}
<div className="kanban-column__actions">
  {visibleActions.map(action => (...))}
  {column.actions.length > 3 && (...)}
</div>
```

同时移除相关变量定义：
- `visibleActions`
- `moreActionsMenu`

**Step 4: 更新 CSS 样式**

编辑 `src/components/pool-kanban/KanbanColumn.css`，替换样式：

```css
.kanban-column__title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  height: 40px;
  margin-bottom: 0;
}

.kanban-column__sub-states {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
}

.kanban-column__sub-state-option {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  white-space: nowrap;
}

/* Segmented 组件自定义样式 */
.kanban-column__sub-states .ant-segmented {
  background: #f5f5f5;
  padding: 2px;
  font-size: 12px;
}

.kanban-column__sub-states .ant-segmented-item {
  padding: 2px 8px;
  min-height: 24px;
  line-height: 20px;
}

/* 总计徽章 - 使用蓝色替代红色 */
.kanban-column__total-badge .ant-badge-count {
  background: #1890ff;
}

.kanban-column__total-badge .ant-badge-count-zero {
  background: #d9d9d9;
}

/* 移除 tabs 样式 */
/* .kanban-column__tabs { ... } - 删除 */

/* 移除 actions 样式 */
/* .kanban-column__actions { ... } - 删除 */
```

**Step 5: 更新测试文件**

编辑 `src/components/pool-kanban/__tests__/KanbanColumn.test.tsx`，更新测试：

```tsx
it('should render sub-state Segmented instead of Tabs', () => {
  const { container } = render(
    <KanbanColumn
      column={mockColumn}
      cards={[]}
      selectedIds={new Set()}
      onSelectionChange={vi.fn()}
      onAction={vi.fn()}
    />
  );
  // 验证存在 Segmented 组件
  expect(container.querySelector('.ant-segmented')).toBeInTheDocument();
});

it('should not render column actions area', () => {
  const { container } = render(
    <KanbanColumn
      column={mockColumn}
      cards={[]}
      selectedIds={new Set()}
      onSelectionChange={vi.fn()}
      onAction={vi.fn()}
    />
  );
  // 验证列操作区域不存在
  expect(container.querySelector('.kanban-column__actions')).not.toBeInTheDocument();
});
```

**Step 6: 运行测试验证**

```bash
cd .worktrees/kanban-ui-opt
npm test -- src/components/pool-kanban/__tests__/KanbanColumn.test.tsx
```

预期: 测试通过

**Step 7: 提交**

```bash
cd .worktrees/kanban-ui-opt
git add src/components/pool-kanban/KanbanColumn.tsx src/components/pool-kanban/KanbanColumn.css src/components/pool-kanban/__tests__/KanbanColumn.test.tsx
git commit -m "feat(kanban): replace Tabs with Segmented for sub-states, remove column actions"
```

---

## Task 3: 更新 PoolKanbanView 传递列操作到卡片

**Files:**
- Modify: `src/components/pool-kanban/PoolKanbanView.tsx`
- Test: `src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx`

**Step 1: 创建获取列操作的辅助函数**

在 `KanbanBoard` 组件前添加函数：

```tsx
/**
 * 根据卡片状态获取所属列的操作配置
 */
function getColumnActionsForCard(
  card: PoolItem,
  columns: ColumnGroupConfig[]
): ColumnAction[] {
  const column = columns.find(col =>
    col.subStates.some(sub => sub.value === card.status)
  );
  return column?.actions || [];
}
```

**Step 2: 修改 KanbanColumn 渲染，传递操作配置**

由于移除了列级操作按钮，`onAction` 回调不再需要，但需要将操作配置传递给卡片。调整 `KanbanBoard` 中的列渲染：

```tsx
return (
  <KanbanColumn
    key={column.id}
    column={column}
    cards={columnCards}
    selectedIds={selectedIds}
    onSelectionChange={onSelectionChange}
    onAction={(actionKey, cards) => {
      // 列级操作触发时，选择所有卡片并执行
      onColumnAction(actionKey, cards);
    }}
  />
);
```

注意：由于列操作按钮已移除，此回调实际上不会被触发，但保留接口兼容性。

**Step 3: 更新 KanbanColumn 传递操作配置到卡片**

编辑 `src/components/pool-kanban/KanbanColumn.tsx`，修改卡片渲染：

```tsx
{currentCards.map(card => (
  <KanbanCard
    key={card.id}
    card={card}
    selected={selectedIds.has(card.id)}
    onSelect={handleCardSelect}
    onAction={handleCardAction}
    columnActions={column.actions}  // 新增：传递列操作配置
  />
))}
```

**Step 4: 运行测试验证**

```bash
cd .worktrees/kanban-ui-opt
npm test -- src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx
```

预期: 测试通过

**Step 5: 提交**

```bash
cd .worktrees/kanban-ui-opt
git add src/components/pool-kanban/PoolKanbanView.tsx src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx
git commit -m "feat(kanban): pass column actions to KanbanCard"
```

---

## Task 4: 顶部工具栏集成批量操作

**Files:**
- Modify: `src/components/pool-kanban/PoolKanbanView.tsx`
- Modify: `src/components/pool-kanban/PoolKanbanView.css`
- Test: `src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx`

**Step 1: 修改 KanbanToolbar 接口和实现**

编辑 `src/components/pool-kanban/PoolKanbanView.tsx`，更新 `KanbanToolbarProps`：

```tsx
interface KanbanToolbarProps {
  totalColumns: number;
  visibleColumns: number;
  collapsedColumns: number;
  selectedCount: number;
  hasSelection: boolean;
  onExpandAll: () => void;
  onCollapseEmpty: () => void;
  onBatchAction?: (action: string) => void;
  onClearSelection?: () => void;
}
```

更新 `KanbanToolbar` 组件实现：

```tsx
function KanbanToolbar({
  totalColumns,
  visibleColumns,
  collapsedColumns,
  selectedCount,
  hasSelection,
  onExpandAll,
  onCollapseEmpty,
  onBatchAction,
  onClearSelection,
}: KanbanToolbarProps) {
  return (
    <div className="pool-kanban-view__toolbar">
      <div className="pool-kanban-view__title-section">
        <h2 className="pool-kanban-view__title">电子凭证池</h2>
        {collapsedColumns > 0 && !hasSelection && (
          <span className="pool-kanban-view__layout-info">
            {visibleColumns} / {totalColumns} 列
          </span>
        )}
        {hasSelection && (
          <span className="pool-kanban-view__selection-info">
            已选 {selectedCount} 项
          </span>
        )}
      </div>
      <div className="pool-kanban-view__actions">
        <Space>
          {hasSelection ? (
            /* 批量操作模式 */
            <>
              <Button
                size="small"
                onClick={() => onBatchAction?.('edit')}
              >
                批量编辑
              </Button>
              <Button
                size="small"
                onClick={() => onBatchAction?.('recheck')}
              >
                批量检测
              </Button>
              <Button
                size="small"
                danger
                onClick={() => onBatchAction?.('delete')}
              >
                批量删除
              </Button>
              <Button
                size="small"
                onClick={() => onClearSelection?.()}
              >
                取消选择
              </Button>
            </>
          ) : (
            /* 默认布局控制模式 */
            <>
              {collapsedColumns > 0 && (
                <Button
                  size="small"
                  icon={<Expand size={14} />}
                  onClick={onExpandAll}
                >
                  展开全部
                </Button>
              )}
              <Button
                size="small"
                icon={<Columns3 size={14} />}
                onClick={onCollapseEmpty}
              >
                折叠空列
              </Button>
            </>
          )}
        </Space>
      </div>
    </div>
  );
}
```

**Step 2: 添加批量操作回调实现**

在 `PoolKanbanView` 组件中添加：

```tsx
// 处理批量操作
const handleBatchAction = useCallback((action: string) => {
  const actionType = ACTION_KEY_TO_BATCH_TYPE[action];
  if (actionType) {
    setPendingAction(actionType);
    setPendingActionLabel(ACTION_LABELS[action] || '执行操作');
  }
}, []);

// 取消选择
const handleClearSelection = useCallback(() => {
  batchAction.clearSelection();
  setPendingAction(null);
  setPendingActionLabel('');
}, [batchAction]);
```

**Step 3: 更新 KanbanToolbar 调用**

修改 `KanbanToolbar` 的调用：

```tsx
<KanbanToolbar
  totalColumns={layoutInfo.totalColumns}
  visibleColumns={layoutInfo.visibleColumns}
  collapsedColumns={layoutInfo.collapsedColumns}
  selectedCount={selectedCount}
  hasSelection={selectedCount > 0}
  onExpandAll={handleExpandAll}
  onCollapseEmpty={handleCollapseAllEmpty}
  onBatchAction={handleBatchAction}
  onClearSelection={handleClearSelection}
/>
```

**Step 4: 更新 CSS 样式**

编辑 `src/components/pool-kanban/PoolKanbanView.css`，添加：

```css
.pool-kanban-view__selection-info {
  padding: 2px 8px;
  background: #e6f7ff;
  border-radius: 4px;
  font-size: 12px;
  color: #1890ff;
}
```

**Step 5: 隐藏底部批量操作栏（使用顶部替代）**

在渲染部分注释掉或条件隐藏 `BatchActionBar`：

```tsx
{/* 批量操作已移至顶部工具栏，保留作为结果提示 */}
{batchAction.state.result && (
  <BatchActionBar
    selectedCount={selectedCount}
    actionLabel={pendingActionLabel}
    isExecuting={batchAction.state.isExecuting}
    onExecute={handleExecuteBatchAction}
    onCancel={handleCloseResult}
    result={batchAction.state.result}
  />
)}
```

**Step 6: 更新测试**

编辑 `src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx`，添加：

```tsx
it('should show batch action buttons when items are selected', () => {
  const { getByText, container } = render(
    <PoolKanbanView />
  );
  // 模拟选中操作后...
  expect(getByText('批量编辑')).toBeInTheDocument();
  expect(getByText('批量删除')).toBeInTheDocument();
});

it('should show layout controls when no selection', () => {
  const { getByText } = render(
    <PoolKanbanView />
  );
  expect(getByText('折叠空列')).toBeInTheDocument();
});
```

**Step 7: 运行测试验证**

```bash
cd .worktrees/kanban-ui-opt
npm test -- src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx
```

预期: 测试通过

**Step 8: 提交**

```bash
cd .worktrees/kanban-ui-opt
git add src/components/pool-kanban/PoolKanbanView.tsx src/components/pool-kanban/PoolKanbanView.css src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx
git commit -m "feat(kanban): integrate batch actions into top toolbar"
```

---

## Task 5: 运行完整测试套件并修复

**Files:**
- Test: All affected test files

**Step 1: 运行看板组件所有测试**

```bash
cd .worktrees/kanban-ui-opt
npm test -- src/components/pool-kanban/
```

预期: 所有测试通过

**Step 2: 运行前端测试套件**

```bash
cd .worktrees/kanban-ui-opt
npm run test:run
```

预期: 所有测试通过

**Step 3: 类型检查**

```bash
cd .worktrees/kanban-ui-opt
npm run typecheck
```

预期: 无类型错误

**Step 4: 修复发现的问题**

如有问题，逐个修复并提交。

**Step 5: 提交修复（如有）**

```bash
cd .worktrees/kanban-ui-opt
git commit -am "fix(kanban): fix tests and type issues"
```

---

## Task 6: 更新文档

**Files:**
- Modify: `src/components/pool-kanban/README.md`

**Step 1: 更新 README**

编辑 `src/components/pool-kanban/README.md`，添加变更说明：

```markdown
## 变更记录

### 2026-01-10: UI 优化

- 移除列级操作按钮区域，操作下沉到卡片级
- 子状态标签从 Tabs 改为 Segmented 组件，内嵌到列标题
- 批量操作集成到顶部工具栏
```

**Step 2: 提交文档**

```bash
cd .worktrees/kanban-ui-opt
git add src/components/pool-kanban/README.md
git commit -m "docs(kanban): update README with UI optimization changes"
```

---

## Task 7: 合并到主分支

**Step 1: 切换到主分支并拉取最新**

```bash
git checkout main
git pull origin main
```

**Step 2: 合并功能分支**

```bash
git merge feature/kanban-ui-optimization --no-ff
```

**Step 3: 推送合并**

```bash
git push origin main
```

**Step 4: 清理 worktree**

```bash
git worktree remove .worktrees/kanban-ui-opt
git branch -d feature/kanban-ui-optimization
```

---

## 验收清单

- [ ] 列头显示内嵌式子状态选择器（Segmented）
- [ ] 列操作按钮区域已移除
- [ ] 卡片显示对应列的操作按钮
- [ ] 顶部工具栏在有选中时显示批量操作
- [ ] 顶部工具栏无选中时显示布局控制
- [ ] 所有测试通过
- [ ] 类型检查无错误
- [ ] 文档已更新
