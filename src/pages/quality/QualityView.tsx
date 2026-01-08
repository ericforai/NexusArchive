// Input: 复杂度数据、图表和详情组件
// Output: 质量监控主页面
// Pos: src/pages/quality/ 主页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { RefreshCw, Download } from 'lucide-react';
import { useComplexityData } from './useComplexityData';
import { ComplexityChart } from './components/ComplexityChart';
import { ComplexityDetail } from './components/ComplexityDetail';

const COLORS = {
    high: 'text-red-500 bg-red-50 border-red-200',
    medium: 'text-amber-500 bg-amber-50 border-amber-200',
    low: 'text-green-500 bg-green-50 border-green-200'
};

/**
 * 代码质量监控页面
 */
export const QualityView: React.FC = () => {
    const {
        data,
        loading,
        error,
        latestSnapshot,
        getRecentSnapshots,
        getAllViolations
    } = useComplexityData();

    /** 刷新数据 */
    const handleRefresh = () => {
        window.location.reload();
    };

    /** 导出报告 */
    const handleExport = () => {
        if (!data) return;
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `complexity-report-${new Date().toISOString().split('T')[0]}.json`;
        a.click();
        URL.revokeObjectURL(url);
    };

    // 加载状态
    if (loading) {
        return (
            <div className="flex items-center justify-center h-96">
                <div className="text-slate-500">加载中...</div>
            </div>
        );
    }

    // 错误状态
    if (error) {
        return (
            <div className="flex flex-col items-center justify-center h-96 gap-4">
                <div className="text-red-500">加载失败: {error}</div>
                <button
                    onClick={handleRefresh}
                    className="px-4 py-2 bg-slate-100 rounded hover:bg-slate-200"
                >
                    重试
                </button>
            </div>
        );
    }

    // 无数据状态
    if (!latestSnapshot) {
        return (
            <div className="flex flex-col items-center justify-center h-96 gap-4 text-slate-500">
                <div>暂无复杂度数据</div>
                <div className="text-sm">提交代码后会自动生成快照</div>
            </div>
        );
    }

    const violations = getAllViolations();
    const recentSnapshots = getRecentSnapshots(30);

    return (
        <div className="p-6 space-y-6 bg-white min-h-screen">
            {/* 页头 */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">代码质量监控</h1>
                    <p className="text-slate-500 text-sm mt-1">
                        最后更新: {new Date(latestSnapshot.timestamp).toLocaleString('zh-CN')}
                    </p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={handleRefresh}
                        className="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded hover:bg-slate-50"
                    >
                        <RefreshCw size={16} />
                        刷新
                    </button>
                    <button
                        onClick={handleExport}
                        className="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded hover:bg-slate-50"
                    >
                        <Download size={16} />
                        导出报告
                    </button>
                </div>
            </div>

            {/* 概览仪表板 */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 统计卡片 */}
                <div className="space-y-4">
                    <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
                        <div className="text-sm text-slate-500 mb-1">总违规数</div>
                        <div className="text-3xl font-bold text-slate-900">
                            {latestSnapshot.summary.total}
                        </div>
                    </div>
                    <div className={`border rounded-lg p-4 ${COLORS.high}`}>
                        <div className="text-sm opacity-80 mb-1">高严重度</div>
                        <div className="text-3xl font-bold">
                            {latestSnapshot.summary.high}
                        </div>
                    </div>
                    <div className={`border rounded-lg p-4 ${COLORS.medium}`}>
                        <div className="text-sm opacity-80 mb-1">中严重度</div>
                        <div className="text-3xl font-bold">
                            {latestSnapshot.summary.medium}
                        </div>
                    </div>
                    <div className={`border rounded-lg p-4 ${COLORS.low}`}>
                        <div className="text-sm opacity-80 mb-1">低严重度</div>
                        <div className="text-3xl font-bold">
                            {latestSnapshot.summary.low}
                        </div>
                    </div>
                </div>

                {/* 趋势图表 */}
                <div className="lg:col-span-2 bg-white border border-slate-200 rounded-lg p-4">
                    <h2 className="text-lg font-semibold text-slate-900 mb-4">违规趋势</h2>
                    <ComplexityChart snapshots={recentSnapshots} />
                </div>
            </div>

            {/* Top 5 最严重文件 */}
            {violations.length > 0 && (
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
                    <h2 className="text-lg font-semibold text-slate-900 mb-3">最严重文件 Top 5</h2>
                    <div className="space-y-2">
                        {violations.slice(0, 5).map((file) => {
                            const severity = file.complexity > 15 || file.maxFunctionLines > 100 ? 'high' : 'medium';
                            const icon = severity === 'high' ? '🔴' : '🟡';
                            return (
                                <div
                                    key={file.path}
                                    className="flex items-center justify-between bg-white border border-slate-200 rounded p-3"
                                >
                                    <div className="flex items-center gap-3">
                                        <span className="text-lg">{icon}</span>
                                        <span className="font-mono text-sm">{file.path}</span>
                                    </div>
                                    <div className="text-sm text-slate-600">
                                        {file.maxFunctionLines > 50 ? `${file.maxFunctionLines} 行` : `复杂度 ${file.complexity}`}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            {/* 详细报告 */}
            <div className="bg-white border border-slate-200 rounded-lg p-4">
                <h2 className="text-lg font-semibold text-slate-900 mb-4">详细报告</h2>
                <ComplexityDetail violations={violations} />
            </div>
        </div>
    );
};
