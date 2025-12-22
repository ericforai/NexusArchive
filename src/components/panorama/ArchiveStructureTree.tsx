// Input: React、lucide-react 图标、本地模块 api/archives
// Output: React 组件 ArchiveStructureTree
// Pos: 归档全景子组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { ChevronRight, ChevronDown, Folder, FileText, Calendar } from 'lucide-react';
import { archivesApi, Archive } from '../../api/archives';

interface ArchiveStructureTreeProps {
    onSelectVoucher: (voucherId: string) => void;
}

interface TreeNode {
    id: string;
    label: string;
    type: 'year' | 'month' | 'voucher';
    children?: TreeNode[];
}

// Mock 数据已移除 - 档案树通过 API 动态构建

export const ArchiveStructureTree: React.FC<ArchiveStructureTreeProps> = ({ onSelectVoucher }) => {
    const [treeData, setTreeData] = useState<TreeNode[]>([]);
    const [expandedNodes, setExpandedNodes] = useState<string[]>([]);
    const [selectedNode, setSelectedNode] = useState<string>('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

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
            const res = await archivesApi.getArchives({ page: 1, limit: 200, categoryCode: 'AC01' });
            if (res.code === 200 && res.data) {
                const records = (res.data as any).records || [];
                if (records.length === 0) {
                    // 无数据时显示空状态
                    setTreeData([]);
                } else {
                    const tree = buildTreeFromArchives(records as Archive[]);
                    setTreeData(tree);
                    // 自动展开所有年度和月份
                    const expandIds = tree.flatMap(y => [y.id, ...(y.children?.map(m => m.id) || [])]);
                    setExpandedNodes(expandIds);
                }
            } else {
                // 加载失败显示空状态
                setTreeData([]);
            }
        } catch (e) {
            console.warn('Failed to load tree', e);
            setTreeData([]);
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
                </div>
                <div className="flex items-center gap-3">
                    {loading && <span className="text-[12px] text-slate-400">加载中...</span>}
                </div>
            </div>
            <div className="flex-1 overflow-y-auto py-2">
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
