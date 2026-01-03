// Input: React、lucide-react、destructionApi、useFondsStore
// Output: ExpiredArchivesPage 组件
// Pos: 到期档案识别页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Calendar, Filter, Loader2, FileText, CheckSquare, Square, Download } from 'lucide-react';
import { destructionApi, ExpiredArchive } from '../../api/destruction';
import { useFondsStore } from '../../store';
import { toast } from '../../utils/notificationService';

/**
 * 到期档案识别页面
 * 
 * 功能：
 * 1. 展示到期档案列表
 * 2. 筛选：按全宗、按年度、按保管期限
 * 3. 批量操作：批量生成鉴定清单
 * 
 * PRD 来源: Section 13 - 档案销毁
 */
export const ExpiredArchivesPage: React.FC = () => {
    const { currentFonds } = useFondsStore();
    const [archives, setArchives] = useState<ExpiredArchive[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const pageSize = 20;
    const [selectedIds, setSelectedIds] = useState<string[]>([]);
    const [filters, setFilters] = useState({
        fondsNo: '',
        fiscalYear: '',
        retentionPeriod: '',
    });

    const loadArchives = async () => {
        setLoading(true);
        try {
            const params: any = {
                page,
                size: pageSize,
            };
            if (filters.fondsNo) params.fondsNo = filters.fondsNo;
            if (filters.fiscalYear) params.fiscalYear = filters.fiscalYear;
            if (filters.retentionPeriod) params.retentionPeriod = filters.retentionPeriod;
            if (currentFonds?.fondsNo && !filters.fondsNo) {
                params.fondsNo = currentFonds.fondsNo;
            }
            
            const res = await destructionApi.getExpiredArchives(params);
            if (res.code === 200 && res.data) {
                setArchives(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载到期档案失败', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadArchives();
    }, [page, filters, currentFonds]);

    const handleSelectAll = () => {
        if (selectedIds.length === archives.length) {
            setSelectedIds([]);
        } else {
            setSelectedIds(archives.map(a => a.id));
        }
    };

    const handleSelect = (id: string) => {
        setSelectedIds(prev => 
            prev.includes(id) 
                ? prev.filter(i => i !== id)
                : [...prev, id]
        );
    };

    const handleGenerateAppraisal = async () => {
        if (selectedIds.length === 0) {
            toast.warning('请至少选择一个档案');
            return;
        }
        if (!window.confirm(`确认生成鉴定清单吗？将包含 ${selectedIds.length} 个档案`)) {
            return;
        }
        try {
            const res = await destructionApi.generateAppraisalList(selectedIds);
            if (res.code === 200) {
                toast.success('鉴定清单生成成功');
                setSelectedIds([]);
                // 可以跳转到鉴定清单页面
            } else {
                toast.error(res.message || '生成失败');
            }
        } catch (error: any) {
            toast.error(error?.response?.data?.message || '生成失败');
        }
    };

    return (
        <div className="p-6 space-y-4">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <Calendar className="mr-2" size={28} />
                        到期档案识别
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">识别已到保管期限的档案，准备进行销毁鉴定</p>
                </div>
            </div>

            {/* 筛选条件 */}
            <div className="bg-white border border-slate-200 rounded-lg p-4">
                <div className="flex items-center gap-4">
                    <Filter className="text-slate-400" size={20} />
                    <div className="flex-1 grid grid-cols-3 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">全宗号</label>
                            <input
                                type="text"
                                value={filters.fondsNo}
                                onChange={(e) => setFilters({ ...filters, fondsNo: e.target.value })}
                                placeholder="留空则使用当前全宗"
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">年度</label>
                            <input
                                type="text"
                                value={filters.fiscalYear}
                                onChange={(e) => setFilters({ ...filters, fiscalYear: e.target.value })}
                                placeholder="如：2020"
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">保管期限</label>
                            <select
                                value={filters.retentionPeriod}
                                onChange={(e) => setFilters({ ...filters, retentionPeriod: e.target.value })}
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                            >
                                <option value="">全部</option>
                                <option value="10">10年</option>
                                <option value="30">30年</option>
                                <option value="PERMANENT">永久</option>
                            </select>
                        </div>
                    </div>
                    <button
                        onClick={loadArchives}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm"
                    >
                        查询
                    </button>
                </div>
            </div>

            {/* 批量操作 */}
            {archives.length > 0 && (
                <div className="bg-white border border-slate-200 rounded-lg p-4 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <button
                            onClick={handleSelectAll}
                            className="p-2 hover:bg-slate-100 rounded"
                        >
                            {selectedIds.length === archives.length ? (
                                <CheckSquare className="text-primary-600" size={20} />
                            ) : (
                                <Square className="text-slate-400" size={20} />
                            )}
                        </button>
                        <span className="text-sm text-slate-600">
                            已选择 {selectedIds.length} / {archives.length} 项
                        </span>
                    </div>
                    <button
                        onClick={handleGenerateAppraisal}
                        disabled={selectedIds.length === 0}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed text-sm flex items-center gap-2"
                    >
                        <FileText size={16} />
                        生成鉴定清单
                    </button>
                </div>
            )}

            {/* 档案列表 */}
            <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
                {loading ? (
                    <div className="flex items-center justify-center h-64">
                        <Loader2 className="animate-spin text-slate-400" size={32} />
                    </div>
                ) : archives.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                        <Calendar size={48} className="mb-4" />
                        <p>暂无到期档案</p>
                    </div>
                ) : (
                    <>
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 border-b">
                                <tr>
                                    <th className="px-4 py-3 w-12"></th>
                                    <th className="px-4 py-3">档号</th>
                                    <th className="px-4 py-3">题名</th>
                                    <th className="px-4 py-3">全宗号</th>
                                    <th className="px-4 py-3">年度</th>
                                    <th className="px-4 py-3">保管期限</th>
                                    <th className="px-4 py-3">到期日期</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {archives.map(archive => (
                                    <tr key={archive.id} className="hover:bg-slate-50">
                                        <td className="px-4 py-3">
                                            <button
                                                onClick={() => handleSelect(archive.id)}
                                                className="p-1 hover:bg-slate-200 rounded"
                                            >
                                                {selectedIds.includes(archive.id) ? (
                                                    <CheckSquare className="text-primary-600" size={16} />
                                                ) : (
                                                    <Square className="text-slate-400" size={16} />
                                                )}
                                            </button>
                                        </td>
                                        <td className="px-4 py-3 font-mono text-xs">{archive.archiveCode}</td>
                                        <td className="px-4 py-3">{archive.title}</td>
                                        <td className="px-4 py-3">{archive.fondsNo}</td>
                                        <td className="px-4 py-3">{archive.fiscalYear}</td>
                                        <td className="px-4 py-3">{archive.retentionPeriod}</td>
                                        <td className="px-4 py-3 text-red-600">{archive.expiredDate}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                        {/* 分页 */}
                        <div className="px-4 py-3 border-t flex items-center justify-between">
                            <div className="text-sm text-slate-600">
                                共 {total} 条，第 {page} / {Math.ceil(total / pageSize)} 页
                            </div>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => setPage(p => Math.max(1, p - 1))}
                                    disabled={page <= 1}
                                    className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                                >
                                    上一页
                                </button>
                                <button
                                    onClick={() => setPage(p => Math.min(Math.ceil(total / pageSize), p + 1))}
                                    disabled={page >= Math.ceil(total / pageSize)}
                                    className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                                >
                                    下一页
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default ExpiredArchivesPage;


