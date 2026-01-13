// Input: React
// Output: React 组件 RelationshipQueryView
// Pos: src/pages/utilization/RelationshipQueryView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useMemo, useState } from 'react';
import {
  Search,
  FileText,
  Receipt,
  FileSpreadsheet,
  Building,
  CreditCard,
  AlertTriangle,
  Loader2,
  CheckCircle2,
  ArrowRight
} from 'lucide-react';
import { autoAssociationApi, RelationGraph, RelationGraphNode } from '../../api/autoAssociation';

const typeMeta: Record<string, { icon: typeof FileText; bg: string; text: string; label: string }> = {
  contract: { icon: Building, bg: 'bg-indigo-50', text: 'text-indigo-700', label: '合同' },
  invoice: { icon: Receipt, bg: 'bg-purple-50', text: 'text-purple-700', label: '发票' },
  voucher: { icon: FileText, bg: 'bg-blue-50', text: 'text-blue-700', label: '凭证' },
  receipt: { icon: CreditCard, bg: 'bg-emerald-50', text: 'text-emerald-700', label: '回单' },
  report: { icon: FileSpreadsheet, bg: 'bg-amber-50', text: 'text-amber-700', label: '报表' },
  ledger: { icon: FileSpreadsheet, bg: 'bg-slate-50', text: 'text-slate-700', label: '账簿' },
  other: { icon: FileText, bg: 'bg-slate-50', text: 'text-slate-700', label: '其他' }
};

const NodeCard: React.FC<{
  node: RelationGraphNode;
  active?: boolean;
  relation?: string;
  onSelect?: () => void;
}> = ({ node, active, relation, onSelect }) => {
  const meta = typeMeta[node.type] || typeMeta.other;
  const Icon = meta.icon;
  return (
    <div
      onClick={onSelect}
      className={`p-4 rounded-xl border cursor-pointer transition-all ${active ? 'border-primary-500 shadow-lg shadow-primary-500/10 bg-white' : 'border-slate-200 bg-white/80 hover:border-primary-200'
        }`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className={`p-2 rounded-lg ${meta.bg} ${meta.text}`}>
          <Icon size={18} />
        </div>
        <span className="text-[10px] px-2 py-1 rounded-full bg-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
          {meta.label}
        </span>
      </div>
      <div className="mt-2">
        <div className="text-sm font-bold text-slate-800 truncate">{node.code || node.name || node.id}</div>
        <div className="text-xs text-slate-500 truncate">{node.name}</div>
      </div>
      <div className="mt-3 text-xs text-slate-500 space-y-1">
        {node.amount && <div className="font-mono text-slate-700">{node.amount}</div>}
        {node.date && <div>{node.date}</div>}
        {node.status && (
          <div className="inline-flex items-center gap-1 px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded-full border border-emerald-100">
            <CheckCircle2 size={12} /> {node.status}
          </div>
        )}
        {relation && <div className="text-[11px] text-primary-600">关系：{relation}</div>}
      </div>
    </div>
  );
};

export const RelationshipQueryView: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('JZ-202311-0052');
  const [graph, setGraph] = useState<RelationGraph | null>(null);
  const [selectedNode, setSelectedNode] = useState<RelationGraphNode | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const centerNode = useMemo(() => {
    if (!graph || !graph.nodes?.length) return null;
    return graph.nodes.find(n => n.id === graph.centerId) || graph.nodes[0];
  }, [graph]);

  const upstreamNodes = useMemo(() => {
    if (!graph || !graph.nodes) return [];
    const ids = new Set(graph.edges?.filter(e => e.to === graph.centerId).map(e => e.from));
    return graph.nodes.filter(n => ids.has(n.id) && n.id !== graph.centerId);
  }, [graph]);

  const downstreamNodes = useMemo(() => {
    if (!graph || !graph.nodes) return [];
    const ids = new Set(graph.edges?.filter(e => e.from === graph.centerId).map(e => e.to));
    return graph.nodes.filter(n => ids.has(n.id) && n.id !== graph.centerId);
  }, [graph]);

  const getRelationLabel = (nodeId: string) => {
    if (!graph?.edges?.length) return '';
    const edge = graph.edges.find(e => e.from === nodeId || e.to === nodeId);
    return edge?.relationType || edge?.description || '';
  };

  const loadGraph = async (archiveId: string) => {
    if (!archiveId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await autoAssociationApi.getRelationGraph(archiveId);
      setGraph(data);
      const center = data.nodes.find(n => n.id === (data.centerId || archiveId)) || data.nodes[0] || null;
      setSelectedNode(center);
    } catch (e: any) {
      setGraph(null);
      setSelectedNode(null);
      if (e?.response?.status === 401) {
        setError('请先登录系统后再查询关系数据');
      } else if (e?.response?.status === 403) {
        setError('您没有权限查看此档案的关系数据');
      } else {
        setError(e?.response?.data?.message || '查询失败，请稍后重试');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (searchQuery) {
      loadGraph(searchQuery);
    }
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadGraph(searchQuery.trim());
  };

  return (
    <div className="h-full flex flex-col bg-slate-50/50">
      {/* Header Search */}
      <div className="px-8 py-6 bg-white border-b border-slate-200 flex justify-between items-center shadow-sm z-10">
        <div>
          <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            穿透联查
          </h2>
          <p className="text-slate-500 mt-1 text-sm">输入任意档号，自动生成全链路业务关系。</p>
        </div>
        <form onSubmit={handleSearch} className="flex items-center gap-2 w-full max-w-md">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
              placeholder="请输入凭证号 / 发票号 / 合同号..."
            />
          </div>
          <button type="submit" className="px-6 py-2.5 bg-primary-600 text-white font-medium rounded-xl hover:bg-primary-700 shadow-lg shadow-primary-500/30 transition-all">
            查询
          </button>
        </form>
      </div>

      {/* Canvas Area */}
      <div className="flex-1 relative overflow-hidden flex">
        <div className="flex-1 relative overflow-auto bg-[radial-gradient(#e2e8f0_1px,transparent_1px)] [background-size:20px_20px] flex flex-col">
          {loading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white/60 backdrop-blur-sm z-20 text-slate-600">
              <Loader2 className="animate-spin mr-2" size={18} /> 查询中...
            </div>
          )}
          {error && (
            <div className="m-6 bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-sm flex items-center gap-2">
              <AlertTriangle size={16} /> {error}
            </div>
          )}

          {graph && graph.nodes?.length ? (
            <div className="p-8 space-y-6">
              <div className="grid grid-cols-1 xl:grid-cols-[1fr_260px_1fr] gap-6 items-start">
                <div className="space-y-3">
                  {upstreamNodes.length === 0 && (
                    <div className="text-xs text-slate-500">暂无上游数据</div>
                  )}
                  {upstreamNodes.map(node => (
                    <NodeCard
                      key={node.id}
                      node={node}
                      relation={getRelationLabel(node.id)}
                      active={selectedNode?.id === node.id}
                      onSelect={() => setSelectedNode(node)}
                    />
                  ))}
                </div>

                <div className="flex flex-col gap-3 items-stretch">
                  {centerNode && (
                    <NodeCard
                      node={centerNode}
                      active
                      relation="核心单据"
                      onSelect={() => setSelectedNode(centerNode)}
                    />
                  )}
                  <div className="bg-white border border-slate-200 rounded-xl p-3 text-xs text-slate-500 space-y-2">
                    <div className="font-semibold text-slate-700">关系说明</div>
                    {graph.edges?.length ? graph.edges.map((edge, idx) => (
                      <div key={idx} className="flex items-center gap-2">
                        <span className="text-slate-700 font-mono">{edge.from}</span>
                        <ArrowRight size={14} className="text-slate-400" />
                        <span className="text-slate-700 font-mono">{edge.to}</span>
                        <span className="px-2 py-0.5 bg-blue-50 text-blue-700 rounded-full border border-blue-100">{edge.relationType || '关联'}</span>
                      </div>
                    )) : <div>暂无关联记录</div>}
                  </div>
                </div>

                <div className="space-y-3">
                  {downstreamNodes.length === 0 && (
                    <div className="text-xs text-slate-500">暂无下游数据</div>
                  )}
                  {downstreamNodes.map(node => (
                    <NodeCard
                      key={node.id}
                      node={node}
                      relation={getRelationLabel(node.id)}
                      active={selectedNode?.id === node.id}
                      onSelect={() => setSelectedNode(node)}
                    />
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="flex-1 flex items-center justify-center text-slate-400">
              <div className="text-center">
                <FileText size={32} className="mx-auto mb-3" />
                <p className="text-sm">暂无关系数据，请输入档号查询</p>
              </div>
            </div>
          )}
        </div>

        {/* Detail Drawer */}
        <div className={`w-96 bg-white border-l border-slate-200 shadow-xl transition-all duration-300 flex flex-col ${selectedNode ? 'translate-x-0' : 'translate-x-full absolute right-0 h-full'}`}>
          {selectedNode ? (
            <>
              <div className="p-6 border-b border-slate-100 flex justify-between items-start">
                <div>
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-1">{selectedNode.type.toUpperCase()}</span>
                  <h3 className="text-xl font-bold text-slate-800 leading-tight">{selectedNode.code || selectedNode.name}</h3>
                </div>
              </div>

              <div className="p-6 flex-1 overflow-y-auto space-y-6">
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-sm text-slate-500">当前状态</span>
                    <span className="px-2 py-1 bg-emerald-100 text-emerald-700 text-xs font-bold rounded">{selectedNode.status || '未知'}</span>
                  </div>
                  <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
                    <div className="bg-emerald-500 w-full h-full rounded-full"></div>
                  </div>
                </div>

                <div className="space-y-4 text-sm text-slate-700">
                  <div>
                    <div className="text-xs text-slate-400 font-medium">名称</div>
                    <div className="font-medium">{selectedNode.name || '--'}</div>
                  </div>
                  {selectedNode.amount && (
                    <div>
                      <div className="text-xs text-slate-400 font-medium">金额</div>
                      <div className="font-mono font-bold">{selectedNode.amount}</div>
                    </div>
                  )}
                  <div>
                    <div className="text-xs text-slate-400 font-medium">业务日期</div>
                    <div>{selectedNode.date || '--'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-400 font-medium">摘要</div>
                    <div className="text-slate-600 leading-relaxed">
                      {getRelationLabel(selectedNode.id) || '暂无关联描述'}
                    </div>
                  </div>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center flex-col text-slate-300 p-8 text-center">
              <FileText size={48} className="mb-4 opacity-50" />
              <p className="font-medium">选择任意节点查看详情</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default RelationshipQueryView;
