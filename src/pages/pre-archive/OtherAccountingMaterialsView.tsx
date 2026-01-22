// src/pages/pre-archive/PoolPage.tsx
// Input: URL search params, 视图模式状态, 仪表板筛选状态
// Output: 带视图切换器和仪表板的容器页面
// Pos: src/pages/pre-archive/PoolPage.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { List, Columns3 } from 'lucide-react';
import { PoolKanbanView } from '@/components/pool-kanban';
import { PoolDashboard } from '@/components/pool-dashboard';
import { ArchiveListPage } from '@/pages/archives/ArchiveListPage';
import { SimplifiedPreArchiveStatus, DEFAULT_DASHBOARD_FILTER } from '@/config/pool-columns.config';
import { PRE_ARCHIVE_OTHER_CONFIG } from '@/config/pre-archive-columns.config';
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
        type="button"
        className={currentMode === 'list' ? 'active' : ''}
        onClick={() => onModeChange('list')}
        title="列表视图 - 适合查看大量数据和批量操作"
      >
        <List size={16} />
        列表
      </button>
      <button
        type="button"
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
 * 其他会计资料库页面容器
 *
 * 职责：
 * 1. 展示非凭证、非账簿、非报表的其他零散会计资料
 * 2. 预设门类过滤器为 'OTHER'，实现与资料收集端的对齐
 */
export const OtherAccountingMaterialsView: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // 视图模式状态 (list/kanban)
  const getInitialViewMode = useCallback((): ViewMode => {
    const viewParam = searchParams.get('view') as ViewMode | null;
    if (viewParam === 'list' || viewParam === 'kanban') return viewParam;
    return localStorage.getItem(VIEW_MODE_STORAGE_KEY) as ViewMode || DEFAULT_VIEW_MODE;
  }, [searchParams]);

  const [viewMode, setViewMode] = useState<ViewMode>(getInitialViewMode);

  // 状态维护
  // 状态维护
  const [dashboardFilter, setDashboardFilter] = useState<SimplifiedPreArchiveStatus | null>(null);

  // 【核心】预设门类为 AC04 (其他资料)，且在仪表板中不可更改
  const [categoryFilter] = useState<string | null>('AC04');

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

  // 处理批量归档 (保留原PoolPage的逻辑，如果需要)
  const handleBatchArchive = useCallback(() => {
    // TODO: 实现批量归档逻辑
    console.log('Batch archive for READY_TO_ARCHIVE items');
  }, []);

  // 兼容旧的 /kanban 路由 - 重定向到新格式 (保留原PoolPage的逻辑，如果需要)
  useEffect(() => {
    if (window.location.pathname.endsWith('/kanban')) {
      navigate(`/system/pre-archive/pool?view=kanban`, { replace: true });
    }
  }, [navigate]);

  return (
    <div className="pool-page">
      <div className="pool-page__header">
        <div className="pool-page__title-section">
          <h1 className="pool-page__title">其他会计资料</h1>
        </div>
        <ViewSwitcher currentMode={viewMode} onModeChange={handleViewChange} />
      </div>

      {/* 仪表板：仅显示待处理统计，门类固定 */}
      <PoolDashboard
        activeFilter={dashboardFilter}
        onFilterChange={setDashboardFilter}
        categoryFilter={categoryFilter}
        onCategoryChange={() => { }} // 锁定门类
        showCategoryPicker={false}   // 隐藏门类选择器，因为在当前路径下它就是其他资料
        showActions={true}
        onBatchArchive={handleBatchArchive} // 保持批量归档功能
      />

      <div className="pool-page__content">
        {viewMode === 'kanban' ? (
          <PoolKanbanView
            filter={dashboardFilter}
            categoryFilter={categoryFilter}
          />
        ) : (
          <ArchiveListPage
            routeConfig="pool"
            statusFilter={dashboardFilter}
            categoryFilter={categoryFilter}
            subTitle="其他会计资料库"
            config={PRE_ARCHIVE_OTHER_CONFIG}
          />
        )}
      </div>
    </div>
  );
};

export default OtherAccountingMaterialsView;
