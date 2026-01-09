// Input: React、lucide-react、API types
// Output: HistoryTab 组件
// Pos: src/pages/admin/LegacyImportPage/components/HistoryTab.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import {
    RefreshCw,
    Download,
    Loader2,
} from 'lucide-react';
import type { ImportHistoryItem } from '../types';

/**
 * HistoryTab 组件属性
 */
export interface HistoryTabProps {
    /** 是否正在加载 */
    loading: boolean;
    /** 任务列表 */
    tasks: ImportHistoryItem[];
    /** 当前页码 */
    currentPage: number;
    /** 总记录数 */
    total: number;
    /** 状态筛选 */
    statusFilter: string;
    /** 页码变化回调 */
    onPageChange: (page: number) => void;
    /** 状态筛选变化回调 */
    onStatusFilterChange: (status: string) => void;
    /** 刷新回调 */
    onRefresh: () => void;
    /** 下载错误报告回调 */
    onDownloadErrorReport: (importId: string) => void;
}

/**
 * 导入历史标签页组件
 *
 * 功能：
 * - 显示导入历史列表
 * - 状态筛选
 * - 分页导航
 * - 下载错误报告
 */
export const HistoryTab: React.FC<HistoryTabProps> = ({
    loading,
    tasks,
    currentPage,
    total,
    statusFilter,
    onPageChange,
    onStatusFilterChange,
    onRefresh,
    onDownloadErrorReport,
}) => {
    return (
        <div className="max-w-6xl mx-auto">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                {/* 筛选器 */}
                <HistoryFilter
                    statusFilter={statusFilter}
                    onStatusFilterChange={onStatusFilterChange}
                    onRefresh={onRefresh}
                />

                {/* 任务列表 */}
                <TaskList
                    loading={loading}
                    tasks={tasks}
                    onDownloadErrorReport={onDownloadErrorReport}
                />

                {/* 分页 */}
                {total > 20 && (
                    <Pagination
                        currentPage={currentPage}
                        total={total}
                        pageSize={20}
                        onPageChange={onPageChange}
                    />
                )}
            </div>
        </div>
    );
};

/**
 * 历史筛选器组件
 */
interface HistoryFilterProps {
    statusFilter: string;
    onStatusFilterChange: (status: string) => void;
    onRefresh: () => void;
}

const HistoryFilter: React.FC<HistoryFilterProps> = ({
    statusFilter,
    onStatusFilterChange,
    onRefresh,
}) => {
    return (
        <div className="mb-4 flex items-center gap-4">
            <select
                value={statusFilter}
                onChange={(e) => onStatusFilterChange(e.target.value)}
                className="px-3 py-2 border border-slate-300 rounded-lg"
            >
                <option value="">全部状态</option>
                <option value="SUCCESS">成功</option>
                <option value="PARTIAL_SUCCESS">部分成功</option>
                <option value="FAILED">失败</option>
                <option value="PENDING">待处理</option>
                <option value="PROCESSING">处理中</option>
            </select>
            <button
                onClick={onRefresh}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-2"
                type="button"
            >
                <RefreshCw className="w-4 h-4" />
                刷新
            </button>
        </div>
    );
};

/**
 * 任务列表组件
 */
interface TaskListProps {
    loading: boolean;
    tasks: ImportHistoryItem[];
    onDownloadErrorReport: (importId: string) => void;
}

const TaskList: React.FC<TaskListProps> = ({ loading, tasks, onDownloadErrorReport }) => {
    if (loading) {
        return (
            <div className="text-center py-8">
                <Loader2 className="w-8 h-8 animate-spin mx-auto text-slate-400" />
            </div>
        );
    }

    if (tasks.length === 0) {
        return <div className="text-center py-8 text-slate-500">暂无导入记录</div>;
    }

    return (
        <div className="overflow-x-auto">
            <table className="w-full text-sm">
                <thead className="bg-slate-50">
                    <tr>
                        <th className="px-4 py-3 text-left">文件名</th>
                        <th className="px-4 py-3 text-left">操作人</th>
                        <th className="px-4 py-3 text-left">全宗号</th>
                        <th className="px-4 py-3 text-left">总行数</th>
                        <th className="px-4 py-3 text-left">成功/失败</th>
                        <th className="px-4 py-3 text-left">状态</th>
                        <th className="px-4 py-3 text-left">创建时间</th>
                        <th className="px-4 py-3 text-left">操作</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                    {tasks.map((task) => (
                        <TaskRow
                            key={task.id}
                            task={task}
                            onDownloadErrorReport={onDownloadErrorReport}
                        />
                    ))}
                </tbody>
            </table>
        </div>
    );
};

/**
 * 任务行组件
 */
interface TaskRowProps {
    task: ImportHistoryItem;
    onDownloadErrorReport: (importId: string) => void;
}

const TaskRow: React.FC<TaskRowProps> = ({ task, onDownloadErrorReport }) => {
    return (
        <tr className="hover:bg-slate-50">
            <td className="px-4 py-3">{task.fileName || '-'}</td>
            <td className="px-4 py-3">
                {task.operatorName || (typeof task.operatorId === 'string' ? task.operatorId : '-')}
            </td>
            <td className="px-4 py-3">{task.fondsNo || '-'}</td>
            <td className="px-4 py-3">{task.totalRows ?? 0}</td>
            <td className="px-4 py-3">
                <span className="text-green-600">{task.successRows ?? 0}</span> /{' '}
                <span className="text-red-600">{task.failedRows ?? 0}</span>
            </td>
            <td className="px-4 py-3">
                <span className={`px-2 py-1 rounded text-xs ${getStatusBadge(task.status || 'PENDING')}`}>
                    {task.status || 'PENDING'}
                </span>
            </td>
            <td className="px-4 py-3">{formatDate(task.createdAt)}</td>
            <td className="px-4 py-3">
                {task.errorReportPath && (
                    <button
                        onClick={() => onDownloadErrorReport(task.id)}
                        className="text-primary-600 hover:text-primary-800 flex items-center gap-1"
                        type="button"
                    >
                        <Download className="w-4 h-4" />
                        错误报告
                    </button>
                )}
            </td>
        </tr>
    );
};

/**
 * 分页组件
 */
interface PaginationProps {
    currentPage: number;
    total: number;
    pageSize: number;
    onPageChange: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({
    currentPage,
    total,
    pageSize,
    onPageChange,
}) => {
    const totalPages = Math.ceil(total / pageSize);

    return (
        <div className="mt-4 flex items-center justify-between">
            <div className="text-sm text-slate-600">
                共 {total} 条记录，第 {currentPage} / {totalPages} 页
            </div>
            <div className="flex gap-2">
                <button
                    onClick={() => onPageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="px-3 py-1 border border-slate-300 rounded disabled:opacity-50"
                    type="button"
                >
                    上一页
                </button>
                <button
                    onClick={() => onPageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages}
                    className="px-3 py-1 border border-slate-300 rounded disabled:opacity-50"
                    type="button"
                >
                    下一页
                </button>
            </div>
        </div>
    );
};

/**
 * 获取状态标签样式
 */
function getStatusBadge(status: string): string {
    const styles = {
        SUCCESS: 'bg-green-100 text-green-800',
        PARTIAL_SUCCESS: 'bg-yellow-100 text-yellow-800',
        FAILED: 'bg-red-100 text-red-800',
        PENDING: 'bg-gray-100 text-gray-800',
        PROCESSING: 'bg-blue-100 text-blue-800',
    };
    return styles[status as keyof typeof styles] || styles.PENDING;
}

/**
 * 格式化日期
 */
function formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    try {
        const date = new Date(dateStr);
        return isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
    } catch {
        return '-';
    }
}

export default HistoryTab;
