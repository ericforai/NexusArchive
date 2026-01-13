// Input: React、简单图谱组件、Zustand store
// Output: React 组件 RelationshipQueryView（简化版）
// Pos: src/pages/utilization/RelationshipQueryView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback, useEffect, useState } from 'react';
import {
  Search,
  FileText,
  AlertTriangle,
  Loader2,
  X,
  RotateCcw,
  Info
} from 'lucide-react';
import { SimpleGraphView } from '@/components/relation-graph/SimpleGraphView';
import { useRelationGraphStore } from '@/store/useRelationGraphStore';
import type { RelationNodeData } from '@/types/relationGraph';
import { ARCHIVE_TYPE_STYLES } from '@/types/relationGraph';
import { toast } from 'react-hot-toast';

/**
 * 搜索栏组件
 */
const SearchBar: React.FC<{
  value: string;
  onChange: (value: string) => void;
  onSearch: () => void;
  isLoading: boolean;
}> = ({ value, onChange, onSearch, isLoading }) => {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch();
  };

  return (
    <form onSubmit={handleSubmit} className="flex items-center gap-2 w-full max-w-md">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
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
};

/**
 * 节点详情抽屉
 */
const NodeDetailDrawer: React.FC<{
  nodeData: RelationNodeData | null;
  onClose: () => void;
}> = ({ nodeData, onClose }) => {
  if (!nodeData) return null;

  const meta = ARCHIVE_TYPE_STYLES[nodeData.type] || ARCHIVE_TYPE_STYLES.other;

  return (
    <div className="w-96 bg-white border-l border-slate-200 shadow-xl flex flex-col h-full animate-in slide-in-from-right duration-300">
      {/* 头部 */}
      <div className="p-6 border-b border-slate-100 flex justify-between items-start">
        <div>
          <span className={`text-xs font-bold uppercase tracking-wider block mb-1 ${meta.text}`}>
            {meta.label}
          </span>
          <h3 className="text-xl font-bold text-slate-800 leading-tight">
            {nodeData.code || nodeData.name}
          </h3>
        </div>
        <button
          onClick={onClose}
          className="p-1 hover:bg-slate-100 rounded-lg transition-colors"
        >
          <X size={20} className="text-slate-400" />
        </button>
      </div>

      {/* 内容 */}
      <div className="p-6 flex-1 overflow-y-auto space-y-6">
        {/* 状态卡片 */}
        <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm text-slate-500">当前状态</span>
            <span className={`px-2 py-1 text-xs font-bold rounded ${
              nodeData.status === '已归档' || nodeData.status === 'ARCHIVED'
                ? 'bg-emerald-100 text-emerald-700'
                : 'bg-slate-100 text-slate-600'
            }`}>
              {nodeData.status || '未知'}
            </span>
          </div>
          <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
            <div className="bg-emerald-500 w-full h-full rounded-full" />
          </div>
        </div>

        {/* 详情列表 */}
        <div className="space-y-4 text-sm text-slate-700">
          <div>
            <div className="text-xs text-slate-400 font-medium">名称</div>
            <div className="font-medium">{nodeData.name || '--'}</div>
          </div>

          {nodeData.amount && (
            <div>
              <div className="text-xs text-slate-400 font-medium">金额</div>
              <div className="font-mono font-bold">{nodeData.amount}</div>
            </div>
          )}

          <div>
            <div className="text-xs text-slate-400 font-medium">业务日期</div>
            <div>{nodeData.date || '--'}</div>
          </div>

          <div>
            <div className="text-xs text-slate-400 font-medium">档案类型</div>
            <div>{meta.label}</div>
          </div>

          <div>
            <div className="text-xs text-slate-400 font-medium">距中心深度</div>
            <div>{nodeData.depth} 度</div>
          </div>
        </div>
      </div>
    </div>
  );
};

/**
 * 帮助提示
 */
const HelpTip: React.FC = () => (
  <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 text-sm text-blue-800 flex items-start gap-3">
    <Info size={18} className="text-blue-500 flex-shrink-0 mt-0.5" />
    <div>
      <div className="font-medium mb-1">使用提示</div>
      <ul className="text-blue-700 space-y-1 text-xs">
        <li>• 点击节点可展开/折叠其关联关系</li>
        <li>• 最多展开 3 度关系，超出时自动折叠早期节点</li>
      </ul>
    </div>
  </div>
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

/**
 * 关系联查主页面 - 简化版
 */
export const RelationshipQueryView: React.FC = () => {
  // 本地状态
  const [searchQuery, setSearchQuery] = useState('JZ-202311-0052');
  const [selectedNodeData, setSelectedNodeData] = useState<RelationNodeData | null>(null);

  // Store 状态
  const nodes = useRelationGraphStore(s => s.nodes);
  const isInitialLoading = useRelationGraphStore(s => s.isInitialLoading);
  const initialError = useRelationGraphStore(s => s.initialError);
  const initializeGraph = useRelationGraphStore(s => s.initializeGraph);
  const resetGraph = useRelationGraphStore(s => s.resetGraph);
  const expandNode = useRelationGraphStore(s => s.expandNode);

  // 初始化：自动加载示例数据
  useEffect(() => {
    if (searchQuery) {
      initializeGraph(searchQuery);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 搜索处理
  const handleSearch = useCallback(() => {
    const query = searchQuery.trim();
    if (!query) {
      toast.error('请输入档号');
      return;
    }

    setSelectedNodeData(null);
    resetGraph();
    initializeGraph(query);
  }, [searchQuery, initializeGraph, resetGraph]);

  // 重置处理
  const handleReset = useCallback(() => {
    setSearchQuery('');
    setSelectedNodeData(null);
    resetGraph();
    toast.success('已重置图谱');
  }, [resetGraph]);

  // 节点点击处理
  const handleNodeClick = useCallback((nodeId: string, data: RelationNodeData) => {
    setSelectedNodeData(data);
    // 展开节点的关联关系
    expandNode(nodeId);
  }, [expandNode]);

  // 关闭抽屉
  const handleCloseDrawer = useCallback(() => {
    setSelectedNodeData(null);
  }, []);

  // 判断是否显示画布
  const showCanvas = nodes.length > 0;

  return (
    <div className="h-full flex flex-col bg-slate-50/50">
      {/* 头部 */}
      <div className="px-8 py-6 bg-white border-b border-slate-200 flex justify-between items-center shadow-sm z-10">
        <div>
          <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            穿透联查
          </h2>
          <p className="text-slate-500 mt-1 text-sm">
            输入任意档号，点击节点可展开其关联关系（最多3度）
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
        {/* 画布/空状态 */}
        {showCanvas ? (
          <div className="flex-1 relative">
            {/* 帮助提示 */}
            <div className="absolute top-4 left-4 z-10 w-72">
              <HelpTip />
            </div>

            {/* 简单图谱画布 */}
            <SimpleGraphView onNodeClick={handleNodeClick} />
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

        {/* 详情抽屉 */}
        <NodeDetailDrawer nodeData={selectedNodeData} onClose={handleCloseDrawer} />
      </div>
    </div>
  );
};

export default RelationshipQueryView;
