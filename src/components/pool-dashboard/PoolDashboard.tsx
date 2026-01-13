// Input: usePoolKanban hook, filter state
// Output: Dashboard with 5 statistical cards
// Pos: src/components/pool-dashboard/PoolDashboard.tsx

import React, { useMemo } from 'react';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { usePoolDashboard } from '@/hooks/usePoolDashboard';
import { DashboardCard } from './DashboardCard';
import {
  SimplifiedPreArchiveStatus,
  STATUS_CONFIG,
} from '@/config/pool-columns.config';
import './PoolDashboard.css';

interface PoolDashboardProps {
  /** 当前激活的筛选状态 */
  activeFilter: SimplifiedPreArchiveStatus | null;
  /** 筛选状态变更回调 */
  onFilterChange: (status: SimplifiedPreArchiveStatus | null) => void;
  /** 是否显示批量操作按钮 */
  showActions?: boolean;
  /** 批量归档回调 */
  onBatchArchive?: () => void;
}

/**
 * 电子凭证池仪表板
 *
 * 展示 5 个核心状态的统计卡片，支持点击筛选看板数据
 */
export const PoolDashboard: React.FC<PoolDashboardProps> = ({
  activeFilter,
  onFilterChange,
  showActions = false,
  onBatchArchive,
}) => {
  const { refetch } = usePoolKanban();
  const { stats, totalCount } = usePoolDashboard();

  // 计算每个状态的卡片配置
  const cards = useMemo(() => {
    return Object.values(SimplifiedPreArchiveStatus).map((status) => {
      const count = stats[status];
      const isActive = activeFilter === status;
      const config = STATUS_CONFIG[status];

      // "可归档"状态显示批量操作按钮
      const showAction = showActions && status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE && count > 0;
      const actionLabel = `批量归档 (${count})`;

      return {
        status,
        count,
        isActive,
        showAction,
        actionLabel: showAction ? actionLabel : undefined,
      };
    });
  }, [stats, activeFilter, showActions]);

  const handleCardClick = (status: SimplifiedPreArchiveStatus) => {
    // 切换筛选状态：如果点击当前激活状态，则取消筛选
    const newFilter = activeFilter === status ? null : status;
    onFilterChange(newFilter);
  };

  const handleBatchArchive = () => {
    onBatchArchive?.();
  };

  return (
    <div className="pool-dashboard">
      <div className="pool-dashboard__cards">
        {cards.map((card) => (
          <DashboardCard
            key={card.status}
            status={card.status}
            count={card.count}
            isActive={card.isActive}
            showAction={card.showAction}
            actionLabel={card.actionLabel}
            onClick={() =>
              card.showAction && card.status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE
                ? handleBatchArchive()
                : handleCardClick(card.status)
            }
          />
        ))}
      </div>
    </div>
  );
};

export default PoolDashboard;
