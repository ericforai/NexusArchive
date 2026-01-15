// Input: React、lucide-react、enterpriseArchitectureApi、useFondsStore
// Output: EnterpriseArchitecturePage 组件
// Pos: 集团架构树视图页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Building2, FolderOpen, FileText, ChevronRight, ChevronDown, Loader2, Database, HardDrive, RefreshCw } from 'lucide-react';
import { enterpriseArchitectureApi, EnterpriseArchitectureTree, EntityNode, FondsNode } from '../../api/enterpriseArchitecture';
import { useFondsStore } from '../../store';
import { adminApi } from '../../api/admin';
import { toast } from '../../utils/notificationService';

export const EnterpriseArchitecturePage: React.FC = () => {
    const [tree, setTree] = useState<EnterpriseArchitectureTree | null>(null);
    const [loading, setLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [expandedEntities, setExpandedEntities] = useState<Set<string>>(new Set());
    const [expandedFonds, setExpandedFonds] = useState<Set<string>>(new Set());
    const navigate = useNavigate();

    // 直接获取 setCurrentFonds（不依赖 fondsList，因为数据源不同）
    const setCurrentFonds = useFondsStore((state) => state.setCurrentFonds);

    useEffect(() => {
        loadTree();
    }, []);

    /**
     * 过滤部门数据（前端双重保险）
     * 电子会计档案系统只管理法人实体，不管理部门
     */
    const filterDepartmentEntities = (entities: EntityNode[]): EntityNode[] => {
        return entities.filter(entity => {
            // 有税号的，肯定是法人实体
            if (entity.taxId) {
                return true;
            }
            // 名称以"部"结尾且没有税号的，视为部门（过滤）
            if (entity.name.endsWith('部')) {
                return false;
            }
            // 名称包含"部门"的，视为部门（过滤）
            if (entity.name.includes('部门')) {
                return false;
            }
            return true;
        });
    };

    const loadTree = async () => {
        setLoading(true);
        try {
            const res = await enterpriseArchitectureApi.getTree();
            if (res.code === 200 && res.data) {
                // 前端过滤：确保不显示部门数据（双重保险）
                const filteredEntities = filterDepartmentEntities(res.data.entities);
                setTree({ ...res.data, entities: filteredEntities });
                // 默认展开所有法人
                const entityIds = new Set(filteredEntities.map(e => e.id));
                setExpandedEntities(entityIds);
            }
        } catch (error) {
            console.error('加载集团架构树失败', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSyncFromErp = async () => {
        if (!window.confirm('确认从 YonSuite 同步法人实体吗？此操作将同步法人数据（不包含部门）。')) return;
        setSyncing(true);
        try {
            const res = await adminApi.syncOrgFromErp();
            if (res.code === 200 && res.data) {
                toast.success(res.data.message || '同步成功');
                // 重新加载集团架构树
                await loadTree();
                // 显示详细结果
                if (res.data.errors && res.data.errors.length > 0) {
                    toast.warning(`同步完成，但有 ${res.data.errorCount} 条失败`);
                }
            } else {
                toast.error(res.message || '同步失败');
            }
        } catch (e: any) {
            toast.error(e?.response?.data?.message || '同步失败');
        } finally {
            setSyncing(false);
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

    // toggleFonds 预留用于全宗展开/收起功能
    const _toggleFonds = (fondsId: string) => {
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
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <Building2 className="w-6 h-6 text-primary-600" />
                            <div>
                                <h1 className="text-xl font-semibold text-slate-900">集团架构树视图</h1>
                                <p className="text-sm text-slate-500 mt-1">直观展示&quot;法人 &rarr; 全宗 &rarr; 档案&quot;的层级关系和统计信息</p>
                            </div>
                        </div>
                        <button
                            onClick={handleSyncFromErp}
                            disabled={syncing || loading}
                            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50"
                        >
                            <RefreshCw size={16} className={`mr-2 ${syncing ? 'animate-spin' : ''}`} />
                            {syncing ? '同步中...' : '从 ERP 同步法人'}
                        </button>
                    </div>
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
                                                    <span className={`px-2 py-0.5 rounded text-xs ${entity.status === 'ACTIVE'
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
                                    {expandedEntities.has(entity.id) && (
                                        <div className="bg-white border-t border-slate-200">
                                            {entity.fonds && entity.fonds.length > 0 ? (
                                                entity.fonds.map((fonds: FondsNode) => (
                                                <div key={fonds.id} className="border-b border-slate-100 last:border-b-0">
                                                    <div className="px-4 py-3 pl-12 hover:bg-slate-50 transition-colors">
                                                        <div className="flex items-center justify-between">
                                                            <div className="flex items-center gap-3 flex-1">
                                                                <FolderOpen className="w-4 h-4 text-blue-600" />
                                                                <div>
                                                                    <div className="flex items-center gap-2">
                                                                        <span
                                                                            className="font-medium text-slate-800 cursor-pointer hover:text-primary-600 hover:underline"
                                                                            onClick={(e) => {
                                                                                e.stopPropagation();
                                                                                // 直接使用 API 返回的全宗数据构造 BasFonds 对象
                                                                                setCurrentFonds({
                                                                                    id: fonds.id,
                                                                                    fondsCode: fonds.fondsCode,
                                                                                    fondsName: fonds.fondsName,
                                                                                });
                                                                                navigate('/system/archive');
                                                                            }}
                                                                        >
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
                                                ))
                                            ) : (
                                                <div className="px-4 py-3 pl-12 text-sm text-slate-400 flex items-center gap-2">
                                                    <FolderOpen className="w-4 h-4" />
                                                    <span>该法人下暂无全宗</span>
                                                </div>
                                            )}
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






