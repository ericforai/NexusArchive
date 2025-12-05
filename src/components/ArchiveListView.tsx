import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import {
  Search, Filter, Download, Eye, MoreHorizontal, FileText,
  ChevronLeft, ChevronRight, ArrowUpDown, Loader2, X,
  Receipt, ShieldCheck, CheckCircle2, AlertCircle, Layers,
  Upload, FileCheck, Archive, Book,
  Plus, AlertTriangle, Clock, XCircle, Link as LinkIcon, Trash2,
  Settings2, Play, Zap, BarChart2, Lock, ArrowRight, Package
} from 'lucide-react';
import { ModuleConfig, TableColumn, GenericRow } from '../types';
import { poolApi } from '../api/pool';
import { archivesApi } from '../api/archives';
import { ARCHIVE_BOX_CONFIG } from '../constants';
import { RelationshipVisualizer } from './RelationshipVisualizer';
import { client } from '../api/client';
import { adminApi } from '../api/admin';

interface ArchiveListViewProps {
  title: string;
  subTitle?: string;
  config: ModuleConfig;
}

// Mock candidates for linking with dynamic scores
const MOCK_CANDIDATES = [
  { id: '1', type: 'invoice', code: 'INV-202311-089', name: '阿里云计算服务费发票', amount: '¥ 12,800.00', date: '2023-11-02', score: 98 },
  { id: '2', type: 'contract', code: 'CON-2023-098', name: '年度技术服务协议', amount: '¥ 150,000.00', date: '2023-01-01', score: 92 },
  { id: '3', type: 'invoice', code: 'INV-202311-092', name: '服务器采购报销', amount: '¥ 45,200.00', date: '2023-11-03', score: 85 },
  { id: '4', type: 'invoice', code: 'INV-202310-011', name: '办公用品采购', amount: '¥ 2,300.00', date: '2023-10-15', score: 45 },
];

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

export const ArchiveListView: React.FC<ArchiveListViewProps> = ({ title, subTitle, config }) => {
  // State for Real CRUD
  const [localData, setLocalData] = useState<GenericRow[]>(config.data);
  const [isLoadingPool, setIsLoadingPool] = useState(false);
  const [orgOptions, setOrgOptions] = useState<{ label: string; value: string }[]>([]);
  const [searchTerm, setSearchTerm] = useState('');

  // Load pool data from backend
  const loadPoolData = async () => {
    setIsLoadingPool(true);
    try {
      const poolItems = await poolApi.getList();
      setLocalData(poolItems as GenericRow[]);
    } catch (error) {
      console.error('Failed to load pool data:', error);
      showToast('加载数据失败', 'error');
    } finally {
      setIsLoadingPool(false);
    }
  };

  // Update local data when config changes (view switch)
  useEffect(() => {
    setSelectedRows([]); // Clear selection when view changes
    if (subTitle === '电子凭证池') {
      // Load from backend for pool view
      loadPoolData();
    } else if (subTitle === '会计凭证') {
      // Load from backend for Accounting Vouchers
      const loadArchives = async () => {
        setIsLoadingPool(true);
        try {
          const result = await archivesApi.getArchives({ categoryCode: 'AC01' }); // Filter by Accounting Vouchers

          // result is ApiResponse<PageResult<Archive>>
          // We need to access result.data.records
          const pageResult = result.data;
          const items = (pageResult as any).records || [];

          const mappedItems = Array.isArray(items) ? items.map((item: any) => ({
            id: item.id,
            voucherNo: item.archiveCode, // Use archiveCode as voucherNo for now
            archivalCode: item.archiveCode,
            entity: item.orgName,
            period: `${item.fiscalYear}-${item.fiscalPeriod || ''}`,
            subject: item.title,
            type: item.categoryCode === 'AC01' ? '会计凭证' : item.categoryCode,
            amount: '-', // Amount is not in the main Archive table, would need metadata
            date: item.createdAt ? item.createdAt.split('T')[0] : '',
            status: item.status === 'archived' ? '已归档' : item.status
          })) : [];

          setLocalData(mappedItems);
        } catch (error) {
          console.error('Failed to load archives:', error);
          showToast('加载档案数据失败', 'error');
        } finally {
          setIsLoadingPool(false);
        }
      };
      loadArchives();
    } else {
      // Use config data for other views
      setLocalData(config.data);
    }
  }, [config.data, subTitle]);

  // Load organizations for filtering (basic example)
  useEffect(() => {
    const loadOrgs = async () => {
      try {
        const res = await adminApi.listOrg();
        if (res.code === 200 && res.data) {
          setOrgOptions(
            (res.data as any[]).map((o) => ({
              label: o.name,
              value: o.id
            }))
          );
        }
      } catch (e) {
        // ignore
      }
    };
    loadOrgs();
  }, []);

  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [filterQuery, setFilterQuery] = useState('');
  const [orgFilter, setOrgFilter] = useState<string>('');

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newRowData, setNewRowData] = useState<Partial<GenericRow>>({});

  const [selectedRows, setSelectedRows] = useState<string[]>([]);

  // Linking Feature State
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [linkingRow, setLinkingRow] = useState<GenericRow | null>(null);
  const [selectedCandidates, setSelectedCandidates] = useState<string[]>([]);

  // Auto Match Feature State
  const [isRuleModalOpen, setIsRuleModalOpen] = useState(false);
  const [matchRules, setMatchRules] = useState<MatchRule[]>(DEFAULT_RULES);
  const [isMatching, setIsMatching] = useState(false);
  const [matchThreshold, setMatchThreshold] = useState(80);

  // Match Preview State
  const [isMatchPreviewOpen, setIsMatchPreviewOpen] = useState(false);
  const [matchPreviewData, setMatchPreviewData] = useState<GenericRow[]>([]);


  // View Feature State
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [viewRow, setViewRow] = useState<GenericRow | null>(null);
  const [viewMode, setViewMode] = useState<'preview' | 'relation'>('preview');

  // 4-Nature Compliance Feature State
  const [isComplianceModalOpen, setIsComplianceModalOpen] = useState(false);

  // Toast Notification State
  const [toast, setToast] = useState<{ visible: boolean; message: string; type: 'success' | 'error' }>({ visible: false, message: '', type: 'success' });

  // Export State
  const [isExporting, setIsExporting] = useState<string | null>(null);

  // --- Actions ---

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ visible: true, message, type });
    setTimeout(() => {
      setToast(prev => ({ ...prev, visible: false }));
    }, 3000);
  };

  const handleExport = () => {
    if (localData.length === 0) {
      showToast('没有数据可导出', 'error');
      return;
    }
    const headers = config.columns.map(c => c.header).join(',');
    const rows = localData.map(row => config.columns.map(c => row[c.key]).join(',')).join('\n');
    const csvContent = `data:text/csv;charset=utf-8,\uFEFF${headers}\n${rows}`;
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `${title}_${subTitle || 'data'}_export.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('导出成功，正在下载文件');
  };

  // AIP Package Export Handler
  const handleAipExport = async (row: GenericRow) => {
    const archivalCode = row.archivalCode || row.code;
    if (!archivalCode) {
      showToast('档号不存在，无法导出', 'error');
      return;
    }

    setIsExporting(archivalCode);
    try {
      await archivesApi.exportAipPackage(archivalCode);
      showToast(`AIP 包导出成功: ${archivalCode}`);
    } catch (error) {
      console.error('导出失败:', error);
      showToast('导出失败，请检查档案包是否存在', 'error');
    } finally {
      setIsExporting(null);
    }
  };

  // Formal Archiving Handler
  const [isArchiving, setIsArchiving] = useState(false);

  const handleFormalArchiving = async () => {
    if (selectedRows.length === 0) {
      showToast('请先选择要归档的凭证', 'error');
      return;
    }

    if (!confirm(`确定要将选中的 ${selectedRows.length} 条凭证正式归档吗？\n归档后将生成 AIP 档案包。`)) {
      return;
    }

    setIsArchiving(true);
    try {
      await poolApi.archiveItems(selectedRows);
      showToast(`成功归档 ${selectedRows.length} 条凭证`);
      setSelectedRows([]);
      // Reload pool data
      loadPoolData();
    } catch (error) {
      console.error('归档失败:', error);
      showToast('归档失败: ' + (error instanceof Error ? error.message : '未知错误'), 'error');
    } finally {
      setIsArchiving(false);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('确定要删除这条记录吗？')) {
      setLocalData(prev => prev.filter(row => row.id !== id));
      showToast('记录已删除');
    }
  };

  const handleBatchDelete = () => {
    if (selectedRows.length === 0) {
      alert('请先选择要删除的记录');
      return;
    }
    if (confirm(`确定要删除选中的 ${selectedRows.length} 条记录吗？`)) {
      const count = selectedRows.length;
      setLocalData(prev => prev.filter(row => !selectedRows.includes(row.id)));
      setSelectedRows([]);
      showToast(`已成功删除 ${count} 条记录`);
    }
  };

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const newId = Math.random().toString(36).substr(2, 9);
    const timestamp = new Date().toISOString().split('T')[0];
    const rowToAdd: GenericRow = { ...newRowData, id: newId };
    config.columns.forEach(col => {
      if (!rowToAdd[col.key]) {
        if (col.type === 'date') rowToAdd[col.key] = timestamp;
        if (col.type === 'status') rowToAdd[col.key] = '待处理';
        if (col.type === 'progress') rowToAdd[col.key] = 0;
      }
    });
    setLocalData([rowToAdd, ...localData]);
    setIsAddModalOpen(false);
    setNewRowData({});
    showToast('新增记录成功');
  };

  const toggleRowSelection = (id: string) => {
    if (selectedRows.includes(id)) {
      setSelectedRows(selectedRows.filter(rowId => rowId !== id));
    } else {
      setSelectedRows([...selectedRows, id]);
    }
  };

  const toggleAllSelection = () => {
    if (selectedRows.length === localData.length) {
      setSelectedRows([]);
    } else {
      setSelectedRows(localData.map(r => r.id));
    }
  };

  // --- Linking Logic ---
  const openLinkModal = (row: GenericRow) => {
    setLinkingRow(row);
    setSelectedCandidates([]);
    setIsLinkModalOpen(true);
  };

  const toggleCandidateSelection = (id: string) => {
    if (selectedCandidates.includes(id)) {
      setSelectedCandidates(selectedCandidates.filter(c => c !== id));
    } else {
      setSelectedCandidates([...selectedCandidates, id]);
    }
  };

  const handleLinkConfirm = () => {
    if (!linkingRow) return;

    console.log('Confirming Link. Selected Candidates:', selectedCandidates);

    const selectedDocs = MOCK_CANDIDATES.filter(c => selectedCandidates.includes(c.id));
    console.log('Selected Docs:', selectedDocs);

    const invoiceCount = selectedDocs.filter(d => d.type === 'invoice').length;
    console.log('Calculated Invoice Count:', invoiceCount);

    const contractDoc = selectedDocs.find(d => d.type === 'contract');

    // Calculate average match score from selected candidates
    const totalScore = selectedDocs.reduce((sum, doc) => sum + doc.score, 0);
    const avgScore = selectedDocs.length > 0 ? Math.round(totalScore / selectedDocs.length) : 0;

    const updatedData = localData.map(item => {
      if (item.id === linkingRow.id) {
        return {
          ...item,
          invoiceCount: `${invoiceCount} 张`, // Always update to reflect current selection
          contractNo: contractDoc ? contractDoc.code : '-', // Clear contract if not selected
          matchScore: avgScore, // Use dynamic score from candidates
          autoLink: '手工关联',
          status: '已关联',
          linkedFileId: selectedCandidates[0] // Store the first selected file ID as the linked file
        };
      }
      return item;
    });
    setLocalData(updatedData);
    setIsLinkModalOpen(false);
    setLinkingRow(null);
    showToast(`成功关联 ${selectedCandidates.length} 个单据`);
  };

  // --- Auto Match Logic ---
  const toggleRule = (id: string) => {
    setMatchRules(prev => prev.map(r => r.id === id ? { ...r, enabled: !r.enabled } : r));
  };

  const runAutoMatch = () => {
    setIsMatching(true);
    setIsRuleModalOpen(false);

    // Simulate Algorithm Delay
    setTimeout(() => {
      const previewResults = localData.map(row => {
        if (row.autoLink === '手工关联' || row.status === '已关联') return row;

        let newScore = Math.floor(Math.random() * 30) + 60; // 60-90 base
        if (Math.random() > 0.3) newScore += 15; // Boost chance
        newScore = Math.min(newScore, 100);

        const isHighConfidence = newScore >= matchThreshold;
        const isSuspected = newScore >= 60 && newScore < matchThreshold;

        if (isHighConfidence || isSuspected) {
          return {
            ...row,
            matchScore: newScore,
            // Temporary status for preview
            _previewStatus: isHighConfidence ? 'high' : 'suspect',
            _previewDesc: isHighConfidence ? '高置信度匹配' : '疑似匹配 (需人工确认)',
            // Mock potential links
            _proposedLinks: [
              { type: 'invoice', code: `INV-AUTO-${Math.floor(Math.random() * 1000)}`, score: newScore }
            ]
          };
        }
        return row;
      });

      setMatchPreviewData(previewResults);
      setIsMatching(false);
      setIsMatchPreviewOpen(true);
    }, 1500);
  };

  const confirmAutoMatch = () => {
    const updatedData = matchPreviewData.map(row => {
      if (row._previewStatus === 'high') {
        return {
          ...row,
          status: '已关联',
          autoLink: '规则引擎',
          invoiceCount: '1 张', // Mock update
          // Clean up temp fields
          _previewStatus: undefined,
          _previewDesc: undefined,
          _proposedLinks: undefined
        };
      }
      // Keep suspected as is, or mark as '待确认'
      if (row._previewStatus === 'suspect') {
        return {
          ...row,
          status: '待确认',
          autoLink: '智能推荐',
          matchScore: row.matchScore,
          _previewStatus: undefined,
          _previewDesc: undefined,
          _proposedLinks: undefined
        };
      }
      return row;
    });

    setLocalData(updatedData);
    setIsMatchPreviewOpen(false);
    const matchCount = updatedData.filter(r => r.status === '已关联').length;
    showToast(`自动匹配完成，已自动关联 ${matchCount} 条凭证`);
  };

  // --- Render Helpers ---

  const filteredData = localData.filter(row => {
    const matchesText = !filterQuery || Object.values(row).some(val =>
      String(val).toLowerCase().includes(filterQuery.toLowerCase())
    );
    const matchesOrg = !orgFilter || row.departmentId === orgFilter || row.orgId === orgFilter;
    return matchesText && matchesOrg;
  });

  // Pagination Logic
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  // Reset to first page when filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [filterQuery, subTitle]);

  const paginatedData = filteredData.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const renderCell = (row: GenericRow, column: TableColumn) => {
    const value = row[column.key];
    if (column.key === 'invoiceCount') {
      console.log('Rendering invoiceCount:', value, 'for row:', row.id);
    }

    if (column.type === 'status') {
      let colorClass = 'bg-slate-100 text-slate-600 border-slate-200';
      let Icon = null;
      const statusLower = String(value).toLowerCase();

      // Chinese Status Mappings
      if (['已完成', '已归档', '已通过', '已归还', '正常', '机密', '已关联', '已审核', '已记账', '匹配成功', '打开', '通风中'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-emerald-50 text-emerald-700 border-emerald-100';
        Icon = CheckCircle2;
      } else if (['处理中', '待处理', '待审批', '查看', '激活', '闭合'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-blue-50 text-blue-700 border-blue-100';
        Icon = Clock;
      } else if (['错误', '审计失败', '已拒绝', '警告', '异常', '离线', '锁定'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-rose-50 text-rose-700 border-rose-100';
        Icon = XCircle;
      } else if (['未关联', '空闲'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-slate-100 text-slate-500 border-slate-200';
        Icon = AlertTriangle;
      } else if (['内部'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-amber-50 text-amber-700 border-amber-100';
      }

      return <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${colorClass}`}>{Icon && <Icon size={12} />}{value}</span>;
    }

    if (column.key === 'invoiceCount') {
      const count = parseInt(String(value));
      if (count > 0 && subTitle === '凭证关联') {
        return (
          <button
            onClick={(e) => {
              e.stopPropagation();
              // Open Visualizer directly
              setViewRow(row);
              setIsViewModalOpen(true);
              setViewMode('relation');
            }}
            className="text-primary-600 hover:text-primary-800 font-medium hover:underline flex items-center gap-1"
            title="点击查看关联详情"
          >
            {value} <ArrowRight size={12} />
          </button>
        );
      }
    }

    if (column.type === 'progress') {
      const numValue = Number(value);
      let color = 'bg-blue-500';
      if (numValue === 100) color = 'bg-emerald-500';
      else if (numValue > 90) color = 'bg-blue-500';
      else if (numValue > 60) color = 'bg-amber-500';
      else color = 'bg-rose-500';
      return (
        <div className="w-full max-w-[120px] flex items-center gap-2">
          <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
            <div className={`h-full rounded-full ${color} transition-all duration-1000`} style={{ width: `${numValue}%` }}></div>
          </div>
          <span className="text-xs font-medium text-slate-600">{value}%</span>
        </div>
      );
    }

    if (column.type === 'action') {
      return (
        <div className="flex gap-1">
          {subTitle === '凭证关联' && (
            <button onClick={(e) => { e.stopPropagation(); openLinkModal(row); }} className="text-slate-400 hover:text-primary-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="关联单据"><LinkIcon size={16} /></button>
          )}
          <button onClick={(e) => { e.stopPropagation(); setViewRow(row); setIsViewModalOpen(true); }} className="text-slate-400 hover:text-primary-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="查看"><Eye size={16} /></button>
          {(row.archivalCode || row.code) && (
            <button
              onClick={(e) => { e.stopPropagation(); handleAipExport(row); }}
              className="text-slate-400 hover:text-emerald-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100 disabled:opacity-50 disabled:cursor-not-allowed"
              title="导出 AIP 包"
              disabled={isExporting === (row.archivalCode || row.code)}
            >
              {isExporting === (row.archivalCode || row.code) ? <Loader2 size={16} className="animate-spin" /> : <Package size={16} />}
            </button>
          )}
          <button onClick={(e) => { e.stopPropagation(); handleDelete(row.id) }} className="text-slate-400 hover:text-rose-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="删除"><Trash2 size={16} /></button>
        </div>
      )
    }
    return <span className="text-slate-700 font-medium">{value}</span>;
  };

  return (
    <div className="p-8 space-y-6 max-w-[1600px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500 relative">

      {/* Toast (Portal) */}
      {toast.visible && createPortal(
        <div className="fixed top-6 left-1/2 transform -translate-x-1/2 z-[100] animate-in slide-in-from-top-2 fade-in duration-300">
          <div className={`flex items-center gap-3 px-5 py-3 rounded-xl shadow-2xl border ${toast.type === 'success' ? 'bg-slate-800 text-white border-slate-700' : 'bg-rose-600 text-white border-rose-500'}`}>
            {toast.type === 'success' ? <CheckCircle2 size={20} className="text-emerald-400" /> : <AlertTriangle size={20} className="text-white" />}
            <span className="font-medium text-sm">{toast.message}</span>
            <button onClick={() => setToast(prev => ({ ...prev, visible: false }))} className="ml-2 text-slate-400 hover:text-white"><XCircle size={16} /></button>
          </div>
        </div>,
        document.body
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-baseline gap-3">
            <h2 className="text-2xl font-bold text-slate-800">{title}</h2>
            {subTitle && <><span className="text-slate-300 text-2xl">/</span><span className="text-xl font-medium text-primary-600">{subTitle}</span></>}
          </div>
          <p className="text-slate-500 mt-1">管理和查看 {subTitle || title} 的相关记录。</p>
        </div>
        <div className="flex items-center gap-3">
          {/* 4-Nature Compliance Button */}
          <button
            onClick={() => setIsComplianceModalOpen(true)}
            className="px-4 py-2 bg-indigo-50 border border-indigo-200 text-indigo-700 rounded-lg text-sm font-medium hover:bg-indigo-100 flex items-center shadow-sm transition-all active:scale-95"
          >
            <ShieldCheck size={16} className="mr-2" /> 四性检测
          </button>
          <div className="h-8 w-px bg-slate-200 mx-1"></div>

          {/* Special Toolbar for Linking */}
          {subTitle === '凭证关联' && (
            <>
              <button onClick={() => setIsRuleModalOpen(true)} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95">
                <Settings2 size={16} className="mr-2" /> 匹配规则
              </button>
              <button onClick={runAutoMatch} disabled={isMatching} className="px-4 py-2 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg text-sm font-medium hover:bg-emerald-100 flex items-center shadow-sm transition-all active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed">
                {isMatching ? <Loader2 size={16} className="mr-2 animate-spin" /> : <Zap size={16} className="mr-2" />} {isMatching ? '匹配中...' : '自动匹配'}
              </button>
              <div className="h-8 w-px bg-slate-200 mx-1"></div>
            </>
          )}

          <div className={`flex items-center bg-white border border-slate-200 rounded-lg overflow-hidden transition-all duration-300 ${isFilterOpen ? 'w-64 opacity-100 shadow-sm' : 'w-0 opacity-0 border-none'}`}>
            <div className="pl-3 text-slate-400"><Search size={14} /></div>
            <input type="text" placeholder="搜索..." className="w-full px-3 py-2 text-sm outline-none bg-transparent" value={filterQuery} onChange={(e) => setFilterQuery(e.target.value)} />
          </div>
          <button onClick={() => setIsFilterOpen(!isFilterOpen)} className={`px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95 ${isFilterOpen ? 'bg-slate-100 text-primary-600 border-primary-200' : ''}`}>
            <Filter size={16} className="mr-2" /> 筛选
          </button>
          {isFilterOpen && orgOptions.length > 0 && (
            <select
              className="border border-slate-200 rounded px-2 py-2 text-sm"
              value={orgFilter}
              onChange={(e) => setOrgFilter(e.target.value)}
            >
              <option value="">全部组织</option>
              {orgOptions.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          )}
          <button onClick={handleExport} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95">
            <Download size={16} className="mr-2" /> 导出
          </button>
          {(subTitle === '电子凭证池' || subTitle === '会计凭证') && (
            <>
              <input
                type="file"
                id="file-upload"
                className="hidden"
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (!file) return;

                  const formData = new FormData();
                  formData.append('file', file);

                  try {
                    showToast('正在上传...', 'success');
                    // Use client for upload
                    const response = await client.post('/ingest/upload', formData, {
                      headers: {
                        'Content-Type': 'multipart/form-data'
                      }
                    });

                    console.log('Response status:', response.status);

                    if (response.status === 200) {
                      const result = response.data;
                      console.log('Upload result:', result);
                      if (result.code === 200 && result.data) {
                        showToast(`✅ 上传成功: ${result.data.fileName}`, 'success');
                        // Reload pool data from backend
                        loadPoolData();
                      } else {
                        showToast('上传失败: ' + result.message, 'error');
                      }
                    } else {
                      showToast(`上传失败: 服务器错误 (${response.status})`, 'error');
                    }
                  } catch (error) {
                    console.error('Upload error:', error);
                    showToast('上传出错: ' + (error as Error).message, 'error');
                  }

                  // Reset input
                  e.target.value = '';
                }}
              />
              <label htmlFor="file-upload" className="cursor-pointer px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95">
                <Upload size={16} className="mr-2" /> 上传
              </label>
              <button
                onClick={handleFormalArchiving}
                disabled={isArchiving || selectedRows.length === 0}
                className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 flex items-center shadow-sm transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isArchiving ? (
                  <>
                    <Loader2 size={16} className="mr-2 animate-spin" /> 归档中...
                  </>
                ) : (
                  <>
                    <Layers size={16} className="mr-2" /> 正式归档{selectedRows.length > 0 && ` (${selectedRows.length})`}
                  </>
                )}
              </button>
            </>
          )}
          {selectedRows.length > 0 && subTitle === '凭证关联' && (
            <button
              onClick={() => {
                showToast(`已将 ${selectedRows.length} 条记录移交归档`, 'success');
                // Remove selected rows from list (simulate transfer)
                setLocalData(prev => prev.filter(row => !selectedRows.includes(row.id)));
                setSelectedRows([]);
              }}
              className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 flex items-center shadow-lg shadow-green-500/30 transition-all active:scale-95"
            >
              <CheckCircle2 size={16} className="mr-2" /> 移交归档 ({selectedRows.length})
            </button>
          )}
          {selectedRows.length > 0 && subTitle === '会计凭证' && (
            <button
              onClick={() => {
                const archiveCode = `QZ-${new Date().getFullYear()}-KJ-${String(Math.floor(Math.random() * 9999)).padStart(4, '0')}`;

                // Add to Archive Box Config
                const newBox = {
                  id: `box-${Date.now()}`,
                  boxCode: archiveCode,
                  spec: 'A4标准盒',
                  thickness: '2cm',
                  docCount: String(selectedRows.length),
                  fullness: Math.min(selectedRows.length * 5, 100),
                  printStatus: '待打印'
                };
                ARCHIVE_BOX_CONFIG.data.unshift(newBox);

                showToast(`已生成案卷号: ${archiveCode}，共 ${selectedRows.length} 条记录，已自动加入装盒列表`, 'success');
                setSelectedRows([]);
              }}
              className="px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 flex items-center shadow-lg shadow-purple-500/30 transition-all active:scale-95"
            >
              <Layers size={16} className="mr-2" /> 组卷 ({selectedRows.length})
            </button>
          )}
          {selectedRows.length > 0 && subTitle === '档案装盒' && (
            <>
              <button
                onClick={() => {
                  showToast(`已将 ${selectedRows.length} 个案卷正式归档，档案已锁定`, 'success');
                  setLocalData(prev => prev.map(row =>
                    selectedRows.includes(row.id) ? { ...row, status: '已归档' } : row
                  ));
                  setSelectedRows([]);
                }}
                className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 flex items-center shadow-lg shadow-green-500/30 transition-all active:scale-95"
              >
                <Layers size={16} className="mr-2" /> 正式归档 ({selectedRows.length})
              </button>
            </>
          )}
          <button onClick={() => setIsAddModalOpen(true)} className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all active:scale-95">
            <Plus size={16} className="mr-2" /> 新增
          </button>
        </div>
      </div>

      {/* Content Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden flex flex-col h-[calc(100vh-240px)]">
        {/* Table Toolbar */}
        <div className="p-4 border-b border-slate-100 bg-slate-50/30 flex items-center gap-4 shrink-0">
          <span className="text-sm text-slate-500">已选择 <strong className="text-slate-800">{selectedRows.length}</strong> 项</span>
          <div className="h-4 w-px bg-slate-300"></div>
          <button onClick={handleBatchDelete} className="text-sm text-slate-600 hover:text-rose-600 font-medium disabled:opacity-50 disabled:cursor-not-allowed" disabled={selectedRows.length === 0}>批量删除</button>
          <button onClick={() => { setFilterQuery(''); setSelectedRows([]); setLocalData(config.data) }} className="text-sm text-slate-600 hover:text-primary-600 font-medium">重置视图</button>
        </div>

        {/* Dynamic Table */}
        <div className="overflow-auto flex-1 relative">
          {isMatching && (
            <div className="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-20 flex items-center justify-center flex-col">
              <Loader2 size={48} className="text-primary-500 animate-spin mb-4" />
              <p className="text-slate-600 font-medium animate-pulse">正在执行智能匹配算法...</p>
            </div>
          )}
          <table className="w-full text-left border-collapse relative">
            <thead className="sticky top-0 z-10 shadow-sm">
              <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider border-b border-slate-200">
                <th className="p-4 w-4 bg-slate-50"><input type="checkbox" checked={selectedRows.length === localData.length && localData.length > 0} onChange={toggleAllSelection} className="rounded border-slate-300 text-primary-600 focus:ring-primary-500 cursor-pointer" /></th>
                {config.columns.map((col) => (
                  <th key={col.key} className="p-4 font-medium bg-slate-50 whitespace-nowrap">{col.header}</th>
                ))}
                <th className="p-4 font-medium text-right bg-slate-50 sticky right-0 shadow-[-12px_0_12px_-12px_rgba(0,0,0,0.1)]">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {paginatedData.map((row, idx) => (
                <tr key={`${row.id}-${idx}`} className={`hover:bg-slate-50 transition-colors group ${selectedRows.includes(row.id) ? 'bg-primary-50/30' : ''}`}>
                  <td className="p-4"><input type="checkbox" checked={selectedRows.includes(row.id)} onChange={() => toggleRowSelection(row.id)} className="rounded border-slate-300 text-primary-600 focus:ring-primary-500 cursor-pointer" /></td>
                  {config.columns.map((col) => (
                    <td key={`${row.id}-${col.key}`} className="p-4 whitespace-nowrap">{renderCell(row, col)}</td>
                  ))}
                  <td className="p-4 text-right sticky right-0 bg-white group-hover:bg-slate-50 shadow-[-12px_0_12px_-12px_rgba(0,0,0,0.1)]">
                    {renderCell(row, { key: 'action', header: '操作', type: 'action' })}
                  </td>
                </tr>
              ))}
              {filteredData.length === 0 && (
                <tr><td colSpan={config.columns.length + 2} className="p-12 text-center text-slate-400">暂无数据</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-between items-center shrink-0">
          <span className="text-xs text-slate-500">
            显示 {filteredData.length > 0 ? (currentPage - 1) * itemsPerPage + 1 : 0} 到 {Math.min(currentPage * itemsPerPage, filteredData.length)} 条，共 {filteredData.length} 条
          </span>
          <div className="flex gap-1">
            <button
              onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
              disabled={currentPage === 1}
              className="px-3 py-1 border border-slate-200 rounded bg-white text-xs text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50"
            >
              上一页
            </button>

            {Array.from({ length: Math.ceil(filteredData.length / itemsPerPage) }, (_, i) => i + 1).map(page => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`px-3 py-1 border rounded text-xs font-medium transition-colors ${currentPage === page
                  ? 'border-primary-500 bg-primary-50 text-primary-600'
                  : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                  }`}
              >
                {page}
              </button>
            ))}

            <button
              onClick={() => setCurrentPage(prev => Math.min(prev + 1, Math.ceil(filteredData.length / itemsPerPage)))}
              disabled={currentPage >= Math.ceil(filteredData.length / itemsPerPage)}
              className="px-3 py-1 border border-slate-200 rounded bg-white text-xs text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      {/* 4-Nature Compliance Modal */}
      {
        isComplianceModalOpen && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl border border-slate-100">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-indigo-100 rounded-lg text-indigo-600"><ShieldCheck size={24} /></div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-800">四性检测智能诊断报告</h3>
                    <p className="text-xs text-slate-500">依据标准 DA/T 70-2018 执行检测</p>
                  </div>
                </div>
                <button onClick={() => setIsComplianceModalOpen(false)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
              </div>

              <div className="p-8 grid grid-cols-1 md:grid-cols-3 gap-8">
                {/* Overall Score */}
                <div className="md:col-span-1 flex flex-col items-center justify-center bg-slate-50 rounded-2xl p-6 border border-slate-100">
                  <div className="relative w-32 h-32 flex items-center justify-center">
                    <svg className="w-full h-full transform -rotate-90">
                      <circle cx="64" cy="64" r="60" stroke="#e2e8f0" strokeWidth="8" fill="transparent" />
                      <circle cx="64" cy="64" r="60" stroke="#4f46e5" strokeWidth="8" fill="transparent" strokeDasharray="377" strokeDashoffset="37.7" strokeLinecap="round" />
                    </svg>
                    <div className="absolute flex flex-col items-center">
                      <span className="text-4xl font-bold text-indigo-600">90</span>
                      <span className="text-xs font-bold text-slate-400 uppercase">综合得分</span>
                    </div>
                  </div>
                  <p className="text-sm text-center mt-4 text-slate-600">检测对象: <span className="font-bold">{localData.length}</span> 份档案<br />状态: <span className="text-emerald-600 font-bold">通过</span></p>
                </div>

                {/* Details */}
                <div className="md:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {[
                    { label: '真实性', score: 98, color: 'bg-emerald-500', items: ['电子签名有效', '时间戳完整'] },
                    { label: '完整性', score: 100, color: 'bg-blue-500', items: ['元数据齐全', '附件无缺失'] },
                    { label: '可用性', score: 95, color: 'bg-cyan-500', items: ['格式标准兼容', '索引库正常'] },
                    { label: '安全性', score: 85, color: 'bg-amber-500', items: ['病毒扫描通过', '权限配置待优化'] },
                  ].map((item, idx) => (
                    <div key={idx} className="border border-slate-100 rounded-xl p-4 hover:shadow-sm transition-shadow">
                      <div className="flex justify-between items-center mb-2">
                        <span className="font-bold text-slate-700">{item.label}</span>
                        <span className={`text-xs font-bold text-white px-2 py-0.5 rounded-full ${item.color.replace('bg-', 'bg-')}`}>{item.score}</span>
                      </div>
                      <div className="w-full bg-slate-100 h-1.5 rounded-full mb-3">
                        <div className={`h-full rounded-full ${item.color}`} style={{ width: `${item.score}%` }}></div>
                      </div>
                      <ul className="space-y-1">
                        {item.items.map((check, i) => (
                          <li key={i} className="text-xs text-slate-500 flex items-center gap-1.5">
                            <CheckCircle2 size={10} className="text-emerald-500" /> {check}
                          </li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
              </div>

              <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end">
                <button onClick={() => setIsComplianceModalOpen(false)} className="px-6 py-2 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 shadow-lg shadow-indigo-500/20 transition-all active:scale-95">生成详细报告</button>
              </div>
            </div>
          </div>
        )
      }

      {/* Rule, View, Add, Link Modals are implicitly here (reusing previous logic) */}
      {
        isMatchPreviewOpen && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl border border-slate-100 flex flex-col max-h-[85vh]">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                <div>
                  <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                    <Zap size={20} className="text-amber-500" /> 智能匹配结果预演
                  </h3>
                  <p className="text-xs text-slate-500 mt-1">请确认以下匹配建议，高置信度将自动关联，疑似匹配需人工审核</p>
                </div>
                <button onClick={() => setIsMatchPreviewOpen(false)}><X size={20} className="text-slate-400" /></button>
              </div>

              <div className="flex-1 overflow-auto p-0">
                <table className="w-full text-left border-collapse">
                  <thead className="sticky top-0 bg-slate-50 z-10 shadow-sm">
                    <tr>
                      <th className="p-4 text-xs font-medium text-slate-500">凭证号</th>
                      <th className="p-4 text-xs font-medium text-slate-500">摘要/金额</th>
                      <th className="p-4 text-xs font-medium text-slate-500">推荐关联</th>
                      <th className="p-4 text-xs font-medium text-slate-500">匹配度</th>
                      <th className="p-4 text-xs font-medium text-slate-500">建议操作</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {matchPreviewData.filter(r => r._previewStatus).map((row, idx) => (
                      <tr key={idx} className="hover:bg-slate-50">
                        <td className="p-4 font-medium text-slate-700">{row.voucherNo}</td>
                        <td className="p-4">
                          <div className="text-xs text-slate-500">{row.date}</div>
                          <div className="font-mono font-bold text-slate-700">{row.amount}</div>
                        </td>
                        <td className="p-4">
                          {row._proposedLinks?.map((link: any, i: number) => (
                            <div key={i} className="flex items-center gap-2 text-xs bg-slate-100 px-2 py-1 rounded mb-1">
                              <Receipt size={12} className="text-slate-400" />
                              <span>{link.code}</span>
                            </div>
                          ))}
                        </td>
                        <td className="p-4">
                          <div className="flex items-center gap-2">
                            <div className="w-16 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                              <div className={`h-full rounded-full ${row.matchScore >= 90 ? 'bg-emerald-500' : 'bg-amber-500'}`} style={{ width: `${row.matchScore}%` }}></div>
                            </div>
                            <span className={`text-xs font-bold ${row.matchScore >= 90 ? 'text-emerald-600' : 'text-amber-600'}`}>{row.matchScore}%</span>
                          </div>
                        </td>
                        <td className="p-4">
                          {row._previewStatus === 'high' ? (
                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium border border-emerald-100">
                              <CheckCircle2 size={12} /> 自动关联
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-amber-50 text-amber-600 text-xs font-medium border border-amber-100">
                              <AlertTriangle size={12} /> 需人工确认
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                    {matchPreviewData.filter(r => r._previewStatus).length === 0 && (
                      <tr><td colSpan={5} className="p-8 text-center text-slate-400">本次运行未发现新的匹配项</td></tr>
                    )}
                  </tbody>
                </table>
              </div>

              <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-3">
                <button onClick={() => setIsMatchPreviewOpen(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">取消</button>
                <button
                  onClick={confirmAutoMatch}
                  className="px-6 py-2 bg-primary-600 text-white font-medium rounded-lg hover:bg-primary-700 shadow-lg shadow-primary-500/30 transition-all active:scale-95 flex items-center gap-2"
                  disabled={matchPreviewData.filter(r => r._previewStatus).length === 0}
                >
                  <CheckCircle2 size={18} /> 确认并应用
                </button>
              </div>
            </div>
          </div>
        )
      }

      {
        isRuleModalOpen && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                <h3 className="text-lg font-bold text-slate-800">关联规则配置</h3>
                <button onClick={() => setIsRuleModalOpen(false)}><X size={20} className="text-slate-400" /></button>
              </div>
              <div className="p-6 space-y-4">
                <div className="mb-4"><label className="text-sm font-bold">自动关联阈值: {matchThreshold}%</label><input type="range" min="60" max="100" value={matchThreshold} onChange={(e) => setMatchThreshold(Number(e.target.value))} className="w-full h-2 bg-slate-200 rounded-lg accent-primary-500" /></div>
                {matchRules.map(rule => (
                  <div key={rule.id} className="flex justify-between items-center"><span className="text-sm">{rule.label}</span><input type="checkbox" checked={rule.enabled} onChange={() => toggleRule(rule.id)} className="toggle-checkbox h-5 w-5 text-primary-600 rounded border-slate-300" /></div>
                ))}
              </div>
              <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                <button onClick={() => setIsRuleModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button>
                <button onClick={runAutoMatch} className="px-4 py-2 bg-primary-600 text-white rounded-lg">保存并运行</button>
              </div>
            </div>
          </div>
        )
      }

      {
        isViewModalOpen && viewRow && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-6xl h-[85vh] border border-slate-100 flex flex-col">
              {/* Header */}
              <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl shrink-0">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-primary-100 text-primary-600 rounded-lg">
                    <FileText size={20} />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-800">档案详情</h3>
                    <p className="text-xs text-slate-500 font-mono">{viewRow.code}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsViewModalOpen(false)}
                    className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600 transition-colors"
                  >
                    <X size={20} />
                  </button>
                </div>
              </div>

              {/* Content */}
              <div className="flex-1 overflow-hidden grid grid-cols-1 lg:grid-cols-3 divide-y lg:divide-y-0 lg:divide-x divide-slate-100">
                {/* Left: Metadata */}
                <div className="col-span-1 bg-slate-50/30 p-6 overflow-y-auto">
                  <h4 className="text-sm font-bold text-slate-800 mb-4 flex items-center gap-2">
                    {subTitle === '会计账簿' ? (
                      <>
                        <Book size={16} className="text-blue-500" />
                        账簿信息
                      </>
                    ) : subTitle === '财务报告' ? (
                      <>
                        <FileText size={16} className="text-purple-500" />
                        报告信息
                      </>
                    ) : (
                      <>
                        <Receipt size={16} className="text-primary-500" />
                        凭证信息
                      </>
                    )}
                  </h4>
                  <div className="space-y-4">
                    {config.columns.filter(c => c.type !== 'action').map((col) => (
                      <div key={col.key} className="group">
                        <label className="text-xs font-medium text-slate-500 mb-1 block">{col.header}</label>
                        <div className="text-sm text-slate-800 font-medium break-all bg-white border border-slate-200 rounded-lg p-3 group-hover:border-primary-200 transition-colors">
                          {renderCell(viewRow, col)}
                        </div>
                      </div>
                    ))}

                    {/* Additional System Info */}
                    <div className="pt-4 border-t border-slate-200 mt-6">
                      <h4 className="text-sm font-bold text-slate-800 mb-4 flex items-center gap-2">
                        <ShieldCheck size={16} className="text-emerald-500" />
                        系统元数据
                      </h4>
                      <div className="space-y-3">
                        <div>
                          <label className="text-xs font-medium text-slate-500">存储ID</label>
                          <div className="text-xs font-mono text-slate-600 mt-1">{viewRow.id}</div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-slate-500">四性检测状态</label>
                          <div className="mt-1 flex items-center gap-2">
                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium border border-emerald-100">
                              <CheckCircle2 size={12} /> 检测通过
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Right: Preview / Visualizer */}
                <div className="col-span-1 lg:col-span-2 bg-slate-100/50 flex flex-col relative">
                  {/* View Mode Tabs */}
                  {subTitle === '凭证关联' && (
                    <div className="absolute top-4 right-4 z-20 bg-white rounded-lg shadow-sm border border-slate-200 p-1 flex gap-1">
                      <button
                        onClick={() => setViewMode('preview')}
                        className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${viewMode === 'preview' ? 'bg-primary-50 text-primary-600' : 'text-slate-600 hover:bg-slate-50'}`}
                      >
                        档案预览
                      </button>
                      <button
                        onClick={() => setViewMode('relation')}
                        className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${viewMode === 'relation' ? 'bg-primary-50 text-primary-600' : 'text-slate-600 hover:bg-slate-50'}`}
                      >
                        关联全景
                      </button>
                    </div>
                  )}

                  {viewMode === 'relation' && subTitle === '凭证关联' ? (
                    <RelationshipVisualizer
                      voucher={viewRow}
                      onDocumentClick={(doc) => {
                        // Open a nested preview or switch view row temporarily (simplified for now)
                        // In a real app, this might stack modals or change the main view
                        console.log('Clicked document:', doc);
                        showToast(`正在打开关联文档: ${doc.code}`, 'success');
                        // Ideally, we would fetch the full document details here. 
                        // For this demo, we can just show a toast or maybe switch the preview content if we had the ID.
                      }}
                    />
                  ) : (
                    <>
                      <div className="absolute inset-0 flex items-center justify-center text-slate-400 pointer-events-none">
                        <Loader2 size={32} className="animate-spin" />
                      </div>
                      <iframe
                        src={(viewRow.linkedFileId || viewRow.id) ? (() => {
                          // Enhanced Preview Logic with Multi-Type Support
                          let content = '';

                          // 1. Accounting Books (Ledger) Template
                          if (subTitle === '会计账簿') {
                            const title = viewRow.name || '总分类账';
                            content = `
                              <!DOCTYPE html>
                              <html>
                                <head>
                                  <style>
                                    body { margin: 0; padding: 20px; font-family: "SimSun", "Songti SC", serif; background: #f1f5f9; display: flex; justify-content: center; }
                                    .paper { width: 210mm; min-height: 297mm; background: white; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); padding: 40px; position: relative; box-sizing: border-box; }
                                    .header { text-align: center; margin-bottom: 20px; }
                                    .title { font-size: 28px; font-weight: bold; color: #1e293b; letter-spacing: 2px; border-bottom: 3px double #1e293b; display: inline-block; padding-bottom: 5px; }
                                    .meta { display: flex; justify-content: space-between; margin-bottom: 15px; font-size: 14px; color: #475569; }
                                    .ledger-table { width: 100%; border-collapse: collapse; font-size: 13px; }
                                    .ledger-table th, .ledger-table td { border: 1px solid #94a3b8; padding: 8px; text-align: center; }
                                    .ledger-table th { background: #f1f5f9; font-weight: bold; color: #334155; }
                                    .amount { font-family: "Courier New", monospace; text-align: right; }
                                    .watermark { position: absolute; top: 40%; left: 50%; transform: translate(-50%, -50%) rotate(-30deg); font-size: 100px; color: rgba(0,0,0,0.03); pointer-events: none; white-space: nowrap; }
                                  </style>
                                </head>
                                <body>
                                  <div class="paper">
                                    <div class="watermark">NEXUS ARCHIVE</div>
                                    <div class="header"><div class="title">${title}</div></div>
                                    <div class="meta">
                                      <span>科目: 1002 银行存款</span>
                                      <span>期间: ${viewRow.year || '2023'}年</span>
                                      <span>币种: 人民币</span>
                                    </div>
                                    <table class="ledger-table">
                                      <thead>
                                        <tr>
                                          <th width="15%">日期</th>
                                          <th width="15%">凭证号</th>
                                          <th width="30%">摘要</th>
                                          <th width="15%">借方</th>
                                          <th width="15%">贷方</th>
                                          <th width="10%">方向</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        <tr><td>2023-01-01</td><td>-</td><td>期初余额</td><td></td><td></td><td>借</td></tr>
                                        <tr><td>2023-01-15</td><td>记-001</td><td>收到货款</td><td class="amount">50,000.00</td><td></td><td>借</td></tr>
                                        <tr><td>2023-01-20</td><td>记-005</td><td>支付采购款</td><td></td><td class="amount">20,000.00</td><td>借</td></tr>
                                        <tr><td>2023-01-31</td><td>-</td><td>本月合计</td><td class="amount">50,000.00</td><td class="amount">20,000.00</td><td>借</td></tr>
                                      </tbody>
                                    </table>
                                  </div>
                                </body>
                              </html>`;
                          }
                          // 2. Financial Reports Template
                          else if (subTitle === '财务报告') {
                            const title = viewRow.title || viewRow.name || '资产负债表';
                            const isIncomeStatement = title.includes('利润') || title.includes('损益');

                            if (isIncomeStatement) {
                              // Income Statement Template
                              content = `
                                <!DOCTYPE html>
                                <html>
                                  <head>
                                    <style>
                                      body { margin: 0; padding: 20px; font-family: "SimSun", "Songti SC", serif; background: #f1f5f9; display: flex; justify-content: center; }
                                      .paper { width: 210mm; min-height: 297mm; background: white; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); padding: 40px; position: relative; box-sizing: border-box; }
                                      .header { text-align: center; margin-bottom: 30px; }
                                      .title { font-size: 26px; font-weight: bold; color: #0f172a; margin-bottom: 10px; }
                                      .subtitle { font-size: 14px; color: #64748b; }
                                      .report-table { width: 100%; border-collapse: collapse; font-size: 13px; border: 2px solid #0f172a; }
                                      .report-table th, .report-table td { border: 1px solid #cbd5e1; padding: 8px; }
                                      .report-table th { background: #e2e8f0; font-weight: bold; text-align: center; }
                                      .section-header { background: #f8fafc; font-weight: bold; }
                                      .amount { font-family: "Courier New", monospace; text-align: right; }
                                      .watermark { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 80px; color: rgba(0,0,0,0.03); pointer-events: none; }
                                    </style>
                                  </head>
                                  <body>
                                    <div class="paper">
                                      <div class="watermark">CONFIDENTIAL</div>
                                      <div class="header">
                                        <div class="title">${title}</div>
                                        <div class="subtitle">编制单位：Nexus 集团 &nbsp;&nbsp; 日期：${viewRow.date || '2023-12-31'} &nbsp;&nbsp; 单位：元</div>
                                      </div>
                                      <table class="report-table">
                                        <thead>
                                          <tr>
                                            <th width="40%">项目</th>
                                            <th width="10%">行次</th>
                                            <th width="25%">本期金额</th>
                                            <th width="25%">上期金额</th>
                                          </tr>
                                        </thead>
                                        <tbody>
                                          <tr class="section-header"><td colspan="4">一、营业收入</td></tr>
                                          <tr><td>其中：主营业务收入</td><td style="text-align:center">1</td><td class="amount">1,500,000.00</td><td class="amount">1,200,000.00</td></tr>
                                          <tr class="section-header"><td colspan="4">二、营业成本</td></tr>
                                          <tr><td>其中：主营业务成本</td><td style="text-align:center">2</td><td class="amount">800,000.00</td><td class="amount">600,000.00</td></tr>
                                          <tr><td>税金及附加</td><td style="text-align:center">3</td><td class="amount">50,000.00</td><td class="amount">40,000.00</td></tr>
                                          <tr style="font-weight:bold; background:#f1f5f9"><td>三、营业利润</td><td style="text-align:center">10</td><td class="amount">650,000.00</td><td class="amount">560,000.00</td></tr>
                                          <tr><td>加：营业外收入</td><td style="text-align:center">11</td><td class="amount">10,000.00</td><td class="amount">5,000.00</td></tr>
                                          <tr style="font-weight:bold; background:#f1f5f9"><td>四、利润总额</td><td style="text-align:center">15</td><td class="amount">660,000.00</td><td class="amount">565,000.00</td></tr>
                                          <tr><td>减：所得税费用</td><td style="text-align:center">16</td><td class="amount">165,000.00</td><td class="amount">141,250.00</td></tr>
                                          <tr style="font-weight:bold; background:#e2e8f0"><td>五、净利润</td><td style="text-align:center">20</td><td class="amount">495,000.00</td><td class="amount">423,750.00</td></tr>
                                        </tbody>
                                      </table>
                                      <div style="margin-top:40px; display:flex; justify-content:space-between; font-size:14px;">
                                        <span>企业负责人：(签章)</span>
                                        <span>财务负责人：(签章)</span>
                                        <span>制表人：系统自动</span>
                                      </div>
                                    </div>
                                  </body>
                                </html>`;
                            } else {
                              // Balance Sheet Template (Default)
                              content = `
                                <!DOCTYPE html>
                                <html>
                                  <head>
                                    <style>
                                      body { margin: 0; padding: 20px; font-family: "SimSun", "Songti SC", serif; background: #f1f5f9; display: flex; justify-content: center; }
                                      .paper { width: 210mm; min-height: 297mm; background: white; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); padding: 40px; position: relative; box-sizing: border-box; }
                                      .header { text-align: center; margin-bottom: 30px; }
                                      .title { font-size: 26px; font-weight: bold; color: #0f172a; margin-bottom: 10px; }
                                      .subtitle { font-size: 14px; color: #64748b; }
                                      .report-table { width: 100%; border-collapse: collapse; font-size: 13px; border: 2px solid #0f172a; }
                                      .report-table th, .report-table td { border: 1px solid #cbd5e1; padding: 8px; }
                                      .report-table th { background: #e2e8f0; font-weight: bold; text-align: center; }
                                      .section-header { background: #f8fafc; font-weight: bold; }
                                      .amount { font-family: "Courier New", monospace; text-align: right; }
                                      .watermark { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 80px; color: rgba(0,0,0,0.03); pointer-events: none; }
                                    </style>
                                  </head>
                                  <body>
                                    <div class="paper">
                                      <div class="watermark">CONFIDENTIAL</div>
                                      <div class="header">
                                        <div class="title">${title}</div>
                                        <div class="subtitle">编制单位：Nexus 集团 &nbsp;&nbsp; 日期：${viewRow.date || '2023-12-31'} &nbsp;&nbsp; 单位：元</div>
                                      </div>
                                      <table class="report-table">
                                        <thead>
                                          <tr>
                                            <th width="40%">资产</th>
                                            <th width="10%">行次</th>
                                            <th width="25%">期末余额</th>
                                            <th width="25%">年初余额</th>
                                          </tr>
                                        </thead>
                                        <tbody>
                                          <tr class="section-header"><td colspan="4">流动资产：</td></tr>
                                          <tr><td>货币资金</td><td style="text-align:center">1</td><td class="amount">1,250,000.00</td><td class="amount">1,000,000.00</td></tr>
                                          <tr><td>应收账款</td><td style="text-align:center">2</td><td class="amount">450,000.00</td><td class="amount">320,000.00</td></tr>
                                          <tr><td>存货</td><td style="text-align:center">3</td><td class="amount">890,000.00</td><td class="amount">750,000.00</td></tr>
                                          <tr style="font-weight:bold; background:#f1f5f9"><td>流动资产合计</td><td style="text-align:center">10</td><td class="amount">2,590,000.00</td><td class="amount">2,070,000.00</td></tr>
                                        </tbody>
                                      </table>
                                      <div style="margin-top:40px; display:flex; justify-content:space-between; font-size:14px;">
                                        <span>企业负责人：(签章)</span>
                                        <span>财务负责人：(签章)</span>
                                        <span>制表人：系统自动</span>
                                      </div>
                                    </div>
                                  </body>
                                </html>`;
                            }
                          }
                          // 3. Default: Vouchers / Invoices
                          else {
                            const isInvoice = viewRow.type === 'invoice' || (viewRow.name && viewRow.name.includes('发票'));
                            const title = isInvoice ? '电子发票（增值税普通发票）' : '记账凭证';
                            const color = isInvoice ? '#3b82f6' : '#ef4444';

                            content = `
                              <!DOCTYPE html>
                              <html>
                                <head>
                                  <style>
                                    body { margin: 0; padding: 20px; font-family: "SimSun", "Songti SC", serif; background: #f1f5f9; display: flex; justify-content: center; }
                                    .paper {
                                      width: 210mm;
                                      min-height: 140mm; /* Half A4 for voucher */
                                      background: white;
                                      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                                      padding: 40px;
                                      position: relative;
                                      box-sizing: border-box;
                                    }
                                    .header { text-align: center; border-bottom: 2px solid ${color}; padding-bottom: 20px; margin-bottom: 30px; }
                                    .title { font-size: 24px; font-weight: bold; color: ${color}; letter-spacing: 4px; }
                                    .meta { display: flex; justify-content: space-between; margin-bottom: 20px; color: #64748b; font-size: 14px; }
                                    .content-table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
                                    .content-table th, .content-table td { border: 1px solid #e2e8f0; padding: 12px; text-align: left; }
                                    .content-table th { background: #f8fafc; font-weight: bold; width: 100px; }
                                    .amount { font-family: "Courier New", monospace; font-weight: bold; font-size: 18px; }
                                    .stamp {
                                      position: absolute;
                                      bottom: 40px;
                                      right: 60px;
                                      width: 120px;
                                      height: 120px;
                                      border: 3px solid #ef4444;
                                      border-radius: 50%;
                                      color: #ef4444;
                                      display: flex;
                                      align-items: center;
                                      justify-content: center;
                                      font-size: 14px;
                                      font-weight: bold;
                                      transform: rotate(-15deg);
                                      opacity: 0.8;
                                      pointer-events: none;
                                    }
                                    .watermark {
                                      position: absolute;
                                      top: 50%;
                                      left: 50%;
                                      transform: translate(-50%, -50%) rotate(-45deg);
                                      font-size: 80px;
                                      color: rgba(0,0,0,0.03);
                                      pointer-events: none;
                                      white-space: nowrap;
                                    }
                                  </style>
                                </head>
                                <body>
                                  <div class="paper">
                                    <div class="watermark">NEXUS ARCHIVE</div>
                                    <div class="header">
                                      <div class="title">${title}</div>
                                      <div style="margin-top: 10px; font-size: 12px; color: #94a3b8;">E-ARCHIVE ORIGINAL COPY</div>
                                    </div>
                                    
                                    <div class="meta">
                                      <span>凭证编号: <b>${viewRow.code || viewRow.id}</b></span>
                                      <span>日期: ${viewRow.date || new Date().toISOString().split('T')[0]}</span>
                                    </div>

                                    <table class="content-table">
                                      <tr>
                                        <th>摘要</th>
                                        <td colspan="3">${viewRow.name || '标准业务记账'}</td>
                                      </tr>
                                      <tr>
                                        <th>会计主体</th>
                                        <td>${viewRow.entity || '总公司'}</td>
                                        <th>凭证类型</th>
                                        <td>${viewRow.type === 'invoice' ? '原始凭证' : '记账凭证'}</td>
                                      </tr>
                                      <tr>
                                        <th>金额</th>
                                        <td colspan="3" class="amount">${viewRow.amount || '¥ 0.00'}</td>
                                      </tr>
                                      <tr>
                                        <th>备注</th>
                                        <td colspan="3" style="color: #94a3b8; font-style: italic;">
                                          本凭证已通过四性检测（真实性、完整性、可用性、安全性）。
                                          <br/>档号: ${viewRow.archivalCode || '待归档'}
                                        </td>
                                      </tr>
                                    </table>

                                    <div style="display: flex; justify-content: space-between; margin-top: 40px; padding-top: 20px; border-top: 1px dashed #cbd5e1;">
                                      <span>制单人: 系统自动</span>
                                      <span>审核人: 财务主管</span>
                                      <span>记账人: 结算中心</span>
                                    </div>

                                    <div class="stamp">
                                      <div style="text-align: center; line-height: 1.4;">
                                        财务专用章<br/>
                                        <span style="font-size: 10px">VALIDATED</span>
                                      </div>
                                    </div>
                                  </div>
                                </body>
                              </html>`;
                          }

                          return URL.createObjectURL(new Blob([content], { type: 'text/html' }));
                        })() : ''}
                        className="w-full h-full relative z-10 bg-white"
                        title="File Preview"
                      />
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        )
      }

      {/* Re-including Add Modal and Linking Modal logic to ensure file integrity */}
      {
        isAddModalOpen && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                <h3 className="text-lg font-bold text-slate-800">新增记录</h3>
                <button onClick={() => setIsAddModalOpen(false)}><X size={20} className="text-slate-400" /></button>
              </div>
              <form onSubmit={handleAddSubmit} className="p-6 space-y-4">
                {config.columns.filter(c => c.type !== 'status' && c.type !== 'progress' && c.type !== 'action').map(col => (
                  <div key={col.key}><label className="text-sm font-medium">{col.header}</label><input className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm" value={newRowData[col.key] || ''} onChange={(e) => setNewRowData({ ...newRowData, [col.key]: e.target.value })} /></div>
                ))}
                <div className="pt-4 flex justify-end gap-2"><button type="button" onClick={() => setIsAddModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button><button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg">确认</button></div>
              </form>
            </div>
          </div>
        )
      }

      {
        isLinkModalOpen && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl border border-slate-100 flex flex-col max-h-[90vh]">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                <h3 className="text-lg font-bold text-slate-800">手动凭证关联</h3>
                <button onClick={() => setIsLinkModalOpen(false)}><X size={20} className="text-slate-400" /></button>
              </div>
              <div className="p-6 overflow-y-auto flex-1 space-y-2">
                <div className="bg-amber-50 border border-amber-100 rounded-lg p-3 mb-4 text-xs text-amber-800">注意：手动关联将覆盖自动结果。</div>
                {MOCK_CANDIDATES.map(c => (
                  <div key={c.id} onClick={() => toggleCandidateSelection(c.id)} className={`flex justify-between p-3 rounded-xl border cursor-pointer ${selectedCandidates.includes(c.id) ? 'border-primary-500 bg-primary-50' : 'border-slate-200'}`}>
                    <div><p className="font-bold">{c.name}</p><p className="text-xs text-slate-500">{c.code}</p></div>
                    <div className="text-right"><span className={`font-bold ${c.score > 90 ? 'text-emerald-600' : 'text-amber-600'}`}>{c.score}%</span></div>
                  </div>
                ))}
              </div>
              <div className="p-6 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                <button onClick={() => setIsLinkModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button>
                <button onClick={handleLinkConfirm} disabled={selectedCandidates.length === 0} className="px-6 py-2 bg-primary-600 text-white rounded-lg disabled:opacity-50">确认关联</button>
              </div>
            </div>
          </div>
        )
      }
    </div >
  );
};
