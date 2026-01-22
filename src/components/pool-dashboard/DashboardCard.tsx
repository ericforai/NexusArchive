// Input: count, status, isActive, onClick, actionLabel
// Output: Statistical card with action button
// Pos: src/components/pool-dashboard/DashboardCard.tsx

import React from 'react';
import { STATUS_CONFIG, type SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';
import * as Icons from 'lucide-react';
import './DashboardCard.css';

interface DashboardCardProps {
  status: SimplifiedPreArchiveStatus;
  count: number;
  isActive: boolean;
  /** 卡片主体点击事件：切换筛选状态 */
  onCardClick: () => void;
  /** 操作按钮点击事件：执行批量操作 */
  onActionClick?: () => void;
  actionLabel?: string;
  showAction?: boolean;
}

/**
 * 仪表板统计卡片
 *
 * 显示每个状态的凭证数量，支持点击筛选
 */
export const DashboardCard: React.FC<DashboardCardProps> = ({
  status,
  count,
  isActive,
  onCardClick,
  onActionClick,
  actionLabel,
  showAction = false,
}) => {
  const config = STATUS_CONFIG[status];
  // 将 kebab-case 转换为 PascalCase (例如: 'alert-circle' -> 'AlertCircle')
  const iconName = config.icon.split('-').map(word =>
    word.charAt(0).toUpperCase() + word.slice(1)
  ).join('');
  const IconComponent = Icons[iconName as keyof typeof Icons] as React.ComponentType<{ className?: string }>;

  return (
    <div
      className={`dashboard-card ${isActive ? 'dashboard-card--active' : ''}`}
      onClick={onCardClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onCardClick();
        }
      }}
      aria-label={`${config.label}：${count} 条`}
      aria-pressed={isActive}
    >
      <div className="dashboard-card__header" style={{ '--card-color': config.color } as React.CSSProperties}>
        <div className="dashboard-card__icon">
          <IconComponent className="lucide-icon" />
        </div>
        <div className="dashboard-card__count">{count}</div>
      </div>

      <div className="dashboard-card__body">
        <div className="dashboard-card__title">{config.label}</div>
        <div className="dashboard-card__description">{config.description}</div>
      </div>

      {showAction && actionLabel && onActionClick && (
        <div className="dashboard-card__footer">
          <button
            className="dashboard-card__action"
            onClick={(e) => {
              e.stopPropagation();
              onActionClick();
            }}
          >
            {actionLabel}
          </button>
        </div>
      )}

      {isActive && (
        <div className="dashboard-card__indicator" style={{ backgroundColor: config.color }} />
      )}
    </div>
  );
};

export default DashboardCard;
