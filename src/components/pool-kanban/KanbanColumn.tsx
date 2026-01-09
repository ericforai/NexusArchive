// src/components/pool-kanban/KanbanColumn.tsx
// Input: ColumnGroupConfig, PoolItem[], selection state, and action callbacks
// Output: Rendered kanban column with sub-state tabs, action buttons, and card list
// Pos: src/components/pool-kanban/KanbanColumn.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { useState, useCallback, useMemo } from 'react';
import { Tabs, Badge, Button, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { MoreHorizontal } from 'lucide-react';
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
  // Current selected sub-state tab
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

  // Get cards for current tab
  const currentCards = useMemo(() => {
    return cards.filter(card => card.status === activeTab);
  }, [cards, activeTab]);

  // Handle card selection
  const handleCardSelect = useCallback((cardId: string) => {
    onSelectionChange(cardId);
  }, [onSelectionChange]);

  // Handle card action
  const handleCardAction = useCallback((cardId: string, action: string) => {
    // Card actions to be implemented in subsequent tasks
    console.log('Card action:', action, 'for card:', cardId);
  }, []);

  // Handle column action click
  const handleColumnAction = useCallback((actionKey: string) => {
    // Auto-select all cards in current column
    currentCards.forEach(card => {
      onSelectionChange(card.id);
    });
    // Trigger action
    onAction(actionKey, currentCards);
  }, [currentCards, onSelectionChange, onAction]);

  // More actions menu
  const moreActionsMenu: MenuProps['items'] = column.actions.slice(3).map(action => ({
    key: action.key,
    label: action.label,
    danger: action.danger,
    onClick: () => handleColumnAction(action.key),
  }));

  // Fixed display action buttons (max 3)
  const visibleActions = column.actions.slice(0, 3);

  return (
    <div className="kanban-column">
      {/* Column header */}
      <div className="kanban-column__header">
        <div className="kanban-column__title-row">
          <h3 className="kanban-column__title">{column.title}</h3>
          <Badge count={cards.length} showZero />
        </div>

        {/* Sub-state tabs */}
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          size="small"
          className="kanban-column__tabs"
        >
          {column.subStates.map(sub => (
            <Tabs.TabPane
              key={sub.value}
              tab={
                <span className="kanban-column__tab">
                  {sub.label}
                  <Badge count={subStateCounts.get(sub.value) || 0} />
                </span>
              }
            />
          ))}
        </Tabs>
      </div>

      {/* Column action buttons */}
      <div className="kanban-column__actions">
        {visibleActions.map(action => (
          <Button
            key={action.key}
            size="small"
            danger={action.danger}
            onClick={() => handleColumnAction(action.key)}
          >
            {action.label}
          </Button>
        ))}

        {column.actions.length > 3 && (
          <Dropdown menu={{ items: moreActionsMenu }} trigger={['click']}>
            <Button size="small" icon={<MoreHorizontal size={14} />}>
              更多
            </Button>
          </Dropdown>
        )}
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
            />
          ))
        )}
      </div>
    </div>
  );
}
