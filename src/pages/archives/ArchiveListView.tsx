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
import React, { useState } from 'react';
import {
  Search, Filter, Download, Layers, FileText,
  Settings2, Zap, Receipt, Upload, CheckCircle2,
  ShieldCheck, Trash2, AlertTriangle, Loader2, Eye, X
} from 'lucide-react';
import { createPortal } from 'react-dom';

import { ArchiveListController, ArchiveRouteMode, useArchiveActions, useSmartMatching } from '../../features/archives';
import ArchiveDetailDrawer from './ArchiveDetailDrawer';
import MatchPreviewModal from './MatchPreviewModal';

// 诊断日志：验证模块导入
console.log('%c[ArchiveListView] ArchiveDetailDrawer imported:', typeof ArchiveDetailDrawer, ArchiveDetailDrawer.name, 'color: #8b5cf6; font-weight: bold;');

// Shared Components (Assuming these exist or are local)
// In a real refactor, these should be imported. For this file update, I will keep necessary imports.
import { GenericRow, ViewState } from '../../types';

// Local Constants
const PRE_ARCHIVE_STATUS_LABELS: any = {
  'PENDING_CHECK': { label: '待检测', color: 'bg-slate-100 text-slate-600', description: '等待执行四性检测' },
  'CHECK_FAILED': { label: '检测失败', color: 'bg-rose-100 text-rose-600', description: '四性检测未通过，需修正' },
  'PENDING_METADATA': { label: '待补全', color: 'bg-amber-100 text-amber-600', description: '缺少关键元数据' },
  'PENDING_ARCHIVE': { label: '待归档', color: 'bg-blue-100 text-blue-600', description: '检测通过，等待正式归档' },
  'MATCHED': { label: '已匹配', color: 'bg-emerald-100 text-emerald-600', description: '已关联记账凭证' },
  'ARCHIVED': { label: '已归档', color: 'bg-green-100 text-green-600', description: '已完成长期归档' },
};

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

  // Local UI state (purely presentational)
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [isRuleModalOpen, setIsRuleModalOpen] = useState(false);
  const [activeLinkTab, setActiveLinkTab] = useState<'list' | 'report'>('list');
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

  // Link Modal State
  // Helper for rendering cells
  const renderCell = (row: GenericRow, column: any) => {
    const value = row[column.key];

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
        // Pool statuses
        'PENDING_CHECK': 'bg-slate-100 text-slate-600 border-slate-200',
        'CHECK_FAILED': 'bg-rose-50 text-rose-700 border-rose-200',
        'PENDING_METADATA': 'bg-amber-50 text-amber-700 border-amber-200',
        'PENDING_ARCHIVE': 'bg-blue-50 text-blue-700 border-blue-200',
        'MATCHED': 'bg-emerald-50 text-emerald-700 border-emerald-200',
      };

      const rawStatus = (row as any).rawStatus || row.status;
      const colorClass = statusColors[rawStatus] || statusColors[String(value)] || 'bg-slate-100 text-slate-600 border-slate-200';

      return (
        <span className={`px-2.5 py-1 rounded-full text-xs font-medium border whitespace-nowrap ${colorClass}`}>
          {PRE_ARCHIVE_STATUS_LABELS[rawStatus]?.label || value}
        </span>
      );
    }

    // Money Type
    if (column.type === 'money') {
      if (!value || value === '-') return <span className="text-slate-400 font-mono text-right block">-</span>;
      return <span className={`font-mono font-semibold text-right block text-slate-700`}>{value}</span>;
    }

    // Voucher Number (Interactive)
    if (['erpVoucherNo', 'voucherNo'].includes(column.key)) {
      return (
        <div className="flex items-center gap-3 group relative">
          <button
            onClick={(e) => { e.stopPropagation(); openViewModal(row); }}
            className="font-medium text-slate-700 hover:text-primary-600 transition-colors border-b border-transparent hover:border-primary-600 hover:border-dashed text-left"
          >
            {value}
          </button>

          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity absolute left-full ml-2 bg-white/90 backdrop-blur shadow-sm border border-slate-200 rounded px-1.5 py-0.5 z-10 whitespace-nowrap">
            <button onClick={(e) => { e.stopPropagation(); openViewModal(row); }} className="p-1 text-primary-600"><Eye size={14} /></button>
            {mode.isPoolView && !mode.isLinkingView && (
              <button onClick={(e) => { e.stopPropagation(); archiveActions.handleDelete(row.id); }} className="p-1 text-rose-600"><Trash2 size={14} /></button>
            )}
          </div>
        </div>
      );
    }

    return <span className="text-slate-700 font-medium">{value}</span>;
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= Math.ceil(page.pageInfo.total / page.pageInfo.pageSize)) {
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
        {mode.isPoolView ? '电子凭证池中暂时没有待处理的凭证' : '当前列表为空，请调整筛选条件或新增记录'}
      </p>
      {mode.isPoolView && (
        <label className="cursor-pointer px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 transition-colors shadow-lg shadow-primary-500/20 active:scale-95">
          上传凭证
          <input
            type="file"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) archiveActions.handleUpload(file);
              e.target.value = '';
            }}
          />
        </label>
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
        <div className="flex justify-between items-end">
          {/* Title */}
          <div className="space-y-4">
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 flex items-center gap-3">
                {mode.title}
                <span className="text-slate-300 font-light text-2xl">/</span>
                <span className="text-slate-500 font-semibold text-2xl">{mode.subTitle}</span>
              </h1>
            </div>
            {/* Tabs for Linking View */}
            {mode.subTitle === '凭证关联' && (
              <div className="bg-slate-100/80 p-1 rounded-xl inline-flex backdrop-blur-sm border border-slate-200/50">
                <button className={`px-5 py-2 text-sm font-semibold rounded-lg ${activeLinkTab === 'list' ? 'bg-white text-primary-600 shadow' : 'text-slate-500'}`} onClick={() => setActiveLinkTab('list')}>
                  <Layers size={16} className="inline mr-2" /> 凭证列表
                </button>
                <button className={`px-5 py-2 text-sm font-semibold rounded-lg ${activeLinkTab === 'report' ? 'bg-white text-primary-600 shadow' : 'text-slate-500'}`} onClick={() => setActiveLinkTab('report')}>
                  <FileText size={16} className="inline mr-2" /> 合规报告
                </button>
              </div>
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

            <button onClick={() => setIsFilterOpen(!isFilterOpen)} className="p-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50">
              <Filter size={18} />
            </button>

            <button onClick={controller.actions.exportCsv} className="p-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50">
              <Download size={18} />
            </button>

            {mode.isPoolView && (
              <label className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 cursor-pointer flex items-center gap-2">
                <Upload size={16} /> 上传
                <input type="file" className="hidden" onChange={(e) => { const f = e.target.files?.[0]; if (f) archiveActions.handleUpload(f); e.target.value = ''; }} />
              </label>
            )}
          </div>
        </div>
      </div>

      {/* Statistics for Pool View */}
      {mode.isPoolView && (
        <div className="px-4 py-2 bg-white border-b border-slate-100 overflow-x-auto">
          <div className="flex items-center gap-2">
            <button
              onClick={() => pool.setStatusFilter(null)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium ${pool.statusFilter === null ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-600'}`}
            >
              全部
            </button>
            {Object.entries(PRE_ARCHIVE_STATUS_LABELS).map(([key, config]: any) => (
              <button
                key={key}
                onClick={() => pool.setStatusFilter(key)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1 ${pool.statusFilter === key ? 'ring-2 ring-primary-500 bg-white' : 'bg-white hover:bg-slate-50 border'}`}
              >
                <span className={`w-2 h-2 rounded-full ${config.color.replace('text-', 'bg-').split(' ')[0]}`} />
                {config.label}
                <span className="bg-slate-100 px-1 rounded text-[10px]">{pool.statusStats[key] || 0}</span>
              </button>
            ))}

            <div className="ml-auto w-px h-6 bg-slate-200 mx-2" />
            <button onClick={() => archiveActions.handlePoolCheck('all')} className="flex items-center gap-1 text-xs px-3 py-1.5 bg-slate-100 hover:bg-slate-200 rounded">
              <ShieldCheck size={14} /> 全部检测
            </button>
          </div>
        </div>
      )}

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
        {activeLinkTab === 'list' ? (
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
                    {/* Actions column for pool view */}
                    {mode.isPoolView && (
                      <th className="p-4 text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-100 w-20">
                        操作
                      </th>
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.isLoading ? (
                    <tr>
                      <td colSpan={10} className="p-20 text-center">
                        <Loader2 size={32} className="animate-spin text-primary-500 mx-auto mb-4" />
                        <p className="text-slate-500">加载数据中...</p>
                      </td>
                    </tr>
                  ) : data.rows.length === 0 ? (
                    <tr><td colSpan={10}>{renderEmptyState()}</td></tr>
                  ) : (
                    data.rows.map((row) => (
                      <tr key={row.id} className="group hover:bg-blue-50/30 transition-colors duration-150">
                        <td className="p-4 text-center">
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
                        {/* Actions column for pool view */}
                        {mode.isPoolView && (
                          <td className="p-4 text-sm">
                            <div className="flex items-center gap-2">
                              <button
                                onClick={(e) => { e.stopPropagation(); openViewModal(row); }}
                                className="p-1.5 text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                                title="预览"
                              >
                                <Eye size={16} />
                              </button>
                              <button
                                onClick={(e) => { e.stopPropagation(); archiveActions.handleDelete(row.id); }}
                                className="p-1.5 text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                                title="删除"
                              >
                                <Trash2 size={16} />
                              </button>
                            </div>
                          </td>
                        )}
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
                  共 {page.pageInfo.total} 条，当前第 {page.pageInfo.page} / {Math.ceil(page.pageInfo.total / page.pageInfo.pageSize)} 页
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => handlePageChange(page.pageInfo.page - 1)}
                    disabled={page.pageInfo.page <= 1}
                    className="px-3 py-1 border rounded bg-white hover:bg-slate-50 disabled:opacity-50"
                  >
                    上一页
                  </button>
                  <button
                    onClick={() => handlePageChange(page.pageInfo.page + 1)}
                    disabled={page.pageInfo.page >= Math.ceil(page.pageInfo.total / page.pageInfo.pageSize)}
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

      {/* Archive Detail Drawer */}
      {/* DIAGNOSTIC: Log drawer render attempt */}
      {(() => {
        console.log('[ArchiveListView] About to render ArchiveDetailDrawer with:', {
          isViewModalOpen,
          viewRowId: viewRow?.id,
          viewRowCode: viewRow?.code
        });
        return null;
      })()}
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

      {/* 匹配规则配置弹窗 */}
      {isRuleModalOpen && (
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
                  ui.showToast('规则已保存，稍后请点击“智能匹配”执行', 'success');
                }}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
              >
                保存配置
              </button>
            </div>
          </div>
        </div>
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
