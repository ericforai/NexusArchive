// Input: React、lucide-react 图标、本地模块 api/archives、store/useFondsStore
// Output: React 组件 ArchiveStructureTree
// Pos: src/pages/panorama/ArchiveStructureTree.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState, useCallback } from 'react';
import { ChevronRight, ChevronDown, Folder, FileText, Calendar } from 'lucide-react';
import { archivesApi, Archive } from '../../api/archives';
import { useFondsStore } from '../../store';

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
    // 获取当前全宗（显式依赖，提高代码可读性）
    const currentFonds = useFondsStore((state) => state.currentFonds);

    const [treeData, setTreeData] = useState<TreeNode[]>([]);
    const [expandedNodes, setExpandedNodes] = useState<string[]>([]);
    const [selectedNode, setSelectedNode] = useState<string>('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const buildTreeFromArchives = useCallback((archives: Archive[]): TreeNode[] => {
        const yearMap = new Map<string, TreeNode>();

        archives.forEach((archive) => {
            // 处理 docDate：可能是数组 [2025, 11, 7] 或字符串
            let docDateStr: string | null = null;
            if (Array.isArray(archive.docDate)) {
                // 格式: [2025, 11, 7] -> 转换为字符串
                const dateArr = archive.docDate as unknown[];
                const y = dateArr[0];
                const m = dateArr[1];
                const d = dateArr[2];
                if (typeof y === 'number' && typeof m === 'number' && typeof d === 'number') {
                    docDateStr = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
                }
            } else if (archive.docDate) {
                docDateStr = String(archive.docDate);
            }

            const year = archive.fiscalYear || (docDateStr ? docDateStr.slice(0, 4) : '未分类');

            // Filter out 2014 (demo data/expired archives)
            if (year === '2014') return;

            // 解析月份：支持多种格式
            let monthLabel = '未知月';
            let monthId = `${year}-未知`;

            if (docDateStr) {
                if (docDateStr.includes('-')) {
                    // 格式: "2025-11-15"
                    const parts = docDateStr.split('-');
                    if (parts.length >= 2) {
                        monthLabel = `${parts[1]}月`;
                        monthId = `${parts[0]}-${parts[1]}`;
                    }
                } else if (docDateStr.length === 6 && /^\d{6}$/.test(docDateStr)) {
                    // 格式: "202511"
                    monthLabel = `${docDateStr.slice(4, 6)}月`;
                    monthId = `${docDateStr.slice(0, 4)}-${docDateStr.slice(4, 6)}`;
                } else if (docDateStr.length === 8 && /^\d{8}$/.test(docDateStr)) {
                    // 格式: "20251115"
                    monthLabel = `${docDateStr.slice(4, 6)}月`;
                    monthId = `${docDateStr.slice(0, 4)}-${docDateStr.slice(4, 6)}`;
                }
            }

            const voucherNode: TreeNode = {
                id: archive.id,
                label: `${archive.archiveCode || archive.title || archive.id}`,
                type: 'voucher'
            };

            if (!yearMap.has(year)) {
                yearMap.set(year, {
                    id: year,
                    label: year,  // 直接显示年份，不添加"年度"后缀
                    type: 'year',
                    children: []
                });
            }

            const yearNode = yearMap.get(year)!;
            let monthNode = yearNode.children?.find(child => child.id === monthId);
            if (!monthNode) {
                monthNode = {
                    id: monthId,
                    label: monthLabel,
                    type: 'month',
                    children: []
                };
                yearNode.children?.push(monthNode);
            }
            monthNode.children?.push(voucherNode);
        });

        const tree = Array.from(yearMap.values()).sort((a, b) => b.id.localeCompare(a.id));

        // Sort months naturally (Ascending: 01 -> 12)
        tree.forEach(yearNode => {
            yearNode.children?.sort((a, b) => a.id.localeCompare(b.id));

            // Sort vouchers by label (Archive Code)
            yearNode.children?.forEach(monthNode => {
                monthNode.children?.sort((a, b) => a.label.localeCompare(b.label));
            });
        });

        return tree;
    }, []);

    const loadTree = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            // 显式传递全宗号，提高代码可读性和数据隔离明确性
            const res = await archivesApi.getArchives({
                page: 1,
                limit: 100,
                categoryCode: 'AC01',
                fondsNo: currentFonds?.fondsCode  // 显式全宗过滤
            });
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
            console.error('[ArchiveStructureTree] Failed to load tree:', e);
            setTreeData([]);
        } finally {
            setLoading(false);
        }
    }, [buildTreeFromArchives, currentFonds?.fondsCode]);  // 全宗切换时自动刷新

    useEffect(() => {
        loadTree();
    }, [loadTree]);

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
