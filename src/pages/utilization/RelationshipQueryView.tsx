// Input: React、简单图谱组件、Zustand store
// Output: React 组件 RelationshipQueryView（后端方向优先 + 演示数据加载 + 附件回退修复）
// Pos: src/pages/utilization/RelationshipQueryView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback, useEffect, useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Search,
  FileText,
  AlertTriangle,
  Loader2,
  RotateCcw
} from 'lucide-react';
import { ThreeColumnLayout } from '@/components/relation-graph';
import { useRelationGraphStore } from '@/store/useRelationGraphStore';
import type { RelationNodeData, RelationEdgeData } from '@/types/relationGraph';
import { toast } from 'react-hot-toast';
import ArchiveDetailDrawer from '@/pages/archives/ArchiveDetailDrawer';
import { autoAssociationApi } from '@/api/autoAssociation';
import { originalVoucherApi } from '@/api/originalVoucher';
import { attachmentsApi } from '@/api/attachments';
import { archivesApi } from '@/api/archives';
import type { GenericRow, ModuleConfig } from '@/types';
import {
  detectPaymentMainline,
  buildPrintNodeScope,
  dedupeAttachments
} from './relationDrilldown';

/**
 * 搜索栏组件
 */
const SearchBar: React.FC<{
  value: string;
  onChange: (value: string) => void;
  onSearch: () => void;
  isLoading: boolean;
}> = ({ value, onChange, onSearch, isLoading }) => (
  <form onSubmit={(e) => { e.preventDefault(); onSearch(); }} className="flex items-center gap-2 w-full max-w-lg">
    <div className="relative flex-1">
      <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all text-sm placeholder:text-[11px] placeholder:leading-tight"
        placeholder="请输入凭证号 / 发票号 / 合同号..."
        disabled={isLoading}
      />
    </div>
    <button
      type="submit"
      disabled={isLoading || !value.trim()}
      className="px-6 py-2.5 bg-primary-600 text-white font-medium rounded-xl hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-primary-500/30 transition-all flex items-center gap-2"
    >
      {isLoading ? <Loader2 size={18} className="animate-spin" /> : '查询'}
    </button>
  </form>
);

/**
 * 空状态
 */
const EmptyState: React.FC<{
  onLoadDemo: () => void;
}> = ({ onLoadDemo }) => (
  <div className="flex-1 flex items-center justify-center text-slate-400">
    <div className="text-center">
      <FileText size={48} className="mx-auto mb-4 opacity-50" />
      <p className="text-sm text-slate-500 mb-4">暂无关系数据，请输入档号查询</p>
      <button
        onClick={onLoadDemo}
        className="px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 transition-colors"
      >
        加载示例数据
      </button>
    </div>
  </div>
);

const DEMO_ARCHIVE_QUERY = 'JZ-2025-01-001';

const EMPTY_MODULE_CONFIG: ModuleConfig = {
  columns: [],
  data: [],
};

const parseAmountToNumber = (amount?: string): number => {
  if (!amount) return 0;
  const normalized = amount.replace(/[^\d.-]/g, '');
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
};

const resolvePreviewTitle = (type: RelationNodeData['type']): string => {
  if (type === 'voucher') return '凭证预览';
  if (type === 'report' || type === 'ledger') return '档案预览';
  return '原始凭证预览';
};

const inferFileTypeFromName = (name?: string): string | undefined => {
  if (!name) return undefined;
  const lower = name.toLowerCase();
  if (lower.endsWith('.pdf')) return 'application/pdf';
  if (lower.endsWith('.png')) return 'image/png';
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'image/jpeg';
  if (lower.endsWith('.gif')) return 'image/gif';
  if (lower.endsWith('.webp')) return 'image/webp';
  return undefined;
};

const normalizeClientRelativeUrl = (url?: string): string | undefined => {
  if (!url) return undefined;
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  // 历史占位下载接口兼容：/api/archives/{archiveId}/download -> /archive/{archiveId}/content
  const legacyMatched = url.match(/^\/api\/archives\/([^/]+)\/download$/) || url.match(/^\/archives\/([^/]+)\/download$/);
  if (legacyMatched?.[1]) {
    return `/archive/${legacyMatched[1]}/content`;
  }
  // axios client 已包含 /api baseURL，避免传入 /api/... 产生 /api/api/...
  if (url.startsWith('/api/')) return url.replace(/^\/api/, '');
  // 静态资源 URL 需要转为绝对地址，避免 axios baseURL=/api 时被错误拼接为 /api/xxx
  if (typeof window !== 'undefined' && url.startsWith('/') && !url.startsWith('/archive/') && !url.startsWith('/original-vouchers/')) {
    return `${window.location.origin}${url}`;
  }
  return url;
};

const isDemoArchiveId = (id?: string): boolean => Boolean(id && id.startsWith('demo-'));

const extractArchiveIdFromLegacyDownloadUrl = (url: string): string | null => {
  // 兼容后端历史占位 URL: /api/archives/{archiveId}/download
  const matched = url.match(/^\/api\/archives\/([^/]+)\/download$/) || url.match(/^\/archives\/([^/]+)\/download$/);
  return matched?.[1] || null;
};

const extractFileIdFromDownloadUrl = (url: string): string | null => {
  const matched = url.match(/^\/(?:archive|original-vouchers)\/files\/download\/([^/?#]+)/);
  return matched?.[1] || null;
};

const mapNodeToDrawerRow = async (nodeData: RelationNodeData): Promise<GenericRow> => {
  const attachments: Array<{
    id: string;
    fileName?: string;
    fileUrl?: string;
    type?: string;
    fileId?: string;
    archiveId?: string;
    previewResourceType?: 'archiveMain' | 'file';
  }> = [];

  // 1) 虚拟文件节点：FILE_{fileId}，直接预览该文件（不走中心凭证）
  if (nodeData.id.startsWith('FILE_')) {
    const fileId = nodeData.id.slice(5);
    attachments.push({
      id: fileId,
      fileId,
      fileName: nodeData.name || nodeData.code || fileId,
      fileUrl: `/archive/files/download/${fileId}`,
      type: inferFileTypeFromName(nodeData.name || nodeData.code),
      previewResourceType: 'file',
    });
  }

  // 2) 虚拟原始凭证节点：OV_{originalVoucherId}
  if (attachments.length === 0 && nodeData.id.startsWith('OV_')) {
    const originalVoucherId = nodeData.id.slice(3);
    try {
      const originalFiles = await originalVoucherApi.getOriginalVoucherFiles(originalVoucherId);
      if (originalFiles.length > 0) {
        originalFiles.forEach(file => {
          attachments.push({
            id: file.id,
            fileId: file.id,
            fileName: file.fileName,
            fileUrl: `/original-vouchers/files/download/${file.id}`,
            type: file.fileType,
            previewResourceType: 'file',
          });
        });
      }
    } catch (error) {
      console.error('获取原始凭证文件失败:', error);
      // 继续后续兜底
    }
  }

  // 3) 实体节点若是原始凭证ID，按原始凭证文件查询
  if (attachments.length === 0) {
    try {
      const originalFiles = await originalVoucherApi.getOriginalVoucherFiles(nodeData.id);
      if (originalFiles.length > 0) {
        originalFiles.forEach(file => {
          attachments.push({
            id: file.id,
            fileId: file.id,
            fileName: file.fileName,
            fileUrl: `/original-vouchers/files/download/${file.id}`,
            type: file.fileType,
            previewResourceType: 'file',
          });
        });
      }
    } catch (error) {
      console.error('按原始凭证ID查询文件失败:', error);
      // 非原始凭证节点会进入这里，继续走通用关系附件查询
    }
  }

  if (attachments.length === 0) {
    try {
      const archiveAttachments = await attachmentsApi.getByArchive(nodeData.id);
      archiveAttachments.forEach(file => {
        attachments.push({
          id: file.id,
          fileId: file.id,
          fileName: file.fileName,
          fileUrl: `/archive/files/download/${file.id}`,
          type: file.fileType,
          previewResourceType: 'file',
        });
      });
    } catch (error) {
      if ((error as any)?.response?.status !== 403) {
        console.error('查询附件失败:', error);
      }
      // 继续回退到关系附件接口
    }
  }

  // 4) 归档文件列表兜底：有些节点不是附件关系，但有自己的归档文件
  if (attachments.length === 0 && !isDemoArchiveId(nodeData.id)) {
    try {
      const archiveFilesResp = await archivesApi.getArchiveFiles(nodeData.id);
      const archiveFiles = archiveFilesResp?.data || [];
      archiveFiles.forEach((file: any) => {
        if (!file?.id) return;
        attachments.push({
          id: file.id,
          fileId: file.id,
          fileName: file.fileName || file.originalName || file.name,
          fileUrl: `/archive/files/download/${file.id}`,
          type: file.fileType,
          previewResourceType: 'file',
        });
      });
    } catch (error) {
      if ((error as any)?.response?.status !== 403) {
        console.error('查询附件失败:', error);
      }
      // 继续回退到关系附件接口
    }
  }

  if (attachments.length === 0) {
    try {
      const linkedFiles = await autoAssociationApi.getLinkedFiles(nodeData.id);
      const seenIds = new Set<string>();
      for (const file of linkedFiles.files) {
        const candidateUrl = file.url?.trim();
        // 跳过空/占位 URL
        if (!candidateUrl || candidateUrl === '#') continue;

        // 兼容后端历史占位 URL：先解析 archiveId，再查询真实文件列表
        const legacyArchiveId = extractArchiveIdFromLegacyDownloadUrl(candidateUrl);
        if (legacyArchiveId) {
          let resolved = false;
          try {
            const filesResp = await archivesApi.getArchiveFiles(legacyArchiveId);
            const archiveFiles = filesResp?.data || [];
            for (const af of archiveFiles) {
              if (!af?.id || seenIds.has(af.id)) continue;
              seenIds.add(af.id);
              attachments.push({
                id: af.id,
                fileId: af.id,
                fileName: af.fileName || af.originalName || af.name || file.name,
                fileUrl: `/archive/files/download/${af.id}`,
                type: af.fileType || inferFileTypeFromName(af.fileName || af.name),
                previewResourceType: 'file',
              });
              resolved = true;
            }
          } catch {
            // legacy 链路失败则继续走 URL 重写兜底
          }

          // 硬兜底：即使拿不到 files 列表，也使用可用的 archive content 接口
          if (!resolved) {
            attachments.push({
              id: file.id || legacyArchiveId,
              fileId: undefined,
              archiveId: legacyArchiveId,
              fileName: file.name || legacyArchiveId,
              fileUrl: `/archive/${legacyArchiveId}/content`,
              type: inferFileTypeFromName(file.name),
              previewResourceType: 'archiveMain',
            });
          }
          continue;
        }

        const normalizedUrl = normalizeClientRelativeUrl(candidateUrl);
        const resolvedFileId = extractFileIdFromDownloadUrl(normalizedUrl || '');
        attachments.push({
          id: resolvedFileId || file.id || normalizedUrl || '',
          fileId: resolvedFileId || undefined,
          fileName: file.name,
          fileUrl: normalizedUrl,
          type: file.type,
          previewResourceType: resolvedFileId ? 'file' : undefined,
        });
      }
    } catch {
      // 保持空数组，交由抽屉空态展示
    }
  }

  const amount = parseAmountToNumber(nodeData.amount);
  const sourceData = JSON.stringify({
    voucherNo: nodeData.code || nodeData.id,
    voucherDate: nodeData.date || '',
    summary: nodeData.name || '',
    debitTotal: amount,
    creditTotal: amount,
    entries: [],
  });

  return {
    id: nodeData.id,
    code: nodeData.code || nodeData.id,
    date: nodeData.date || '',
    summary: nodeData.name || '',
    archivalCode: nodeData.code || nodeData.id,
    archivalCategory: nodeData.type === 'voucher' ? 'VOUCHER' : 'OTHER',
    previewTitle: resolvePreviewTitle(nodeData.type),
    sourceData,
    attachments,
  };
};

/**
 * 关系联查主页面 - 简化版
 */
export const RelationshipQueryView: React.FC = () => {
  const [urlSearchParams] = useSearchParams();
  // 本地状态 - 移除默认值，避免页面加载时请求不存在的档号
  const [searchQuery, setSearchQuery] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedDrawerRow, setSelectedDrawerRow] = useState<GenericRow | null>(null);
  const [highlightedArchiveId, setHighlightedArchiveId] = useState<string | null>(null);
  const [manuallyExpandedNodeIds, setManuallyExpandedNodeIds] = useState<Set<string>>(new Set());

  // Store 状态
  const nodes = useRelationGraphStore(s => s.nodes);
  const edges = useRelationGraphStore(s => s.edges);
  const isInitialLoading = useRelationGraphStore(s => s.isInitialLoading);
  const initialError = useRelationGraphStore(s => s.initialError);
  const originalQueryId = useRelationGraphStore(s => s.originalQueryId);
  const redirectMessage = useRelationGraphStore(s => s.redirectMessage);
  const directionalView = useRelationGraphStore(s => s.directionalView);
  const initializeGraph = useRelationGraphStore(s => s.initializeGraph);
  const resetGraph = useRelationGraphStore(s => s.resetGraph);

  const executeSearch = useCallback(async (query: string) => {
    setDrawerOpen(false);
    setSelectedDrawerRow(null);
    setHighlightedArchiveId(null);
    setManuallyExpandedNodeIds(new Set());
    resetGraph();
    await initializeGraph(query);
  }, [initializeGraph, resetGraph]);

  // 搜索处理
  const handleSearch = useCallback(async () => {
    const query = searchQuery.trim();
    if (!query) {
      toast.error('请输入档号');
      return;
    }
    await executeSearch(query);
  }, [searchQuery, executeSearch]);

  // 演示数据加载：无需用户先输入档号
  const handleLoadDemo = useCallback(async () => {
    setSearchQuery(DEMO_ARCHIVE_QUERY);
    await executeSearch(DEMO_ARCHIVE_QUERY);
    toast.success(`已加载演示数据：${DEMO_ARCHIVE_QUERY}`);
  }, [executeSearch]);

  // 监听 store 中的 redirectMessage，显示提示
  useEffect(() => {
    if (redirectMessage && originalQueryId) {
      toast.success(redirectMessage, { 
        duration: 4000
      });
      setHighlightedArchiveId(originalQueryId);
    }
  }, [redirectMessage, originalQueryId]);

  // 支持从 URL 参数自动填充并触发查询
  useEffect(() => {
    const voucherNo = urlSearchParams.get('voucherNo')?.trim();
    const autoSearch = urlSearchParams.get('autoSearch') === '1';
    if (!voucherNo) {
      return;
    }
    setSearchQuery(voucherNo);
    if (autoSearch) {
      setDrawerOpen(false);
      setSelectedDrawerRow(null);
      setHighlightedArchiveId(null);
      resetGraph();
      void initializeGraph(voucherNo);
    }
  }, [urlSearchParams, initializeGraph, resetGraph]);

  // 重置处理
  const handleReset = useCallback(() => {
    setSearchQuery('');
    setDrawerOpen(false);
    setSelectedDrawerRow(null);
    setHighlightedArchiveId(null);
    setManuallyExpandedNodeIds(new Set());
    resetGraph();
    toast.success('已重置图谱');
  }, [resetGraph]);

  // 节点点击处理：复用标准凭证抽屉，默认定位到“关联附件”
  const handleNodeClick = useCallback(async (nodeId: string, nodeData: RelationNodeData) => {
    setHighlightedArchiveId(nodeId);
    setManuallyExpandedNodeIds(prev => new Set(prev).add(nodeId));
    const loadingId = toast.loading('正在加载预览...');
    try {
      const row = await mapNodeToDrawerRow(nodeData);
      setSelectedDrawerRow(row);
      setDrawerOpen(true);
    } catch (error) {
      console.error('加载预览失败:', error);
      toast.error('加载预览失败，请稍后重试');
    } finally {
      toast.dismiss(loadingId);
    }
  }, []);

  // 关闭抽屉
  const handleCloseDrawer = useCallback(() => {
    setDrawerOpen(false);
    setSelectedDrawerRow(null);
  }, []);

  // 判断是否显示内容
  const showContent = nodes.length > 0;
  
  // 获取中心节点
  const centerNode = nodes.find(n => n.data?.isCenter)?.data || (nodes[0]?.data);
  
  // 转换 edges 为 RelationEdgeData 格式
  const relationEdges: RelationEdgeData[] = useMemo(() => 
    edges.map(edge => ({
      id: edge.id,
      from: edge.source as string,
      to: edge.target as string,
      relationType: (edge.data?.relationType || 'default') as any,
      description: edge.data?.description
    })),
    [edges]
  );
  
  // 转换 nodes 为 RelationNodeData 格式
  const relationNodes: RelationNodeData[] = useMemo(() =>
    nodes.map(node => node.data as RelationNodeData).filter(Boolean),
    [nodes]
  );

  const mainline = useMemo(() => {
    if (!centerNode) return { nodeIds: [], missingSteps: [] };
    const backendMainline = directionalView?.mainline || [];
    if (backendMainline.length > 0) {
      return { nodeIds: backendMainline, missingSteps: [] };
    }
    return detectPaymentMainline(relationNodes, relationEdges, centerNode.id);
  }, [centerNode, directionalView?.mainline, relationNodes, relationEdges]);

  const handleBatchPrint = useCallback(async () => {
    const scopeNodeIds = buildPrintNodeScope(mainline.nodeIds, Array.from(manuallyExpandedNodeIds));
    if (scopeNodeIds.length === 0) {
      toast.error('没有可打印的关联单据');
      return;
    }

    const scopeNodes = scopeNodeIds
      .map(id => relationNodes.find(node => node.id === id))
      .filter((node): node is RelationNodeData => Boolean(node));

    if (scopeNodes.length === 0) {
      toast.error('未找到可打印节点');
      return;
    }

    const loadingId = toast.loading('正在归集打印单据...');
    try {
      const rows = await Promise.all(scopeNodes.map(node => mapNodeToDrawerRow(node)));
      const rawAttachments = rows.flatMap(row => row.attachments || []);
      const validAttachments = rawAttachments.filter(att => Boolean(att.fileUrl));
      const dedupedAttachments = dedupeAttachments(validAttachments);

      if (dedupedAttachments.length === 0) {
        toast.error('没有可打印附件，请先补充影像或关联文件');
        return;
      }

      const message = [
        `准备打印 ${dedupedAttachments.length} 份附件`,
        `主线缺口 ${mainline.missingSteps.length} 个`
      ].join('，');

      dedupedAttachments.forEach((attachment) => {
        if (!attachment.fileUrl) return;
        window.open(attachment.fileUrl, '_blank', 'noopener,noreferrer');
      });

      toast.success(message, { duration: 4500 });
    } catch (error) {
      console.error('批量打印归集失败:', error);
      toast.error('批量打印归集失败，请稍后重试');
    } finally {
      toast.dismiss(loadingId);
    }
  }, [mainline.missingSteps.length, mainline.nodeIds, manuallyExpandedNodeIds, relationNodes]);

  return (
    <div className="h-full flex flex-col bg-slate-50/50">
      {/* 头部 */}
      <div className="px-8 py-6 bg-white border-b border-slate-200 flex justify-between items-center shadow-sm z-10">
        <div>
          <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            穿透联查
          </h2>
          <p className="text-slate-500 mt-1 text-sm">
            输入任意档号，自动生成全链路业务关系（左侧：依据/凭证/来源，右侧：流向/归档/结果）
          </p>
        </div>
        <div className="flex items-center gap-4">
          <button
            onClick={handleBatchPrint}
            disabled={nodes.length === 0}
            className="px-4 py-2.5 bg-emerald-600 text-white text-sm rounded-xl hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed"
            title="批量打印主线+手动展开节点"
          >
            批量打印关联单据
          </button>
          <SearchBar
            value={searchQuery}
            onChange={setSearchQuery}
            onSearch={handleSearch}
            isLoading={isInitialLoading}
          />
          <button
            onClick={handleReset}
            disabled={nodes.length === 0}
            className="p-2.5 bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            title="重置图谱"
          >
            <RotateCcw size={18} />
          </button>
        </div>
      </div>

      {/* 主内容区 */}
      <div className="flex-1 relative overflow-hidden flex">
        {/* 三栏布局/空状态 */}
        {showContent && centerNode ? (
          <div className="flex-1 relative overflow-auto">
            {/* 三栏布局 */}
            <ThreeColumnLayout
              centerNode={centerNode}
              nodes={relationNodes}
              relations={relationEdges}
              directionalView={directionalView || undefined}
              onNodeClick={handleNodeClick}
              highlightedArchiveId={highlightedArchiveId}
              mainlineNodeIds={mainline.nodeIds}
              missingSteps={mainline.missingSteps}
            />
          </div>
        ) : (
          <EmptyState onLoadDemo={handleLoadDemo} />
        )}

        {/* 初始加载 */}
        {isInitialLoading && (
          <div className="absolute inset-0 flex items-center justify-center bg-white/60 backdrop-blur-sm z-20">
            <div className="text-slate-600 flex items-center gap-2">
              <Loader2 size={20} className="animate-spin" />
              <span>查询中...</span>
            </div>
          </div>
        )}

        {/* 错误提示 */}
        {initialError && !isInitialLoading && (
          <div className="absolute top-4 left-1/2 -translate-x-1/2 z-20 bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-sm flex items-center gap-2 shadow-lg max-w-md">
            <AlertTriangle size={16} />
            <span>{initialError}</span>
          </div>
        )}

        {/* 标准详情抽屉（复用档案管理同款组件） */}
        <ArchiveDetailDrawer
          open={drawerOpen}
          onClose={handleCloseDrawer}
          row={selectedDrawerRow}
          config={EMPTY_MODULE_CONFIG}
          isPoolView={true}
          defaultTab="attachments"
        />
      </div>
    </div>
  );
};

export default RelationshipQueryView;
