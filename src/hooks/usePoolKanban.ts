// Input: React Query hook, pool API, column config, status simplification map
// Output: usePoolKanban hook for kanban data management with filter support
// Pos: src/hooks/usePoolKanban.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useQuery } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { poolApi } from '@/api/pool';
import { useFondsStore } from '@/store/useFondsStore';
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
  'READY_TO_ARCHIVE': SimplifiedPreArchiveStatus.READY_TO_ARCHIVE,  // 数据库实际使用的状态

  // COMPLETED
  'PENDING_APPROVAL': SimplifiedPreArchiveStatus.COMPLETED,
  'ARCHIVING': SimplifiedPreArchiveStatus.COMPLETED,
  'ARCHIVED': SimplifiedPreArchiveStatus.COMPLETED,
  'COMPLETED': SimplifiedPreArchiveStatus.COMPLETED,  // 数据库实际使用的状态
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
  /** 筛选特定门类 (VOUCHER/LEDGER/REPORT/OTHER) */
  categoryFilter?: string | null;
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
 * 获取所有预处理凭证，按列 and/or 门类分组筛选
 * 修复：使用 useMemo 稳定返回值引用
 */
export function usePoolKanban(options: UsePoolKanbanOptions = {}): UsePoolKanbanResult {
  const currentFonds = useFondsStore(state => state.currentFonds);

  // 获取所有预处理凭证
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['pool', 'kanban', currentFonds?.fondsCode || 'default', options.categoryFilter || 'all'],
    queryFn: async () => {
      const result = await poolApi.getList(options.categoryFilter);
      return result;
    },
    staleTime: 0,  // 强制重新获取，不使用缓存
    gcTime: 0,      // 立即清理缓存
  });

  /**
   * 根据状态获取列ID
   */
  const getColumnIdForStatus = useCallback((status: string): string => {
    for (const column of POOL_COLUMN_GROUPS) {
      if (column.subStates.some(sub => sub.value === status)) {
        return column.id;
      }
    }
    return '';
  }, []);

  const cards: KanbanCard[] = useMemo(() => {
    let baseCards = (data || []).map(card => ({
      ...card,
      _columnId: getColumnIdForStatus(card.status),
    }));

    // 门类筛选：VOUCHER、AC04、OTHER、AC03 包含 null 值
    if (options.categoryFilter === 'AC03') { // Handle AC03 first because it has special REPORT matching
      // AC03 还需要匹配 REPORT 类型
      baseCards = baseCards.filter(card =>
        !card.voucherType || card.voucherType === 'AC03' || card.voucherType === 'REPORT'
      );
    } else if (options.categoryFilter === 'VOUCHER' || options.categoryFilter === 'AC04' || options.categoryFilter === 'OTHER') {
      baseCards = baseCards.filter(card =>
        !card.voucherType || card.voucherType === options.categoryFilter
      );
    } else if (options.categoryFilter) {
      baseCards = baseCards.filter(card => card.voucherType === options.categoryFilter);
    }

    return baseCards;
  }, [data, getColumnIdForStatus, options.categoryFilter]);

  /**
   * 获取指定列和子状态的卡片
   */
  const getCardsForColumn = useCallback((columnId: string, subState: string): KanbanCard[] => {
    let filteredCards = cards.filter(card => card.status === subState);

    // 如果有状态筛选条件，只返回匹配简化状态的卡片
    if (options.filter) {
      filteredCards = filteredCards.filter(card => {
        const simplifiedStatus = toSimplifiedStatus(card.status);
        return simplifiedStatus === options.filter;
      });
    }

    return filteredCards;
  }, [cards, options.filter]);

  /**
   * 获取指定列和子状态的卡片数量
   */
  const getSubStateCount = useCallback((columnId: string, subState: string): number => {
    return cards.filter(card => card.status === subState).length;
  }, [cards]);

  /**
   * 获取指定列的总卡片数量
   */
  const getTotalCount = useCallback((columnId: string): number => {
    const column = POOL_COLUMN_GROUPS.find(c => c.id === columnId);
    if (!column) return 0;

    return column.subStates.reduce(
      (sum, sub) => sum + getSubStateCount(columnId, sub.value),
      0
    );
  }, [getSubStateCount]);

  const doRefetch = useCallback(async () => {
    await refetch();
  }, [refetch]);

  // 使用 useMemo 稳定返回值引用
  return useMemo(() => ({
    columns: POOL_COLUMN_GROUPS,
    cards,
    loading: isLoading,
    error: error as Error | null,
    refetch: doRefetch,
    getCardsForColumn,
    getSubStateCount,
    getTotalCount,
  }), [cards, isLoading, error, doRefetch, getCardsForColumn, getSubStateCount, getTotalCount]);
}
