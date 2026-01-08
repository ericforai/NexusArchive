// Input: Recharts、复杂度快照数据
// Output: 趋势图表组件
// Pos: src/pages/quality/components/ 图表组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';
import type { ComplexitySnapshot } from '../types';

interface ComplexityChartProps {
    /** 快照数据 */
    snapshots: ComplexitySnapshot[];
}

const COLORS = {
    total: '#94a3b8',   // slate-400
    high: '#ef4444',    // red-500
    medium: '#f59e0b',  // amber-500
    low: '#22c55e'      // green-500
};

/**
 * 复杂度趋势图表
 */
export const ComplexityChart: React.FC<ComplexityChartProps> = ({ snapshots }) => {
    // 转换数据为图表格式
    const chartData = snapshots.map((snapshot, index) => ({
        index: index + 1,
        date: new Date(snapshot.timestamp).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }),
        total: snapshot.summary.total,
        high: snapshot.summary.high,
        medium: snapshot.summary.medium,
        low: snapshot.summary.low
    }));

    if (chartData.length === 0) {
        return (
            <div className="flex items-center justify-center h-64 text-slate-400">
                暂无历史数据
            </div>
        );
    }

    return (
        <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="index"
                    tickFormatter={(value) => `#${value}`}
                    stroke="#64748b"
                />
                <YAxis stroke="#64748b" />
                <Tooltip
                    contentStyle={{
                        backgroundColor: 'white',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px'
                    }}
                    labelFormatter={(value) => `提交 #${value}`}
                />
                <Legend />
                <Line
                    type="monotone"
                    dataKey="total"
                    stroke={COLORS.total}
                    name="总违规"
                    strokeWidth={2}
                    dot={false}
                />
                <Line
                    type="monotone"
                    dataKey="high"
                    stroke={COLORS.high}
                    name="高严重度"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                />
                <Line
                    type="monotone"
                    dataKey="medium"
                    stroke={COLORS.medium}
                    name="中严重度"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                />
                <Line
                    type="monotone"
                    dataKey="low"
                    stroke={COLORS.low}
                    name="低严重度"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                />
            </LineChart>
        </ResponsiveContainer>
    );
};
