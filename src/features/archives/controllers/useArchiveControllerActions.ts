/**
 * useArchiveActions - Actions Management Hook
 *
 * Handles actions like export, reload, etc.
 */
import { useCallback } from 'react';
import { UseArchiveActionsOptions, ControllerActions } from './types';

export function useArchiveCsvActions(options: UseArchiveActionsOptions): ControllerActions {
    const {
        mode,
        data,
        showToast,
        reload,
    } = options;

    const { rows } = data;

    // Export CSV
    const exportCsv = useCallback(() => {
        if (rows.length === 0) {
            showToast('没有数据可导出', 'error');
            return;
        }
        const headers = mode.config.columns.map((c: any) => c.header).join(',');
        const csvRows = rows.map(row =>
            mode.config.columns.map((c: any) => row[c.key]).join(',')
        ).join('\n');
        const csvContent = `data:text/csv;charset=utf-8,\uFEFF${headers}\n${csvRows}`;
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", `${mode.title}_${mode.subTitle || 'data'}_export.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast('导出成功，正在下载文件');
    }, [rows, mode.config, mode.title, mode.subTitle, showToast]);

    return {
        reload,
        exportCsv,
    };
}
