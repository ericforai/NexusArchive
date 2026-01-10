# 看板界面优化设计

**日期**: 2026-01-10
**状态**: 设计完成，待实施
**作者**: Claude Code

---

## 背景

当前电子凭证池看板界面存在以下问题：
1. **视觉层级混乱** — 列标题、子状态标签、列操作按钮垂直堆叠，缺乏视觉分隔
2. **界面拥挤** — 每列 3+ 个操作按钮占用过多垂直空间
3. **颜色语义不清** — 红色徽章（计数）和红色删除按钮使用相同红色，易混淆
4. **子状态标签定位** — 看起来像全局导航，实际是列内过滤器

**优化目标**：
- **清晰视觉层级** — 通过视觉分隔让各元素各司其职
- **减少界面拥挤** — 释放垂直空间，让卡片内容更突出

---

## 设计方案

### 整体布局结构

优化后每列只保留 **两个区域**：

```
┌─────────────────────────────────────┐
│ 【待处理】 3   草稿 | 待检测        │  ← 列标题区（子状态内嵌）
├─────────────────────────────────────┤
│                                     │
│  ┌──────────────────────────────┐  │
│  │ 卡片 1                        │  │
│  │ [查看] [编辑] [删除]          │  ← 卡片级操作
│  └──────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

**变更点**：
- 移除 `kanban-column__actions` 整个区域
- 子状态标签从 `Tabs` 组件改为内嵌式 `Segmented` 组件
- 列头高度从 ~120px 减少到 ~50px
- **节省空间：~70px/列，四列共 ~280px**

---

### 1. 内嵌式子状态标签

子状态标签采用 **Ant Design Segmented** 组件，内嵌在列标题右侧：

```tsx
<div className="kanban-column__header">
  <div className="kanban-column__title-row">
    <h3 className="kanban-column__title">待处理</h3>

    <div className="kanban-column__sub-states">
      <Segmented
        options={column.subStates.map(sub => ({
          label: (
            <span className="sub-state-option">
              {sub.label}
              <Badge count={subStateCounts.get(sub.value)} size="small" />
            </span>
          ),
          value: sub.value,
        }))}
        value={activeTab}
        onChange={setActiveTab}
        size="small"
      />
    </div>

    <Badge count={cards.length} showZero className="total-badge" />
  </div>
</div>
```

**样式要点**：
- 使用 `Segmented` 组件，`size="small"`
- 每个选项内嵌小尺寸徽章显示计数
- 总计徽章改用 **蓝色/灰色**，避免与删除按钮混淆

---

### 2. 卡片级操作

移除列级按钮后，所有操作下沉到卡片上：

```tsx
<div className="kanban-card__actions">
  {/* 核心操作：直接显示 */}
  <Button size="small" type="link">查看</Button>
  <Button size="small" type="link">编辑</Button>

  {/* 扩展操作：根据卡片所属列动态显示 */}
  {columnActions.map(action => (
    <Button
      size="small"
      type="link"
      danger={action.danger}
      onClick={() => handleCardAction(action.key)}
    >
      {action.label}
    </Button>
  ))}

  <Button size="small" type="link" danger>删除</Button>
</div>
```

**处理逻辑**：
- 卡片组件接收 `columnActions` prop
- 父组件根据卡片所属列传入对应操作配置
- 使用 `type="link"` 减少视觉重量
- 操作过多时（>4个）自动折叠为「···」下拉

---

### 3. 顶部批量操作工具栏

批量操作集成到页面顶部工具栏，有选中时 **替换** 布局控制按钮：

```tsx
function KanbanToolbar({
  selectedCount,
  hasSelection,
  onBatchAction,
  onClearSelection,
  ...otherProps
}: KanbanToolbarProps) {
  return (
    <div className="pool-kanban-view__toolbar">
      <div className="pool-kanban-view__title-section">
        <h2 className="pool-kanban-view__title">电子凭证池</h3>
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
              <Button size="small" onClick={() => onBatchAction('edit')}>
                批量编辑
              </Button>
              <Button size="small" onClick={() => onBatchAction('recheck')}>
                批量检测
              </Button>
              <Button size="small" danger onClick={() => onBatchAction('delete')}>
                批量删除
              </Button>
              <Button size="small" onClick={onClearSelection}>
                取消选择
              </Button>
            </>
          ) : (
            /* 默认布局控制模式 */
            <>
              {collapsedColumns > 0 && (
                <Button size="small" icon={<Expand />}>展开全部</Button>
              )}
              <Button size="small" icon={<Columns3 />}>折叠空列</Button>
            </>
          )}
        </Space>
      </div>
    </div>
  );
}
```

**交互逻辑**：
- 有选中项时，右侧按钮组切换为批量操作
- 取消选择后恢复布局控制按钮

---

## 样式规范

### 颜色语义区分

| 元素 | 原色 | 优化后 | 说明 |
|------|------|--------|------|
| 总计徽章 | 🔴 红色 | 🔵 蓝色 `#1890ff` | 避免与危险操作混淆 |
| 子状态计数 | 🔴 红色 | ⚫ 灰色 | 内嵌徽章，降低视觉重量 |
| 删除按钮 | 🔴 红色 | 🔴 红色（保持） | 危险操作语义明确 |

### CSS 关键变更

```css
/* 列标题区：横向布局 */
.kanban-column__title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 40px;
}

/* 内嵌子状态选择器 */
.kanban-column__sub-states {
  flex: 1;
  display: flex;
  justify-content: center;
}

.kanban-column__sub-states .ant-segmented {
  background: #f5f5f5;
  padding: 2px;
  font-size: 12px;
}

/* 总计徽章 - 使用蓝色 */
.total-badge .ant-badge-count {
  background: #1890ff;
}

/* 卡片操作区 - link 样式 */
.kanban-card__actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.kanban-card__actions .ant-btn-link {
  padding: 0 4px;
  height: auto;
  font-size: 12px;
}
```

---

## 实施清单

### Phase 1: 样式与布局调整
- [ ] 更新 `KanbanColumn.css` — 移除 `__actions` 样式，添加 `__sub-states` 样式
- [ ] 更新 `PoolKanbanView.css` — 添加顶部工具栏按钮切换样式
- [ ] 更新 `KanbanCard.css` — 优化卡片操作区样式

### Phase 2: 组件改造
- [ ] 改造 `KanbanColumn.tsx` — 使用 `Segmented` 替代 `Tabs`，移除操作按钮区
- [ ] 扩展 `KanbanCard.tsx` — 接收 `columnActions` prop，渲染卡片级操作
- [ ] 改造 `PoolKanbanView.tsx` / `KanbanToolbar` — 集成批量操作按钮切换逻辑

### Phase 3: 数据流调整
- [ ] 更新 `usePoolBatchAction` hook — 适配顶部工具栏的批量操作触发
- [ ] 确保单卡片操作正确执行（无需勾选即可触发）

### Phase 4: 测试与验证
- [ ] 更新现有测试用例
- [ ] 手动验证各列操作正确显示
- [ ] 验证批量操作流程完整

---

## 涉及文件

| 文件 | 变更类型 |
|------|----------|
| `src/components/pool-kanban/KanbanColumn.tsx` | 重构 |
| `src/components/pool-kanban/KanbanColumn.css` | 修改 |
| `src/components/pool-kanban/KanbanCard.tsx` | 扩展 |
| `src/components/pool-kanban/KanbanCard.css` | 修改 |
| `src/components/pool-kanban/PoolKanbanView.tsx` | 修改 |
| `src/components/pool-kanban/PoolKanbanView.css` | 修改 |
| `src/hooks/usePoolBatchAction.ts` | 可能调整 |
| `src/config/pool-columns.config.ts` | 可能调整 |

---

## 设计原型

### 优化前

```
┌─────────────────────┐
│ 待处理 ● 0          │  ← 列标题
├─────────────────────┤
│ 草稿 | 待检测       │  ← Tabs 子状态
├─────────────────────┤
│ [重新检测] [删除]   │  ← 列操作按钮
├─────────────────────┤
│                     │
│   暂无文件          │
│                     │
└─────────────────────┘
```

### 优化后

```
┌─────────────────────────────┐
│ 待处理(3)  草稿 | 待检测  0  │  ← 标题+子状态内嵌
├─────────────────────────────┤
│                             │
│   暂无文件                  │
│                             │
└─────────────────────────────┘
```
