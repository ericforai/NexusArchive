// src/pages/pre-archive/PoolPage.tsx
// Input: URL search params, 视图模式状态
// Output: 带视图切换器的容器页面
// Pos: src/pages/pre-archive/PoolPage.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { List, Columns3 } from 'lucide-react';
import { PoolKanbanView } from '@/components/pool-kanban';
import { ArchiveListPage } from '@/pages/archives/ArchiveListPage';
import './PoolPage.css';

const VIEW_MODE_STORAGE_KEY = 'pool.viewMode';
const DEFAULT_VIEW_MODE: ViewMode = 'list';

type ViewMode = 'list' | 'kanban';

interface ViewSwitcherProps {
  currentMode: ViewMode;
  onModeChange: (mode: ViewMode) => void;
}

/**
 * 视图切换器组件
 */
function ViewSwitcher({ currentMode, onModeChange }: ViewSwitcherProps) {
  return (
    <div className="pool-page__switcher">
      <button
        className={currentMode === 'list' ? 'active' : ''}
        onClick={() => onModeChange('list')}
        title="列表视图 - 适合查看大量数据和批量操作"
      >
        <List size={16} />
        列表
      </button>
      <button
        className={currentMode === 'kanban' ? 'active' : ''}
        onClick={() => onModeChange('kanban')}
        title="看板视图 - 直观展示处理流程"
      >
        <Columns3 size={16} />
        看板
      </button>
    </div>
  );
}

/**
 * 电子凭证池页面容器
 *
 * 职责：
 * 1. 维护视图模式状态 (list/kanban)
 * 2. 同步 URL query 参数 (?view=list|kanban)
 * 3. 记忆用户的视图偏好
 * 4. 渲染视图切换器和对应的子视图
 */
export const PoolPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // 从 localStorage 读取用户偏好
  const getInitialViewMode = useCallback((): ViewMode => {
    // 优先级: URL 参数 > localStorage > 默认值
    const viewParam = searchParams.get('view') as ViewMode | null;
    if (viewParam === 'list' || viewParam === 'kanban') {
      return viewParam;
    }
    const stored = localStorage.getItem(VIEW_MODE_STORAGE_KEY);
    if (stored === 'list' || stored === 'kanban') {
      return stored;
    }
    return DEFAULT_VIEW_MODE;
  }, [searchParams]);

  const [viewMode, setViewMode] = useState<ViewMode>(getInitialViewMode);

  // 同步 URL 参数变化到状态
  useEffect(() => {
    const viewParam = searchParams.get('view') as ViewMode | null;
    if (viewParam && (viewParam === 'list' || viewParam === 'kanban') && viewParam !== viewMode) {
      setViewMode(viewParam);
    }
  }, [searchParams, viewMode]);

  // 处理视图切换
  const handleViewChange = useCallback((mode: ViewMode) => {
    setViewMode(mode);
    setSearchParams({ view: mode });
    localStorage.setItem(VIEW_MODE_STORAGE_KEY, mode);
  }, [setSearchParams]);

  // 兼容旧的 /kanban 路由 - 重定向到新格式
  useEffect(() => {
    if (window.location.pathname.endsWith('/kanban')) {
      navigate(`/system/pre-archive/pool?view=kanban`, { replace: true });
    }
  }, [navigate]);

  return (
    <div className="pool-page">
      <div className="pool-page__header">
        <div className="pool-page__title-section">
          <h1 className="pool-page__title">电子凭证池</h1>
        </div>
        <ViewSwitcher currentMode={viewMode} onModeChange={handleViewChange} />
      </div>

      <div className="pool-page__content">
        {viewMode === 'kanban' ? (
          <PoolKanbanView />
        ) : (
          <ArchiveListPage routeConfig="pool" />
        )}
      </div>
    </div>
  );
};

export default PoolPage;
