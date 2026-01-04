// Input: React、lucide-react、fondsHistoryApi、fondsApi
// Output: FondsHistoryListPage 组件
// Pos: 全宗沿革历史查看页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { History, Search, Loader2, Calendar, User, FileText } from 'lucide-react';
import { fondsHistoryApi, FondsHistoryDetail } from '../../api/fondsHistory';
import { fondsApi, BasFonds } from '../../api/fonds';

/**
 * 全宗沿革历史查看页面
 * 
 * 功能：
 * 1. 按全宗查询沿革历史
 * 2. 展示事件类型、时间、快照信息
 * 3. 支持导出沿革报告
 * 
 * PRD 来源: Section 1.1 - 全宗沿革可追溯
 */
export const FondsHistoryListPage: React.FC = () => {
    const [fondsList, setFondsList] = useState<BasFonds[]>([]);
    const [selectedFondsNo, setSelectedFondsNo] = useState<string>('');
    const [historyList, setHistoryList] = useState<FondsHistoryDetail[]>([]);
    const [loading, setLoading] = useState(false);
    const [searching, setSearching] = useState(false);

    useEffect(() => {
        loadFondsList();
    }, []);

    const loadFondsList = async () => {
        setLoading(true);
        try {
            const res = await fondsApi.list();
            if (res.code === 200 && res.data) {
                setFondsList(res.data);
            }
        } catch (error) {
            console.error('加载全宗列表失败', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async () => {
        if (!selectedFondsNo) {
            return;
        }

        setSearching(true);
        try {
            const res = await fondsHistoryApi.getHistory(selectedFondsNo);
            if (res.code === 200 && res.data) {
                setHistoryList(res.data);
            } else {
                setHistoryList([]);
            }
        } catch (error) {
            console.error('查询沿革历史失败', error);
            setHistoryList([]);
        } finally {
            setSearching(false);
        }
    };

    const getEventTypeLabel = (eventType: string) => {
        const labels: Record<string, string> = {
            'MIGRATE': '迁移',
            'MERGE': '合并',
            'SPLIT': '分立',
            'RENAME': '重命名',
        };
        return labels[eventType] || eventType;
    };

    const getEventTypeColor = (eventType: string) => {
        const colors: Record<string, string> = {
            'MIGRATE': 'bg-blue-100 text-blue-700',
            'MERGE': 'bg-green-100 text-green-700',
            'SPLIT': 'bg-purple-100 text-purple-700',
            'RENAME': 'bg-orange-100 text-orange-700',
        };
        return colors[eventType] || 'bg-slate-100 text-slate-700';
    };

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center gap-3">
                        <History className="w-6 h-6 text-primary-600" />
                        <h1 className="text-xl font-semibold text-slate-900">全宗沿革历史</h1>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">查询和查看全宗的沿革历史记录</p>
                </div>

                {/* Search */}
                <div className="px-6 py-4 border-b border-slate-200 bg-slate-50">
                    <div className="flex gap-4 items-end">
                        <div className="flex-1">
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                选择全宗
                            </label>
                            <select
                                value={selectedFondsNo}
                                onChange={(e) => setSelectedFondsNo(e.target.value)}
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                disabled={loading}
                            >
                                <option value="">请选择全宗</option>
                                {fondsList.map((f) => (
                                    <option key={f.id} value={f.fondsCode}>
                                        {f.fondsName} ({f.fondsCode})
                                    </option>
                                ))}
                            </select>
                        </div>
                        <button
                            onClick={handleSearch}
                            disabled={!selectedFondsNo || searching}
                            className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                        >
                            {searching ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                                <Search className="w-4 h-4" />
                            )}
                            查询
                        </button>
                    </div>
                </div>

                {/* History List */}
                <div className="flex-1 overflow-y-auto p-6">
                    {loading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                        </div>
                    ) : historyList.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                            <History className="w-12 h-12 mb-2" />
                            <p>{selectedFondsNo ? '该全宗暂无沿革历史记录' : '请选择全宗后查询'}</p>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {historyList.map((history) => (
                                <div
                                    key={history.id}
                                    className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                                >
                                    <div className="flex items-start justify-between mb-3">
                                        <div className="flex items-center gap-3">
                                            <span className={`px-3 py-1 rounded-full text-xs font-medium ${getEventTypeColor(history.eventType)}`}>
                                                {getEventTypeLabel(history.eventType)}
                                            </span>
                                            <span className="text-sm text-slate-600">
                                                全宗号: {history.fondsNo}
                                            </span>
                                        </div>
                                        <span className="text-xs text-slate-400">
                                            {new Date(history.createdAt).toLocaleString('zh-CN')}
                                        </span>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4 mb-3">
                                        {history.fromFondsNo && (
                                            <div className="flex items-center gap-2 text-sm">
                                                <span className="text-slate-500">源全宗:</span>
                                                <span className="font-medium">{history.fromFondsNo}</span>
                                            </div>
                                        )}
                                        {history.toFondsNo && (
                                            <div className="flex items-center gap-2 text-sm">
                                                <span className="text-slate-500">目标全宗:</span>
                                                <span className="font-medium">{history.toFondsNo}</span>
                                            </div>
                                        )}
                                        <div className="flex items-center gap-2 text-sm">
                                            <Calendar className="w-4 h-4 text-slate-400" />
                                            <span className="text-slate-500">生效日期:</span>
                                            <span className="font-medium">{history.effectiveDate}</span>
                                        </div>
                                        {history.createdBy && (
                                            <div className="flex items-center gap-2 text-sm">
                                                <User className="w-4 h-4 text-slate-400" />
                                                <span className="text-slate-500">操作人:</span>
                                                <span className="font-medium">{history.createdBy}</span>
                                            </div>
                                        )}
                                    </div>

                                    {history.reason && (
                                        <div className="mb-3">
                                            <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                                <FileText className="w-4 h-4" />
                                                <span>原因说明</span>
                                            </div>
                                            <p className="text-sm text-slate-700 bg-slate-50 p-2 rounded">
                                                {history.reason}
                                            </p>
                                        </div>
                                    )}

                                    {history.snapshot && Object.keys(history.snapshot).length > 0 && (
                                        <details className="mt-3">
                                            <summary className="text-sm text-slate-500 cursor-pointer hover:text-slate-700">
                                                查看快照信息
                                            </summary>
                                            <pre className="mt-2 p-3 bg-slate-50 rounded text-xs overflow-x-auto">
                                                {JSON.stringify(history.snapshot, null, 2)}
                                            </pre>
                                        </details>
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

export default FondsHistoryListPage;



