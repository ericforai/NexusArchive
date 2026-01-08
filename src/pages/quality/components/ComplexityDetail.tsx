// Input: 文件违规列表
// Output: 详细报告表格组件
// Pos: src/pages/quality/components/ 详情组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useMemo } from 'react';
import type { FileViolation } from '../types';

interface ComplexityDetailProps {
    /** 违规文件列表 */
    violations: FileViolation[];
}

type SortField = 'path' | 'lines' | 'maxFunctionLines' | 'complexity';
type FilterType = 'all' | 'high' | 'medium' | 'low';

/**
 * 获取严重程度
 */
const getSeverity = (file: FileViolation): 'high' | 'medium' | 'low' => {
    if (file.complexity > 15 || file.maxFunctionLines > 100) return 'high';
    if (file.complexity > 10 || file.maxFunctionLines > 50) return 'medium';
    return 'low';
};

/**
 * 严重程度颜色映射
 */
const severityColor = {
    high: 'text-red-500',
    medium: 'text-amber-500',
    low: 'text-green-500'
};

const severityBg = {
    high: 'bg-red-50',
    medium: 'bg-amber-50',
    low: 'bg-green-50'
};

const severityIcon = {
    high: '🔴',
    medium: '🟡',
    low: '🟢'
};

/**
 * 详细报告表格组件
 */
export const ComplexityDetail: React.FC<ComplexityDetailProps> = ({ violations }) => {
    const [sortField, setSortField] = useState<SortField>('complexity');
    const [sortAsc, setSortAsc] = useState(false);
    const [filter, setFilter] = useState<FilterType>('all');
    const [searchQuery, setSearchQuery] = useState('');

    /** 筛选和排序后的数据 */
    const filteredData = useMemo(() => {
        let result = [...violations];

        // 筛选
        if (filter !== 'all') {
            result = result.filter(v => getSeverity(v) === filter);
        }

        // 搜索
        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            result = result.filter(v => v.path.toLowerCase().includes(query));
        }

        // 排序
        result.sort((a, b) => {
            const aVal = a[sortField];
            const bVal = b[sortField];
            const comparison = aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
            return sortAsc ? comparison : -comparison;
        });

        return result;
    }, [violations, sortField, sortAsc, filter, searchQuery]);

    /** 切换排序 */
    const toggleSort = (field: SortField) => {
        if (sortField === field) {
            setSortAsc(!sortAsc);
        } else {
            setSortField(field);
            setSortAsc(false);
        }
    };

    /** 获取排序图标 */
    const getSortIcon = (field: SortField) => {
        if (sortField !== field) return '⇅';
        return sortAsc ? '↑' : '↓';
    };

    if (violations.length === 0) {
        return (
            <div className="flex items-center justify-center h-64 text-slate-400">
                暂无违规数据
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {/* 工具栏 */}
            <div className="flex flex-wrap gap-4 items-center">
                <div className="flex items-center gap-2">
                    <label className="text-sm text-slate-600">筛选:</label>
                    <select
                        value={filter}
                        onChange={(e) => setFilter(e.target.value as FilterType)}
                        className="border border-slate-300 rounded px-3 py-1.5 text-sm"
                    >
                        <option value="all">全部 ({violations.length})</option>
                        <option value="high">高严重度</option>
                        <option value="medium">中严重度</option>
                        <option value="low">低严重度</option>
                    </select>
                </div>
                <div className="flex items-center gap-2">
                    <label className="text-sm text-slate-600">搜索:</label>
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="文件名..."
                        className="border border-slate-300 rounded px-3 py-1.5 text-sm w-48"
                    />
                </div>
                <div className="ml-auto text-sm text-slate-500">
                    显示 {filteredData.length} / {violations.length} 条
                </div>
            </div>

            {/* 表格 */}
            <div className="border border-slate-200 rounded-lg overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-slate-50 border-b border-slate-200">
                        <tr>
                            <th
                                className="px-4 py-3 text-left cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('path')}
                            >
                                文件名 {getSortIcon('path')}
                            </th>
                            <th
                                className="px-4 py-3 text-right cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('lines')}
                            >
                                行数 {getSortIcon('lines')}
                            </th>
                            <th
                                className="px-4 py-3 text-right cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('maxFunctionLines')}
                            >
                                最大函数 {getSortIcon('maxFunctionLines')}
                            </th>
                            <th
                                className="px-4 py-3 text-right cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('complexity')}
                            >
                                复杂度 {getSortIcon('complexity')}
                            </th>
                            <th className="px-4 py-3 text-left">违规项</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredData.map((file, index) => {
                            const severity = getSeverity(file);
                            return (
                                <tr
                                    key={file.path}
                                    className={index % 2 === 0 ? 'bg-white' : 'bg-slate-50'}
                                >
                                    <td className="px-4 py-3 font-mono text-xs truncate max-w-md">
                                        {file.path}
                                    </td>
                                    <td className="px-4 py-3 text-right text-slate-600">
                                        {file.lines}
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        <span className={severityColor[severity]}>
                                            {file.maxFunctionLines}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        <span className={severityColor[severity]}>
                                            {file.complexity}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3">
                                        {file.violations.map(v => (
                                            <span
                                                key={v}
                                                className={`inline-block px-2 py-0.5 rounded text-xs mr-1 ${severityBg[severity]} ${severityColor[severity]}`}
                                            >
                                                {severityIcon[severity]} {v}
                                            </span>
                                        ))}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};
