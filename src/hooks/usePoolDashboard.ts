// Input: usePoolKanban hook
// Output: Dashboard statistics
// Pos: src/hooks/usePoolDashboard.ts

import { useMemo } from 'react';
import { usePoolKanban, toSimplifiedStatus } from './usePoolKanban';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';

/**
 * 仪表板统计数据接口
 */
export interface DashboardStats {
  [SimplifiedPreArchiveStatus.PENDING_CHECK]: number;
  [SimplifiedPreArchiveStatus.NEEDS_ACTION]: number;
  [SimplifiedPreArchiveStatus.READY_TO_MATCH]: number;
  [SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]: number;
  [SimplifiedPreArchiveStatus.COMPLETED]: number;
}

/**
 * 仪表板统计 Hook
 *
 * 计算每个简化状态的凭证数量
 */
export function usePoolDashboard(): {
  stats: DashboardStats;
  totalCount: number;
  readyToArchiveCount: number;
  needsActionCount: number;
} {
  const { cards } = usePoolKanban();

  const stats = useMemo<DashboardStats>(() => {
    const counts = {
      [SimplifiedPreArchiveStatus.PENDING_CHECK]: 0,
      [SimplifiedPreArchiveStatus.NEEDS_ACTION]: 0,
      [SimplifiedPreArchiveStatus.READY_TO_MATCH]: 0,
      [SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]: 0,
      [SimplifiedPreArchiveStatus.COMPLETED]: 0,
    } as DashboardStats;

    // 遍历所有卡片，将旧状态映射到新状态并计数
    cards.forEach(card => {
      const simplifiedStatus = toSimplifiedStatus(card.status);
      counts[simplifiedStatus]++;
    });

    return counts;
  }, [cards]);

  const totalCount = useMemo(() => {
    return Object.values(stats).reduce((sum, count) => sum + count, 0);
  }, [stats]);

  return {
    stats,
    totalCount,
    readyToArchiveCount: stats[SimplifiedPreArchiveStatus.READY_TO_ARCHIVE],
    needsActionCount: stats[SimplifiedPreArchiveStatus.NEEDS_ACTION],
  };
}
