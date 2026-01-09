// src/components/pool-kanban/PoolKanbanView.tsx
// Input: ColumnGroupConfig, PoolItem[], selection state, and action callbacks
// Output: Rendered kanban board view with all columns and batch operations
// Pos: src/components/pool-kanban/PoolKanbanView.tsx
import { useCallback, useState, useMemo } from 'react';
import { Spin } from 'antd';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { usePoolBatchAction, type BatchActionType } from '@/hooks/usePoolBatchAction';
import { BatchActionBar } from './BatchActionBar';
import { KanbanColumn } from './KanbanColumn';
import type { PoolItem } from '@/api/pool';
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

/**
 * 电子凭证池看板视图
 *
 * 展示档案预处理流程的四列看板，支持批量操作
 */
export function PoolKanbanView({ className }: PoolKanbanViewProps) {
  const {
    columns,
    loading,
    error,
    refetch,
    getCardsForColumn,
  } = usePoolKanban();

  // 使用批量操作 Hook 管理选择状态和批量操作
  const batchAction = usePoolBatchAction();

  // 当前待执行的批量操作类型（由列操作按钮触发）
  const [pendingAction, setPendingAction] = useState<BatchActionType | null>(null);

  // 当前待执行的操作标签
  const [pendingActionLabel, setPendingActionLabel] = useState<string>('');

  // 处理卡片选择切换 - 委托给 Hook
  const handleSelectionChange = useCallback((cardId: string) => {
    batchAction.toggleSelection(cardId);
  }, [batchAction]);

  // 处理列级操作按钮点击
  const handleColumnAction = useCallback((actionKey: string, columnCards: PoolItem[]) => {
    // 自动选中该列当前子状态的所有文件
    const cardIds = columnCards.map(card => card.id);
    batchAction.selectAll(cardIds);

    // 设置待执行的操作
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
      // 操作成功后清空选择并刷新数据
      batchAction.clearSelection();
      setPendingAction(null);
      setPendingActionLabel('');
      refetch();
    }
  }, [pendingAction, batchAction, refetch]);

  // 取消操作（清空选择和待执行操作）
  const handleCancelBatchAction = useCallback(() => {
    batchAction.clearSelection();
    setPendingAction(null);
    setPendingActionLabel('');
    batchAction.clearResult();
  }, [batchAction]);

  // 关闭结果提示
  const handleCloseResult = useCallback(() => {
    batchAction.clearResult();
    batchAction.clearSelection();
    setPendingAction(null);
    setPendingActionLabel('');
    refetch();
  }, [batchAction, refetch]);

  // 计算选中的卡片数量
  const selectedCount = batchAction.getSelectedCount();

  // 判断是否显示批量操作栏
  const showBatchActionBar = useMemo(() => {
    return selectedCount > 0 || batchAction.state.result !== null;
  }, [selectedCount, batchAction.state.result]);

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
              selectedIds={batchAction.state.selection}
              onSelectionChange={handleSelectionChange}
              onAction={handleColumnAction}
            />
          );
        })}
      </div>

      {/* 批量操作栏 */}
      {showBatchActionBar && (
        <BatchActionBar
          selectedCount={selectedCount}
          actionLabel={pendingActionLabel}
          isExecuting={batchAction.state.isExecuting}
          onExecute={handleExecuteBatchAction}
          onCancel={batchAction.state.result ? handleCloseResult : handleCancelBatchAction}
          result={batchAction.state.result}
        />
      )}
    </div>
  );
}
