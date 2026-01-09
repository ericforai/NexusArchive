# 电子凭证池看板面板实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 `/system/pre-archive/pool` 页面实现看板面板，可视化展示档案预处理流程，降低会计人员上手难度

**Architecture:**
- 前端：React 19 + TypeScript + Vite，使用 Ant Design 组件库
- 状态管理：Zustand + React Query (@tanstack/react-query) 用于服务端状态
- 布局：Flexbox 实现响应式四列看板，每列内部使用二级标签页切换子状态
- 批量操作：选中状态管理 + 底部浮动操作栏

**Tech Stack:**
- React 19.2, TypeScript 5.8, Vite 6
- Ant Design 6 (Card, Tabs, Badge, Button, Modal)
- React Query (@tanstack/react-query)
- Zustand (状态管理)
- Vitest (单元测试)

---

## Phase 1: 基础看板结构

### Task 1.1: 创建看板配置文件

**Files:**
- Create: `src/config/pool-columns.config.ts`
- Create: `src/config/pool-columns.config.test.ts`

**Step 1: 看板配置文件测试**

```typescript
// src/config/pool-columns.config.test.ts
import { describe, it, expect } from 'vitest';
import { POOL_COLUMN_GROUPS, getColumnByState, getSubStateLabel } from './pool-columns.config';

describe('PoolColumnsConfig', () => {
  describe('POOL_COLUMN_GROUPS', () => {
    it('should have 4 column groups', () => {
      expect(POOL_COLUMN_GROUPS).toHaveLength(4);
    });

    it('should have correct group structure', () => {
      const firstGroup = POOL_COLUMN_GROUPS[0];
      expect(firstGroup).toHaveProperty('id');
      expect(firstGroup).toHaveProperty('title');
      expect(firstGroup).toHaveProperty('subStates');
      expect(firstGroup).toHaveProperty('actions');
    });

    it('should have correct subStates for pending group', () => {
      const pendingGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'pending');
      expect(pendingGroup?.subStates).toEqual([
        { value: 'DRAFT', label: '草稿' },
        { value: 'PENDING_CHECK', label: '待检测' },
      ]);
    });
  });

  describe('getColumnByState', () => {
    it('should return correct column for DRAFT state', () => {
      const result = getColumnByState('DRAFT');
      expect(result?.id).toBe('pending');
    });

    it('should return correct column for CHECK_FAILED state', () => {
      const result = getColumnByState('CHECK_FAILED');
      expect(result?.id).toBe('needs-attention');
    });

    it('should return null for unknown state', () => {
      const result = getColumnByState('UNKNOWN' as any);
      expect(result).toBeNull();
    });
  });

  describe('getSubStateLabel', () => {
    it('should return correct label for DRAFT', () => {
      expect(getSubStateLabel('DRAFT')).toBe('草稿');
    });

    it('should return correct label for CHECK_FAILED', () => {
      expect(getSubStateLabel('CHECK_FAILED')).toBe('检测失败');
    });

    it('should return state name for unknown state', () => {
      expect(getSubStateLabel('UNKNOWN' as any)).toBe('UNKNOWN');
    });
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- pool-columns.config.test.ts
```

Expected: FAIL with "Cannot find module './pool-columns.config'"

**Step 3: 实现看板配置**

```typescript
// src/config/pool-columns.config.ts
import type { PreArchiveStatus } from '@/types/models';

export interface SubStateConfig {
  value: PreArchiveStatus;
  label: string;
}

export interface ColumnAction {
  key: string;
  label: string;
  icon?: string;
  danger?: boolean;
}

export interface ColumnGroupConfig {
  id: string;
  title: string;
  subStates: SubStateConfig[];
  actions: ColumnAction[];
}

/**
 * 电子凭证池看板列分组配置
 *
 * 将10种预处理状态分组为4个主列，便于用户理解流程
 */
export const POOL_COLUMN_GROUPS: ColumnGroupConfig[] = [
  {
    id: 'pending',
    title: '待处理',
    subStates: [
      { value: 'DRAFT', label: '草稿' },
      { value: 'PENDING_CHECK', label: '待检测' },
    ],
    actions: [
      { key: 'recheck', label: '重新检测' },
      { key: 'delete', label: '删除', danger: true },
    ],
  },
  {
    id: 'needs-attention',
    title: '需要处理',
    subStates: [
      { value: 'CHECK_FAILED', label: '检测失败' },
      { value: 'PENDING_METADATA', label: '待补录' },
    ],
    actions: [
      { key: 'edit-metadata', label: '编辑元数据' },
      { key: 'retry-check', label: '重试检测' },
      { key: 'delete', label: '删除', danger: true },
    ],
  },
  {
    id: 'ready',
    title: '准备就绪',
    subStates: [
      { value: 'MATCH_PENDING', label: '待匹配' },
      { value: 'MATCHED', label: '已匹配' },
    ],
    actions: [
      { key: 'smart-match', label: '智能匹配' },
      { key: 'manual-link', label: '手动关联' },
      { key: 'move-to-archive', label: '移入待归档' },
    ],
  },
  {
    id: 'processing',
    title: '处理中',
    subStates: [
      { value: 'PENDING_ARCHIVE', label: '待归档' },
      { value: 'PENDING_APPROVAL', label: '审批中' },
      { value: 'ARCHIVING', label: '归档中' },
    ],
    actions: [
      { key: 'view-detail', label: '查看详情' },
      { key: 'cancel-archive', label: '取消归档', danger: true },
      { key: 'batch-approve', label: '批量审批' },
    ],
  },
];

/**
 * 状态到列的映射表
 */
const STATE_TO_COLUMN_MAP = new Map<PreArchiveStatus, string>(
  POOL_COLUMN_GROUPS.flatMap(group =>
    group.subStates.map(sub => [sub.value, group.id])
  )
);

/**
 * 状态到标签的映射表
 */
const STATE_TO_LABEL_MAP = new Map<PreArchiveStatus, string>(
  POOL_COLUMN_GROUPS.flatMap(group =>
    group.subStates.map(sub => [sub.value, sub.label])
  )
);

/**
 * 根据状态获取所属列配置
 */
export function getColumnByState(state: PreArchiveStatus): ColumnGroupConfig | null {
  const columnId = STATE_TO_COLUMN_MAP.get(state);
  return POOL_COLUMN_GROUPS.find(g => g.id === columnId) || null;
}

/**
 * 获取状态的显示标签
 */
export function getSubStateLabel(state: PreArchiveStatus): string {
  return STATE_TO_LABEL_MAP.get(state) || state;
}

/**
 * 获取指定列的所有状态值
 */
export function getColumnStates(columnId: string): PreArchiveStatus[] {
  const column = POOL_COLUMN_GROUPS.find(g => g.id === columnId);
  return column?.subStates.map(s => s.value) || [];
}
```

**Step 4: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- pool-columns.config.test.ts
```

Expected: PASS

**Step 5: 提交**

```bash
git add src/config/pool-columns.config.ts src/config/pool-columns.config.test.ts
git commit -m "feat(pool): add kanban column groups configuration"
```

---

### Task 1.2: 创建看板数据 Hook

**Files:**
- Create: `src/hooks/usePoolKanban.ts`
- Create: `src/hooks/usePoolKanban.test.ts`

**Step 1: 编写测试**

```typescript
// src/hooks/usePoolKanban.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { usePoolKanban } from './usePoolKanban';
import { poolApi } from '@/api/pool';
import { POOL_COLUMN_GROUPS } from '@/config/pool-columns.config';

// Mock API
vi.mock('@/api/pool', () => ({
  poolApi: {
    list: vi.fn(),
  },
}));

describe('usePoolKanban', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should initialize with empty state', () => {
    vi.mocked(poolApi.list).mockResolvedValue({ data: [], total: 0 });

    const { result } = renderHook(() => usePoolKanban());

    expect(result.current.loading).toBe(true);
    expect(result.current.columns).toEqual(POOL_COLUMN_GROUPS);
  });

  it('should group cards by column and sub-state', async () => {
    const mockCards = [
      { id: '1', status: 'DRAFT', title: 'Card 1' },
      { id: '2', status: 'PENDING_CHECK', title: 'Card 2' },
      { id: '3', status: 'CHECK_FAILED', title: 'Card 3' },
    ] as any;

    vi.mocked(poolApi.list).mockResolvedValue({ data: mockCards, total: 3 });

    const { result } = renderHook(() => usePoolKanban());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Verify pending column has DRAFT cards
    const pendingCards = result.current.getCardsForColumn('pending', 'DRAFT');
    expect(pendingCards).toHaveLength(1);
    expect(pendingCards[0].id).toBe('1');

    // Verify needs-attention column has CHECK_FAILED cards
    const needsCards = result.current.getCardsForColumn('needs-attention', 'CHECK_FAILED');
    expect(needsCards).toHaveLength(1);
    expect(needsCards[0].id).toBe('3');
  });

  it('should return count for each sub-state', async () => {
    const mockCards = [
      { id: '1', status: 'DRAFT' },
      { id: '2', status: 'DRAFT' },
      { id: '3', status: 'PENDING_CHECK' },
    ] as any;

    vi.mocked(poolApi.list).mockResolvedValue({ data: mockCards, total: 3 });

    const { result } = renderHook(() => usePoolKanban());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const draftCount = result.current.getSubStateCount('pending', 'DRAFT');
    expect(draftCount).toBe(2);

    const pendingCheckCount = result.current.getSubStateCount('pending', 'PENDING_CHECK');
    expect(pendingCheckCount).toBe(1);
  });

  it('should refresh data when refetch is called', async () => {
    const mockCards = [{ id: '1', status: 'DRAFT' }] as any;
    vi.mocked(poolApi.list).mockResolvedValue({ data: mockCards, total: 1 });

    const { result } = renderHook(() => usePoolKanban());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(poolApi.list).toHaveBeenCalledTimes(1);

    await result.current.refetch();

    expect(poolApi.list).toHaveBeenCalledTimes(2);
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- usePoolKanban.test.ts
```

Expected: FAIL with "Cannot find module './usePoolKanban'"

**Step 3: 实现 Hook**

```typescript
// src/hooks/usePoolKanban.ts
import { useQuery } from '@tanstack/react-query';
import { poolApi } from '@/api/pool';
import { POOL_COLUMN_GROUPS, type ColumnGroupConfig } from '@/config/pool-columns.config';
import type { OriginalVoucher } from '@/types/models';

interface KanbanCard extends OriginalVoucher {
  _columnId?: string;
}

interface UsePoolKanbanResult {
  columns: ColumnGroupConfig[];
  cards: KanbanCard[];
  loading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
  getCardsForColumn: (columnId: string, subState: string) => KanbanCard[];
  getSubStateCount: (columnId: string, subState: string) => number;
  getTotalCount: (columnId: string) => number;
}

/**
 * 看板数据管理 Hook
 *
 * 获取所有预处理凭证，按列和子状态分组
 */
export function usePoolKanban(): UsePoolKanbanResult {
  // 获取所有预处理凭证
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['pool', 'kanban'],
    queryFn: async () => {
      const response = await poolApi.list({ page: 1, limit: 10000 });
      return response.data;
    },
  });

  const cards: KanbanCard[] = (data || []).map(card => ({
    ...card,
    _columnId: getColumnIdForStatus(card.status),
  }));

  /**
   * 根据状态获取列ID
   */
  function getColumnIdForStatus(status: string): string {
    for (const column of POOL_COLUMN_GROUPS) {
      if (column.subStates.some(sub => sub.value === status)) {
        return column.id;
      }
    }
    return '';
  }

  /**
   * 获取指定列和子状态的卡片
   */
  function getCardsForColumn(columnId: string, subState: string): KanbanCard[] {
    return cards.filter(card => card.status === subState);
  }

  /**
   * 获取指定列和子状态的卡片数量
   */
  function getSubStateCount(columnId: string, subState: string): number {
    return cards.filter(card => card.status === subState).length;
  }

  /**
   * 获取指定列的总卡片数量
   */
  function getTotalCount(columnId: string): number {
    const column = POOL_COLUMN_GROUPS.find(c => c.id === columnId);
    if (!column) return 0;

    return column.subStates.reduce(
      (sum, sub) => sum + getSubStateCount(columnId, sub.value),
      0
    );
  }

  return {
    columns: POOL_COLUMN_GROUPS,
    cards,
    loading: isLoading,
    error: error as Error | null,
    refetch: async () => {
      await refetch();
    },
    getCardsForColumn,
    getSubStateCount,
    getTotalCount,
  };
}
```

**Step 4: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- usePoolKanban.test.ts
```

Expected: PASS

**Step 5: 提交**

```bash
git add src/hooks/usePoolKanban.ts src/hooks/usePoolKanban.test.ts
git commit -m "feat(pool): add usePoolKanban hook for data management"
```

---

### Task 1.3: 创建看板卡片组件

**Files:**
- Create: `src/components/pool-kanban/KanbanCard.tsx`
- Create: `src/components/pool-kanban/KanbanCard.test.tsx`

**Step 1: 编写测试**

```typescript
// src/components/pool-kanban/KanbanCard.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { KanbanCard } from './KanbanCard';

describe('KanbanCard', () => {
  const mockCard = {
    id: 'test-id',
    title: '测试凭证',
    fileName: 'test.pdf',
    amount: 12345.67,
    bizDate: '2025-01-09',
    orgName: '测试公司',
    status: 'DRAFT',
  };

  it('should render card information', () => {
    render(<KanbanCard card={mockCard as any} selected={false} />);

    expect(screen.getByText('测试凭证')).toBeInTheDocument();
    expect(screen.getByText('test.pdf')).toBeInTheDocument();
    expect(screen.getByText('¥12,345.67')).toBeInTheDocument();
    expect(screen.getByText('2025-01-09')).toBeInTheDocument();
    expect(screen.getByText('测试公司')).toBeInTheDocument();
  });

  it('should show selected state', () => {
    const { container } = render(
      <KanbanCard card={mockCard as any} selected={true} />
    );

    const card = container.querySelector('.kanban-card');
    expect(card).toHaveClass('kanban-card--selected');
  });

  it('should call onSelect when selection area is clicked', () => {
    const onSelect = vi.fn();
    const { container } = render(
      <KanbanCard card={mockCard as any} selected={false} onSelect={onSelect} />
    );

    const selectArea = container.querySelector('.kanban-card__select-area');
    fireEvent.click(selectArea!);

    expect(onSelect).toHaveBeenCalledWith('test-id');
  });

  it('should call onAction when action button is clicked', () => {
    const onAction = vi.fn();
    render(
      <KanbanCard card={mockCard as any} selected={false} onAction={onAction} />
    );

    const viewButton = screen.getByText('查看');
    fireEvent.click(viewButton);

    expect(onAction).toHaveBeenCalledWith('test-id', 'view');
  });

  it('should hide amount when not available', () => {
    const cardWithoutAmount = { ...mockCard, amount: null };
    render(<KanbanCard card={cardWithoutAmount as any} selected={false} />);

    expect(screen.queryByText(/¥/)).not.toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- KanbanCard.test.tsx
```

Expected: FAIL with "Cannot find module './KanbanCard'"

**Step 3: 实现卡片组件**

```typescript
// src/components/pool-kanban/KanbanCard.tsx
import { memo } from 'react';
import { FileText, Calendar, Money, Building } from 'lucide-react';
import { Button, Badge } from 'antd';
import type { OriginalVoucher } from '@/types/models';
import { getSubStateLabel } from '@/config/pool-columns.config';
import './KanbanCard.css';

export interface KanbanCardProps {
  card: OriginalVoucher;
  selected: boolean;
  onSelect?: (cardId: string) => void;
  onAction?: (cardId: string, action: 'view' | 'edit' | 'delete') => void;
}

export const KanbanCard = memo<KanbanCardProps>(({
  card,
  selected,
  onSelect,
  onAction,
}) => {
  const statusLabel = getSubStateLabel(card.status);

  const handleSelectAreaClick = () => {
    onSelect?.(card.id);
  };

  const handleAction = (action: 'view' | 'edit' | 'delete') => {
    onAction?.(card.id, action);
  };

  return (
    <div className={`kanban-card ${selected ? 'kanban-card--selected' : ''}`}>
      {/* 左侧选择区域 */}
      <div
        className="kanban-card__select-area"
        onClick={handleSelectAreaClick}
        aria-label="选择卡片"
      >
        <div className="kanban-card__select-indicator" />
      </div>

      {/* 右侧内容区域 */}
      <div className="kanban-card__content">
        {/* 标题和状态 */}
        <div className="kanban-card__header">
          <span className="kanban-card__title">{card.title || '未命名凭证'}</span>
          <Badge status="processing" text={statusLabel} />
        </div>

        {/* 文件信息 */}
        {card.fileName && (
          <div className="kanban-card__file">
            <FileText size={14} />
            <span className="kanban-card__file-name">{card.fileName}</span>
          </div>
        )}

        {/* 详情字段 */}
        <div className="kanban-card__details">
          {card.amount != null && (
            <div className="kanban-card__detail">
              <Money size={14} />
              <span>¥{card.amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}</span>
            </div>
          )}

          {card.bizDate && (
            <div className="kanban-card__detail">
              <Calendar size={14} />
              <span>{card.bizDate}</span>
            </div>
          )}

          {card.orgName && (
            <div className="kanban-card__detail kanban-card__detail--full">
              <Building size={14} />
              <span>{card.orgName}</span>
            </div>
          )}
        </div>

        {/* 操作按钮 */}
        <div className="kanban-card__actions">
          <Button size="small" onClick={() => handleAction('view')}>
            查看
          </Button>
          <Button size="small" onClick={() => handleAction('edit')}>
            编辑
          </Button>
          <Button size="small" danger onClick={() => handleAction('delete')}>
            删除
          </Button>
        </div>
      </div>

      {/* 取消选择按钮（选中时显示） */}
      {selected && onSelect && (
        <button
          className="kanban-card__cancel-select"
          onClick={() => onSelect(card.id)}
          aria-label="取消选择"
        >
          ✕
        </button>
      )}
    </div>
  );
});

KanbanCard.displayName = 'KanbanCard';
```

**Step 4: 创建样式文件**

```css
/* src/components/pool-kanban/KanbanCard.css */
.kanban-card {
  display: flex;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
  transition: all 0.2s ease;
  position: relative;
}

.kanban-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.kanban-card--selected {
  background: #f0f7ff;
  border-color: #1890ff;
}

.kanban-card__select-area {
  width: 6px;
  cursor: pointer;
  background: transparent;
  transition: background 0.2s;
  flex-shrink: 0;
}

.kanban-card--selected .kanban-card__select-area {
  background: #1890ff;
}

.kanban-card__select-indicator {
  width: 100%;
  height: 100%;
  opacity: 0;
  background: linear-gradient(90deg, #1890ff 0%, transparent 100%);
  transition: opacity 0.2s;
}

.kanban-card:hover .kanban-card__select-indicator {
  opacity: 0.3;
}

.kanban-card__content {
  flex: 1;
  padding: 12px;
}

.kanban-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kanban-card__title {
  font-weight: 500;
  font-size: 14px;
  color: #262626;
}

.kanban-card__file {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.kanban-card__file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kanban-card__details {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 12px;
}

.kanban-card__detail {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #595959;
}

.kanban-card__detail--full {
  width: 100%;
}

.kanban-card__actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.kanban-card__cancel-select {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border: none;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.kanban-card__cancel-select:hover {
  background: rgba(0, 0, 0, 0.2);
}
```

**Step 5: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- KanbanCard.test.tsx
```

Expected: PASS

**Step 6: 提交**

```bash
git add src/components/pool-kanban/KanbanCard.tsx src/components/pool-kanban/KanbanCard.css src/components/pool-kanban/KanbanCard.test.tsx
git commit -m "feat(pool): add KanbanCard component with selection state"
```

---

### Task 1.4: 创建看板列组件

**Files:**
- Create: `src/components/pool-kanban/KanbanColumn.tsx`
- Create: `src/components/pool-kanban/KanbanColumn.test.tsx`

**Step 1: 编写测试**

```typescript
// src/components/pool-kanban/KanbanColumn.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { KanbanColumn } from './KanbanColumn';
import { POOL_COLUMN_GROUPS } from '@/config/pool-columns.config';

describe('KanbanColumn', () => {
  const mockColumn = POOL_COLUMN_GROUPS[0];
  const mockCards = [
    { id: '1', status: 'DRAFT', title: 'Card 1' },
    { id: '2', status: 'DRAFT', title: 'Card 2' },
  ] as any[];

  it('should render column header with title', () => {
    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(screen.getByText('待处理')).toBeInTheDocument();
  });

  it('should render sub-state tabs', () => {
    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(screen.getByText('草稿')).toBeInTheDocument();
    expect(screen.getByText('待检测')).toBeInTheDocument();
  });

  it('should show card count in tabs', () => {
    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // DRAFT has 2 cards
    const draftTab = screen.getByText(/草稿/);
    expect(draftTab.textContent).toContain('2');

    // PENDING_CHECK has 0 cards
    const pendingTab = screen.getByText(/待检测/);
    expect(pendingTab.textContent).toContain('0');
  });

  it('should filter cards by selected sub-state', () => {
    const mixedCards = [
      ...mockCards, // DRAFT
      { id: '3', status: 'PENDING_CHECK', title: 'Card 3' },
    ] as any[];

    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mixedCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Should only show DRAFT cards by default
    const cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(2);
  });

  it('should switch sub-state when tab is clicked', () => {
    const cardsWithPending = [
      ...mockCards,
      { id: '3', status: 'PENDING_CHECK', title: 'Card 3' },
    ] as any[];

    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={cardsWithPending}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Initially showing DRAFT (2 cards)
    let cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(2);

    // Click on PENDING_CHECK tab
    const pendingTab = screen.getByText(/待检测/);
    fireEvent.click(pendingTab);

    // Now showing PENDING_CHECK (1 card)
    cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(1);
  });

  it('should render column action buttons', () => {
    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(screen.getByText('重新检测')).toBeInTheDocument();
    expect(screen.getByText('删除')).toBeInTheDocument();
  });

  it('should call onAction when column action is clicked', () => {
    const onAction = vi.fn();

    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={onAction}
      />
    );

    const recheckButton = screen.getByText('重新检测');
    fireEvent.click(recheckButton);

    expect(onAction).toHaveBeenCalledWith('recheck', mockCards);
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- KanbanColumn.test.tsx
```

Expected: FAIL with "Cannot find module './KanbanColumn'"

**Step 3: 实现列组件**

```typescript
// src/components/pool-kanban/KanbanColumn.tsx
import { useState, useCallback, useMemo } from 'react';
import { Tabs, Badge, Button, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { MoreHorizontal } from 'lucide-react';
import { KanbanCard } from './KanbanCard';
import type { ColumnGroupConfig } from '@/config/pool-columns.config';
import type { OriginalVoucher } from '@/types/models';
import './KanbanColumn.css';

const { TabPane } = Tabs;

export interface KanbanColumnProps {
  column: ColumnGroupConfig;
  cards: OriginalVoucher[];
  selectedIds: Set<string>;
  onSelectionChange: (cardId: string) => void;
  onAction: (action: string, cards: OriginalVoucher[]) => void;
}

export function KanbanColumn({
  column,
  cards,
  selectedIds,
  onSelectionChange,
  onAction,
}: KanbanColumnProps) {
  // 当前选中的子状态标签
  const [activeTab, setActiveTab] = useState(column.subStates[0].value);

  // 计算每个子状态的卡片数量
  const subStateCounts = useMemo(() => {
    const counts = new Map<string, number>();
    column.subStates.forEach(sub => {
      counts.set(
        sub.value,
        cards.filter(card => card.status === sub.value).length
      );
    });
    return counts;
  }, [cards, column.subStates]);

  // 获取当前标签的卡片
  const currentCards = useMemo(() => {
    return cards.filter(card => card.status === activeTab);
  }, [cards, activeTab]);

  // 处理卡片选择
  const handleCardSelect = useCallback((cardId: string) => {
    onSelectionChange(cardId);
  }, [onSelectionChange]);

  // 处理卡片操作
  const handleCardAction = useCallback((cardId: string, action: string) => {
    // 卡片操作暂不处理，由后续任务实现
    console.log('Card action:', action, 'for card:', cardId);
  }, []);

  // 处理列级操作点击
  const handleColumnAction = useCallback((actionKey: string) => {
    // 自动选中当前列的所有卡片
    currentCards.forEach(card => {
      onSelectionChange(card.id);
    });
    // 触发操作
    onAction(actionKey, currentCards);
  }, [currentCards, onSelectionChange, onAction]);

  // 更多操作菜单
  const moreActionsMenu: MenuProps['items'] = column.actions.slice(3).map(action => ({
    key: action.key,
    label: action.label,
    danger: action.danger,
    onClick: () => handleColumnAction(action.key),
  }));

  // 固定显示的操作按钮（最多3个）
  const visibleActions = column.actions.slice(0, 3);

  return (
    <div className="kanban-column">
      {/* 列头 */}
      <div className="kanban-column__header">
        <div className="kanban-column__title-row">
          <h3 className="kanban-column__title">{column.title}</h3>
          <Badge count={cards.length} showZero />
        </div>

        {/* 子状态标签页 */}
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          size="small"
          className="kanban-column__tabs"
        >
          {column.subStates.map(sub => (
            <TabPane
              key={sub.value}
              tab={
                <span className="kanban-column__tab">
                  {sub.label}
                  <Badge count={subStateCounts.get(sub.value) || 0} />
                </span>
              }
            />
          ))}
        </Tabs>
      </div>

      {/* 列操作按钮区 */}
      <div className="kanban-column__actions">
        {visibleActions.map(action => (
          <Button
            key={action.key}
            size="small"
            danger={action.danger}
            onClick={() => handleColumnAction(action.key)}
          >
            {action.label}
          </Button>
        ))}

        {column.actions.length > 3 && (
          <Dropdown menu={{ items: moreActionsMenu }} trigger={['click']}>
            <Button size="small" icon={<MoreHorizontal size={14} />}>
              更多
            </Button>
          </Dropdown>
        )}
      </div>

      {/* 卡片列表 */}
      <div className="kanban-column__cards">
        {currentCards.length === 0 ? (
          <div className="kanban-column__empty">
            <p>暂无文件</p>
          </div>
        ) : (
          currentCards.map(card => (
            <KanbanCard
              key={card.id}
              card={card}
              selected={selectedIds.has(card.id)}
              onSelect={handleCardSelect}
              onAction={handleCardAction}
            />
          ))
        )}
      </div>
    </div>
  );
}
```

**Step 4: 创建样式文件**

```css
/* src/components/pool-kanban/KanbanColumn.css */
.kanban-column {
  display: flex;
  flex-direction: column;
  min-width: 280px;
  max-width: 400px;
  flex: 1;
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
  height: 100%;
}

.kanban-column__header {
  margin-bottom: 12px;
}

.kanban-column__title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kanban-column__title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.kanban-column__tabs {
  margin-bottom: 0;
}

.kanban-column__tab {
  display: flex;
  align-items: center;
  gap: 6px;
}

.kanban-column__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.kanban-column__cards {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
}

.kanban-column__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: #8c8c8c;
  font-size: 14px;
}

/* 滚动条样式 */
.kanban-column__cards::-webkit-scrollbar {
  width: 6px;
}

.kanban-column__cards::-webkit-scrollbar-track {
  background: transparent;
}

.kanban-column__cards::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.kanban-column__cards::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
```

**Step 5: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- KanbanColumn.test.tsx
```

Expected: PASS

**Step 6: 提交**

```bash
git add src/components/pool-kanban/KanbanColumn.tsx src/components/pool-kanban/KanbanColumn.css src/components/pool-kanban/KanbanColumn.test.tsx
git commit -m "feat(pool): add KanbanColumn component with sub-state tabs"
```

---

### Task 1.5: 创建看板主视图组件

**Files:**
- Create: `src/components/pool-kanban/PoolKanbanView.tsx`
- Create: `src/components/pool-kanban/PoolKanbanView.test.tsx`

**Step 1: 编写测试**

```typescript
// src/components/pool-kanban/PoolKanbanView.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PoolKanbanView } from './PoolKanbanView';

// Mock hooks
vi.mock('@/hooks/usePoolKanban', () => ({
  usePoolKanban: () => ({
    columns: [
      {
        id: 'pending',
        title: '待处理',
        subStates: [{ value: 'DRAFT', label: '草稿' }],
        actions: [{ key: 'recheck', label: '重新检测' }],
      },
    ],
    cards: [
      { id: '1', status: 'DRAFT', title: 'Card 1' },
    ],
    loading: false,
    error: null,
    refetch: vi.fn(),
    getCardsForColumn: vi.fn(() => []),
    getSubStateCount: vi.fn(() => 0),
    getTotalCount: vi.fn(() => 1),
  }),
}));

// Mock API
vi.mock('@/api/pool', () => ({
  poolApi: {
    list: vi.fn(() => Promise.resolve({ data: [], total: 0 })),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}

describe('PoolKanbanView', () => {
  it('should render kanban board with columns', () => {
    const wrapper = createWrapper();
    render(<PoolKanbanView />, { wrapper });

    expect(screen.getByText('电子凭证池')).toBeInTheDocument();
    expect(screen.getByText('待处理')).toBeInTheDocument();
  });

  it('should show loading state', () => {
    vi.doMock('@/hooks/usePoolKanban', () => ({
      usePoolKanban: () => ({
        columns: [],
        cards: [],
        loading: true,
        error: null,
        refetch: vi.fn(),
        getCardsForColumn: vi.fn(() => []),
        getSubStateCount: vi.fn(() => 0),
        getTotalCount: vi.fn(() => 0),
      }),
    }));

    const wrapper = createWrapper();
    const { container } = render(<PoolKanbanView />, { wrapper });

    expect(container.querySelector('.loading-spinner')).toBeInTheDocument();
  });

  it('should update selection when card is clicked', () => {
    const wrapper = createWrapper();
    const { container } = render(<PoolKanbanView />, { wrapper });

    // Click on a card
    const card = container.querySelector('.kanban-card__select-area');
    fireEvent.click(card!);

    // Verify selection state is updated (via checking selected class)
    const selectedCard = container.querySelector('.kanban-card--selected');
    expect(selectedCard).toBeInTheDocument();
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- PoolKanbanView.test.tsx
```

Expected: FAIL with "Cannot find module './PoolKanbanView'"

**Step 3: 实现主视图组件**

```typescript
// src/components/pool-kanban/PoolKanbanView.tsx
import { useState, useCallback } from 'react';
import { Spin } from 'antd';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { KanbanColumn } from './KanbanColumn';
import type { OriginalVoucher } from '@/types/models';
import './PoolKanbanView.css';

export interface PoolKanbanViewProps {
  /** 额外的类名 */
  className?: string;
}

/**
 * 电子凭证池看板视图
 *
 * 展示档案预处理流程的四列看板
 */
export function PoolKanbanView({ className }: PoolKanbanViewProps) {
  const {
    columns,
    cards,
    loading,
    error,
    refetch,
    getCardsForColumn,
    getTotalCount,
  } = usePoolKanban();

  // 选中的卡片ID集合
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // 处理卡片选择切换
  const handleSelectionChange = useCallback((cardId: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(cardId)) {
        next.delete(cardId);
      } else {
        next.add(cardId);
      }
      return next;
    });
  }, []);

  // 处理列级操作
  const handleColumnAction = useCallback((action: string, columnCards: OriginalVoucher[]) => {
    // 操作逻辑由后续任务实现
    console.log('Column action:', action, 'with cards:', columnCards);
  }, []);

  if (loading) {
    return (
      <div className="pool-kanban-view pool-kanban-view--loading">
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="pool-kanban-view pool-kanban-view--error">
        <p>加载失败: {error.message}</p>
        <button onClick={() => refetch()}>重试</button>
      </div>
    );
  }

  return (
    <div className={`pool-kanban-view ${className || ''}`}>
      {/* 顶部工具栏 */}
      <div className="pool-kanban-view__toolbar">
        <h2 className="pool-kanban-view__title">电子凭证池</h2>
        <div className="pool-kanban-view__actions">
          {/* TODO: 添加新建上传、批量导入按钮 */}
        </div>
      </div>

      {/* 看板列 */}
      <div className="pool-kanban-view__board">
        {columns.map(column => {
          const columnCards = getCardsForColumn(column.id, column.subStates[0].value);

          return (
            <KanbanColumn
              key={column.id}
              column={column}
              cards={columnCards}
              selectedIds={selectedIds}
              onSelectionChange={handleSelectionChange}
              onAction={handleColumnAction}
            />
          );
        })}
      </div>

      {/* 批量操作栏 - 后续任务实现 */}
      {selectedIds.size > 0 && (
        <div className="pool-kanban-view__batch-bar">
          <span>已选 {selectedIds.size} 个文件</span>
          <div className="pool-kanban-view__batch-actions">
            <button onClick={() => setSelectedIds(new Set())}>取消选择</button>
            <button className="primary">执行操作</button>
          </div>
        </div>
      )}
    </div>
  );
}
```

**Step 4: 创建样式文件**

```css
/* src/components/pool-kanban/PoolKanbanView.css */
.pool-kanban-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.pool-kanban-view--loading,
.pool-kanban-view--error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.pool-kanban-view__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.pool-kanban-view__title {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #262626;
}

.pool-kanban-view__actions {
  display: flex;
  gap: 12px;
}

.pool-kanban-view__board {
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
  min-height: 0;
}

/* 滚动条样式 */
.pool-kanban-view__board::-webkit-scrollbar {
  height: 8px;
}

.pool-kanban-view__board::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 4px;
}

.pool-kanban-view__board::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 4px;
}

.pool-kanban-view__board::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

.pool-kanban-view__batch-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.pool-kanban-view__batch-actions {
  display: flex;
  gap: 8px;
}

.pool-kanban-view__batch-actions button.primary {
  background: #1890ff;
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.pool-kanban-view__batch-actions button.primary:hover {
  background: #40a9ff;
}
```

**Step 5: 更新类型定义**

确保 `OriginalVoucher` 类型包含所需字段：

```typescript
// src/types/models/pre-archive.ts
// 确保以下字段存在
export interface OriginalVoucher {
  id: string;
  title: string;
  fileName: string;
  amount: number | null;
  bizDate: string;
  orgName: string;
  status: PreArchiveStatus;
  // ... 其他字段
}
```

**Step 6: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- PoolKanbanView.test.tsx
```

Expected: PASS

**Step 7: 提交**

```bash
git add src/components/pool-kanban/PoolKanbanView.tsx src/components/pool-kanban/PoolKanbanView.css src/components/pool-kanban/PoolKanbanView.test.tsx
git commit -m "feat(pool): add PoolKanbanView main component"
```

---

### Task 1.6: 创建组件导出文件

**Files:**
- Create: `src/components/pool-kanban/index.ts`

**Step 1: 创建导出文件**

```typescript
// src/components/pool-kanban/index.ts
/**
 * 电子凭证池看板组件
 *
 * 提供四列看板视图，展示档案预处理流程
 */

export { PoolKanbanView } from './PoolKanbanView';
export type { PoolKanbanViewProps } from './PoolKanbanView';

export { KanbanColumn } from './KanbanColumn';
export type { KanbanColumnProps } from './KanbanColumn';

export { KanbanCard } from './KanbanCard';
export type { KanbanCardProps } from './KanbanCard';
```

**Step 2: 提交**

```bash
git add src/components/pool-kanban/index.ts
git commit -m "feat(pool): add component exports for kanban"
```

---

## Phase 2: 批量操作功能

### Task 2.1: 创建批量操作 Hook

**Files:**
- Create: `src/hooks/usePoolBatchAction.ts`
- Create: `src/hooks/usePoolBatchAction.test.ts`

**Step 1: 编写测试**

```typescript
// src/hooks/usePoolBatchAction.test.ts
import { describe, it, expect, vi } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { usePoolBatchAction } from './usePoolBatchAction';

// Mock API
vi.mock('@/api/pool', () => ({
  poolApi: {
    batchRecheck: vi.fn(() => Promise.resolve({ success: true })),
    batchDelete: vi.fn(() => Promise.resolve({ success: true })),
    moveToReady: vi.fn(() => Promise.resolve({ success: true })),
  },
}));

describe('usePoolBatchAction', () => {
  it('should initialize with empty selection', () => {
    const { result } = renderHook(() => usePoolBatchAction());

    expect(result.current.selectedIds).toEqual(new Set());
    expect(result.current.isProcessing).toBe(false);
  });

  it('should select all cards when selectAll is called', () => {
    const { result } = renderHook(() => usePoolBatchAction());
    const cardIds = ['1', '2', '3'];

    act(() => {
      result.current.selectAll(cardIds);
    });

    expect(result.current.selectedIds.size).toBe(3);
    expect(Array.from(result.current.selectedIds)).toEqual(cardIds);
  });

  it('should toggle card selection', () => {
    const { result } = renderHook(() => usePoolBatchAction());

    act(() => {
      result.current.toggleSelection('card-1');
    });

    expect(result.current.selectedIds.has('card-1')).toBe(true);

    act(() => {
      result.current.toggleSelection('card-1');
    });

    expect(result.current.selectedIds.has('card-1')).toBe(false);
  });

  it('should clear selection when clearSelection is called', () => {
    const { result } = renderHook(() => usePoolBatchAction());

    act(() => {
      result.current.selectAll(['1', '2', '3']);
    });

    expect(result.current.selectedIds.size).toBe(3);

    act(() => {
      result.current.clearSelection();
    });

    expect(result.current.selectedIds.size).toBe(0);
  });

  it('should execute action and clear selection on success', async () => {
    const { result } = renderHook(() => usePoolBatchAction());
    const { poolApi } = require('@/api/pool');

    act(() => {
      result.current.selectAll(['1', '2']);
    });

    await act(async () => {
      const response = await result.current.executeAction('recheck', Array.from(result.current.selectedIds));
      expect(response.success).toBe(true);
    });

    await waitFor(() => {
      expect(result.current.selectedIds.size).toBe(0);
    });

    expect(poolApi.batchRecheck).toHaveBeenCalledWith(['1', '2']);
  });

  it('should handle action errors', async () => {
    const { poolApi } = require('@/api/pool');
    poolApi.batchRecheck.mockRejectedValue(new Error('API Error'));

    const { result } = renderHook(() => usePoolBatchAction());

    act(() => {
      result.current.selectAll(['1', '2']);
    });

    await act(async () => {
      const response = await result.current.executeAction('recheck', ['1', '2']);
      expect(response.success).toBe(false);
      expect(response.error).toBeTruthy();
    });

    // Selection should be preserved on error
    expect(result.current.selectedIds.size).toBe(2);
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- usePoolBatchAction.test.ts
```

Expected: FAIL with "Cannot find module './usePoolBatchAction'"

**Step 3: 实现 Hook**

```typescript
// src/hooks/usePoolBatchAction.ts
import { useState, useCallback } from 'react';
import { message } from 'antd';
import { poolApi } from '@/api/pool';

export interface ActionResult {
  success: boolean;
  error?: Error;
  message?: string;
}

export interface UsePoolBatchActionResult {
  selectedIds: Set<string>;
  isProcessing: boolean;
  selectAll: (cardIds: string[]) => void;
  toggleSelection: (cardId: string) => void;
  clearSelection: () => void;
  executeAction: (action: string, cardIds: string[]) => Promise<ActionResult>;
  getSelectedCount: () => number;
}

/**
 * 批量操作 Hook
 *
 * 管理卡片选择状态和批量操作执行
 */
export function usePoolBatchAction(): UsePoolBatchActionResult {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [isProcessing, setIsProcessing] = useState(false);

  /**
   * 选中所有卡片
   */
  const selectAll = useCallback((cardIds: string[]) => {
    setSelectedIds(new Set(cardIds));
  }, []);

  /**
   * 切换单个卡片选择状态
   */
  const toggleSelection = useCallback((cardId: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(cardId)) {
        next.delete(cardId);
      } else {
        next.add(cardId);
      }
      return next;
    });
  }, []);

  /**
   * 清空选择
   */
  const clearSelection = useCallback(() => {
    setSelectedIds(new Set());
  }, []);

  /**
   * 获取选中数量
   */
  const getSelectedCount = useCallback(() => {
    return selectedIds.size;
  }, [selectedIds]);

  /**
   * 执行批量操作
   */
  const executeAction = useCallback(async (action: string, cardIds: string[]): Promise<ActionResult> => {
    if (cardIds.length === 0) {
      return { success: false, error: new Error('未选择任何文件') };
    }

    setIsProcessing(true);

    try {
      let result;

      switch (action) {
        case 'recheck':
        case 'retry-check':
          result = await poolApi.batchRecheck(cardIds);
          message.success(`已提交 ${cardIds.length} 个文件进行检测`);
          break;

        case 'delete':
          result = await poolApi.batchDelete(cardIds);
          message.success(`已删除 ${cardIds.length} 个文件`);
          break;

        case 'move-to-archive':
          result = await poolApi.moveToReady(cardIds);
          message.success(`已将 ${cardIds.length} 个文件移入待归档`);
          break;

        case 'batch-approve':
          result = await poolApi.batchApprove(cardIds);
          message.success(`已批准 ${cardIds.length} 个文件`);
          break;

        case 'smart-match':
          result = await poolApi.smartMatch(cardIds);
          message.success(`已对 ${cardIds.length} 个文件进行智能匹配`);
          break;

        case 'cancel-archive':
          result = await poolApi.cancelArchive(cardIds);
          message.success(`已取消 ${cardIds.length} 个文件的归档`);
          break;

        default:
          return { success: false, error: new Error(`未知操作: ${action}`) };
      }

      // 操作成功后清空选择
      setSelectedIds(new Set());

      return { success: true };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '操作失败';
      message.error(errorMessage);
      return { success: false, error: error as Error };
    } finally {
      setIsProcessing(false);
    }
  }, []);

  return {
    selectedIds,
    isProcessing,
    selectAll,
    toggleSelection,
    clearSelection,
    executeAction,
    getSelectedCount,
  };
}
```

**Step 4: 更新 poolApi 添加批量操作方法**

```typescript
// src/api/pool.ts
// 确保包含以下批量操作方法

export const poolApi = {
  // ... 现有方法

  /**
   * 批量重新检测
   */
  batchRecheck: async (ids: string[]) => {
    return client.post('/pool/batch/recheck', { ids });
  },

  /**
   * 批量删除
   */
  batchDelete: async (ids: string[]) => {
    return client.post('/pool/batch/delete', { ids });
  },

  /**
   * 移入待归档
   */
  moveToReady: async (ids: string[]) => {
    return client.post('/pool/move-to-ready', { ids });
  },

  /**
   * 批量审批
   */
  batchApprove: async (ids: string[]) => {
    return client.post('/pool/batch/approve', { ids });
  },

  /**
   * 智能匹配
   */
  smartMatch: async (ids: string[]) => {
    return client.post('/pool/smart-match', { ids });
  },

  /**
   * 取消归档
   */
  cancelArchive: async (ids: string[]) => {
    return client.post('/pool/cancel-archive', { ids });
  },
};
```

**Step 5: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- usePoolBatchAction.test.ts
```

Expected: PASS

**Step 6: 提交**

```bash
git add src/hooks/usePoolBatchAction.ts src/hooks/usePoolBatchAction.test.ts src/api/pool.ts
git commit -m "feat(pool): add usePoolBatchAction hook for batch operations"
```

---

### Task 2.2: 创建批量操作栏组件

**Files:**
- Create: `src/components/pool-kanban/BatchActionBar.tsx`
- Create: `src/components/pool-kanban/BatchActionBar.test.tsx`

**Step 1: 编写测试**

```typescript
// src/components/pool-kanban/BatchActionBar.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BatchActionBar } from './BatchActionBar';

describe('BatchActionBar', () => {
  it('should not render when no items selected', () => {
    const { container } = render(
      <BatchActionBar
        selectedCount={0}
        isProcessing={false}
        onCancel={vi.fn()}
        onExecute={vi.fn()}
        actionLabel="执行检测"
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('should render when items are selected', () => {
    render(
      <BatchActionBar
        selectedCount={5}
        isProcessing={false}
        onCancel={vi.fn()}
        onExecute={vi.fn()}
        actionLabel="执行检测"
      />
    );

    expect(screen.getByText('已选 5 个文件')).toBeInTheDocument();
    expect(screen.getByText('取消选择')).toBeInTheDocument();
    expect(screen.getByText('执行检测')).toBeInTheDocument();
  });

  it('should call onCancel when cancel button is clicked', () => {
    const onCancel = vi.fn();

    render(
      <BatchActionBar
        selectedCount={3}
        isProcessing={false}
        onCancel={onCancel}
        onExecute={vi.fn()}
        actionLabel="执行检测"
      />
    );

    const cancelButton = screen.getByText('取消选择');
    fireEvent.click(cancelButton);

    expect(onCancel).toHaveBeenCalled();
  });

  it('should call onExecute when execute button is clicked', () => {
    const onExecute = vi.fn();

    render(
      <BatchActionBar
        selectedCount={3}
        isProcessing={false}
        onCancel={vi.fn()}
        onExecute={onExecute}
        actionLabel="执行检测"
      />
    );

    const executeButton = screen.getByText('执行检测');
    fireEvent.click(executeButton);

    expect(onExecute).toHaveBeenCalled();
  });

  it('should disable buttons when processing', () => {
    render(
      <BatchActionBar
        selectedCount={3}
        isProcessing={true}
        onCancel={vi.fn()}
        onExecute={vi.fn()}
        actionLabel="执行检测"
      />
    );

    const executeButton = screen.getByText('执行检测');
    expect(executeButton).toBeDisabled();
  });

  it('should show processing state', () => {
    render(
      <BatchActionBar
        selectedCount={3}
        isProcessing={true}
        onCancel={vi.fn()}
        onExecute={vi.fn()}
        actionLabel="执行检测"
      />
    );

    expect(screen.getByText(/处理中/)).toBeInTheDocument();
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- BatchActionBar.test.tsx
```

Expected: FAIL with "Cannot find module './BatchActionBar'"

**Step 3: 实现组件**

```typescript
// src/components/pool-kanban/BatchActionBar.tsx
import { Button } from 'antd';
import { SpinningLoading } from '@/components/ui'; // 或使用 antd 的 Spin
import './BatchActionBar.css';

export interface BatchActionBarProps {
  /** 选中的文件数量 */
  selectedCount: number;
  /** 是否正在处理 */
  isProcessing: boolean;
  /** 取消选择回调 */
  onCancel: () => void;
  /** 执行操作回调 */
  onExecute: () => void;
  /** 操作按钮标签 */
  actionLabel: string;
  /** 操作是否危险 */
  danger?: boolean;
}

/**
 * 批量操作底部浮动栏
 *
 * 当有文件被选中时显示在底部，提供取消和执行按钮
 */
export function BatchActionBar({
  selectedCount,
  isProcessing,
  onCancel,
  onExecute,
  actionLabel,
  danger = false,
}: BatchActionBarProps) {
  if (selectedCount === 0) {
    return null;
  }

  return (
    <div className="batch-action-bar">
      <span className="batch-action-bar__count">
        {isProcessing ? '处理中...' : `已选 ${selectedCount} 个文件`}
      </span>

      <div className="batch-action-bar__actions">
        <Button
          onClick={onCancel}
          disabled={isProcessing}
        >
          取消选择
        </Button>

        <Button
          type="primary"
          danger={danger}
          onClick={onExecute}
          disabled={isProcessing}
          loading={isProcessing}
        >
          {actionLabel}
        </Button>
      </div>
    </div>
  );
}
```

**Step 4: 创建样式文件**

```css
/* src/components/pool-kanban/BatchActionBar.css */
.batch-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  z-index: 1000;
  animation: slideUp 0.2s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.batch-action-bar__count {
  font-size: 14px;
  color: #595959;
}

.batch-action-bar__actions {
  display: flex;
  gap: 12px;
}
```

**Step 5: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- BatchActionBar.test.tsx
```

Expected: PASS

**Step 6: 更新组件导出**

```typescript
// src/components/pool-kanban/index.ts
// 添加导出
export { BatchActionBar } from './BatchActionBar';
export type { BatchActionBarProps } from './BatchActionBar';
```

**Step 7: 提交**

```bash
git add src/components/pool-kanban/BatchActionBar.tsx src/components/pool-kanban/BatchActionBar.css src/components/pool-kanban/BatchActionBar.test.tsx src/components/pool-kanban/index.ts
git commit -m "feat(pool): add BatchActionBar component"
```

---

### Task 2.3: 集成批量操作到看板视图

**Files:**
- Modify: `src/components/pool-kanban/PoolKanbanView.tsx`

**Step 1: 更新看板视图使用批量操作 Hook**

```typescript
// src/components/pool-kanban/PoolKanbanView.tsx
import { useState, useCallback, useMemo } from 'react';
import { Spin, Modal } from 'antd';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { usePoolBatchAction } from '@/hooks/usePoolBatchAction';
import { KanbanColumn } from './KanbanColumn';
import { BatchActionBar } from './BatchActionBar';
import type { OriginalVoucher } from '@/types/models';
import './PoolKanbanView.css';

export interface PoolKanbanViewProps {
  /** 额外的类名 */
  className?: string;
}

/**
 * 电子凭证池看板视图
 *
 * 展示档案预处理流程的四列看板
 */
export function PoolKanbanView({ className }: PoolKanbanViewProps) {
  const {
    columns,
    cards,
    loading,
    error,
    refetch,
    getCardsForColumn,
    getTotalCount,
  } = usePoolKanban();

  const {
    selectedIds,
    isProcessing,
    selectAll,
    toggleSelection,
    clearSelection,
    executeAction,
  } = usePoolBatchAction();

  // 当前正在执行的操作信息
  const [pendingAction, setPendingAction] = useState<{
    key: string;
    label: string;
    cardIds: string[];
    danger?: boolean;
  } | null>(null);

  // 处理卡片选择切换
  const handleSelectionChange = useCallback((cardId: string) => {
    toggleSelection(cardId);
  }, [toggleSelection]);

  // 处理列级操作点击
  const handleColumnAction = useCallback((
    actionKey: string,
    columnCards: OriginalVoucher[]
  ) => {
    const column = columns.find(col =>
      col.actions.some(a => a.key === actionKey)
    );

    if (!column) return;

    const action = column.actions.find(a => a.key === actionKey);
    if (!action) return;

    // 自动选中该列当前子状态的所有卡片
    const cardIds = columnCards.map(c => c.id);
    selectAll(cardIds);

    // 设置待执行的操作
    setPendingAction({
      key: actionKey,
      label: action.label,
      cardIds,
      danger: action.danger,
    });
  }, [columns, selectAll]);

  // 确认并执行操作
  const handleConfirmAction = useCallback(async () => {
    if (!pendingAction) return;

    const result = await executeAction(pendingAction.key, pendingAction.cardIds);

    if (result.success) {
      setPendingAction(null);
      // 刷新数据
      await refetch();
    }
  }, [pendingAction, executeAction, refetch]);

  // 取消操作
  const handleCancelAction = useCallback(() => {
    setPendingAction(null);
    clearSelection();
  }, [clearSelection]);

  // 获取所有列的卡片（按当前选中的子状态）
  const cardsByColumn = useMemo(() => {
    const map = new Map<string, OriginalVoucher[]>();
    columns.forEach(column => {
      const firstSubState = column.subStates[0].value;
      map.set(column.id, getCardsForColumn(column.id, firstSubState));
    });
    return map;
  }, [columns, getCardsForColumn]);

  if (loading) {
    return (
      <div className="pool-kanban-view pool-kanban-view--loading">
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="pool-kanban-view pool-kanban-view--error">
        <p>加载失败: {error.message}</p>
        <button onClick={() => refetch()}>重试</button>
      </div>
    );
  }

  return (
    <div className={`pool-kanban-view ${className || ''}`}>
      {/* 顶部工具栏 */}
      <div className="pool-kanban-view__toolbar">
        <h2 className="pool-kanban-view__title">电子凭证池</h2>
        <div className="pool-kanban-view__actions">
          {/* TODO: 添加新建上传、批量导入按钮 */}
        </div>
      </div>

      {/* 看板列 */}
      <div className="pool-kanban-view__board">
        {columns.map(column => {
          const columnCards = cardsByColumn.get(column.id) || [];

          return (
            <KanbanColumn
              key={column.id}
              column={column}
              cards={columnCards}
              selectedIds={selectedIds}
              onSelectionChange={handleSelectionChange}
              onAction={(action, cards) => handleColumnAction(action, cards)}
            />
          );
        })}
      </div>

      {/* 批量操作栏 */}
      {selectedIds.size > 0 && !pendingAction && (
        <BatchActionBar
          selectedCount={selectedIds.size}
          isProcessing={isProcessing}
          onCancel={clearSelection}
          onExecute={() => {
            // 显示操作确认对话框
            const action = columns
              .flatMap(col => col.actions)
              .find(a => a.key === 'recheck'); // 默认操作

            if (action) {
              setPendingAction({
                key: action.key,
                label: action.label,
                cardIds: Array.from(selectedIds),
                danger: action.danger,
              });
            }
          }}
          actionLabel="执行操作"
        />
      )}

      {/* 操作确认对话框 */}
      <Modal
        title="确认批量操作"
        open={!!pendingAction}
        onOk={handleConfirmAction}
        onCancel={handleCancelAction}
        confirmLoading={isProcessing}
        okButtonProps={{ danger: pendingAction?.danger }}
        okText="确认执行"
        cancelText="取消"
      >
        {pendingAction && (
          <p>
            确定要对 <strong>{pendingAction.cardIds.length}</strong> 个文件执行
            "<strong>{pendingAction.label}</strong>" 操作吗？
          </p>
        )}
      </Modal>
    </div>
  );
}
```

**Step 2: 提交**

```bash
git add src/components/pool-kanban/PoolKanbanView.tsx
git commit -m "feat(pool): integrate batch operations into kanban view"
```

---

## Phase 3: 响应式优化

### Task 3.1: 实现动态列宽和空列折叠

**Files:**
- Create: `src/hooks/useKanbanLayout.ts`
- Create: `src/hooks/useKanbanLayout.test.ts`

**Step 1: 编写测试**

```typescript
// src/hooks/useKanbanLayout.test.ts
import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useKanbanLayout } from './useKanbanLayout';

describe('useKanbanLayout', () => {
  it('should calculate visibility based on card counts', () => {
    const { result } = renderHook(() =>
      useKanbanLayout({
        'pending': 5,
        'needs-attention': 0,
        'ready': 3,
        'processing': 0,
      })
    );

    expect(result.current.visibleColumns).toEqual(['pending', 'ready']);
    expect(result.current.collapsedColumns).toEqual(['needs-attention', 'processing']);
  });

  it('should show all columns when autoCollapse is false', () => {
    const { result } = renderHook(() =>
      useKanbanLayout(
        {
          'pending': 5,
          'needs-attention': 0,
          'ready': 3,
          'processing': 0,
        },
        { autoCollapse: false }
      )
    );

    expect(result.current.visibleColumns).toHaveLength(4);
    expect(result.current.collapsedColumns).toHaveLength(0);
  });

  it('should toggle column visibility', () => {
    const { result } = renderHook(() =>
      useKanbanLayout({
        'pending': 5,
        'needs-attention': 0,
        'ready': 3,
        'processing': 0,
      })
    );

    expect(result.current.visibleColumns).not.toContain('needs-attention');

    // Toggle needs-attention column
    result.current.toggleColumn('needs-attention');

    expect(result.current.visibleColumns).toContain('needs-attention');
  });

  it('should calculate column width based on visible count', () => {
    const { result } = renderHook(() =>
      useKanbanLayout({
        'pending': 5,
        'needs-attention': 3,
        'ready': 0,
        'processing': 2,
      })
    );

    // 3 visible columns
    expect(result.current.getColumnWidth()).toBeCloseTo(33.33, 1);
  });
});
```

**Step 2: 运行测试验证失败**

```bash
cd /Users/user/nexusarchive && npm run test -- useKanbanLayout.test.ts
```

Expected: FAIL with "Cannot find module './useKanbanLayout'"

**Step 3: 实现 Hook**

```typescript
// src/hooks/useKanbanLayout.ts
import { useState, useCallback, useMemo } from 'react';

export interface KanbanLayoutOptions {
  /** 是否自动折叠空列 */
  autoCollapse?: boolean;
  /** 容器宽度 */
  containerWidth?: number;
  /** 最小列宽 */
  minColumnWidth?: number;
  /** 最大列宽 */
  maxColumnWidth?: number;
}

export interface UseKanbanLayoutResult {
  /** 可见的列ID */
  visibleColumns: string[];
  /** 折叠的列ID */
  collapsedColumns: string[];
  /** 切换列的可见性 */
  toggleColumn: (columnId: string) => void;
  /** 展开列 */
  expandColumn: (columnId: string) => void;
  /** 折叠列 */
  collapseColumn: (columnId: string) => void;
  /** 获取列宽百分比 */
  getColumnWidth: () => number;
  /** 获取列样式对象 */
  getColumnStyle: (columnId: string) => React.CSSProperties;
}

const ALL_COLUMNS = ['pending', 'needs-attention', 'ready', 'processing'];

/**
 * 看板布局管理 Hook
 *
 * 处理列的可见性、折叠状态和宽度计算
 */
export function useKanbanLayout(
  columnCounts: Record<string, number>,
  options: KanbanLayoutOptions = {}
): UseKanbanLayoutResult {
  const {
    autoCollapse = true,
    containerWidth = 1200,
    minColumnWidth = 280,
    maxColumnWidth = 400,
  } = options;

  // 手动隐藏的列
  const [hiddenColumns, setHiddenColumns] = useState<Set<string>>(new Set());

  // 计算折叠的列（无数据且启用自动折叠）
  const collapsedColumns = useMemo(() => {
    if (!autoCollapse) return [];

    return ALL_COLUMNS.filter(
      colId => columnCounts[colId] === 0 && !hiddenColumns.has(colId)
    );
  }, [columnCounts, autoCollapse, hiddenColumns]);

  // 计算可见的列
  const visibleColumns = useMemo(() => {
    return ALL_COLUMNS.filter(
      colId => !collapsedColumns.includes(colId) && !hiddenColumns.has(colId)
    );
  }, [collapsedColumns, hiddenColumns]);

  /**
   * 切换列的可见性
   */
  const toggleColumn = useCallback((columnId: string) => {
    setHiddenColumns(prev => {
      const next = new Set(prev);
      if (next.has(columnId)) {
        next.delete(columnId);
      } else {
        next.add(columnId);
      }
      return next;
    });
  }, []);

  /**
   * 展开列
   */
  const expandColumn = useCallback((columnId: string) => {
    setHiddenColumns(prev => {
      const next = new Set(prev);
      next.delete(columnId);
      return next;
    });
  }, []);

  /**
   * 折叠列
   */
  const collapseColumn = useCallback((columnId: string) => {
    setHiddenColumns(prev => new Set(prev).add(columnId));
  }, []);

  /**
   * 获取列宽百分比
   */
  const getColumnWidth = useCallback(() => {
    if (visibleColumns.length === 0) return 100;

    // 基于可见列数计算均分宽度
    const baseWidth = 100 / visibleColumns.length;

    // 检查是否受最小/最大宽度限制
    const pixelWidth = (containerWidth * baseWidth) / 100;

    if (pixelWidth < minColumnWidth) {
      // 使用最小宽度，允许横向滚动
      return (minColumnWidth / containerWidth) * 100;
    }

    if (pixelWidth > maxColumnWidth) {
      return (maxColumnWidth / containerWidth) * 100;
    }

    return baseWidth;
  }, [visibleColumns.length, containerWidth, minColumnWidth, maxColumnWidth]);

  /**
   * 获取列样式对象
   */
  const getColumnStyle = useCallback((columnId: string): React.CSSProperties => {
    const isCollapsed = collapsedColumns.includes(columnId);

    if (isCollapsed) {
      return {
        width: '60px',
        flex: '0 0 60px',
        overflow: 'hidden',
      };
    }

    const width = getColumnWidth();
    return {
      width: `${width}%`,
      flex: `0 0 ${width}%`,
    };
  }, [collapsedColumns, getColumnWidth]);

  return {
    visibleColumns,
    collapsedColumns,
    toggleColumn,
    expandColumn,
    collapseColumn,
    getColumnWidth,
    getColumnStyle,
  };
}
```

**Step 4: 运行测试验证通过**

```bash
cd /Users/user/nexusarchive && npm run test -- useKanbanLayout.test.ts
```

Expected: PASS

**Step 5: 提交**

```bash
git add src/hooks/useKanbanLayout.ts src/hooks/useKanbanLayout.test.ts
git commit -m "feat(pool): add useKanbanLayout hook for responsive layout"
```

---

### Task 3.2: 创建折叠列组件

**Files:**
- Create: `src/components/pool-kanban/CollapsedColumn.tsx`
- Create: `src/components/pool-kanban/CollapsedColumn.css`

**Step 1: 实现组件**

```typescript
// src/components/pool-kanban/CollapsedColumn.tsx
import { memo } from 'react';
import { Badge, Button } from 'antd';
import { ChevronRight } from 'lucide-react';
import type { ColumnGroupConfig } from '@/config/pool-columns.config';
import './CollapsedColumn.css';

export interface CollapsedColumnProps {
  column: ColumnGroupConfig;
  cardCount: number;
  onExpand: () => void;
}

/**
 * 折叠列组件
 *
 * 当列为空时显示为窄条，点击可展开
 */
export const CollapsedColumn = memo<CollapsedColumnProps>(({
  column,
  cardCount,
  onExpand,
}) => {
  return (
    <div className="collapsed-column">
      <button
        className="collapsed-column__trigger"
        onClick={onExpand}
        title={column.title}
      >
        <ChevronRight size={16} />
        <span className="collapsed-column__dot" />
      </button>

      {cardCount > 0 && (
        <Badge
          count={cardCount}
          className="collapsed-column__badge"
          title={`${cardCount} 个文件`}
        />
      )}
    </div>
  );
});

CollapsedColumn.displayName = 'CollapsedColumn';
```

**Step 2: 创建样式文件**

```css
/* src/components/pool-kanban/CollapsedColumn.css */
.collapsed-column {
  position: relative;
  width: 60px;
  flex: 0 0 60px;
  height: 100%;
  background: #f5f5f5;
  border-left: 1px solid #e8e8e8;
  border-right: 1px solid #e8e8e8;
}

.collapsed-column__trigger {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.collapsed-column__trigger:hover {
  background: #e6f7ff;
}

.collapsed-column__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8c8c8c;
}

.collapsed-column__badge {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
}

.collapsed-column__badge .ant-badge-count {
  box-shadow: 0 0 0 1px #fff;
}
```

**Step 3: 更新组件导出**

```typescript
// src/components/pool-kanban/index.ts
export { CollapsedColumn } from './CollapsedColumn';
export type { CollapsedColumnProps } from './CollapsedColumn';
```

**Step 4: 提交**

```bash
git add src/components/pool-kanban/CollapsedColumn.tsx src/components/pool-kanban/CollapsedColumn.css src/components/pool-kanban/index.ts
git commit -m "feat(pool): add CollapsedColumn component for empty columns"
```

---

### Task 3.3: 集成响应式布局到看板视图

**Files:**
- Modify: `src/components/pool-kanban/PoolKanbanView.tsx`

**Step 1: 更新看板视图集成响应式布局**

```typescript
// 在 PoolKanbanView.tsx 中添加响应式布局支持
import { useKanbanLayout } from '@/hooks/useKanbanLayout';
import { CollapsedColumn } from './CollapsedColumn';

// 在组件中添加
const columnCounts = useMemo(() => {
  const counts: Record<string, number> = {};
  columns.forEach(col => {
    counts[col.id] = getTotalCount(col.id);
  });
  return counts;
}, [columns, getTotalCount]);

const {
  visibleColumns,
  collapsedColumns,
  expandColumn,
  getColumnStyle,
} = useKanbanLayout(columnCounts);

// 在渲染看板时
<div className="pool-kanban-view__board">
  {/* 可见列 */}
  {visibleColumns.map(columnId => {
    const column = columns.find(c => c.id === columnId);
    if (!column) return null;

    const columnCards = getCardsForColumn(column.id, column.subStates[0].value);

    return (
      <KanbanColumn
        key={column.id}
        column={column}
        cards={columnCards}
        selectedIds={selectedIds}
        onSelectionChange={handleSelectionChange}
        onAction={(action, cards) => handleColumnAction(action, cards)}
        style={getColumnStyle(column.id)}
      />
    );
  })}

  {/* 折叠列 */}
  {collapsedColumns.map(columnId => {
    const column = columns.find(c => c.id === columnId);
    if (!column) return null;

    return (
      <CollapsedColumn
        key={column.id}
        column={column}
        cardCount={getTotalCount(column.id)}
        onExpand={() => expandColumn(column.id)}
      />
    );
  })}
</div>
```

**Step 2: 提交**

```bash
git add src/components/pool-kanban/PoolKanbanView.tsx
git commit -m "feat(pool): integrate responsive layout with collapsible columns"
```

---

## Task 4.1: 路由集成

**Files:**
- Modify: `src/routes/index.tsx`

**Step 1: 添加看板视图路由**

```typescript
// 在路由配置中添加看板视图
import { PoolKanbanView } from '@/components/pool-kanban';

// 更新 /system/pre-archive/pool 路由
{
  path: '/system/pre-archive/pool',
  element: <PoolKanbanView />,
}
```

**Step 2: 提交**

```bash
git add src/routes/index.tsx
git commit -m "feat(pool): add kanban view route for pre-archive pool"
```

---

## Task 5.1: E2E 测试

**Files:**
- Create: `tests/e2e/pool-kanban.spec.ts`

**Step 1: 编写 E2E 测试**

```typescript
// tests/e2e/pool-kanban.spec.ts
import { test, expect } from '@playwright/test';

test.describe('电子凭证池看板', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('http://localhost:15175/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('应该显示四列看板', async ({ page }) => {
    await page.goto('http://localhost:15175/system/pre-archive/pool');

    // 验证四列都存在
    await expect(page.locator('text=待处理')).toBeVisible();
    await expect(page.locator('text=需要处理')).toBeVisible();
    await expect(page.locator('text=准备就绪')).toBeVisible();
    await expect(page.locator('text=处理中')).toBeVisible();
  });

  test('应该显示子状态标签页', async ({ page }) => {
    await page.goto('http://localhost:15175/system/pre-archive/pool');

    // 验证子状态标签
    await expect(page.locator('text=草稿')).toBeVisible();
    await expect(page.locator('text=待检测')).toBeVisible();
    await expect(page.locator('text=检测失败')).toBeVisible();
    await expect(page.locator('text=待补录')).toBeVisible();
  });

  test('应该能切换子状态标签', async ({ page }) => {
    await page.goto('http://localhost:15175/system/pre-archive/pool');

    // 点击"待检测"标签
    await page.click('text=待检测');

    // 验证卡片列表刷新
    await page.waitForTimeout(500);
    // 根据实际数据验证...
  });

  test('应该能选择卡片', async ({ page }) => {
    await page.goto('http://localhost:15175/system/pre-archive/pool');

    // 点击第一张卡片的左侧选择区
    const firstCard = page.locator('.kanban-card').first();
    await firstCard.locator('.kanban-card__select-area').click();

    // 验证选中状态
    await expect(firstCard).toHaveClass(/kanban-card--selected/);

    // 验证批量操作栏出现
    await expect(page.locator('.batch-action-bar')).toBeVisible();
    await expect(page.locator('text=/已选 \\d+ 个文件/')).toBeVisible();
  });

  test('应该能执行列级批量操作', async ({ page }) => {
    await page.goto('http://localhost:15175/system/pre-archive/pool');

    // 点击列级操作按钮
    await page.click('text=重新检测');

    // 验证确认对话框出现
    await expect(page.locator('text=确认批量操作')).toBeVisible();

    // 取消操作
    await page.click('text=取消');
  });

  test('空列应该自动折叠', async ({ page }) => {
    await page.goto('http://localhost:15175/system/pre-archive/pool');

    // 检查是否有折叠的列（窄条样式）
    const collapsedColumns = page.locator('.collapsed-column');
    const count = await collapsedColumns.count();

    if (count > 0) {
      // 点击折叠列展开
      await collapsedColumns.first().click();

      // 验证列展开
      await expect(page.locator('.kanban-column').nth(count)).toBeVisible();
    }
  });
});
```

**Step 2: 运行 E2E 测试**

```bash
cd /Users/user/nexusarchive && npm run test:smoke
```

**Step 3: 提交**

```bash
git add tests/e2e/pool-kanban.spec.ts
git commit -m "test(pool): add E2E tests for kanban view"
```

---

## 完成检查清单

### 功能完整性
- [ ] 四列看板正常显示（待处理、需要处理、准备就绪、处理中）
- [ ] 每列显示正确的子状态标签页
- [ ] 标签页显示各子状态的文件数量
- [ ] 点击标签页能切换子状态
- [ ] 卡片显示完整信息（标题、文件名、金额、日期、单位）
- [ ] 卡片有3个操作按钮（查看、编辑、删除）
- [ ] 点击卡片左侧区域可选中
- [ ] 列级操作按钮正常工作
- [ ] 批量操作确认对话框正确显示
- [ ] 批量操作执行后清空选择并刷新数据
- [ ] 空列自动折叠为窄条
- [ ] 点击折叠列可展开

### 响应式
- [ ] 不同屏幕尺寸下列宽正确计算
- [ ] 最小列宽触发横向滚动
- [ ] 批量操作栏固定在底部

### 测试
- [ ] 单元测试全部通过
- [ ] E2E 测试全部通过
- [ ] 无 TypeScript 错误
- [ ] 无架构违规

### 文档
- [ ] 更新组件目录文档
- [ ] 更新功能模块说明
- [ ] 添加使用指南

---

## 部署说明

### 环境变量
无新增环境变量

### 数据库变更
无新增数据库迁移

### API 变更
新增以下批量操作 API（需要在后端实现）：
- `POST /api/pool/batch/recheck` - 批量重新检测
- `POST /api/pool/batch/delete` - 批量删除
- `POST /api/pool/move-to-ready` - 移入待归档
- `POST /api/pool/batch/approve` - 批量审批
- `POST /api/pool/smart-match` - 智能匹配
- `POST /api/pool/cancel-archive` - 取消归档
