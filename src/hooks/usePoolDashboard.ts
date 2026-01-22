// Input: usePoolKanban hook
// Output: Dashboard statistics
// Pos: src/hooks/usePoolDashboard.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useMemo } from 'react';
import { usePoolKanban, toSimplifiedStatus } from './usePoolKanban';
import { SimplifiedPreArchiveStatus, DashboardStats } from '@/config/pool-columns.config';

/**
 * 仪表板统计 Hook
 *
 * 计算每个简化状态的凭证数量
 * 修复：使用 useMemo 稳定返回值引用
 */
export function usePoolDashboard(options: { categoryFilter?: string | null } = {}): {
  stats: DashboardStats;
  totalCount: number;
  readyToArchiveCount: number;
  needsActionCount: number;
} {
  const { cards } = usePoolKanban({ categoryFilter: options.categoryFilter });

  const stats = useMemo<DashboardStats>(() => {
    const counts = {
      [SimplifiedPreArchiveStatus.PENDING_CHECK]: 0,
      [SimplifiedPreArchiveStatus.NEEDS_ACTION]: 0,
      [SimplifiedPreArchiveStatus.READY_TO_MATCH]: 0,
      [SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]: 0,
      [SimplifiedPreArchiveStatus.COMPLETED]: 0,
    } as DashboardStats;

    cards.forEach(card => {
      const simplifiedStatus = toSimplifiedStatus(card.status);
      counts[simplifiedStatus]++;
    });

    return counts;
  }, [cards]);

  const totalCount = useMemo(() => {
    return Object.values(stats).reduce((sum, count) => sum + count, 0);
  }, [stats]);

  // 使用 useMemo 稳定返回值引用
  return useMemo(() => ({
    stats,
    totalCount,
    readyToArchiveCount: stats[SimplifiedPreArchiveStatus.READY_TO_ARCHIVE],
    needsActionCount: stats[SimplifiedPreArchiveStatus.NEEDS_ACTION],
  }), [stats, totalCount]);
}
