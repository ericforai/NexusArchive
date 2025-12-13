import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { OfdViewer } from './common/OfdViewer';
import { MetadataEditModal } from './common/MetadataEditModal';
import {
  Search, Filter, Download, Eye, MoreHorizontal, FileText,
  ChevronLeft, ChevronRight, ArrowUpDown, Loader2, X,
  Receipt, ShieldCheck, CheckCircle2, AlertCircle, Layers,
  Upload, FileCheck, Archive, Book,
  Plus, AlertTriangle, Clock, XCircle, Link as LinkIcon, Trash2, Edit,
  Settings2, Play, Zap, BarChart2, Lock, ArrowRight, Package
} from 'lucide-react';
import { ModuleConfig, TableColumn, GenericRow, ViewState } from '../types';
import { poolApi, PoolItem } from '../api/pool';
import { archivesApi } from '../api/archives';
import { RelationshipVisualizer } from './RelationshipVisualizer';
import { DemoBadge } from './common/DemoBadge';
import { client } from '../api/client';
import { adminApi } from '../api/admin';
import { useAuthStore } from '../store';
import { triggerAuditRefresh } from '../utils/audit';
import {
  PRE_ARCHIVE_POOL_CONFIG,
  PRE_ARCHIVE_LINK_CONFIG,
  COLLECTION_ONLINE_CONFIG,
  COLLECTION_SCAN_CONFIG,
  COLLECTION_CONFIG,
  ARCHIVE_VIEW_CONFIG,
  ARCHIVE_BOX_CONFIG,
  ACCOUNTING_VOUCHER_CONFIG,
  ACCOUNTING_LEDGER_CONFIG,
  FINANCIAL_REPORT_CONFIG,
  OTHER_ACCOUNTING_MATERIALS_CONFIG,
  QUERY_CONFIG,
  GENERIC_CONFIG,
} from '../constants';

// 路由配置标识符到配置对象的映射
const ROUTE_CONFIG_MAP: Record<string, { config: ModuleConfig; title: string; subTitle: string }> = {
  'pool': { config: PRE_ARCHIVE_POOL_CONFIG, title: '预归档库', subTitle: '电子凭证池' },
  'link': { config: PRE_ARCHIVE_LINK_CONFIG, title: '预归档库', subTitle: '凭证关联' },
  'collection': { config: COLLECTION_CONFIG, title: '资料收集', subTitle: '概览' },
  'online': { config: COLLECTION_ONLINE_CONFIG, title: '资料收集', subTitle: '在线接收' },
  'scan': { config: COLLECTION_SCAN_CONFIG, title: '资料收集', subTitle: '扫描集成' },
  'view': { config: ARCHIVE_VIEW_CONFIG, title: '档案管理', subTitle: '归档查看' },
  'voucher': { config: ACCOUNTING_VOUCHER_CONFIG, title: '档案管理', subTitle: '会计凭证' },
  'ledger': { config: ACCOUNTING_LEDGER_CONFIG, title: '档案管理', subTitle: '会计账簿' },
  'report': { config: FINANCIAL_REPORT_CONFIG, title: '档案管理', subTitle: '财务报告' },
  'other': { config: OTHER_ACCOUNTING_MATERIALS_CONFIG, title: '档案管理', subTitle: '其他会计资料' },
  'box': { config: ARCHIVE_BOX_CONFIG, title: '档案管理', subTitle: '档案装盒' },
  'query': { config: QUERY_CONFIG, title: '档案查询', subTitle: '全文检索' },
};

interface ArchiveListViewProps {
  // 传统模式（向后兼容）
  title?: string;
  subTitle?: string;
  config?: ModuleConfig;
  onNavigate?: (view: ViewState, subItem?: string, resourceId?: string) => void;
  // 路由模式
  routeConfig?: string;
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

const CATEGORY_LABELS: Record<string, string> = {
  AC01: '会计凭证',
  AC02: '会计账簿',
  AC03: '财务报告',
  AC04: '其他会计资料'
};

const STATUS_LABELS: Record<string, string> = {
  draft: '草稿',
  pending: '待归档',
  archived: '已归档'
};

// 预归档状态标签（根据法规要求）
const PRE_ARCHIVE_STATUS_LABELS: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
  PENDING_CHECK: { label: '待检测', color: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400', icon: <Clock size={14} /> },
  CHECK_FAILED: { label: '检测失败', color: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400', icon: <XCircle size={14} /> },
  PENDING_METADATA: { label: '待补录', color: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400', icon: <FileText size={14} /> },
  PENDING_ARCHIVE: { label: '待归档', color: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400', icon: <Package size={14} /> },
  PENDING_APPROVAL: { label: '归档审批中', color: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400', icon: <Clock size={14} /> },
  ARCHIVED: { label: '已归档', color: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400', icon: <CheckCircle2 size={14} /> },
};

const resolveCategoryLabel = (code?: string) => {
  if (!code) return '档案';
  return CATEGORY_LABELS[code] || code;
};

const formatStatus = (status?: string) => {
  if (!status) return '-';
  return STATUS_LABELS[status] || status;
};

const mapArchiveToRow = (archive: any, subTitle: string): GenericRow => {
  const categoryLabel = resolveCategoryLabel(archive?.categoryCode);
  const baseDate = archive?.docDate || archive?.createdAt || archive?.createdTime || '';
  const date = baseDate ? String(baseDate).split('T')[0] : '';
  const statusText = formatStatus(archive?.status);

  if (subTitle === '会计凭证' || subTitle === '凭证关联') {
    const amountValue = archive?.amount;
    const amount = typeof amountValue === 'number'
      ? `¥ ${amountValue.toFixed(2)}`
      : amountValue || '-';

    return {
      id: archive?.id,
      code: archive?.archiveCode,
      voucherNo: archive?.archiveCode,
      archivalCode: archive?.archiveCode,
      entity: archive?.orgName,
      period: archive?.fiscalPeriod || archive?.fiscalYear || '',
      subject: archive?.title,
      type: categoryLabel,
      amount,
      date,
      status: statusText
    };
  }

  if (subTitle === '会计账簿') {
    return {
      id: archive?.id,
      code: archive?.archiveCode,
      ledgerNo: archive?.archiveCode,
      archivalCode: archive?.archiveCode,
      type: categoryLabel,
      entity: archive?.orgName,
      year: archive?.fiscalYear,
      period: archive?.fiscalPeriod || '全年',
      subject: archive?.title,
      pageCount: archive?.pageCount || archive?.customMetadata?.pageCount || '-',
      status: statusText
    };
  }

  if (subTitle === '财务报告') {
    return {
      id: archive?.id,
      code: archive?.archiveCode,
      reportNo: archive?.archiveCode,
      archivalCode: archive?.archiveCode,
      type: categoryLabel,
      year: archive?.fiscalYear,
      unit: archive?.orgName,
      title: archive?.title,
      period: archive?.fiscalPeriod || '',
      date,
      status: statusText
    };
  }

  return {
    id: archive?.id,
    code: archive?.archiveCode,
    archiveNo: archive?.fondsNo,
    archivalCode: archive?.archiveCode,
    category: categoryLabel,
    year: archive?.fiscalYear,
    period: archive?.retentionPeriod || archive?.fiscalPeriod,
    title: archive?.title,
    security: archive?.securityLevel || '-',
    status: statusText,
    orgName: archive?.orgName,
    date
  };
};





export const ArchiveListView: React.FC<ArchiveListViewProps> = ({
  title: propTitle,
  subTitle: propSubTitle,
  config: propConfig,
  onNavigate,
  routeConfig
}) => {
  // 解析配置：优先使用 routeConfig，否则使用传入的 props
  const resolvedConfig = routeConfig
    ? ROUTE_CONFIG_MAP[routeConfig]
    : { config: propConfig, title: propTitle, subTitle: propSubTitle };

  const title = resolvedConfig?.title || propTitle || '档案列表';
  const subTitle = resolvedConfig?.subTitle || propSubTitle || '';
  const config = resolvedConfig?.config || propConfig || GENERIC_CONFIG;

  // State for Real CRUD
  const [localData, setLocalData] = useState<GenericRow[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [orgOptions, setOrgOptions] = useState<{ label: string; value: string }[]>([]);
  const [searchTerm, setSearchTerm] = useState('');

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
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [orgFilter, setOrgFilter] = useState<string>('');
  const [pageInfo, setPageInfo] = useState({ total: 0, page: 1, pageSize: 10 });
  const [currentPage, setCurrentPage] = useState(1);

  const resolveCategoryCode = useCallback(() => {
    switch (subTitle) {
      case '会计凭证':
      case '凭证关联':
        return 'AC01';
      case '会计账簿':
        return 'AC02';
      case '财务报告':
        return 'AC03';
      case '其他会计资料':
        return 'AC04';
      default:
        return undefined;
    }
  }, [subTitle]);

  const resolveDefaultStatus = useCallback(() => {
    if (subTitle === '凭证关联') return 'draft';
    return undefined;
  }, [subTitle]);

  const isPoolView = subTitle === '电子凭证池';

  // Pool Status Filter State
  const [poolStatusFilter, setPoolStatusFilter] = useState<'all' | 'PENDING_CHECK' | 'CHECK_FAILED' | 'PENDING_METADATA' | 'PENDING_ARCHIVE' | 'PENDING_APPROVAL' | 'ARCHIVED' | null>(null);
  const [poolStatusStats, setPoolStatusStats] = useState<Record<string, number>>({});

  // Metadata Edit Modal State
  const [isMetadataEditOpen, setIsMetadataEditOpen] = useState(false);
  const [editingFile, setEditingFile] = useState<{ id: string; fileName: string } | null>(null);

  const openMetadataEdit = (row: GenericRow) => {
    setEditingFile({ id: row.id, fileName: row.fileName || row.code || '' });
    setIsMetadataEditOpen(true);
  };

  // Load pool status statistics
  const loadPoolStatusStats = useCallback(async () => {
    try {
      const response = await client.get('/pool/stats/status');
      if (response.data.code === 200) {
        setPoolStatusStats(response.data.data || {});
      }
    } catch (error) {
      console.error('Failed to load pool status stats:', error);
    }
  }, []);

  // Load stats when pool view is active
  useEffect(() => {
    if (isPoolView) {
      loadPoolStatusStats();
    }
  }, [isPoolView, loadPoolStatusStats]);

  const loadPoolData = useCallback(async (page = currentPage) => {
    setIsLoading(true);
    setErrorMessage(null);
    setSelectedRows([]);
    try {
      const poolItems = await poolApi.getList();
      let filtered = poolItems.filter((item) => {
        // Filter by status if selected
        if (statusFilter) {
          const itemStatus = (item as any).status || 'PENDING_CHECK';
          if (itemStatus !== statusFilter) return false;
        }
        // Filter by search term
        if (!searchTerm) return true;
        return Object.values(item).some((val) =>
          String(val || '').toLowerCase().includes(searchTerm.toLowerCase())
        );
      });
      const total = filtered.length;
      const start = (page - 1) * pageInfo.pageSize;
      const paged = filtered.slice(start, start + pageInfo.pageSize);
      setLocalData(paged as GenericRow[]);
      setPageInfo((prev) => ({ ...prev, total, page }));
      // Refresh stats after data load
      loadPoolStatusStats();
    } catch (error) {
      console.error('Failed to load pool data:', error);
      setErrorMessage('加载数据失败');
      showToast('加载数据失败', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageInfo.pageSize, searchTerm, poolStatusFilter, loadPoolStatusStats]);

  const loadArchiveList = useCallback(async (page = currentPage) => {
    setIsLoading(true);
    setErrorMessage(null);
    setSelectedRows([]);
    try {
      const result = await archivesApi.getArchives({
        page,
        limit: pageInfo.pageSize,
        search: searchTerm || undefined,
        status: statusFilter || resolveDefaultStatus(),
        categoryCode: resolveCategoryCode(),
        orgId: orgFilter || undefined
      });

      if (result.code !== 200 || !result.data) {
        throw new Error(result.message || '加载档案数据失败');
      }

      const pageResult: any = result.data;
      const { records = [], total = 0, size = pageInfo.pageSize, current = page } = pageResult;
      const mappedItems = (records as any[]).map((item) => mapArchiveToRow(item, subTitle || title));
      setLocalData(mappedItems);
      setPageInfo({ total, page: current || page, pageSize: size || pageInfo.pageSize });
      setCurrentPage(current || page);
    } catch (error) {
      console.error('Failed to load archives:', error);
      setErrorMessage(error instanceof Error ? error.message : '加载档案数据失败');
      showToast('加载档案数据失败', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageInfo.pageSize, searchTerm, statusFilter, resolveDefaultStatus, resolveCategoryCode, orgFilter, subTitle, title]);

  const loadCurrentView = useCallback((page = currentPage) => {
    if (isPoolView) {
      return loadPoolData(page);
    }
    return loadArchiveList(page);
  }, [isPoolView, loadPoolData, loadArchiveList, currentPage]);

  // Initial and reactive loading
  useEffect(() => {
    setCurrentPage(1);
  }, [subTitle, searchTerm, statusFilter, orgFilter, resolveCategoryCode, resolveDefaultStatus]);

  useEffect(() => {
    loadCurrentView(currentPage);
  }, [loadCurrentView, currentPage]);

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

  // Attachment Preview State
  const [relatedFiles, setRelatedFiles] = useState<PoolItem[]>([]);
  const [activePreviewId, setActivePreviewId] = useState<string | null>(null);
  const [activeDetailTab, setActiveDetailTab] = useState<'main' | 'attachments'>('main');

  // Load related files when viewing details
  useEffect(() => {
    if (isViewModalOpen && viewRow) {
      setActivePreviewId(viewRow.id);
      setActiveDetailTab('main'); // Reset to main tab
      poolApi.getRelatedFiles(viewRow.id).then(files => {
        setRelatedFiles(files);
      });
    } else {
      setRelatedFiles([]);
      setActivePreviewId(null);
    }
  }, [isViewModalOpen, viewRow]);

  // Toast Notification State
  const [toast, setToast] = useState<{ visible: boolean; message: string; type: 'success' | 'error' }>({ visible: false, message: '', type: 'success' });

  // Export State
  const [isExporting, setIsExporting] = useState<string | null>(null);

  // Archive Confirmation Modal State
  const [isArchiveConfirmOpen, setIsArchiveConfirmOpen] = useState(false);

  // Demo Mode State
  const [demoMode, setDemoMode] = useState(true);

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
      triggerAuditRefresh();
    } catch (error) {
      console.error('导出失败:', error);
      showToast('导出失败: ' + (error instanceof Error ? error.message : '请检查档案包是否存在'), 'error');
    } finally {
      setIsExporting(null);
    }
  };

  // Formal Archiving Handler
  const [isArchiving, setIsArchiving] = useState(false);

  const handleFormalArchiving = () => {
    if (selectedRows.length === 0) {
      showToast('请先选择要归档的凭证', 'error');
      return;
    }
    // Open confirmation modal instead of native confirm()
    setIsArchiveConfirmOpen(true);
  };

  const executeArchiving = async () => {
    console.log('executeArchiving started, isArchiveConfirmOpen:', isArchiveConfirmOpen);
    setIsArchiveConfirmOpen(false);

    setIsArchiving(true);
    try {
      console.log('Calling poolApi.archiveItems with:', selectedRows);
      await poolApi.archiveItems(selectedRows);
      console.log('poolApi.archiveItems returned success');
      showToast(`成功归档 ${selectedRows.length} 条凭证`);
      setSelectedRows([]);
      // Reload pool data
      loadPoolData();
    } catch (error) {
      console.error('归档失败 detailed error:', error);
      showToast('归档失败: ' + (error instanceof Error ? error.message : '未知错误'), 'error');
    } finally {
      setIsArchiving(false);
      console.log('executeArchiving finished');
    }
  };

  const handleDelete = async (id: string) => {
    if (!id) return;
    if (isPoolView) {
      showToast('请通过凭证池接口处理删除或归档', 'error');
      return;
    }
    if (!confirm('确定要删除这条记录吗？')) return;

    try {
      const res = await archivesApi.deleteArchive(id);
      if (res.code !== 200) {
        throw new Error(res.message || '删除失败');
      }
      showToast('记录已删除');
      loadCurrentView(currentPage);
    } catch (error) {
      console.error('删除失败', error);
      showToast('删除失败: ' + (error instanceof Error ? error.message : '未知错误'), 'error');
    }
  };

  const handleBatchDelete = async () => {
    if (selectedRows.length === 0) {
      alert('请先选择要删除的记录');
      return;
    }
    if (isPoolView) {
      showToast('凭证池删除请通过后端接口处理', 'error');
      return;
    }
    if (!confirm(`确定要删除选中的 ${selectedRows.length} 条记录吗？`)) return;

    try {
      await Promise.all(selectedRows.map(async (id) => {
        const res = await archivesApi.deleteArchive(id);
        if (res.code !== 200) {
          throw new Error(res.message || '删除失败');
        }
      }));
      showToast(`已成功删除 ${selectedRows.length} 条记录`);
      setSelectedRows([]);
      loadCurrentView(currentPage);
    } catch (error) {
      console.error('批量删除失败', error);
      showToast('批量删除失败', 'error');
    }
  };

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const now = new Date();
    const categoryCode = resolveCategoryCode() || 'AC01';
    const archiveCode = (newRowData as any).archivalCode || (newRowData as any).archiveCode || (newRowData as any).voucherNo || (newRowData as any).code || `ARC-${now.getTime()}`;
    const payload = {
      fondsNo: (newRowData as any).fondsNo || 'COMP001',
      archiveCode,
      categoryCode,
      title: (newRowData as any).title || (newRowData as any).subject || '新建档案',
      fiscalYear: (newRowData as any).fiscalYear || String(now.getFullYear()),
      fiscalPeriod: (newRowData as any).period || (newRowData as any).fiscalPeriod || '',
      retentionPeriod: (newRowData as any).retentionPeriod || '10Y',
      orgName: (newRowData as any).entity || (newRowData as any).orgName || '默认组织',
      status: resolveDefaultStatus() || 'draft'
    };

    try {
      const res = await archivesApi.createArchive(payload);
      if (res.code !== 200 || !res.data) {
        throw new Error(res.message || '新增失败');
      }
      showToast('新增记录成功');
      setIsAddModalOpen(false);
      setNewRowData({});
      setCurrentPage(1);
      loadCurrentView(1);
    } catch (error) {
      console.error('新增失败', error);
      showToast('新增失败: ' + (error instanceof Error ? error.message : '未知错误'), 'error');
    }
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
  const itemsPerPage = pageInfo.pageSize;
  const totalPages = Math.ceil(pageInfo.total / itemsPerPage) || 1;

  const renderCell = (row: GenericRow, column: TableColumn) => {
    const value = row[column.key];
    if (column.key === 'invoiceCount') {
      console.log('Rendering invoiceCount:', value, 'for row:', row.id);
    }

    if (column.type === 'status') {
      // 优先使用预定义的状态映射 (PRE_ARCHIVE_STATUS_LABELS)
      const exactStatus = String(value);
      if (PRE_ARCHIVE_STATUS_LABELS[exactStatus]) {
        const { label, color, icon } = PRE_ARCHIVE_STATUS_LABELS[exactStatus];
        return <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${color}`}>{icon}{label}</span>;
      }

      let colorClass = 'bg-slate-100 text-slate-600 border-slate-200';
      let Icon = null;
      const statusLower = String(value).toLowerCase();

      // Chinese Status Mappings (Existing logic for other statuses)
      if (['已完成', '已归档', '已通过', '已归还', '正常', '机密', '已关联', '已审核', '已记账', '匹配成功', '打开', '通风中'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-emerald-50 text-emerald-700 border-emerald-100';
        Icon = CheckCircle2;
      } else if (['处理中', '待处理', '待审批', '查看', '激活', '闭合', '草稿', '待归档'].some(s => statusLower.includes(s))) {
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
      const exportCode = !isPoolView ? (row.archivalCode || row.code) : null;
      const fileStatus = (row as any).status || '';
      return (
        <div className="flex gap-1">
          {subTitle === '凭证关联' && (
            <button onClick={(e) => { e.stopPropagation(); openLinkModal(row); }} className="text-slate-400 hover:text-primary-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="关联单据"><LinkIcon size={16} /></button>
          )}
          <button onClick={(e) => { e.stopPropagation(); setViewRow(row); setIsViewModalOpen(true); }} className="text-slate-400 hover:text-primary-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="查看"><Eye size={16} /></button>

          {/* 元数据补录按钮 - 仅对待补录状态显示 */}
          {isPoolView && fileStatus === 'PENDING_METADATA' && (
            <button
              onClick={(e) => { e.stopPropagation(); openMetadataEdit(row); }}
              className="text-slate-400 hover:text-blue-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100"
              title="补录元数据"
            >
              <Edit size={16} />
            </button>
          )}

          {!isPoolView && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                if (onNavigate && row.id) {
                  onNavigate(ViewState.COMPLIANCE_REPORT, subTitle, row.id);
                }
              }}
              className="text-slate-400 hover:text-indigo-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100"
              title="合规性检查"
            >
              <ShieldCheck size={16} />
            </button>
          )}

          {exportCode && (
            <button
              onClick={(e) => { e.stopPropagation(); handleAipExport(row); }}
              className="text-slate-400 hover:text-emerald-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100 disabled:opacity-50 disabled:cursor-not-allowed"
              title="导出 AIP 包"
              disabled={isExporting === exportCode}
            >
              {isExporting === exportCode ? <Loader2 size={16} className="animate-spin" /> : <Package size={16} />}
            </button>
          )}
          {!isPoolView && (
            <button onClick={(e) => { e.stopPropagation(); handleDelete(row.id) }} className="text-slate-400 hover:text-rose-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="删除"><Trash2 size={16} /></button>
          )}
        </div>
      )
    }

    // 格式化文件类型显示 (MIME type -> 用户友好名称)
    if (column.key === 'type') {
      const mimeTypeMapping: Record<string, string> = {
        'application/pdf': 'PDF',
        'application/json': 'JSON',
        'application/ofd': 'OFD',
        'application/xml': 'XML',
        'text/plain': 'TXT',
        'image/jpeg': 'JPG',
        'image/png': 'PNG',
      };
      const displayValue = mimeTypeMapping[String(value).toLowerCase()] || value;
      return <span className="text-slate-700 font-medium">{displayValue}</span>;
    }

    return <span className="text-slate-700 font-medium">{value}</span>;
  };

  return (
    <div className="p-8 space-y-6 max-w-[1600px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500 relative">

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
                <DemoBadge text="关联候选为演示数据，接入真实关联接口后可关闭演示模式。" />
                {demoMode ? (
                  MOCK_CANDIDATES.map(c => (
                    <div key={c.id} onClick={() => toggleCandidateSelection(c.id)} className={`flex justify-between p-3 rounded-xl border cursor-pointer ${selectedCandidates.includes(c.id) ? 'border-primary-500 bg-primary-50' : 'border-slate-200'}`}>
                      <div><p className="font-bold">{c.name}</p><p className="text-xs text-slate-500">{c.code}</p></div>
                      <div className="text-right"><span className={`font-bold ${c.score > 90 ? 'text-emerald-600' : 'text-amber-600'}`}>{c.score}%</span></div>
                    </div>
                  ))
                ) : (
                  <div className="text-sm text-slate-500">当前为生产模式，未接入关联候选接口。</div>
                )}
              </div>
              <div className="p-6 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                <button onClick={() => setIsLinkModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button>
                <button onClick={handleLinkConfirm} disabled={!demoMode || selectedCandidates.length === 0} className="px-6 py-2 bg-primary-600 text-white rounded-lg disabled:opacity-50">确认关联</button>
              </div>
            </div>
          </div>
        )
      }
      {/* Metadata Edit Modal */}
      {
        editingFile && (
          <MetadataEditModal
            isOpen={isMetadataEditOpen}
            onClose={() => { setIsMetadataEditOpen(false); setEditingFile(null); }}
            fileId={editingFile.id}
            fileName={editingFile.fileName}
            onSuccess={() => {
              loadPoolData();
              loadPoolStatusStats();
              showToast('元数据更新成功', 'success');
            }}
          />
        )
      }
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

      {/* Archive Confirmation Modal (Portal) */}
      {isArchiveConfirmOpen && createPortal(
        <div className="fixed inset-0 z-[100] flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setIsArchiveConfirmOpen(false)} />
          <div className="relative bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 p-6 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-emerald-100 dark:bg-emerald-900/30 rounded-full">
                <Layers className="w-6 h-6 text-emerald-600 dark:text-emerald-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-800 dark:text-white">确认归档</h3>
                <p className="text-sm text-slate-500 dark:text-slate-400">此操作不可撤销</p>
              </div>
            </div>
            <p className="text-slate-600 dark:text-slate-300 mb-6">
              确定要将选中的 <span className="font-bold text-emerald-600">{selectedRows.length}</span> 条凭证正式归档吗？
              <br />
              <span className="text-sm text-slate-500">归档后将生成 AIP 档案包。</span>
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setIsArchiveConfirmOpen(false)}
                className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors"
              >
                取消
              </button>
              <button
                id="confirm-archive-btn"
                onClick={executeArchiving}
                disabled={isArchiving}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                {isArchiving ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    归档中...
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={16} />
                    确认归档
                  </>
                )}
              </button>
            </div>
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
            <input
              type="text"
              placeholder="搜索..."
              className="w-full px-3 py-2 text-sm outline-none bg-transparent"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <button onClick={() => setIsFilterOpen(!isFilterOpen)} className={`px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95 ${isFilterOpen ? 'bg-slate-100 text-primary-600 border-primary-200' : ''}`}>
            <Filter size={16} className="mr-2" /> 筛选
          </button>
          {isFilterOpen && (
            <select
              className="border border-slate-200 rounded px-2 py-2 text-sm"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">全部状态</option>
              {Object.entries(PRE_ARCHIVE_STATUS_LABELS).map(([key, { label }]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
          )}
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
              {/* 正式归档按钮已移除，使用提交归档代替 */}
            </>
          )}
          {selectedRows.length > 0 && subTitle === '凭证关联' && (
            <button
              onClick={() => {
                showToast(`已将 ${selectedRows.length} 条记录移交归档`, 'success');
                setSelectedRows([]);
                loadCurrentView(currentPage);
              }}
              className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 flex items-center shadow-lg shadow-green-500/30 transition-all active:scale-95"
            >
              <CheckCircle2 size={16} className="mr-2" /> 移交归档 ({selectedRows.length})
            </button>
          )}
          {/* Pool View: Check and Submit Buttons */}
          {isPoolView && (
            <>
              <button
                onClick={async () => {
                  setIsLoading(true);
                  try {
                    const response = await client.get('/pool/check/all-pending');
                    if (response.data.code === 200) {
                      const reports = response.data.data || [];
                      const passCount = reports.filter((r: any) => r.status === 'PASS').length;
                      const failCount = reports.filter((r: any) => r.status === 'FAIL').length;
                      const warnCount = reports.filter((r: any) => r.status === 'WARNING').length;
                      showToast(`检测完成: 通过 ${passCount}, 失败 ${failCount}, 警告 ${warnCount}`, passCount > 0 ? 'success' : 'error');
                      loadCurrentView(currentPage);
                      loadPoolStatusStats();
                    }
                  } catch (error: any) {
                    showToast('检测失败: ' + (error.message || '未知错误'), 'error');
                  } finally {
                    setIsLoading(false);
                  }
                }}
                className="px-4 py-2 bg-amber-600 text-white rounded-lg text-sm font-medium hover:bg-amber-700 flex items-center shadow-lg shadow-amber-500/30 transition-all active:scale-95"
              >
                <ShieldCheck size={16} className="mr-2" /> 全部检测
              </button>
              {selectedRows.length > 0 && (
                <>
                  <button
                    onClick={async () => {
                      setIsLoading(true);
                      try {
                        const response = await client.post('/pool/check/batch', selectedRows);
                        if (response.data.code === 200) {
                          const reports = response.data.data || [];
                          const passCount = reports.filter((r: any) => r.status === 'PASS').length;
                          showToast(`已检测 ${reports.length} 个文件, 通过 ${passCount} 个`, 'success');
                          loadCurrentView(currentPage);
                          loadPoolStatusStats();
                          setSelectedRows([]);
                        }
                      } catch (error: any) {
                        showToast('批量检测失败', 'error');
                      } finally {
                        setIsLoading(false);
                      }
                    }}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 flex items-center shadow-lg shadow-blue-500/30 transition-all active:scale-95"
                  >
                    <FileCheck size={16} className="mr-2" /> 检测 ({selectedRows.length})
                  </button>
                  <button
                    onClick={async () => {
                      setIsLoading(true);
                      try {
                        const user = useAuthStore.getState().user;
                        const userId = user?.id || 'admin';
                        const userName = user?.fullName || user?.realName || '管理员';
                        const response = await client.post('/pool/submit/batch', {
                          fileIds: selectedRows,
                          applicantId: userId,
                          applicantName: userName,
                          reason: '批量归档申请'
                        });
                        if (response.data.code === 200) {
                          const result = response.data.data;
                          // Handle simple list (backward compatibility if backend not updated) or new BatchOperationResult
                          const successCount = result.successItems ? result.successItems.length : (Array.isArray(result) ? result.length : 0);
                          const failureCount = result.failures ? Object.keys(result.failures).length : 0;

                          if (failureCount === 0 && successCount > 0) {
                            showToast(`已提交 ${successCount} 个归档申请`, 'success');
                          } else if (successCount > 0 && failureCount > 0) {
                            // Partial success
                            // Combine error messages
                            const errorDetails = Object.values(result.failures).join('; ');
                            showToast(`提交部分成功: 成功 ${successCount} 个, 失败 ${failureCount} 个. 详情: ${errorDetails}`, 'error');
                          } else if (successCount === 0 && failureCount > 0) {
                            // All failed
                            const errorDetails = Object.values(result.failures).join('\n');
                            showToast(`提交失败 (${failureCount}个): \n${errorDetails}`, 'error');
                          } else {
                            // No items?
                            showToast('未提交任何申请', 'error');
                          }

                          if (successCount > 0) {
                            loadCurrentView(currentPage);
                            loadPoolStatusStats();
                            setSelectedRows([]);
                          }
                        }
                      } catch (error: any) {
                        showToast('提交归档失败: ' + (error.response?.data?.message || error.message), 'error');
                      } finally {
                        setIsLoading(false);
                      }
                    }}
                    className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 flex items-center shadow-lg shadow-green-500/30 transition-all active:scale-95"
                  >
                    <Archive size={16} className="mr-2" /> 提交归档 ({selectedRows.length})
                  </button>
                </>
              )}
            </>
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
                  setLocalData((prev: GenericRow[]) => prev.map((row: GenericRow) =>
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
          {/* 新增按钮：电子凭证池视图隐藏（合规要求：凭证应从ERP系统采集，不应手工新增） */}
          {!isPoolView && (
            <button onClick={() => setIsAddModalOpen(true)} className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all active:scale-95">
              <Plus size={16} className="mr-2" /> 新增
            </button>
          )}
        </div>
      </div>

      {/* Content Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden flex flex-col h-[calc(100vh-240px)]">
        {/* Table Toolbar */}
        <div className="p-4 border-b border-slate-100 bg-slate-50/30 flex items-center gap-4 shrink-0">
          <span className="text-sm text-slate-500">已选择 <strong className="text-slate-800">{selectedRows.length}</strong> 项</span>
          <div className="h-4 w-px bg-slate-300"></div>
          <button onClick={handleBatchDelete} className="text-sm text-slate-600 hover:text-rose-600 font-medium disabled:opacity-50 disabled:cursor-not-allowed" disabled={selectedRows.length === 0}>批量删除</button>
          <button
            onClick={() => {
              setSearchTerm('');
              setStatusFilter('');
              setOrgFilter('');
              setSelectedRows([]);
              setCurrentPage(1);
              setErrorMessage(null);
            }}
            className="text-sm text-slate-600 hover:text-primary-600 font-medium"
          >
            重置视图
          </button>
        </div>

        {/* Pre-Archive Status Stepper (Pool View Only) */}
        {isPoolView && (
          <div className="mb-6 px-1">
            {/* Stepper Container */}
            <div className="flex items-center justify-between relative">
              {/* Connection Line (Background) */}
              <div className="absolute top-4 left-8 right-8 h-0.5 bg-slate-200 dark:bg-slate-700" />

              {/* "全部" Step */}
              <button
                onClick={() => setPoolStatusFilter(null)}
                className="relative z-10 flex flex-col items-center group"
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center transition-all border-2 ${poolStatusFilter === null
                  ? 'bg-slate-800 border-slate-800 text-white shadow-lg scale-110'
                  : 'bg-white border-slate-300 text-slate-500 hover:border-slate-400 hover:bg-slate-50'
                  }`}>
                  <span className="text-xs font-bold">{Object.values(poolStatusStats).reduce((a: number, b: number) => a + b, 0)}</span>
                </div>
                <span className={`mt-2 text-xs font-medium transition-colors ${poolStatusFilter === null ? 'text-slate-800 dark:text-white' : 'text-slate-500 group-hover:text-slate-700'
                  }`}>全部</span>
              </button>

              {/* Status Steps */}
              {Object.entries(PRE_ARCHIVE_STATUS_LABELS).map(([key, { label, color, icon }], index) => {
                const isActive = poolStatusFilter === key;
                const count = poolStatusStats[key] || 0;
                const isFailedStatus = key === 'CHECK_FAILED';

                return (
                  <button
                    key={key}
                    onClick={() => setPoolStatusFilter(key as any)}
                    className="relative z-10 flex flex-col items-center group"
                  >
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center transition-all border-2 ${isActive
                      ? isFailedStatus
                        ? 'bg-red-500 border-red-500 text-white shadow-lg scale-110'
                        : 'bg-primary-600 border-primary-600 text-white shadow-lg scale-110'
                      : count > 0
                        ? isFailedStatus
                          ? 'bg-red-100 border-red-300 text-red-600 hover:border-red-400'
                          : 'bg-primary-50 border-primary-300 text-primary-600 hover:border-primary-400'
                        : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'
                      }`}>
                      {count > 0 ? (
                        <span className="text-xs font-bold">{count}</span>
                      ) : (
                        <span className="text-xs">-</span>
                      )}
                    </div>
                    <span className={`mt-2 text-xs font-medium transition-colors whitespace-nowrap ${isActive
                      ? isFailedStatus ? 'text-red-600' : 'text-primary-600'
                      : 'text-slate-500 group-hover:text-slate-700'
                      }`}>{label}</span>
                  </button>
                );
              })}
            </div>

            {/* Flow Description */}
            <div className="mt-4 flex items-center justify-center gap-1 text-xs text-slate-400">
              <span>流程：</span>
              <span className="px-1.5 py-0.5 bg-amber-50 text-amber-600 rounded">待检测</span>
              <span>→</span>
              <span className="px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded">待补录</span>
              <span>→</span>
              <span className="px-1.5 py-0.5 bg-purple-50 text-purple-600 rounded">待归档</span>
              <span>→</span>
              <span className="px-1.5 py-0.5 bg-indigo-50 text-indigo-600 rounded">审批中</span>
              <span>→</span>
              <span className="px-1.5 py-0.5 bg-green-50 text-green-600 rounded">已归档</span>
            </div>
          </div>
        )}

        {/* Dynamic Table */}
        <div className="overflow-auto flex-1 relative">
          {errorMessage && !isLoading && (
            <div className="absolute top-4 left-4 right-4 z-0">
              <div className="text-xs text-rose-700 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2 shadow-sm">
                {errorMessage}
              </div>
            </div>
          )}
          {isLoading && (
            <div className="absolute inset-0 bg-white/70 backdrop-blur-[2px] z-10 flex items-center justify-center flex-col">
              <Loader2 size={32} className="text-primary-500 animate-spin mb-2" />
              <p className="text-slate-500 text-sm">正在加载数据...</p>
            </div>
          )}
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
              {localData.map((row, idx) => (
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
              {!isLoading && localData.length === 0 && (
                <tr><td colSpan={config.columns.length + 2} className="p-12 text-center text-slate-400">{errorMessage || '暂无数据'}</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-between items-center shrink-0">
          <span className="text-xs text-slate-500">
            显示 {pageInfo.total > 0 ? (currentPage - 1) * itemsPerPage + 1 : 0} 到 {Math.min(currentPage * itemsPerPage, pageInfo.total)} 条，共 {pageInfo.total} 条
          </span>
          <div className="flex gap-1">
            <button
              onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
              disabled={currentPage === 1}
              className="px-3 py-1 border border-slate-200 rounded bg-white text-xs text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50"
            >
              上一页
            </button>

            {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
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
              onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
              disabled={currentPage >= totalPages}
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
                  {!isPoolView && (viewRow.archivalCode || viewRow.code) && (
                    <button
                      onClick={() => handleAipExport(viewRow)}
                      disabled={isExporting === (viewRow.archivalCode || viewRow.code)}
                      className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:text-emerald-700 hover:border-emerald-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                    >
                      {isExporting === (viewRow.archivalCode || viewRow.code) ? <Loader2 size={14} className="animate-spin" /> : <Package size={14} />}
                      导出AIP
                    </button>
                  )}
                  <button
                    onClick={() => setIsViewModalOpen(false)}
                    className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600 transition-colors"
                  >
                    <X size={20} />
                  </button>
                </div>
              </div>

              {/* Content */}
              {/* Content Container - Panorama Layout */}
              <div className="flex-1 overflow-hidden flex flex-col lg:flex-row bg-slate-50/50">
                {/* Left Panel: Business Data */}
                <div className="flex-1 lg:w-3/5 flex flex-col min-w-0 border-r border-slate-200 bg-white">
                  <div className="flex-1 overflow-y-auto p-6">
                    {/* Voucher Header Info */}
                    <div className="mb-6">
                      <div className="flex items-center gap-3 mb-2">
                        <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                          <FileText size={24} />
                        </div>
                        <div>
                          <h2 className="text-lg font-bold text-slate-800">{viewRow?.code || '未命名档案'}</h2>
                          <p className="text-xs text-slate-500 font-mono">ID: {viewRow?.id}</p>
                        </div>
                        <div className="ml-auto">
                          <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${(!viewRow?.status || viewRow.status === 'draft') ? 'bg-slate-100 text-slate-600 border-slate-200' :
                            viewRow.status === 'archived' ? 'bg-green-50 text-green-700 border-green-200' :
                              'bg-blue-50 text-blue-700 border-blue-200'
                            }`}>
                            {formatStatus(viewRow?.status)}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Metadata Grid */}
                    <div className="bg-slate-50 rounded-xl border border-slate-100 p-5 mb-6">
                      <h3 className="text-sm font-bold text-slate-700 mb-4 flex items-center gap-2">
                        <Layers size={16} /> 业务元数据
                      </h3>
                      <div className="grid grid-cols-2 gap-y-4 gap-x-8">
                        {config.columns.filter(col => !['selection', 'actions', 'status'].includes(col.key)).map(col => (
                          <div key={col.key} className="group">
                            <label className="text-xs font-medium text-slate-400 mb-1 block group-hover:text-primary-600 transition-colors">
                              {col.header}
                            </label>
                            <div className="text-sm font-medium text-slate-700 min-h-[20px] break-words">
                              {renderCell(viewRow!, col)}
                            </div>
                          </div>
                        ))}
                        {/* Extra System Metadata */}
                        <div>
                          <label className="text-xs font-medium text-slate-400 mb-1 block">入池时间</label>
                          <div className="text-sm font-medium text-slate-700">
                            {viewRow?.date || '-'}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-slate-400 mb-1 block">存储ID</label>
                          <div className="text-xs font-mono text-slate-500 truncate" title={String(viewRow?.fileId || '-')}>
                            {String(viewRow?.fileId || '-')}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Accounting Entries Stub */}
                    <div className="border border-slate-200 rounded-xl overflow-hidden">
                      <div className="bg-slate-50 px-4 py-3 border-b border-slate-200 flex items-center justify-between">
                        <h3 className="text-sm font-bold text-slate-700 flex items-center gap-2">
                          <Book size={16} /> 会计分录
                        </h3>
                      </div>
                      <div className="p-8 text-center text-slate-400 text-sm bg-white">
                        暂无分录数据
                      </div>
                    </div>
                  </div>
                </div>

                {/* Right Panel: Files & Preview */}
                <div className="lg:w-2/5 flex flex-col bg-slate-100/50 border-l border-white shadow-inner">
                  {/* Tabs */}
                  <div className="flex items-center px-4 pt-4 gap-2 border-b border-slate-200 bg-white shadow-sm z-10">
                    <button
                      onClick={() => {
                        setActiveDetailTab('main');
                        setActivePreviewId(viewRow?.id || null);
                      }}
                      className={`px-4 py-2.5 text-sm font-medium rounded-t-lg transition-all relative ${activeDetailTab === 'main'
                        ? 'text-primary-600 bg-white border-x border-t border-slate-200 shadow-[0_-2px_5px_rgba(0,0,0,0.02)] -mb-px hover:text-primary-700'
                        : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
                        }`}
                    >
                      原始凭证
                      {activeDetailTab === 'main' && (
                        <div className="absolute top-0 left-0 right-0 h-0.5 bg-primary-500 rounded-t-full" />
                      )}
                    </button>

                    <button
                      onClick={() => setActiveDetailTab('attachments')}
                      className={`px-4 py-2.5 text-sm font-medium rounded-t-lg transition-all relative flex items-center gap-2 ${activeDetailTab === 'attachments'
                        ? 'text-primary-600 bg-white border-x border-t border-slate-200 shadow-[0_-2px_5px_rgba(0,0,0,0.02)] -mb-px hover:text-primary-700'
                        : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
                        }`}
                    >
                      关联附件
                      {relatedFiles.length > 0 && (
                        <span className={`px-1.5 py-0.5 text-[10px] rounded-full ${activeDetailTab === 'attachments' ? 'bg-primary-100 text-primary-700' : 'bg-slate-200 text-slate-600'
                          }`}>
                          {relatedFiles.length}
                        </span>
                      )}
                      {activeDetailTab === 'attachments' && (
                        <div className="absolute top-0 left-0 right-0 h-0.5 bg-primary-500 rounded-t-full" />
                      )}
                    </button>
                  </div>

                  {/* Preview Area Container */}
                  <div className="flex-1 relative flex flex-col overflow-hidden">
                    {/* Attachment List (Visible only when 'attachments' tab is active) */}
                    {activeDetailTab === 'attachments' && (
                      <div className="max-h-[200px] overflow-y-auto bg-white border-b border-slate-200 p-2 space-y-1 shadow-sm relative z-10">
                        {relatedFiles.length === 0 ? (
                          <div className="text-center py-8 text-slate-400 text-xs">暂无关联附件</div>
                        ) : (
                          relatedFiles.map((file, idx) => (
                            <div
                              key={file.id || idx}
                              onClick={() => setActivePreviewId(file.id)}
                              className={`flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all border ${activePreviewId === file.id
                                ? 'bg-blue-50 border-blue-200 shadow-sm'
                                : 'bg-white border-slate-100 hover:border-blue-200 hover:shadow-sm'
                                }`}
                            >
                              <div className={`p-2 rounded-lg ${activePreviewId === file.id ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-500'
                                }`}>
                                {file.fileName?.endsWith('.pdf') ? <FileText size={18} /> :
                                  file.fileName?.match(/\.(jpg|jpeg|png)$/i) ? <Receipt size={18} /> : <FileText size={18} />}
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className={`text-sm font-medium truncate ${activePreviewId === file.id ? 'text-blue-700' : 'text-slate-700'
                                  }`}>
                                  {file.fileName || '未知文件'}
                                </div>
                                <div className="text-xs text-slate-400 mt-0.5 flex items-center gap-2">
                                  <span>{file.type || '附件'}</span>
                                  {activePreviewId === file.id && (
                                    <span className="ml-auto text-blue-500 flex items-center gap-1">
                                      <Eye size={12} /> 预览中
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    )}

                    {/* Actual Previewer */}
                    <div className="flex-1 bg-slate-200 overflow-hidden relative">
                      {activePreviewId ? (
                        // Logic to determine preview content based on activePreviewId
                        (() => {
                          // Find active file details (either main row or from relatedFiles)
                          let fileName = '';
                          const isMain = activePreviewId === viewRow?.id;

                          if (isMain) {
                            fileName = viewRow?.title || (viewRow?.code ? viewRow.code + '.pdf' : 'unknown.pdf');
                          } else {
                            const att = relatedFiles.find(f => f.id === activePreviewId);
                            fileName = att?.fileName || '';
                          }

                          // Determine type for viewer
                          const isImage = fileName.match(/\.(jpg|jpeg|png|gif|bmp|webp)$/i);

                          if (isImage) {
                            return (
                              <div className="w-full h-full flex items-center justify-center bg-slate-800 overflow-auto p-4">
                                <img
                                  src={`/api/pool/preview/${activePreviewId}`}
                                  alt="Preview"
                                  className="max-w-full max-h-full object-contain shadow-2xl"
                                  onError={(e) => {
                                    (e.target as HTMLImageElement).src = '';
                                  }}
                                />
                              </div>
                            );
                          }
                          const type = fileName.toLowerCase().endsWith('.ofd') ? 'ofd' : 'pdf';

                          return (
                            <OfdViewer
                              fileUrl={`/api/pool/preview/${activePreviewId}`}
                              fileName={fileName}
                              fileType={type}
                              className="w-full h-full"
                            />
                          );
                        })()
                      ) : (
                        <div className="absolute inset-0 flex items-center justify-center text-slate-400 bg-slate-100">
                          <div className="text-center">
                            <FileText size={48} className="mx-auto mb-4 opacity-20" />
                            <p>请选择文件预览</p>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
    </div>
  )
}
export default ArchiveListView;
