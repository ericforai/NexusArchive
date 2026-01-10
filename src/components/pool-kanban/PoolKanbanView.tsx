// src/components/pool-kanban/PoolKanbanView.tsx
// Input: ColumnGroupConfig, PoolItem[], selection state, and action callbacks (loading Spin)
// Output: Rendered kanban board view with responsive layout and batch operations
// Pos: src/components/pool-kanban/PoolKanbanView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { useCallback, useState, useMemo, useRef, useEffect } from 'react';
import { Spin, Button, Space } from 'antd';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { usePoolBatchAction, type BatchActionType } from '@/hooks/usePoolBatchAction';
import { useKanbanLayout } from '@/hooks/useKanbanLayout';
import { BatchActionBar } from './BatchActionBar';
import { KanbanColumn } from './KanbanColumn';
import { CollapsedColumn } from './CollapsedColumn';
import { Columns3, Expand } from 'lucide-react';
import type { PoolItem } from '@/api/pool';
import type { ColumnGroupConfig } from '@/config/pool-columns.config';
import './PoolKanbanView.css';

export interface PoolKanbanViewProps {
  /** 额外的类名 */
  className?: string;
}

/**
 * 列操作到批量操作类型的映射
 */
const ACTION_KEY_TO_BATCH_TYPE: Record<string, BatchActionType> = {
  'recheck': 'recheck',
  'delete': 'delete',
  'edit-metadata': 'edit',
  'retry-check': 'recheck',
  'smart-match': 'match',
  'manual-link': 'match',
  'batch-approve': 'archive',
  'cancel-archive': 'cancel',
};

/**
 * 操作标签映射
 */
const ACTION_LABELS: Record<string, string> = {
  'recheck': '执行重新检测',
  'delete': '执行删除',
  'edit-metadata': '编辑元数据',
  'retry-check': '执行重试检测',
  'smart-match': '执行智能匹配',
  'manual-link': '执行手动关联',
  'batch-approve': '提交归档',
  'cancel-archive': '取消归档',
  'view-detail': '查看详情',
  'move-to-archive': '移入待归档',
};

interface KanbanBoardProps {
  columns: ColumnGroupConfig[];
  cardsByColumn: Record<string, PoolItem[]>;
  columnWidth: number;
  selectedIds: Set<string>;
  isCollapsed: (columnId: string) => boolean;
  onSelectionChange: (cardId: string) => void;
  onColumnAction: (actionKey: string, cards: PoolItem[]) => void;
  onToggleCollapse: (columnId: string) => void;
}

/**
 * 看板面板组件 - 渲染所有列
 */
function KanbanBoard({
  columns,
  cardsByColumn,
  columnWidth,
  selectedIds,
  isCollapsed,
  onSelectionChange,
  onColumnAction,
  onToggleCollapse,
}: KanbanBoardProps) {
  return (
    <div
      className="pool-kanban-view__board"
      style={{
        '--column-width': `${columnWidth}px`,
      } as React.CSSProperties}
    >
      {columns.map(column => {
        const columnCards = cardsByColumn[column.id] || [];
        const collapsed = isCollapsed(column.id);

        if (collapsed) {
          return (
            <CollapsedColumn
              key={column.id}
              column={column}
              cardCount={columnCards.length}
              onExpand={() => onToggleCollapse(column.id)}
            />
          );
        }

        return (
          <KanbanColumn
            key={column.id}
            column={column}
            cards={columnCards}
            selectedIds={selectedIds}
            onSelectionChange={onSelectionChange}
            onAction={onColumnAction}
          />
        );
      })}
    </div>
  );
}

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

/**
 * 看板工具栏组件 - 包含标题、布局控制按钮和批量操作按钮
 * 当有选中项时，显示批量操作按钮；否则显示布局控制按钮
 */
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
              <Button size="small" onClick={() => onBatchAction?.('edit')}>
                批量编辑
              </Button>
              <Button size="small" onClick={() => onBatchAction?.('recheck')}>
                批量检测
              </Button>
              <Button size="small" danger onClick={() => onBatchAction?.('delete')}>
                批量删除
              </Button>
              <Button size="small" onClick={() => onClearSelection?.()}>
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

/**
 * 电子凭证池看板视图
 *
 * 展示档案预处理流程的四列看板，支持批量操作和响应式布局
 */
export function PoolKanbanView({ className }: PoolKanbanViewProps) {
  const containerRef = useRef<HTMLDivElement>(null) as React.RefObject<HTMLDivElement>;

  const {
    columns,
    loading,
    error,
    refetch,
    getCardsForColumn,
  } = usePoolKanban();

  const batchAction = usePoolBatchAction();

  const layout = useKanbanLayout({
    containerRef,
    enabled: true,
  });

  const [pendingAction, setPendingAction] = useState<BatchActionType | null>(null);
  const [pendingActionLabel, setPendingActionLabel] = useState<string>('');

  // 处理卡片选择切换
  const handleSelectionChange = useCallback((cardId: string) => {
    batchAction.toggleSelection(cardId);
  }, [batchAction]);

  // 处理列级操作按钮点击
  const handleColumnAction = useCallback((actionKey: string, columnCards: PoolItem[]) => {
    const cardIds = columnCards.map(card => card.id);
    batchAction.selectAll(cardIds);

    const actionType = ACTION_KEY_TO_BATCH_TYPE[actionKey];
    if (actionType) {
      setPendingAction(actionType);
      setPendingActionLabel(ACTION_LABELS[actionKey] || '执行操作');
    }
  }, [batchAction]);

  // 执行批量操作
  const handleExecuteBatchAction = useCallback(async () => {
    if (!pendingAction) return;

    const result = await batchAction.executeAction(pendingAction);
    if (result.success) {
      batchAction.clearSelection();
      setPendingAction(null);
      setPendingActionLabel('');
      refetch();
    }
  }, [pendingAction, batchAction, refetch]);

  // 关闭结果提示
  const handleCloseResult = useCallback(() => {
    batchAction.clearResult();
    batchAction.clearSelection();
    setPendingAction(null);
    setPendingActionLabel('');
    refetch();
  }, [batchAction, refetch]);

  // 构建卡片按列分组的映射
  const cardsByColumn = useMemo(() => {
    const map: Record<string, PoolItem[]> = {};
    columns.forEach(column => {
      const subState = column.subStates[0].value;
      map[column.id] = getCardsForColumn(column.id, subState);
    });
    return map;
  }, [columns, getCardsForColumn]);

  // 布局控制回调
  const handleCollapseAllEmpty = useCallback(() => {
    layout.collapseAllEmpty(cardsByColumn);
  }, [layout, cardsByColumn]);

  const handleExpandAll = useCallback(() => {
    layout.expandAll();
  }, [layout]);

  const handleToggleCollapse = useCallback((columnId: string) => {
    layout.toggleCollapse(columnId);
  }, [layout]);

  // 处理工具栏批量操作按钮点击
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

  // 初始化时自动折叠空列（仅在首次挂载时执行）
  useEffect(() => {
    if (columns.length > 0 && !loading && !error) {
      layout.collapseAllEmpty(cardsByColumn);
    }
  }, []); // 忽略依赖：仅在组件挂载时执行一次

  // 批量操作选中数量
  const selectedCount = batchAction.getSelectedCount();

  // 布局信息
  const layoutInfo = useMemo(() => ({
    totalColumns: columns.length,
    visibleColumns: layout.getVisibleColumnCount(),
    collapsedColumns: layout.collapsedColumns.size,
  }), [columns.length, layout]);

  // 加载状态
  if (loading) {
    return (
      <div className="pool-kanban-view pool-kanban-view--loading">
        <Spin size="large" tip="加载中...">
          <div style={{ minHeight: 120 }} />
        </Spin>
      </div>
    );
  }

  // 错误状态
  if (error) {
    return (
      <div className="pool-kanban-view pool-kanban-view--error">
        <p>加载失败: {error.message}</p>
        <button onClick={() => refetch()}>重试</button>
      </div>
    );
  }

  return (
    <div ref={containerRef as React.RefObject<HTMLDivElement>} className={`pool-kanban-view ${className || ''}`}>
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

      <KanbanBoard
        columns={columns}
        cardsByColumn={cardsByColumn}
        columnWidth={layout.columnWidth}
        selectedIds={batchAction.state.selection}
        isCollapsed={layout.isCollapsed}
        onSelectionChange={handleSelectionChange}
        onColumnAction={handleColumnAction}
        onToggleCollapse={handleToggleCollapse}
      />

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
    </div>
  );
}
