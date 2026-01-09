// Input: React Query hook, pool API, column config
// Output: usePoolKanban hook for kanban data management
// Pos: 看板数据管理 Hook

import { useQuery } from '@tanstack/react-query';
import { poolApi } from '@/api/pool';
import { POOL_COLUMN_GROUPS, type ColumnGroupConfig } from '@/config/pool-columns.config';
import type { PoolItem } from '@/api/pool';

interface KanbanCard extends PoolItem {
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
      return await poolApi.getList();
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
