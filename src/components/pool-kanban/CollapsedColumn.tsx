// src/components/pool-kanban/CollapsedColumn.tsx
// Input: ColumnGroupConfig, cardCount, onExpand callback
// Output: Collapsed kanban column with vertical text and expand button
// Pos: src/components/pool-kanban/CollapsedColumn.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { memo } from 'react';
import { Tooltip } from 'antd';
import { ChevronRight } from 'lucide-react';
import type { ColumnGroupConfig } from '@/config/pool-columns.config';
import './CollapsedColumn.css';

export interface CollapsedColumnProps {
  column: ColumnGroupConfig;
  cardCount: number;
  onExpand: () => void;
}

export const CollapsedColumn = memo<CollapsedColumnProps>(({
  column,
  cardCount,
  onExpand,
}) => {
  const handleExpand = () => {
    onExpand();
  };

  return (
    <div className="collapsed-column">
      {/* Vertical title */}
      <div className="collapsed-column__title" aria-label={column.title}>
        <span className="collapsed-column__title-text">{column.title}</span>
      </div>

      {/* Card count indicator */}
      <Tooltip title={`${cardCount} 个文件`} placement="right">
        <div className="collapsed-column__count">
          <span className="collapsed-column__count-badge">{cardCount}</span>
        </div>
      </Tooltip>

      {/* Expand button */}
      <Tooltip title={`展开 ${column.title}`} placement="right">
        <button
          className="collapsed-column__expand"
          onClick={handleExpand}
          aria-label={`展开 ${column.title}`}
          type="button"
        >
          <ChevronRight size={16} />
        </button>
      </Tooltip>

      {/* Hover hint */}
      <div className="collapsed-column__hint">
        点击展开
      </div>
    </div>
  );
});

CollapsedColumn.displayName = 'CollapsedColumn';
