// src/components/pool-kanban/KanbanColumn.tsx
// Input: ColumnGroupConfig, PoolItem[], selection state, and action callbacks (Segmented items)
// Output: Rendered kanban column with Segmented sub-state selector and card list (with column actions passed to cards)
// Pos: src/components/pool-kanban/KanbanColumn.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { useState, useCallback, useMemo } from 'react';
import { Badge, Segmented } from 'antd';
import { KanbanCard } from './KanbanCard';
import type { ColumnGroupConfig } from '@/config/pool-columns.config';
import type { PoolItem } from '@/api/pool';
import './KanbanColumn.css';

export interface KanbanColumnProps {
  column: ColumnGroupConfig;
  cards: PoolItem[];
  selectedIds: Set<string>;
  onSelectionChange: (cardId: string) => void;
  onAction: (action: string, cards: PoolItem[]) => void;
}

export function KanbanColumn({
  column,
  cards,
  selectedIds,
  onSelectionChange,
  onAction,
}: KanbanColumnProps) {
  // Current selected sub-state
  const [activeTab, setActiveTab] = useState(column.subStates[0].value);

  // Calculate card count for each sub-state
  const subStateCounts = useMemo(() => {
    const counts = new Map<string, number>();
    column.subStates.forEach(sub => {
      counts.set(
        sub.value,
        cards.filter(card => card.status === sub.value).length
      );
    });
    return counts;
  }, [cards, column.subStates]);

  // Get cards for current sub-state
  const currentCards = useMemo(() => {
    return cards.filter(card => card.status === activeTab);
  }, [cards, activeTab]);

  // Handle card selection
  const handleCardSelect = useCallback((cardId: string) => {
    onSelectionChange(cardId);
  }, [onSelectionChange]);

  // Handle card action
  const handleCardAction = useCallback((cardId: string, action: string) => {
    // 单卡片操作：直接触发
    // 找到该卡片并执行操作
    const card = currentCards.find(c => c.id === cardId);
    console.log('KanbanColumn handleCardAction:', { cardId, action, card, currentCards });
    if (card) {
      onAction(action, [card]);
    } else {
      console.warn('Card not found in currentCards:', cardId, 'currentCards:', currentCards.map(c => c.id));
    }
  }, [currentCards, onAction]);

  // Segmented options with badge counts
  const segmentedOptions = useMemo(() => {
    return column.subStates.map(sub => ({
      label: (
        <span className="kanban-column__sub-state-option">
          {sub.label}
          <Badge count={subStateCounts.get(sub.value) || 0} size="small" showZero />
        </span>
      ),
      value: sub.value,
    }));
  }, [column.subStates, subStateCounts]);

  return (
    <div className="kanban-column">
      {/* Column header with embedded Segmented */}
      <div className="kanban-column__header">
        <div className="kanban-column__title-row">
          <h3 className="kanban-column__title">{column.title}</h3>

          {/* Embedded sub-state selector using Segmented */}
          <div className="kanban-column__sub-states">
            <Segmented
              options={segmentedOptions}
              value={activeTab}
              onChange={setActiveTab}
              size="small"
            />
          </div>

          <Badge count={cards.length} showZero className="kanban-column__total-badge" />
        </div>
      </div>

      {/* Card list */}
      <div className="kanban-column__cards">
        {currentCards.length === 0 ? (
          <div className="kanban-column__empty">
            <p>暂无文件</p>
          </div>
        ) : (
          currentCards.map(card => (
            <KanbanCard
              key={card.id}
              card={card}
              selected={selectedIds.has(card.id)}
              onSelect={handleCardSelect}
              onAction={handleCardAction}
              columnActions={column.actions}
            />
          ))
        )}
      </div>
    </div>
  );
}
