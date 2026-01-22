// src/pages/pre-archive/PoolPage.tsx
// Input: URL search params, 视图模式状态, 仪表板筛选状态
// Output: 带视图切换器和仪表板的容器页面
// Pos: src/pages/pre-archive/PoolPage.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { List, Columns3 } from 'lucide-react';
import { message, Modal } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { poolApi } from '@/api/pool';
import { PoolKanbanView } from '@/components/pool-kanban';
import { PoolDashboard } from '@/components/pool-dashboard';
import { ArchiveListPage } from '@/pages/archives/ArchiveListPage';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';
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
 * 记账凭证库页面容器
 *
 * 职责：
 * 1. 维护视图模式状态 (list/kanban)
 * 2. 维护仪表板筛选状态
 * 3. 同步 URL query 参数 (?view=list|kanban)
 * 4. 记忆用户的视图偏好
 * 5. 渲染仪表板、视图切换器和对应的子视图
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

  // 仪表板筛选状态：默认显示"待检测"状态
  const [dashboardFilter, setDashboardFilter] = useState<SimplifiedPreArchiveStatus | null>(
    SimplifiedPreArchiveStatus.PENDING_CHECK
  );

  // 档案门类筛选状态 (VOUCHER/LEDGER/REPORT/OTHER)
  const [categoryFilter, setCategoryFilter] = useState<string | null>('VOUCHER');

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

  const queryClient = useQueryClient();

  // 处理批量归档
  const handleBatchArchive = useCallback(async (status: SimplifiedPreArchiveStatus) => {
    const isArchive = status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE;
    const statusLabel = isArchive ? '批量归档' : '批量检测';

    Modal.confirm({
      title: `确认${statusLabel}？`,
      content: `系统将对所有处于“${isArchive ? '可归档' : '待检测'}”状态的记账凭证执行操作。`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        const hide = message.loading(`正在执行${statusLabel}...`, 0);
        try {
          // 1. 获取目标 ID (使用当前门类筛选，默认 VOUCHER)
          const items = await poolApi.getListByStatus(status, categoryFilter || undefined);
          if (items.length === 0) {
            message.info('当前状态下没有可操作的记录');
            return;
          }
          const ids = items.map((item: any) => item.id);

          if (isArchive) {
            await poolApi.archiveItems(ids);
            message.success(`已成功提交 ${ids.length} 份记录的归档申请`);
          } else {
            await poolApi.checkBatch(ids);
            message.success(`已开始对 ${ids.length} 份记录进行重新检测`);
          }

          // 2. 刷新数据
          queryClient.invalidateQueries({ queryKey: ['pool'] });
        } catch (error: any) {
          console.error(`${statusLabel}失败:`, error);
          message.error(`${statusLabel}执行失败: ${error.message || '系统错误'}`);
        } finally {
          hide();
        }
      }
    });
  }, [queryClient, categoryFilter]);

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
          <h1 className="pool-page__title">记账凭证库</h1>
        </div>
        <ViewSwitcher currentMode={viewMode} onModeChange={handleViewChange} />
      </div>

      {/* 仪表板区域 */}
      <PoolDashboard
        activeFilter={dashboardFilter}
        onFilterChange={setDashboardFilter}
        categoryFilter={categoryFilter}
        onCategoryChange={setCategoryFilter}
        showCategoryPicker={false}
        showActions={true}
        onBatchArchive={handleBatchArchive}
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
          />
        )}
      </div>
    </div>
  );
};

export default PoolPage;
