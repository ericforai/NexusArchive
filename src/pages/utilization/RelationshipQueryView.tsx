// Input: React、简单图谱组件、Zustand store
// Output: React 组件 RelationshipQueryView（简化版）
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
import type { GenericRow, ModuleConfig } from '@/types';

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
  onSearch: () => void;
}> = ({ onSearch }) => (
  <div className="flex-1 flex items-center justify-center text-slate-400">
    <div className="text-center">
      <FileText size={48} className="mx-auto mb-4 opacity-50" />
      <p className="text-sm text-slate-500 mb-4">暂无关系数据，请输入档号查询</p>
      <button
        onClick={onSearch}
        className="px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 transition-colors"
      >
        加载示例数据
      </button>
    </div>
  </div>
);

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

const mapNodeToDrawerRow = async (nodeData: RelationNodeData): Promise<GenericRow> => {
  const attachments: Array<{
    id: string;
    fileName?: string;
    fileUrl?: string;
    type?: string;
  }> = [];

  try {
    const originalFiles = await originalVoucherApi.getOriginalVoucherFiles(nodeData.id);
    if (originalFiles.length > 0) {
      originalFiles.forEach(file => {
        attachments.push({
          id: file.id,
          fileName: file.fileName,
          fileUrl: `/original-vouchers/files/download/${file.id}`,
          type: file.fileType,
        });
      });
    }
  } catch {
    // 非原始凭证节点会进入这里，继续走通用关系附件查询
  }

  if (attachments.length === 0) {
    try {
      const linkedFiles = await autoAssociationApi.getLinkedFiles(nodeData.id);
      linkedFiles.files.forEach(file => {
        attachments.push({
          id: file.id,
          fileName: file.name,
          fileUrl: file.url && file.url !== '#' ? file.url : undefined,
          type: file.type,
        });
      });
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

  // Store 状态
  const nodes = useRelationGraphStore(s => s.nodes);
  const edges = useRelationGraphStore(s => s.edges);
  const isInitialLoading = useRelationGraphStore(s => s.isInitialLoading);
  const initialError = useRelationGraphStore(s => s.initialError);
  const originalQueryId = useRelationGraphStore(s => s.originalQueryId);
  const redirectMessage = useRelationGraphStore(s => s.redirectMessage);
  const initializeGraph = useRelationGraphStore(s => s.initializeGraph);
  const resetGraph = useRelationGraphStore(s => s.resetGraph);

  // 搜索处理
  const handleSearch = useCallback(async () => {
    const query = searchQuery.trim();
    if (!query) {
      toast.error('请输入档号');
      return;
    }

    setDrawerOpen(false);
    setSelectedDrawerRow(null);
    setHighlightedArchiveId(null);
    resetGraph();
    await initializeGraph(query);
    
    // 检查是否有自动转换提示（使用 useEffect 监听 store 状态变化更可靠）
  }, [searchQuery, initializeGraph, resetGraph]);

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
    resetGraph();
    toast.success('已重置图谱');
  }, [resetGraph]);

  // 节点点击处理：复用标准凭证抽屉，默认定位到“关联附件”
  const handleNodeClick = useCallback(async (nodeId: string, nodeData: RelationNodeData) => {
    setHighlightedArchiveId(nodeId);
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
              onNodeClick={handleNodeClick}
              highlightedArchiveId={highlightedArchiveId}
            />
          </div>
        ) : (
          <EmptyState onSearch={handleSearch} />
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
