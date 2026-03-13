// Input: lucide-react, ImportPreviewData
// Output: PreviewResultSection sub-component
// Pos: src/pages/admin/LegacyImportPage/components/import/PreviewResultSection.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import type { ImportPreviewData } from '../../types';

export const PreviewResultSection: React.FC<{ previewResult: ImportPreviewData }> = ({ previewResult }) => (
    <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">预览结果</h2>
        <div className="grid grid-cols-3 gap-4">
            <StatCard label="总行数" value={previewResult.totalRows} />
            <StatCard label="有效" value={previewResult.validRows} color="bg-green-50 text-green-700" />
            <StatCard label="无效" value={previewResult.invalidRows} color="bg-red-50 text-red-700" />
        </div>
    </div>
);

const StatCard = ({ label, value, color = "bg-slate-50" }: any) => (
    <div className={`p-4 rounded-lg ${color}`}>
        <div className="text-sm opacity-70">{label}</div>
        <div className="text-2xl font-bold">{value ?? 0}</div>
    </div>
);
