// src/components/pool-kanban/KanbanCard.tsx
// Input: PoolItem object with selection state, action callbacks, and column-level actions
// Output: Rendered kanban card component with interactive elements and dynamic action buttons
// Pos: src/components/pool-kanban/KanbanCard.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { memo, useMemo } from 'react';
import { FileText, Calendar, DollarSign, Building } from 'lucide-react';
import { Button, Badge } from 'antd';
import type { PoolItem } from '@/api/pool';
import { getSubStateLabel } from '@/config/pool-columns.config';
import type { ColumnAction } from '@/config/pool-columns.config';
import './KanbanCard.css';

export interface KanbanCardProps {
  card: PoolItem;
  selected: boolean;
  onSelect?: (cardId: string) => void;
  onAction?: (cardId: string, action: string) => void;
  columnActions?: ColumnAction[];
}

export const KanbanCard = memo<KanbanCardProps>(({
  card,
  selected,
  onSelect,
  onAction,
  columnActions,
}) => {
  const statusLabel = getSubStateLabel(card.status as any);

  const handleSelectAreaClick = () => {
    onSelect?.(card.id);
  };

  const handleAction = (action: string) => {
    onAction?.(card.id, action);
  };

  // 计算要显示的操作按钮
  const displayActions = useMemo(() => {
    const baseActions: Array<{ key: string; label: string; danger?: boolean }> = [
      { key: 'view', label: '查看' },
      { key: 'edit', label: '编辑' },
    ];
    if (columnActions) {
      const extraActions = columnActions
        .filter(a => a.key !== 'delete')
        .map(a => ({ key: a.key, label: a.label, danger: a.danger }));
      baseActions.push(...extraActions);
    }
    return baseActions;
  }, [columnActions]);

  // 使用 summary 作为标题，docDate 作为业务日期
  const title = card.summary || card.code || '未命名凭证';
  const bizDate = card.docDate || card.date;
  const fileName = card.fileName || `${card.code}.pdf`;

  return (
    <div className={`kanban-card ${selected ? 'kanban-card--selected' : ''}`}>
      {/* 左侧选择区域 */}
      <div
        className="kanban-card__select-area"
        onClick={handleSelectAreaClick}
        aria-label="选择卡片"
      >
        <div className="kanban-card__select-indicator" />
      </div>

      {/* 右侧内容区域 */}
      <div className="kanban-card__content">
        {/* 标题和状态 */}
        <div className="kanban-card__header">
          <span className="kanban-card__title">{title}</span>
          <Badge status="processing" text={statusLabel} />
        </div>

        {/* 文件信息 */}
        <div className="kanban-card__file">
          <FileText size={14} />
          <span className="kanban-card__file-name">{fileName}</span>
        </div>

        {/* 详情字段 */}
        <div className="kanban-card__details">
          {card.amount && (
            <div className="kanban-card__detail">
              <DollarSign size={14} />
              <span>¥{Number(card.amount).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}</span>
            </div>
          )}

          {bizDate && (
            <div className="kanban-card__detail">
              <Calendar size={14} />
              <span>{bizDate}</span>
            </div>
          )}

          {card.source && (
            <div className="kanban-card__detail kanban-card__detail--full">
              <Building size={14} />
              <span>{card.source}</span>
            </div>
          )}
        </div>

        {/* 操作按钮 */}
        <div className="kanban-card__actions">
          {displayActions.map(action => (
            <Button
              key={action.key}
              size="small"
              type="link"
              danger={action.danger}
              onClick={() => handleAction(action.key)}
            >
              {action.label}
            </Button>
          ))}
          <Button
            size="small"
            type="link"
            danger
            onClick={() => handleAction('delete')}
          >
            删除
          </Button>
        </div>
      </div>

      {/* 取消选择按钮（选中时显示） */}
      {selected && onSelect && (
        <button
          className="kanban-card__cancel-select"
          onClick={() => onSelect(card.id)}
          aria-label="取消选择"
        >
          ✕
        </button>
      )}
    </div>
  );
});

KanbanCard.displayName = 'KanbanCard';
