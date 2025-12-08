import React, { useEffect, useState } from 'react';
import { ChevronRight, ChevronDown, Folder, FileText, Calendar } from 'lucide-react';
import { archivesApi, Archive } from '../../api/archives';
import { isDemoMode } from '../../utils/env';
import { safeStorage } from '../../utils/storage';
import { DemoBadge } from '../common/DemoBadge';

interface ArchiveStructureTreeProps {
    onSelectVoucher: (voucherId: string) => void;
}

interface TreeNode {
    id: string;
    label: string;
    type: 'year' | 'month' | 'voucher';
    children?: TreeNode[];
}

const MOCK_TREE_DATA: TreeNode[] = [
    {
        id: '2023',
        label: '2023年度',
        type: 'year',
        children: [
            {
                id: '2023-11',
                label: '11月',
                type: 'month',
                children: [
                    { id: 'V-202311-001', label: '记-001: 阿里云服务费', type: 'voucher' },
                    { id: 'V-202311-002', label: '记-002: 办公用品采购', type: 'voucher' },
                    { id: 'V-202311-003', label: '记-003: 员工差旅报销', type: 'voucher' },
                ]
            },
            {
                id: '2023-10',
                label: '10月',
                type: 'month',
                children: [
                    { id: 'V-202310-001', label: '记-001: 季度房租支付', type: 'voucher' },
                ]
            }
        ]
    },
    {
        id: '2025',
        label: '2025年度',
        type: 'year',
        children: [
            {
                id: '2025-11',
                label: '11月',
                type: 'month',
                children: [
                    { id: 'V-202511-TEST', label: '记-001: 报销差旅费', type: 'voucher' },
                    { id: 'C-202511-002', label: '合-002: 服务器采购合同', type: 'voucher' },
                ]
            }
        ]
    }
];

export const ArchiveStructureTree: React.FC<ArchiveStructureTreeProps> = ({ onSelectVoucher }) => {
    const [treeData, setTreeData] = useState<TreeNode[]>(MOCK_TREE_DATA);
    const [expandedNodes, setExpandedNodes] = useState<string[]>(['2023', '2023-11']);
    const [selectedNode, setSelectedNode] = useState<string>('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [usingDemo, setUsingDemo] = useState(isDemoMode());

    const buildTreeFromArchives = (archives: Archive[]): TreeNode[] => {
        const yearMap = new Map<string, TreeNode>();

        archives.forEach((archive) => {
            const year = archive.fiscalYear || (archive.docDate ? archive.docDate.split('-')[0] : '未分类');
            const month = archive.docDate ? archive.docDate.slice(0, 7) : `${year}-未知`;
            const voucherNode: TreeNode = {
                id: archive.id,
                label: `${archive.archiveCode || archive.title || archive.id}`,
                type: 'voucher'
            };

            if (!yearMap.has(year)) {
                yearMap.set(year, {
                    id: year,
                    label: `${year}年度`,
                    type: 'year',
                    children: []
                });
            }

            const yearNode = yearMap.get(year)!;
            let monthNode = yearNode.children?.find(child => child.id === month);
            if (!monthNode) {
                monthNode = {
                    id: month,
                    label: `${month.split('-')[1] || month}月`,
                    type: 'month',
                    children: []
                };
                yearNode.children?.push(monthNode);
            }
            monthNode.children?.push(voucherNode);
        });

        return Array.from(yearMap.values()).sort((a, b) => b.id.localeCompare(a.id));
    };

    const loadTree = async () => {
        setLoading(true);
        setError(null);
        try {
            if (isDemoMode()) {
                setUsingDemo(true);
                setTreeData(MOCK_TREE_DATA);
                return;
            }
            const res = await archivesApi.getArchives({ page: 1, limit: 200, categoryCode: 'AC01' });
            if (res.code === 200 && res.data) {
                const records = (res.data as any).records || [];
                if (records.length === 0) {
                    setTreeData([]);
                } else {
                    setTreeData(buildTreeFromArchives(records as Archive[]));
                }
                setUsingDemo(false);
            } else {
                setError('目录数据加载失败');
            }
        } catch (e) {
            if (isDemoMode()) {
                setUsingDemo(true);
                setTreeData(MOCK_TREE_DATA);
            } else {
                setError('目录数据加载失败');
                setTreeData([]);
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadTree();
    }, []);

    const toggleNode = (nodeId: string) => {
        if (expandedNodes.includes(nodeId)) {
            setExpandedNodes(expandedNodes.filter(id => id !== nodeId));
        } else {
            setExpandedNodes([...expandedNodes, nodeId]);
        }
    };

    const handleSelect = (node: TreeNode) => {
        setSelectedNode(node.id);
        if (node.type === 'voucher') {
            onSelectVoucher(node.id);
        } else {
            toggleNode(node.id);
        }
    };

    const renderTree = (nodes: TreeNode[], level = 0) => {
        return nodes.map(node => {
            const isExpanded = expandedNodes.includes(node.id);
            const isSelected = selectedNode === node.id;
            const hasChildren = node.children && node.children.length > 0;

            return (
                <div key={node.id} className="">
                    <div
                        className={`flex items-center py-2 px-3 cursor-pointer hover:bg-slate-100 transition-colors ${isSelected ? 'bg-primary-50 text-primary-700 font-medium' : 'text-slate-700'}`}
                        style={{ paddingLeft: `${level * 16 + 12}px` }}
                        onClick={() => handleSelect(node)}
                    >
                        <div className="mr-2 text-slate-400">
                            {hasChildren ? (
                                isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />
                            ) : (
                                <span className="w-4 inline-block" />
                            )}
                        </div>
                        <div className="mr-2">
                            {node.type === 'year' && <Folder size={16} className="text-amber-500" />}
                            {node.type === 'month' && <Calendar size={16} className="text-blue-500" />}
                            {node.type === 'voucher' && <FileText size={16} className="text-slate-500" />}
                        </div>
                        <span className="text-sm truncate">{node.label}</span>
                    </div>
                    {hasChildren && isExpanded && (
                        <div className="border-l border-slate-100 ml-6">
                            {renderTree(node.children!, level + 1)}
                        </div>
                    )}
                </div>
            );
        });
    };

    return (
        <div className="h-full flex flex-col bg-white border-r border-slate-200">
            <div className="p-4 border-b border-slate-100 bg-slate-50 flex items-center justify-between">
                <div>
                    <h3 className="font-bold text-slate-800">档案目录</h3>
                    {usingDemo && <span className="text-[10px] text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded">演示数据</span>}
                </div>
                <div className="flex items-center gap-3">
                    <button
                        onClick={() => {
                            const next = !usingDemo;
                            safeStorage.setItem('demoMode', next ? 'true' : 'false');
                            setUsingDemo(next);
                            if (!next) {
                                loadTree();
                            } else {
                                setTreeData(MOCK_TREE_DATA);
                                setExpandedNodes(['2023', '2023-11']);
                            }
                        }}
                        className="px-3 py-1.5 text-xs rounded border border-slate-200 text-slate-600 hover:bg-slate-100"
                    >
                        {usingDemo ? '关闭演示' : '开启演示'}
                    </button>
                    {loading && <span className="text-[12px] text-slate-400">加载中...</span>}
                </div>
            </div>
            <div className="flex-1 overflow-y-auto py-2">
                {usingDemo && <div className="px-3 pb-2"><DemoBadge text="当前目录为演示数据，接入真实档案目录后可关闭演示模式。" /></div>}
                {error && (
                    <div className="text-center text-xs text-rose-500 px-4 py-2">{error}</div>
                )}
                {!loading && treeData.length === 0 && !error && (
                    <div className="text-center text-xs text-slate-400 px-4 py-2">暂无数据</div>
                )}
                {renderTree(treeData)}
            </div>
        </div>
    );
};
