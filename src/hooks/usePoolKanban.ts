// Input: React Query hook, pool API, column config, status simplification map
// Output: usePoolKanban hook for kanban data management with filter support
// Pos: src/hooks/usePoolKanban.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useQuery } from '@tanstack/react-query';
import { poolApi } from '@/api/pool';
import { POOL_COLUMN_GROUPS, SimplifiedPreArchiveStatus, type ColumnGroupConfig } from '@/config/pool-columns.config';
import type { PoolItem } from '@/api/pool';

interface KanbanCard extends PoolItem {
  _columnId?: string;
}

/**
 * 旧状态到新状态的映射表（向下兼容）
 */
export const STATUS_SIMPLIFICATION_MAP: Record<string, SimplifiedPreArchiveStatus> = {
  // PENDING_CHECK
  'DRAFT': SimplifiedPreArchiveStatus.PENDING_CHECK,
  'PENDING_CHECK': SimplifiedPreArchiveStatus.PENDING_CHECK,

  // NEEDS_ACTION
  'CHECK_FAILED': SimplifiedPreArchiveStatus.NEEDS_ACTION,
  'PENDING_METADATA': SimplifiedPreArchiveStatus.NEEDS_ACTION,

  // READY_TO_MATCH
  'MATCH_PENDING': SimplifiedPreArchiveStatus.READY_TO_MATCH,
  'MATCHED': SimplifiedPreArchiveStatus.READY_TO_MATCH,

  // READY_TO_ARCHIVE
  'PENDING_ARCHIVE': SimplifiedPreArchiveStatus.READY_TO_ARCHIVE,

  // COMPLETED
  'PENDING_APPROVAL': SimplifiedPreArchiveStatus.COMPLETED,
  'ARCHIVING': SimplifiedPreArchiveStatus.COMPLETED,
  'ARCHIVED': SimplifiedPreArchiveStatus.COMPLETED,
};

/**
 * 将旧状态代码转换为简化状态
 */
export function toSimplifiedStatus(oldStatus: string): SimplifiedPreArchiveStatus {
  return STATUS_SIMPLIFICATION_MAP[oldStatus] || SimplifiedPreArchiveStatus.PENDING_CHECK;
}

interface UsePoolKanbanOptions {
  /** 筛选特定状态（null 表示显示全部） */
  filter?: SimplifiedPreArchiveStatus | null;
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
export function usePoolKanban(options: UsePoolKanbanOptions = {}): UsePoolKanbanResult {
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
    let filteredCards = cards.filter(card => card.status === subState);

    // 如果有筛选条件，只返回匹配简化状态的卡片
    if (options.filter) {
      filteredCards = filteredCards.filter(card => {
        const simplifiedStatus = toSimplifiedStatus(card.status);
        return simplifiedStatus === options.filter;
      });
    }

    return filteredCards;
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
