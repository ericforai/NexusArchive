// Input: React、PoolKanbanView component
// Output: Page component for the pool kanban view
// Pos: src/pages/pre-archive/PoolKanbanPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { PoolKanbanView } from '@/components/pool-kanban';

/**
 * 预归档池看板页面
 *
 * 展示记账凭证库的看板视图，支持多列管理和批量操作
 */
export const PoolKanbanPage: React.FC = () => {
  return (
    <div className="pool-kanban-page">
      <PoolKanbanView />
    </div>
  );
};

export default PoolKanbanPage;
