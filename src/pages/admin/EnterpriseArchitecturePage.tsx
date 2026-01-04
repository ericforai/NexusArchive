// Input: React、lucide-react、enterpriseArchitectureApi
// Output: EnterpriseArchitecturePage 组件
// Pos: 集团架构树视图页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Building2, FolderOpen, FileText, ChevronRight, ChevronDown, Loader2, Database, HardDrive } from 'lucide-react';
import { enterpriseArchitectureApi, EnterpriseArchitectureTree, EntityNode, FondsNode } from '../../api/enterpriseArchitecture';

export const EnterpriseArchitecturePage: React.FC = () => {
    const [tree, setTree] = useState<EnterpriseArchitectureTree | null>(null);
    const [loading, setLoading] = useState(false);
    const [expandedEntities, setExpandedEntities] = useState<Set<string>>(new Set());
    const [expandedFonds, setExpandedFonds] = useState<Set<string>>(new Set());

    useEffect(() => {
        loadTree();
    }, []);

    const loadTree = async () => {
        setLoading(true);
        try {
            const res = await enterpriseArchitectureApi.getTree();
            if (res.code === 200 && res.data) {
                setTree(res.data);
                // 默认展开所有法人
                const entityIds = new Set(res.data.entities.map(e => e.id));
                setExpandedEntities(entityIds);
            }
        } catch (error) {
            console.error('加载集团架构树失败', error);
        } finally {
            setLoading(false);
        }
    };

    const toggleEntity = (entityId: string) => {
        const newExpanded = new Set(expandedEntities);
        if (newExpanded.has(entityId)) {
            newExpanded.delete(entityId);
        } else {
            newExpanded.add(entityId);
        }
        setExpandedEntities(newExpanded);
    };

    const toggleFonds = (fondsId: string) => {
        const newExpanded = new Set(expandedFonds);
        if (newExpanded.has(fondsId)) {
            newExpanded.delete(fondsId);
        } else {
            newExpanded.add(fondsId);
        }
        setExpandedFonds(newExpanded);
    };

    const formatSize = (sizeGB: number) => {
        if (sizeGB < 1) {
            return `${(sizeGB * 1024).toFixed(2)} MB`;
        } else if (sizeGB < 1024) {
            return `${sizeGB.toFixed(2)} GB`;
        } else {
            return `${(sizeGB / 1024).toFixed(2)} TB`;
        }
    };

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center gap-3">
                        <Building2 className="w-6 h-6 text-primary-600" />
                        <h1 className="text-xl font-semibold text-slate-900">集团架构树视图</h1>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">直观展示&quot;法人 &rarr; 全宗 &rarr; 档案&quot;的层级关系和统计信息</p>
                </div>

                {/* Tree Content */}
                <div className="flex-1 overflow-y-auto p-6">
                    {loading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                        </div>
                    ) : !tree || tree.entities.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                            <Building2 className="w-12 h-12 mb-2" />
                            <p>暂无数据，请先创建法人和全宗</p>
                        </div>
                    ) : (
                        <div className="space-y-2">
                            {tree.entities.map((entity: EntityNode) => (
                                <div key={entity.id} className="border border-slate-200 rounded-lg overflow-hidden">
                                    {/* Entity Node */}
                                    <div
                                        className="bg-slate-50 px-4 py-3 cursor-pointer hover:bg-slate-100 transition-colors flex items-center justify-between"
                                        onClick={() => toggleEntity(entity.id)}
                                    >
                                        <div className="flex items-center gap-3 flex-1">
                                            {expandedEntities.has(entity.id) ? (
                                                <ChevronDown className="w-5 h-5 text-slate-400" />
                                            ) : (
                                                <ChevronRight className="w-5 h-5 text-slate-400" />
                                            )}
                                            <Building2 className="w-5 h-5 text-primary-600" />
                                            <div className="flex-1">
                                                <div className="flex items-center gap-2">
                                                    <span className="font-semibold text-slate-900">{entity.name}</span>
                                                    {entity.taxId && (
                                                        <span className="text-xs text-slate-500 font-mono">
                                                            ({entity.taxId})
                                                        </span>
                                                    )}
                                                    <span className={`px-2 py-0.5 rounded text-xs ${
                                                        entity.status === 'ACTIVE'
                                                            ? 'bg-green-100 text-green-700'
                                                            : 'bg-slate-100 text-slate-700'
                                                    }`}>
                                                        {entity.status === 'ACTIVE' ? '活跃' : '停用'}
                                                    </span>
                                                </div>
                                                <div className="flex items-center gap-4 mt-1 text-xs text-slate-500">
                                                    <span className="flex items-center gap-1">
                                                        <FolderOpen className="w-3 h-3" />
                                                        全宗: {entity.fondsCount} 个
                                                    </span>
                                                    <span className="flex items-center gap-1">
                                                        <FileText className="w-3 h-3" />
                                                        档案: {entity.archiveCount.toLocaleString()} 件
                                                    </span>
                                                    <span className="flex items-center gap-1">
                                                        <HardDrive className="w-3 h-3" />
                                                        容量: {formatSize(entity.totalSizeGB)}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Fonds Nodes */}
                                    {expandedEntities.has(entity.id) && entity.fonds && entity.fonds.length > 0 && (
                                        <div className="bg-white border-t border-slate-200">
                                            {entity.fonds.map((fonds: FondsNode) => (
                                                <div key={fonds.id} className="border-b border-slate-100 last:border-b-0">
                                                    <div className="px-4 py-3 pl-12 hover:bg-slate-50 transition-colors">
                                                        <div className="flex items-center justify-between">
                                                            <div className="flex items-center gap-3 flex-1">
                                                                <FolderOpen className="w-4 h-4 text-blue-600" />
                                                                <div>
                                                                    <div className="flex items-center gap-2">
                                                                        <span className="font-medium text-slate-800">
                                                                            {fonds.fondsName}
                                                                        </span>
                                                                        <span className="text-xs text-slate-500 font-mono">
                                                                            ({fonds.fondsCode})
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex items-center gap-4 mt-1 text-xs text-slate-500">
                                                                        <span className="flex items-center gap-1">
                                                                            <FileText className="w-3 h-3" />
                                                                            档案: {fonds.archiveCount.toLocaleString()} 件
                                                                        </span>
                                                                        <span className="flex items-center gap-1">
                                                                            <Database className="w-3 h-3" />
                                                                            年度: {fonds.archiveYearCount} 个
                                                                        </span>
                                                                        <span className="flex items-center gap-1">
                                                                            <HardDrive className="w-3 h-3" />
                                                                            容量: {formatSize(fonds.sizeGB)}
                                                                        </span>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default EnterpriseArchitecturePage;



