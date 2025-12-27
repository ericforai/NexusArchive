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
 * 3. 渲染 ArchiveDetailModal
 * 
 * 移除了所有 API 调用和复杂业务逻辑。
 */
import React, { useState } from 'react';
import {
  Search, Filter, Download, Plus, Layers, FileText,
  Settings2, Zap, Receipt, Upload, CheckCircle2,
  ShieldCheck, Trash2, Link as LinkIcon, Edit,
  AlertTriangle, XCircle, Loader2, Archive, Eye, X, Clock
} from 'lucide-react';
import { createPortal } from 'react-dom';

import { ArchiveListController, ArchiveRouteMode, useArchiveActions } from '../../features/archives';
import ArchiveDetailModal from './ArchiveDetailModal';
import { matchingApi } from '../../api/matching';

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

// Re-defining minor constants if not exported
const ARCHIVE_BOX_CONFIG = {
  data: [] as any[] // Mock for 'Archive Box' feature which seems local-only in original
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

// 匹配结果预览数据类型
interface MatchPreviewItem {
  voucherId: string;
  voucherNo: string;
  amount: string;
  date: string;
  matchScore: number;
  matchedDocs: { docNo: string; docType: string; docId: string; evidenceRole: string; score: number }[];
  status: 'high' | 'low' | 'none' | 'confirmed';
}

// ============ Props Definition ============

export interface ArchiveListViewProps {
  routeConfig: ArchiveRouteMode; // Kept for legacy/context compatibility
  controller: ArchiveListController;
  actions: ReturnType<typeof useArchiveActions>;
  onNavigate?: (view: ViewState, subView?: string, param?: string) => void;
}

// ============ Component ============

const ArchiveListView: React.FC<ArchiveListViewProps> = ({
  routeConfig,
  controller,
  actions: archiveActions,
  onNavigate
}) => {
  const { mode, query, page, data, selection, pool, ui } = controller;

  // Local UI state (purely presentational)
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [isRuleModalOpen, setIsRuleModalOpen] = useState(false);
  const [activeLinkTab, setActiveLinkTab] = useState<'list' | 'report'>('list');
  const [showOnboarding, setShowOnboarding] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isMatching, setIsMatching] = useState(false);

  // 匹配规则状态
  const [matchRules, setMatchRules] = useState<MatchRule[]>(DEFAULT_RULES);
  const [matchThreshold, setMatchThreshold] = useState(80);
  const [isMatchPreviewOpen, setIsMatchPreviewOpen] = useState(false);
  const [matchPreviewData, setMatchPreviewData] = useState<MatchPreviewItem[]>([]);

  // Detail Modal State (Controlled by ViewRow)
  const [viewRow, setViewRow] = useState<GenericRow | null>(null);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [activePreviewId, setActivePreviewId] = useState<string | null>(null);

  // Link Modal State
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [linkingRow, setLinkingRow] = useState<GenericRow | null>(null);

  // Metadata Edit State
  const [isMetadataEditOpen, setIsMetadataEditOpen] = useState(false);
  const [editingFile, setEditingFile] = useState<GenericRow | null>(null);

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
            onClick={(e) => { e.stopPropagation(); setViewRow(row); setIsViewModalOpen(true); }}
            className="font-medium text-slate-700 hover:text-primary-600 transition-colors border-b border-transparent hover:border-primary-600 hover:border-dashed text-left"
          >
            {value}
          </button>

          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity absolute left-full ml-2 bg-white/90 backdrop-blur shadow-sm border border-slate-200 rounded px-1.5 py-0.5 z-10 whitespace-nowrap">
            <button onClick={(e) => { e.stopPropagation(); setViewRow(row); setIsViewModalOpen(true); }} className="p-1 text-primary-600"><Eye size={14} /></button>
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
                  onClick={async () => {
                    // 触发批量智能匹配
                    if (selection.selectedIds.length === 0) {
                      ui.showToast('请先选择需要匹配的凭证', 'error');
                      return;
                    }
                    setIsMatching(true);
                    ui.showToast(`正在对 ${selection.selectedIds.length} 条凭证执行智能匹配...`, 'success');
                    try {
                      const batchResult = await matchingApi.executeBatchMatch(selection.selectedIds);

                      // 获取每个凭证的详细匹配结果
                      const previewItems: MatchPreviewItem[] = [];
                      for (const voucherId of selection.selectedIds) {
                        const matchResult = await matchingApi.getMatchResult(voucherId);
                        const row = data.rows.find(r => r.id === voucherId);

                        if (matchResult && matchResult.links) {
                          const matchedDocs = matchResult.links
                            .filter(link => link.status === 'MATCHED' && link.matchedDocNo)
                            .map(link => ({
                              docNo: link.matchedDocNo || '',
                              docType: link.evidenceRoleName || '',
                              docId: link.matchedDocId || '',
                              evidenceRole: link.evidenceRole || '',
                              score: link.score || 0
                            }));

                          const avgScore = matchedDocs.length > 0
                            ? Math.round(matchedDocs.reduce((sum, d) => sum + d.score, 0) / matchedDocs.length)
                            : 0;

                          previewItems.push({
                            voucherId,
                            voucherNo: row?.erpVoucherNo || row?.voucherNo || matchResult.voucherNo || voucherId,
                            amount: row?.amount || '-',
                            date: row?.date || '-',
                            matchScore: avgScore,
                            matchedDocs,
                            status: avgScore >= matchThreshold ? 'high' : avgScore > 0 ? 'low' : 'none'
                          });
                        } else {
                          previewItems.push({
                            voucherId,
                            voucherNo: row?.erpVoucherNo || row?.voucherNo || voucherId,
                            amount: row?.amount || '-',
                            date: row?.date || '-',
                            matchScore: 0,
                            matchedDocs: [],
                            status: 'none'
                          });
                        }
                      }

                      setMatchPreviewData(previewItems);
                      setIsMatchPreviewOpen(true);

                      const highCount = previewItems.filter(i => i.status === 'high').length;
                      const lowCount = previewItems.filter(i => i.status === 'low').length;
                      ui.showToast(`匹配完成：${highCount} 条自动关联，${lowCount} 条需确认`, 'success');

                    } catch (error: any) {
                      ui.showToast('匹配失败: ' + (error.message || '未知错误'), 'error');
                    } finally {
                      setIsMatching(false);
                    }
                  }}
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

      {/* Archive Detail Modal */}
      <ArchiveDetailModal
        open={isViewModalOpen}
        onClose={() => setIsViewModalOpen(false)}
        row={viewRow}
        config={mode.config}
        isPoolView={mode.isPoolView}
        activePreviewId={activePreviewId}
        onPreviewIdChange={setActivePreviewId}
        mainFileId={viewRow?.fileId || null} // Assuming fileId mapping
        relatedFiles={[]} // TODO: Fetch related files in Controller action or here? Ideally controller.
        // For simplicity, we can pass a callback to load?
        // Or updated controller.ui with preview state.
        // For now, passing empty or mock.
        renderCell={renderCell}
        formatStatus={(s) => s || '-'}
        resolveDocumentTypeLabel={(t) => t || '文档'}
        getPreviewUrl={(id) => `/api/pool/preview/${id}`}
        onAipExport={archiveActions.handleAipExport}
        isExporting={archiveActions.isExporting}
        onUploadAttachment={archiveActions.handleUpload}
        isUploading={archiveActions.isUploading}
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

      {/* 匹配结果预览弹窗 */}
      {isMatchPreviewOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl border border-slate-100 flex flex-col max-h-[85vh]">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
              <div>
                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                  <Zap size={20} className="text-amber-500" /> 智能匹配结果预览
                </h3>
                <p className="text-xs text-slate-500 mt-1">请确认以下匹配建议，高置信度将自动关联，疑似匹配需人工审核</p>
              </div>
              <button onClick={() => setIsMatchPreviewOpen(false)}><X size={20} className="text-slate-400 hover:text-slate-600" /></button>
            </div>

            <div className="flex-1 overflow-auto p-0">
              <table className="w-full text-left border-collapse">
                <thead className="sticky top-0 bg-slate-50 z-10 shadow-sm">
                  <tr>
                    <th className="p-4 text-xs font-medium text-slate-500">凭证号</th>
                    <th className="p-4 text-xs font-medium text-slate-500">摘要/金额</th>
                    <th className="p-4 text-xs font-medium text-slate-500">匹配到的原始凭证</th>
                    <th className="p-4 text-xs font-medium text-slate-500">匹配度</th>
                    <th className="p-4 text-xs font-medium text-slate-500">状态</th>
                    <th className="p-4 text-xs font-medium text-slate-500">操作</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {matchPreviewData.length > 0 ? matchPreviewData.map((item, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-4 font-medium text-slate-700">{item.voucherNo}</td>
                      <td className="p-4">
                        <div className="text-xs text-slate-500">{item.date}</div>
                        <div className="font-mono font-bold text-slate-700">{item.amount}</div>
                      </td>
                      <td className="p-4">
                        {item.matchedDocs.length > 0 ? item.matchedDocs.map((doc, i) => (
                          <div key={i} className="flex items-center gap-2 text-xs bg-slate-100 px-2 py-1 rounded mb-1">
                            <Receipt size={12} className="text-slate-400" />
                            <span>{doc.docNo}</span>
                            <span className="text-slate-400">({doc.docType})</span>
                          </div>
                        )) : (
                          <span className="text-xs text-slate-400">无匹配</span>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                            <div className={`h-full rounded-full ${item.matchScore >= matchThreshold ? 'bg-emerald-500' : 'bg-amber-500'}`} style={{ width: `${item.matchScore}%` }}></div>
                          </div>
                          <span className={`text-xs font-bold ${item.matchScore >= matchThreshold ? 'text-emerald-600' : 'text-amber-600'}`}>{item.matchScore}%</span>
                        </div>
                      </td>
                      <td className="p-4">
                        {item.status === 'high' ? (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium border border-emerald-100">
                            <CheckCircle2 size={12} /> 自动关联
                          </span>
                        ) : item.status === 'confirmed' ? (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium border border-emerald-100">
                            <CheckCircle2 size={12} /> 已确认
                          </span>
                        ) : item.status === 'low' ? (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-amber-50 text-amber-600 text-xs font-medium border border-amber-100">
                            <AlertTriangle size={12} /> 需人工确认
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-slate-50 text-slate-500 text-xs font-medium border border-slate-200">
                            <XCircle size={12} /> 无匹配
                          </span>
                        )}
                      </td>
                      <td className="p-4">
                        {item.status === 'low' && item.matchedDocs.length > 0 && (
                          <button
                            onClick={async () => {
                              try {
                                // 调用确认关联 API
                                const links = item.matchedDocs.map(doc => ({
                                  sourceDocId: doc.docId,
                                  evidenceRole: doc.evidenceRole,
                                  linkType: 'SHOULD_LINK'
                                }));
                                await matchingApi.confirmMatch(item.voucherId, links);

                                // 更新本地状态
                                setMatchPreviewData(prev => prev.map(p =>
                                  p.voucherId === item.voucherId ? { ...p, status: 'confirmed' as const } : p
                                ));
                                ui.showToast(`已确认关联: ${item.voucherNo}`, 'success');
                              } catch (error: any) {
                                ui.showToast('确认失败: ' + (error.message || '未知错误'), 'error');
                              }
                            }}
                            className="px-3 py-1.5 bg-primary-500 text-white text-xs font-medium rounded-lg hover:bg-primary-600 transition-all active:scale-95 flex items-center gap-1"
                          >
                            <CheckCircle2 size={12} /> 确认关联
                          </button>
                        )}
                        {item.status === 'high' && (
                          <span className="text-xs text-slate-400">已自动关联</span>
                        )}
                        {item.status === 'confirmed' && (
                          <span className="text-xs text-emerald-500">✓ 已确认</span>
                        )}
                        {item.status === 'none' && (
                          <span className="text-xs text-slate-400">-</span>
                        )}
                      </td>
                    </tr>
                  )) : (
                    <tr><td colSpan={6} className="p-8 text-center text-slate-400">本次匹配未发现匹配项</td></tr>
                  )}
                </tbody>
              </table>
            </div>

            <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-between items-center">
              <div className="text-sm text-slate-500">
                共 {matchPreviewData.length} 条凭证，
                <span className="text-emerald-600 font-medium">{matchPreviewData.filter(i => i.status === 'high').length}</span> 条自动关联，
                <span className="text-amber-600 font-medium">{matchPreviewData.filter(i => i.status === 'low').length}</span> 条需确认
              </div>
              <div className="flex gap-3">
                <button onClick={() => setIsMatchPreviewOpen(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">关闭</button>
                <button
                  onClick={() => {
                    const highCount = matchPreviewData.filter(i => i.status === 'high').length;
                    const lowCount = matchPreviewData.filter(i => i.status === 'low').length;

                    if (highCount === 0 && lowCount === 0) {
                      ui.showToast('本次匹配未找到关联的原始凭证，请检查原始凭证池是否有对应单据', 'error');
                    } else if (highCount === 0) {
                      ui.showToast(`有 ${lowCount} 条凭证需要人工确认关联，请在凭证详情中手动关联`, 'success');
                    } else {
                      ui.showToast(`已确认 ${highCount} 条自动关联${lowCount > 0 ? `，另有 ${lowCount} 条需人工确认` : ''}`, 'success');
                    }

                    setIsMatchPreviewOpen(false);
                    selection.clear();
                    controller.actions.reload();
                  }}
                  className="px-6 py-2 bg-primary-600 text-white font-medium rounded-lg hover:bg-primary-700 shadow-lg shadow-primary-500/30 transition-all active:scale-95 flex items-center gap-2"
                >
                  <CheckCircle2 size={18} /> 确认并应用
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export { ArchiveListView };
export default ArchiveListView;
