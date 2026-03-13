// Input: legacyImportApi, toast, ActiveTab types
// Output: useLegacyImport hook (主控制器)
// Pos: src/pages/admin/LegacyImportPage/hooks/useLegacyImport.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useEffect, useCallback } from 'react';
import { legacyImportApi } from '../../../../api/legacyImport';
import { toast } from '../../../../utils/notificationService';
import type { ActiveTab, ImportPreviewData, ImportResultData, ImportHistoryItem } from '../types';

export const useLegacyImport = () => {
    const [activeTab, setActiveTab] = useState<ActiveTab>('import');
    const [file, setFile] = useState<File | null>(null);
    const [previewResult, setPreviewResult] = useState<ImportPreviewData | null>(null);
    const [importResult, setImportResult] = useState<ImportResultData | null>(null);
    const [loading, setLoading] = useState(false);
    const [importing, setImporting] = useState(false);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [tasks, setTasks] = useState<ImportHistoryItem[]>([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [total, setTotal] = useState(0);
    const [statusFilter, setStatusFilter] = useState<string>('');

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

    useEffect(() => {
        if (activeTab === 'history') loadHistory();
    }, [activeTab, loadHistory]);

    const handlePreview = async () => {
        if (!file) { toast.warning('请先选择文件'); return; }
        setLoading(true);
        try {
            const res = await legacyImportApi.preview(file);
            if (res.code === 200 && res.data) {
                setPreviewResult(res.data);
                toast.success('预览成功');
            } else { toast.error(res.message || '预览失败'); }
        } catch (error: any) { toast.error(error?.response?.data?.message || '预览失败'); }
        finally { setLoading(false); }
    };

    const handleImport = async () => {
        if (!file || !previewResult) { toast.warning('请先预览数据'); return; }
        if (!window.confirm('确认导入数据吗？')) return;
        setImporting(true);
        try {
            const res = await legacyImportApi.import(file);
            if (res.code === 200 && res.data) {
                setImportResult(res.data);
                toast.success('导入成功');
                if (activeTab === 'history') loadHistory();
            } else { toast.error(res.message || '导入失败'); }
        } catch (error: any) { toast.error(error?.response?.data?.message || '导入失败'); }
        finally { setImporting(false); }
    };

    const handleDownloadErrorReport = async (id: string) => {
        try {
            const blob = await legacyImportApi.downloadErrorReport(id);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url; a.download = `error_report_${id}.xlsx`;
            document.body.appendChild(a); a.click();
            document.body.removeChild(a); window.URL.revokeObjectURL(url);
        } catch { toast.error('下载失败'); }
    };

    const handleDownloadTemplate = async (type: 'csv' | 'excel') => {
        try {
            const blob = type === 'csv' ? await legacyImportApi.downloadCsvTemplate() : await legacyImportApi.downloadExcelTemplate();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url; a.download = `template.${type === 'csv' ? 'csv' : 'xlsx'}`;
            document.body.appendChild(a); a.click();
            document.body.removeChild(a); window.URL.revokeObjectURL(url);
        } catch { toast.error('下载模板失败'); }
    };

    const handleFileChange = (newFile: File | null) => {
        setFile(newFile);
        if (newFile) { setPreviewResult(null); setImportResult(null); }
    };

    return {
        activeTab, setActiveTab, file, loading, importing, previewResult, importResult,
        historyLoading, tasks, currentPage, total, statusFilter, setCurrentPage, setStatusFilter,
        loadHistory, handlePreview, handleImport, handleDownloadErrorReport,
        handleDownloadCsvTemplate: () => handleDownloadTemplate('csv'),
        handleDownloadExcelTemplate: () => handleDownloadTemplate('excel'),
        handleFileChange,
    };
};
