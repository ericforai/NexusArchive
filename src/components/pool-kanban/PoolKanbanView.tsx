// src/components/pool-kanban/PoolKanbanView.tsx
// Input: ColumnGroupConfig, PoolItem[], selection state, and action callbacks
// Output: Rendered kanban board view with all columns and batch operations
// Pos: src/components/pool-kanban/PoolKanbanView.tsx
import { useState, useCallback } from 'react';
import { Spin } from 'antd';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { KanbanColumn } from './KanbanColumn';
import type { PoolItem } from '@/api/pool';
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
  const handleColumnAction = useCallback((action: string, columnCards: PoolItem[]) => {
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
