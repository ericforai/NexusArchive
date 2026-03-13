// Input: React、lucide-react、useLegacyImport hook、子组件
// Output: LegacyImportPage 主组件
// Pos: src/pages/admin/LegacyImportPage/index.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { FileText, History } from 'lucide-react';
import { ImportTab } from './components/ImportTab';
import { HistoryTab } from './components/HistoryTab';
import { useLegacyImport } from './hooks/useLegacyImport';

export const LegacyImportPage: React.FC = () => {
    const ctrl = useLegacyImport();

    return (
        <div className="h-full flex flex-col bg-slate-50">
            <div className="bg-white border-b border-slate-200 px-6 py-4">
                <h1 className="text-xl font-semibold flex items-center gap-2"><FileText className="text-primary-600" />历史数据导入</h1>
                <div className="mt-4 flex gap-4">
                    <button onClick={() => ctrl.setActiveTab('import')} className={`px-4 py-2 ${ctrl.activeTab === 'import' ? 'border-b-2 border-primary-600 text-primary-600' : ''}`}>数据导入</button>
                    <button onClick={() => ctrl.setActiveTab('history')} className={`px-4 py-2 ${ctrl.activeTab === 'history' ? 'border-b-2 border-primary-600 text-primary-600' : ''}`}>导入历史</button>
                </div>
            </div>
            <div className="flex-1 overflow-auto p-6">
                {ctrl.activeTab === 'import' ? (
                    <ImportTab
                        file={ctrl.file} loading={ctrl.loading} importing={ctrl.importing}
                        previewResult={ctrl.previewResult} importResult={ctrl.importResult}
                        onFileChange={ctrl.handleFileChange} onPreview={ctrl.handlePreview} onImport={ctrl.handleImport}
                        onDownloadCsvTemplate={ctrl.handleDownloadCsvTemplate} onDownloadExcelTemplate={ctrl.handleDownloadExcelTemplate}
                        onDownloadErrorReport={ctrl.handleDownloadErrorReport}
                    />
                ) : (
                    <HistoryTab
                        loading={ctrl.historyLoading} tasks={ctrl.tasks} currentPage={ctrl.currentPage} total={ctrl.total}
                        statusFilter={ctrl.statusFilter} onPageChange={ctrl.setCurrentPage} onRefresh={ctrl.loadHistory}
                        onStatusFilterChange={(s) => { ctrl.setStatusFilter(s); ctrl.setCurrentPage(1); }}
                        onDownloadErrorReport={ctrl.handleDownloadErrorReport}
                    />
                )}
            </div>
        </div>
    );
};

export default LegacyImportPage;
