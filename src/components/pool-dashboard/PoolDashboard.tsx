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
import { CATEGORY_OPTIONS } from '@/constants/archivalCategories';
import './PoolDashboard.css';

interface PoolDashboardProps {
  /** 当前激活的筛选状态 */
  activeFilter: SimplifiedPreArchiveStatus | null;
  /** 当前激活的门类筛选 */
  categoryFilter?: string | null;
  /** 筛选状态变更回调 */
  onFilterChange: (status: SimplifiedPreArchiveStatus | null) => void;
  /** 门类筛选变更回调 */
  onCategoryChange?: (category: string | null) => void;
  /** 是否显示批量操作按钮 */
  showActions?: boolean;
  /** 是否显示批量归档回调 */
  onBatchArchive?: (status: SimplifiedPreArchiveStatus) => void;
  /** 是否显示门类选择器 (维度筛选器) */
  showCategoryPicker?: boolean;
}

/**
 * 记账凭证库仪表板
 *
 * 展示 5 个核心状态的统计卡片 + 4 个档案门类快速筛选
 */
export const PoolDashboard: React.FC<PoolDashboardProps> = ({
  activeFilter,
  categoryFilter,
  onFilterChange,
  onCategoryChange,
  showActions = false,
  onBatchArchive,
  showCategoryPicker = true,
}) => {
  const { refetch: _refetch } = usePoolKanban({
    filter: activeFilter,
    categoryFilter
  });
  const { stats, totalCount: _totalCount } = usePoolDashboard({ categoryFilter });

  // 计算每个状态的卡片配置
  const cards = useMemo(() => {
    return Object.values(SimplifiedPreArchiveStatus).map((status) => {
      const count = stats[status];
      const isActive = activeFilter === status;
      const _config = STATUS_CONFIG[status];

      let currentActionLabel: string | undefined;
      let currentShowAction = false;

      if (showActions && count > 0) {
        if (status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE) {
          currentShowAction = true;
          currentActionLabel = `批量归档 (${count})`;
        } else if (status === SimplifiedPreArchiveStatus.PENDING_CHECK) {
          currentShowAction = true;
          currentActionLabel = `重新检测 (${count})`; // "重新检测" for PENDING_CHECK
        }
      }

      return {
        status,
        count,
        isActive,
        showAction: currentShowAction,
        actionLabel: currentShowAction ? currentActionLabel : undefined,
      };
    });
  }, [stats, activeFilter, showActions]);

  const handleCardClick = (status: SimplifiedPreArchiveStatus) => {
    const newFilter = activeFilter === status ? null : status;
    onFilterChange(newFilter);
  };

  const handleCardAction = (status: SimplifiedPreArchiveStatus) => {
    onBatchArchive?.(status); // Pass status to the callback
  };

  const handleCategoryClick = (category: string) => {
    const newFilter = categoryFilter === category ? null : category;
    onCategoryChange?.(newFilter);
  };

  return (
    <div className="pool-dashboard space-y-4">
      {/* 状态统计卡片 */}
      <div className="pool-dashboard__cards">
        {cards.map((card) => (
          <DashboardCard
            key={card.status}
            status={card.status}
            count={card.count}
            isActive={card.isActive}
            showAction={card.showAction}
            actionLabel={card.actionLabel}
            onCardClick={() => handleCardClick(card.status)}
            onActionClick={card.showAction ? () => handleCardAction(card.status) : undefined}
          />
        ))}
      </div>

      {/* 门类多维筛选器 (New) */}
      {showCategoryPicker && (
        <div className="flex items-center gap-3 px-4 py-2 bg-white/50 backdrop-blur rounded-xl border border-slate-100 shadow-sm animate-in fade-in slide-in-from-bottom-2 duration-500">
          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mr-2">维度筛选</span>
          <div className="flex gap-2">
            {CATEGORY_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => handleCategoryClick(opt.value)}
                className={`
                flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition-all duration-200
                ${categoryFilter === opt.value
                    ? `bg-${opt.color}-50 text-${opt.color}-600 ring-2 ring-${opt.color}-500/20`
                    : 'bg-slate-50 text-slate-500 hover:bg-slate-100 hover:text-slate-700'}
              `}
              >
                <opt.icon size={14} />
                {opt.label}
              </button>
            ))}
          </div>
          {categoryFilter && (
            <button
              onClick={() => onCategoryChange?.(null)}
              className="text-xs text-slate-400 hover:text-slate-600 transition-colors ml-auto"
            >
              重置维度
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default PoolDashboard;
