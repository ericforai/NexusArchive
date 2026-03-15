// Input: Controller & Actions
// Output: 纯展示组件 ArchiveListView
// Pos: src/pages/archives/ArchiveListView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Archive List View (Refactored)
 *
 * 纯展示组件，职责：
 * 1. 接收 Controller (数据/状态) 和 Actions (交互回调)
 * 2. 渲染 Toolbar, FilterBar, Table, Pagination
 * 3. 渲染 ArchiveDetailDrawer
 *
 * 移除了所有 API 调用和复杂业务逻辑。
 */
import React, { useState, useEffect } from 'react';
import {
  Search, Filter, Download, FileText,
  Settings2, Zap, Receipt, Upload, CheckCircle2,
  AlertTriangle, Loader2, X
} from 'lucide-react';
import { createPortal } from 'react-dom';
import { useLocation, useNavigate } from 'react-router-dom';

import { ArchiveListController, ArchiveRouteMode, useArchiveActions, useSmartMatching } from '../../features/archives';
import { STATUS_CONFIG, resolveStatus } from '@/config/pool-columns.config';
import ArchiveDetailDrawer from './ArchiveDetailDrawer';
import FinancialReportDetailDrawer from './FinancialReportDetailDrawer';
import MatchPreviewModal from './MatchPreviewModal';
import { TablePreviewAction } from '../../components/table';
import { formatVoucherNumber } from '../../utils/voucherNumber';

// 已移除诊断日志：组件导入验证完成

// Shared Components (Assuming these exist or are local)
// In a real refactor, these should be imported. For this file update, I will keep necessary imports.
import { GenericRow, ViewState } from '../../types';

// ============ 匹配规则配置 ============

interface MatchRule {
  id: string;
  label: string;
  description: string;
  enabled: boolean;
  weight: number;
}

const DEFAULT_RULES: MatchRule[] = [
  { id: 'amount', label: '金额精确匹配', description: '凭证金额与单据金额必须完全一致', enabled: true, weight: 40 },
  { id: 'date', label: '日期邻近匹配', description: '业务日期差异在 ±3 天以内', enabled: true, weight: 20 },
  { id: 'vendor', label: '客商名称匹配', description: '支持名称模糊匹配与缩写匹配', enabled: true, weight: 20 },
  { id: 'code', label: '票据号关联', description: '摘要中包含发票号或合同号', enabled: true, weight: 20 },
];

// ============ Props Definition ============

export interface ArchiveListViewProps {
  routeConfig: ArchiveRouteMode; // Kept for legacy/context compatibility
  controller: ArchiveListController;
  actions: ReturnType<typeof useArchiveActions>;
  onNavigate?: (view: ViewState, subView?: string, param?: string) => void;
}

// ============ Component ============

const ArchiveListView: React.FC<ArchiveListViewProps> = ({ controller, actions: archiveActions }) => {
  const { mode, query, page, data, selection, pool, ui } = controller;
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    // 仅在开发环境打印调试信息
    if (import.meta.env.DEV) {
      console.log('[ArchiveListView] state', {
        isLoading: data.isLoading,
        rows: data.rows.length,
        'page.currentPage': page.currentPage,
        'data.pageInfo.page': data.pageInfo.page,
        total: data.pageInfo.total,
      });
    }
  }, [data.isLoading, data.rows.length, page.currentPage, data.pageInfo.page, data.pageInfo.total]);

  // Local UI state (purely presentational)
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [isRuleModalOpen, setIsRuleModalOpen] = useState(false);
  const [viewTab, _setViewTab] = useState<'list' | 'report'>('list'); // 报告视图待开发
  // 智能匹配 Hook (修复即重构)
  const {
    isMatchPreviewOpen,
    matchPreviewData,
    isMatching,
    handleAutoMatch,
    handleConfirmLinks,
    closeMatchPreview
  } = useSmartMatching(() => controller.actions.reload());

  // 匹配规则状态 (UI 纯展示状态保留)
  const [matchRules, setMatchRules] = useState<MatchRule[]>(DEFAULT_RULES);
  const [matchThreshold, setMatchThreshold] = useState(80);

  // Detail Modal State (Controlled by ViewRow)
  const [viewRow, setViewRow] = useState<GenericRow | null>(null);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [activePreviewId, setActivePreviewId] = useState<string | null>(null);
  void activePreviewId; // 联动功能待开发

  // 悬停行状态
  const [hoveredRowId, setHoveredRowId] = useState<string | null>(null);

  // 防抖预览
  let previewClickTimer: NodeJS.Timeout | null = null;
  const handlePreviewClick = (row: GenericRow) => {
    if (previewClickTimer) return;
    previewClickTimer = setTimeout(() => {
      openViewModal(row);
      previewClickTimer = null;
    }, 200);
  };

  const openViewModal = (row: GenericRow) => {
    setViewRow(row);
    setActivePreviewId(row.fileId || row.id || null);
    setIsViewModalOpen(true);
  };

  const closeViewModal = () => {
    setIsViewModalOpen(false);
    setViewRow(null);
    setActivePreviewId(null);
  };

  // Route change listener: auto-close drawer on navigation
  useEffect(() => {
    // Close drawer when route changes (different pathname)
    if (isViewModalOpen) {
      setIsViewModalOpen(false);
      setViewRow(null);
      setActivePreviewId(null);
    }
  }, [location.pathname, isViewModalOpen]);

  // Link Modal State
  // Helper for rendering cells
  const renderCell = (row: GenericRow, column: any) => {
    const value = row[column.key];

    const renderSafeText = (raw: unknown) => {
      if (raw === null || raw === undefined || raw === '') return <span className="text-slate-400">-</span>;
      if (typeof raw === 'string' || typeof raw === 'number' || typeof raw === 'boolean') {
        return <span className="text-slate-700 font-medium">{String(raw)}</span>;
      }
      return <span className="text-slate-400">-</span>;
    };

    if (column.key === 'selection') {
      return (
        <input
          type="checkbox"
          checked={selection.selectedIds.includes(row.id)}
          onChange={() => selection.toggle(row.id)}
          className="rounded border-slate-300 text-primary-600 focus:ring-primary-500 w-4 h-4 cursor-pointer"
          onClick={(e) => e.stopPropagation()}
        />
      );
    }

    if (column.type === 'status') {
      const statusColors: Record<string, string> = {
        'draft': 'bg-slate-100 text-slate-600 border-slate-200',
        'pending': 'bg-blue-50 text-blue-700 border-blue-200',
        'archived': 'bg-green-50 text-green-700 border-green-200',
        'rejected': 'bg-rose-50 text-rose-700 border-rose-200',
      };

      const rawStatus = (row as any).rawStatus || row.status;

      // Use centralized resolver to map ANY status (legacy or new) to standard config
      const standardizedStatus = resolveStatus(rawStatus);
      const config = STATUS_CONFIG[standardizedStatus];

      if (config) {
        // Determine bg/text color based on config.color (simplified mapping for now)
        let colorClass = 'bg-slate-100 text-slate-600 border-slate-200';
        if (config.color === '#10b981') colorClass = 'bg-emerald-50 text-emerald-700 border-emerald-200'; // READY_TO_ARCHIVE
        else if (config.color === '#3b82f6') colorClass = 'bg-blue-50 text-blue-700 border-blue-200'; // READY_TO_MATCH
        else if (config.color === '#f59e0b') colorClass = 'bg-amber-50 text-amber-700 border-amber-200'; // NEEDS_ACTION
        else if (config.color === '#94a3b8') colorClass = 'bg-slate-100 text-slate-600 border-slate-200'; // PENDING_CHECK
        else if (config.color === '#64748b') colorClass = 'bg-slate-100 text-slate-600 border-slate-200'; // COMPLETED

        return (
          <span className={`px-2.5 py-1 rounded-full text-xs font-medium border whitespace-nowrap ${colorClass}`}>
            {config.label}
          </span>
        );
      }

      // Final fallback for non-pool statuses (like draft/rejected from Archive modules)
      const colorClass = statusColors[rawStatus] || statusColors[String(value)] || 'bg-slate-100 text-slate-600 border-slate-200';

      return (
        <span className={`px-2.5 py-1 rounded-full text-xs font-medium border whitespace-nowrap ${colorClass}`}>
          {value || rawStatus}
        </span>
      );
    }

    // Money Type
    if (column.type === 'money') {
      if (value === null || value === undefined || value === '-' || value === '' || value === '0' || value === '0.00') {
        return <span className="text-slate-400 font-mono text-right block">-</span>;
      }
      const parsed = typeof value === 'string'
        ? Number(value.replace(/[^\d.-]/g, ''))
        : Number(value);
      const numValue = Number.isFinite(parsed) ? parsed : NaN;
      if (isNaN(numValue)) return <span className="text-slate-400 font-mono text-right block">-</span>;
      return <span className="font-mono font-semibold text-right block text-slate-700">¥{numValue.toFixed(2)}</span>;
    }

    if (column.type === 'progress') {
      const score = typeof value === 'number'
        ? value
        : Number.isFinite(Number(value)) ? Number(value) : 0;
      const normalized = Math.max(0, Math.min(100, score));
      return <span className="text-slate-700 font-medium">{normalized}%</span>;
    }

    // Date Types
    if (column.type === 'date' || column.type === 'datetime') {
      if (!value) return <span className="text-slate-400">-</span>;
      try {
        const date = new Date(value);
        if (isNaN(date.getTime())) return <span className="text-slate-400">-</span>;
        if (column.type === 'date') {
          return <span className="text-slate-700">{date.toLocaleDateString('zh-CN')}</span>;
        }
        return <span className="text-slate-700">{date.toLocaleString('zh-CN')}</span>;
      } catch {
        return <span className="text-slate-400">-</span>;
      }
    }

    if (['erpVoucherNo', 'voucherNo', 'voucherWord'].includes(column.key)) {
      const voucherNumber = formatVoucherNumber({
        displayValue: row.erpVoucherNo || row.voucherNo,
        voucherWord: row.voucherWord,
        voucherNo: row.voucherNo || row.code || row.id,
        fallback: row.code || row.id,
      });

      return (
        <button
          className="font-medium text-slate-700 transition-colors text-left"
        >
          {voucherNumber}
        </button>
      );
    }

    return renderSafeText(value);
  };

  const handlePageChange = async (newPage: number) => {
    const totalPages = Math.ceil(data.pageInfo.total / data.pageInfo.pageSize);
    if (newPage >= 1 && newPage <= totalPages) {
      page.setCurrentPage(newPage);
    }
  };

  // Render empty state
  const renderEmptyState = () => (
    <div className="flex flex-col items-center justify-center p-12 text-center border-t border-slate-100 min-h-[400px]">
      <div className="w-24 h-24 bg-slate-50 rounded-full flex items-center justify-center mb-4">
        <Receipt size={40} className="text-slate-300" />
      </div>
      <h3 className="text-slate-900 font-medium mb-1">暂无数据</h3>
      <p className="text-slate-500 text-sm max-w-xs mx-auto mb-6">
        {mode.isPoolView ? '记账凭证库中暂时没有待处理的凭证' : '当前列表为空，请调整筛选条件或新增记录'}
      </p>
      {mode.isPoolView && (
        <button
          onClick={() => navigate('/system/collection/upload')}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 transition-colors shadow-lg shadow-primary-500/20 active:scale-95 flex items-center gap-2"
        >
          <Upload size={16} /> 前往资料收件
        </button>
      )}
    </div>
  );

  return (
    <div className="p-8 space-y-6 max-w-[1600px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500 relative">

      {/* Toast Portal */}
      {ui.toast.visible && createPortal(
        <div className="fixed top-6 left-1/2 transform -translate-x-1/2 z-[100] animate-in slide-in-from-top-2 fade-in duration-300">
          <div className={`flex items-center gap-3 px-5 py-3 rounded-xl shadow-2xl border ${ui.toast.type === 'success' ? 'bg-slate-800 text-white border-slate-700' : 'bg-rose-600 text-white border-rose-500'}`}>
            {ui.toast.type === 'success' ? <CheckCircle2 size={20} className="text-emerald-400" /> : <AlertTriangle size={20} className="text-white" />}
            <span className="font-medium text-sm">{ui.toast.message}</span>
          </div>
        </div>,
        document.body
      )}

      {/* Header & Toolbar */}
      <div className="flex flex-col gap-6 mb-8 mt-2 animate-in fade-in slide-in-from-top-4 duration-500">
        <div className="flex justify-between items-center">
          {/* Title with count */}
          <div className="flex items-center gap-4">
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 flex items-center gap-3">
              {mode.title}
              <span className="text-slate-300 font-light text-2xl">/</span>
              <span className="text-slate-500 font-semibold text-2xl">{mode.subTitle}</span>
            </h1>
            {/* 统计数量 - 只在 pool 视图显示 */}
            {mode.isPoolView && (
              <span className="text-sm text-slate-500 bg-slate-100 px-3 py-1.5 rounded-full">
                共 <strong className="text-slate-700">{Object.values(pool.statusStats).reduce((sum, count) => sum + count, 0)}</strong> 条
              </span>
            )}
          </div>

          {/* Right Toolbar */}
          <div className="flex items-center gap-2">
            {/* 凭证关联专属工具栏 */}
            {mode.subTitle === '凭证关联' && (
              <>
                <button
                  onClick={() => setIsRuleModalOpen(true)}
                  className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95"
                  title="匹配规则配置"
                >
                  <Settings2 size={16} className="mr-2" /> 匹配规则
                </button>
                <button
                  onClick={() => handleAutoMatch(selection.selectedIds)}
                  disabled={isMatching}
                  className={`px-4 py-2 text-white rounded-lg text-sm font-medium flex items-center shadow-lg transition-all active:scale-95 ${isMatching ? 'bg-amber-400 cursor-wait' : 'bg-amber-500 hover:bg-amber-600 shadow-amber-500/30'}`}
                  title="批量智能匹配选中凭证与原始单据"
                >
                  {isMatching ? <Loader2 size={16} className="mr-2 animate-spin" /> : <Zap size={16} className="mr-2" />}
                  {isMatching ? '匹配中...' : '智能匹配'}
                </button>
                <div className="h-8 w-px bg-slate-200 mx-1"></div>
              </>
            )}
            <div className="flex items-center bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
              <div className="pl-3 text-slate-400"><Search size={14} /></div>
              <input
                type="text"
                placeholder="搜索..."
                className="w-full px-3 py-2 text-sm outline-none bg-transparent"
                value={query.searchTerm}
                onChange={(e) => query.setSearchTerm(e.target.value)}
              />
            </div>

            <button onClick={() => setIsFilterOpen(!isFilterOpen)} className="p-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50" title="展开/收起高级筛选">
              <Filter size={18} />
            </button>

            <button onClick={controller.actions.exportCsv} className="p-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50" title="导出当前列表为 CSV">
              <Download size={18} />
            </button>

            {mode.isPoolView && (
              <button
                onClick={() => navigate('/system/collection/upload')}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 transition-colors shadow-lg shadow-primary-500/20 active:scale-95 flex items-center gap-2"
              >
                <Upload size={16} /> 资料收件
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Main Table Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden min-h-[500px] flex flex-col">
        {/* Action Bar */}
        {selection.selectedIds.length > 0 && (
          <div className="px-4 py-2 bg-blue-50 border-b border-blue-100 flex items-center justify-between">
            <span className="text-sm text-blue-800 font-medium">已选择 {selection.selectedIds.length} 项</span>
            <div className="flex items-center gap-2">
              {mode.isPoolView ? (
                <button onClick={archiveActions.executeArchiving} className="text-xs bg-blue-600 text-white px-3 py-1.5 rounded hover:bg-blue-700 disabled:opacity-50">
                  {archiveActions.isArchiving ? '提交中...' : '提交归档'}
                </button>
              ) : (
                <button className="text-xs bg-white border border-slate-300 px-3 py-1.5 rounded hover:bg-slate-50">导出所选</button>
              )}
              <button onClick={archiveActions.handleBatchDelete} className="text-xs bg-rose-50 text-rose-600 px-3 py-1.5 rounded hover:bg-rose-100 border border-rose-200">批量删除</button>
            </div>
          </div>
        )}

        {/* List View Tab */}
        {viewTab === 'list' ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead className="bg-slate-50/80 sticky top-0 z-10 backdrop-blur-sm">
                  <tr>
                    <th className="p-4 w-12 text-center border-b border-slate-100">
                      <input
                        type="checkbox"
                        checked={selection.allSelected}
                        onChange={selection.toggleAll}
                        className="rounded border-slate-300 text-primary-600 focus:ring-primary-500 w-4 h-4 cursor-pointer"
                      />
                    </th>
                    {mode.config.columns.map((col: any) => (
                      <th key={col.key} className="p-4 text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-100">
                        {col.header}
                      </th>
                    ))}
                    {/* Actions column - always show */}
                    <th className="p-4 text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-100 w-32 text-right">
                      操作
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.isLoading ? (
                    <tr>
                      <td colSpan={12} className="p-20 text-center">
                        <Loader2 size={32} className="animate-spin text-primary-500 mx-auto mb-4" />
                        <p className="text-slate-500">加载数据中...</p>
                      </td>
                    </tr>
                  ) : data.rows.length === 0 ? (
                    <tr><td colSpan={12}>{renderEmptyState()}</td></tr>
                  ) : (
                    data.rows.map((row) => (
                      <tr
                        key={row.id}
                        className={`
                          cursor-pointer transition-all duration-200
                          ${hoveredRowId === row.id
                            ? 'bg-blue-50 dark:bg-blue-900/20 border-l-4 border-l-blue-500 dark:border-l-blue-400'
                            : 'hover:bg-gray-50 dark:hover:bg-gray-700/50'}
                        `}
                        onMouseEnter={() => setHoveredRowId(row.id)}
                        onMouseLeave={() => setHoveredRowId(null)}
                        onClick={() => handlePreviewClick(row)}
                        role="button"
                        tabIndex={0}
                        aria-label={`预览 ${row.voucherNo || row.code || row.id}`}
                        onKeyPress={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            handlePreviewClick(row);
                          }
                        }}
                      >
                        <td className="p-4 text-center" onClick={(e) => e.stopPropagation()}>
                          <input
                            type="checkbox"
                            checked={selection.selectedIds.includes(row.id)}
                            onChange={() => selection.toggle(row.id)}
                            className="rounded border-slate-300 text-primary-600 w-4 h-4"
                          />
                        </td>
                        {mode.config.columns.map((col: any) => (
                          <td key={col.key} className="p-4 text-sm text-slate-600">
                            {renderCell(row, col)}
                          </td>
                        ))}
                        {/* Actions column - always show */}
                        <td className="p-4 text-sm" onClick={(e) => e.stopPropagation()}>
                          <TablePreviewAction
                            hovered={hoveredRowId === row.id}
                            onPreview={() => handlePreviewClick(row)}
                            showDelete={false}
                          />
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {!data.isLoading && data.rows.length > 0 && (
              <div className="p-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/50">
                <div className="text-sm text-slate-500">
                  共 {data.pageInfo.total} 条，当前第 {data.pageInfo.page} / {Math.ceil(data.pageInfo.total / data.pageInfo.pageSize)} 页
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => handlePageChange(data.pageInfo.page - 1)}
                    disabled={data.pageInfo.page <= 1}
                    className="px-3 py-1 border rounded bg-white hover:bg-slate-50 disabled:opacity-50"
                  >
                    上一页
                  </button>
                  <button
                    onClick={() => handlePageChange(data.pageInfo.page + 1)}
                    disabled={data.pageInfo.page >= Math.ceil(data.pageInfo.total / data.pageInfo.pageSize)}
                    className="px-3 py-1 border rounded bg-white hover:bg-slate-50 disabled:opacity-50"
                  >
                    下一页
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="p-10 text-center text-slate-400">
            <FileText size={48} className="mx-auto mb-4 opacity-20" />
            <p>合规报告视图开发中...</p>
          </div>
        )}
      </div>

      {/* Detail Drawer - 根据类型选择不同的抽屉组件 */}
      {/* 财务报告、会计账簿、其他会计资料使用专门的文件预览抽屉组件 */}
      {(mode.subTitle === '财务报告' || mode.subTitle === '财务报告库' ||
        mode.subTitle === '会计账簿' || mode.subTitle === '会计账簿库' ||
        mode.subTitle === '其他会计资料' || mode.subTitle === '其他会计资料库') ? (
        <FinancialReportDetailDrawer
          key={`file-${viewRow?.id || 'detail'}`}
          open={isViewModalOpen}
          onClose={closeViewModal}
          row={viewRow}
          moduleTitle={mode.subTitle.replace('库', '')} // 移除"库"后缀作为标题
          isPool={mode.isPoolView}
        />
      ) : (
        <ArchiveDetailDrawer
          key={viewRow?.id || 'archive-detail'}
          open={isViewModalOpen}
          onClose={closeViewModal}
          row={viewRow}
          config={mode.config}
          isPoolView={mode.isPoolView}
          onAipExport={archiveActions.handleAipExport}
          isExporting={archiveActions.isExporting}
        />
      )}

      {/* 匹配规则配置弹窗 */}
      {isRuleModalOpen && createPortal(
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
              <h3 className="text-lg font-bold text-slate-800">关联规则配置</h3>
              <button onClick={() => setIsRuleModalOpen(false)}><X size={20} className="text-slate-400 hover:text-slate-600" /></button>
            </div>
            <div className="p-6 space-y-4">
              <div className="mb-4">
                <label className="text-sm font-bold text-slate-700">自动关联阈值: <span className="text-primary-600">{matchThreshold}%</span></label>
                <input
                  type="range"
                  min="60"
                  max="100"
                  value={matchThreshold}
                  onChange={(e) => setMatchThreshold(Number(e.target.value))}
                  className="w-full h-2 bg-slate-200 rounded-lg accent-primary-500 mt-2"
                />
                <div className="flex justify-between text-xs text-slate-400 mt-1">
                  <span>60% (宽松)</span>
                  <span>100% (严格)</span>
                </div>
              </div>
              <div className="space-y-3">
                <p className="text-xs text-slate-500 font-medium">匹配规则</p>
                {matchRules.map(rule => (
                  <div key={rule.id} className="flex justify-between items-center p-3 bg-slate-50 rounded-lg">
                    <div>
                      <span className="text-sm font-medium text-slate-700">{rule.label}</span>
                      <p className="text-xs text-slate-400">{rule.description}</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={rule.enabled}
                      onChange={() => setMatchRules(prev => prev.map(r => r.id === rule.id ? { ...r, enabled: !r.enabled } : r))}
                      className="h-5 w-5 text-primary-600 rounded border-slate-300 focus:ring-primary-500"
                    />
                  </div>
                ))}
              </div>
            </div>
            <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
              <button onClick={() => setIsRuleModalOpen(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg">取消</button>
              <button
                onClick={() => {
                  setIsRuleModalOpen(false);
                  ui.showToast('规则已保存，稍后请点击"智能匹配"执行', 'success');
                }}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
              >
                保存配置
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* 筛选弹窗 */}
      {isFilterOpen && createPortal(
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
              <h3 className="text-lg font-bold text-slate-800">筛选条件</h3>
              <button onClick={() => setIsFilterOpen(false)}><X size={20} className="text-slate-400 hover:text-slate-600" /></button>
            </div>
            <div className="p-6 space-y-4">
              {/* Status Filter */}
              <div>
                <label className="text-sm font-bold text-slate-700 mb-2 block">状态</label>
                <select
                  value={query.statusFilter}
                  onChange={(e) => query.setStatusFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="">全部</option>
                  <option value="draft">草稿</option>
                  <option value="pending">准备归档</option>
                  <option value="archived">已归档</option>
                </select>
              </div>

              {/* Organization Filter */}
              <div>
                <label className="text-sm font-bold text-slate-700 mb-2 block">组织机构</label>
                <select
                  value={query.orgFilter}
                  onChange={(e) => query.setOrgFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="">全部</option>
                  {query.orgOptions.map((org) => (
                    <option key={org.value} value={org.value}>
                      {org.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Sub-Type Filter */}
              {query.subTypeFilter !== undefined && (
                <div>
                  <label className="text-sm font-bold text-slate-700 mb-2 block">子类型</label>
                  <input
                    type="text"
                    value={query.subTypeFilter}
                    onChange={(e) => query.setSubTypeFilter(e.target.value)}
                    placeholder="输入子类型..."
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              )}
            </div>
            <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
              <button
                onClick={() => {
                  query.setStatusFilter('');
                  query.setOrgFilter('');
                  query.setSubTypeFilter('');
                }}
                className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg"
              >
                重置
              </button>
              <button
                onClick={() => setIsFilterOpen(false)}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
              >
                确定
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* 匹配结果预览弹窗 (已重构) */}
      <MatchPreviewModal
        isOpen={isMatchPreviewOpen}
        onClose={closeMatchPreview}
        data={matchPreviewData as any}
        onConfirm={handleConfirmLinks}
      />
    </div>
  );
};

export { ArchiveListView };
export default ArchiveListView;
