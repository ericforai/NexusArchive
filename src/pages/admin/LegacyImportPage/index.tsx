// Input: React、lucide-react、legacyImportApi、子组件
// Output: LegacyImportPage 主组件
// Pos: src/pages/admin/LegacyImportPage/index.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import {
    FileText,
    History,
} from 'lucide-react';
import {
    legacyImportApi,
} from '../../../api/legacyImport';
import { toast } from '../../../utils/notificationService';
import { ImportTab } from './components/ImportTab';
import { HistoryTab } from './components/HistoryTab';
import type { ActiveTab, ImportPreviewData, ImportResultData, ImportHistoryItem } from './types';

/**
 * 历史数据导入页面
 *
 * OpenSpec 来源: openspec-legacy-data-import.md
 *
 * 功能：
 * 1. 文件上传（支持拖拽）
 * 2. 导入预览（显示解析结果和验证错误）
 * 3. 执行导入（显示进度）
 * 4. 导入历史查询
 * 5. 错误报告下载
 *
 * 架构说明：
 * - 主组件负责：标签页切换、状态管理、API调用
 * - ImportTab 组件负责：导入相关UI（合规警告、指南、模板下载、文件上传、预览/导入结果）
 * - HistoryTab 组件负责：历史列表UI（筛选、列表展示、分页）
 * - FileUploader 组件负责：文件选择、拖拽上传、文件验证
 * - useFileUpload hook 负责：文件验证逻辑
 */
export const LegacyImportPage: React.FC = () => {
    // 标签页状态
    const [activeTab, setActiveTab] = useState<ActiveTab>('import');

    // 文件状态
    const [file, setFile] = useState<File | null>(null);

    // 预览和导入状态
    const [previewResult, setPreviewResult] = useState<ImportPreviewData | null>(null);
    const [importResult, setImportResult] = useState<ImportResultData | null>(null);
    const [loading, setLoading] = useState(false);
    const [importing, setImporting] = useState(false);

    // 历史列表状态
    const [historyLoading, setHistoryLoading] = useState(false);
    const [tasks, setTasks] = useState<ImportHistoryItem[]>([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [total, setTotal] = useState(0);
    const [statusFilter, setStatusFilter] = useState<string>('');

    /**
     * 加载导入历史
     */
    const loadHistory = useCallback(async () => {
        setHistoryLoading(true);
        try {
            const res = await legacyImportApi.getTasks(currentPage, 20, statusFilter || undefined);
            if (res.code === 200 && res.data) {
                setTasks(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载导入历史失败', error);
        } finally {
            setHistoryLoading(false);
        }
    }, [currentPage, statusFilter]);

    // 标签页切换时加载历史数据
    useEffect(() => {
        if (activeTab === 'history') {
            loadHistory();
        }
    }, [activeTab, loadHistory]);

    /**
     * 预览导入数据
     */
    const handlePreview = async () => {
        if (!file) {
            toast.warning('请先选择文件');
            return;
        }

        setLoading(true);
        try {
            const res = await legacyImportApi.preview(file);
            if (res.code === 200 && res.data) {
                setPreviewResult(res.data);
                toast.success('预览成功');
            } else {
                toast.error(res.message || '预览失败');
            }
        } catch (error: any) {
            toast.error(error?.response?.data?.message || '预览失败');
        } finally {
            setLoading(false);
        }
    };

    /**
     * 执行导入
     */
    const handleImport = async () => {
        if (!file) {
            toast.warning('请先选择文件');
            return;
        }

        if (!previewResult) {
            toast.warning('请先预览数据');
            return;
        }

        if (!window.confirm('确认导入数据吗？导入过程可能需要较长时间。')) {
            return;
        }

        setImporting(true);
        try {
            const res = await legacyImportApi.import(file);
            if (res.code === 200 && res.data) {
                setImportResult(res.data);
                toast.success('导入成功');
                // 导入成功后，刷新历史列表
                if (activeTab === 'history') {
                    loadHistory();
                }
            } else {
                toast.error(res.message || '导入失败');
            }
        } catch (error: any) {
            toast.error(error?.response?.data?.message || '导入失败');
        } finally {
            setImporting(false);
        }
    };

    /**
     * 下载错误报告
     */
    const handleDownloadErrorReport = async (importId: string) => {
        try {
            const blob = await legacyImportApi.downloadErrorReport(importId);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `error_report_${importId}.xlsx`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            toast.success('错误报告下载成功');
        } catch {
            toast.error('下载错误报告失败');
        }
    };

    /**
     * 下载CSV模板
     */
    const handleDownloadCsvTemplate = async () => {
        try {
            const blob = await legacyImportApi.downloadCsvTemplate();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'legacy-import-template.csv';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            toast.success('CSV模板下载成功');
        } catch {
            toast.error('下载CSV模板失败');
        }
    };

    /**
     * 下载Excel模板
     */
    const handleDownloadExcelTemplate = async () => {
        try {
            const blob = await legacyImportApi.downloadExcelTemplate();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'legacy-import-template.xlsx';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            toast.success('Excel模板下载成功');
        } catch {
            toast.error('下载Excel模板失败');
        }
    };

    /**
     * 处理文件变化
     */
    const handleFileChange = (newFile: File | null) => {
        setFile(newFile);
        if (newFile) {
            // 清除之前的预览和导入结果
            setPreviewResult(null);
            setImportResult(null);
        }
    };

    return (
        <div className="h-full flex flex-col bg-slate-50">
            {/* Header */}
            <PageHeader activeTab={activeTab} onTabChange={setActiveTab} />

            {/* Content */}
            <div className="flex-1 overflow-auto p-6">
                {activeTab === 'import' ? (
                    <ImportTab
                        file={file}
                        loading={loading}
                        importing={importing}
                        previewResult={previewResult}
                        importResult={importResult}
                        onFileChange={handleFileChange}
                        onPreview={handlePreview}
                        onImport={handleImport}
                        onDownloadCsvTemplate={handleDownloadCsvTemplate}
                        onDownloadExcelTemplate={handleDownloadExcelTemplate}
                        onDownloadErrorReport={handleDownloadErrorReport}
                    />
                ) : (
                    <HistoryTab
                        loading={historyLoading}
                        tasks={tasks}
                        currentPage={currentPage}
                        total={total}
                        statusFilter={statusFilter}
                        onPageChange={setCurrentPage}
                        onStatusFilterChange={(status) => {
                            setStatusFilter(status);
                            setCurrentPage(1);
                        }}
                        onRefresh={loadHistory}
                        onDownloadErrorReport={handleDownloadErrorReport}
                    />
                )}
            </div>
        </div>
    );
};

/**
 * 页面头部组件
 */
interface PageHeaderProps {
    activeTab: ActiveTab;
    onTabChange: (tab: ActiveTab) => void;
}

const PageHeader: React.FC<PageHeaderProps> = ({ activeTab, onTabChange }) => {
    return (
        <div className="bg-white border-b border-slate-200 px-6 py-4">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <FileText className="w-6 h-6 text-primary-600" />
                    <h1 className="text-xl font-semibold text-slate-900">历史数据导入</h1>
                </div>
            </div>

            {/* Tabs */}
            <div className="mt-4 flex gap-4 border-b border-slate-200">
                <TabButton
                    active={activeTab === 'import'}
                    onClick={() => onTabChange('import')}
                >
                    数据导入
                </TabButton>
                <TabButton
                    active={activeTab === 'history'}
                    onClick={() => onTabChange('history')}
                    icon={<History className="w-4 h-4 inline-block mr-2" />}
                >
                    导入历史
                </TabButton>
            </div>
        </div>
    );
};

/**
 * 标签页按钮组件
 */
interface TabButtonProps {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
    icon?: React.ReactNode;
}

const TabButton: React.FC<TabButtonProps> = ({ active, onClick, children, icon }) => {
    return (
        <button
            onClick={onClick}
            className={`px-4 py-2 font-medium ${
                active
                    ? 'text-primary-600 border-b-2 border-primary-600'
                    : 'text-slate-600 hover:text-slate-900'
            }`}
            type="button"
        >
            {icon}
            {children}
        </button>
    );
};

// 默认导出以支持懒加载
export default LegacyImportPage;
