// Input: lucide-react
// Output: ComplianceWarning sub-component
// Pos: src/pages/admin/LegacyImportPage/components/import/ComplianceWarning.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { AlertCircle } from 'lucide-react';

export const ComplianceWarning: React.FC = () => (
    <div className="bg-amber-50 border-2 border-amber-300 rounded-lg p-4">
        <div className="flex items-start gap-3">
            <AlertCircle className="w-6 h-6 text-amber-600 mt-0.5 flex-shrink-0" />
            <div className="flex-1">
                <h3 className="font-semibold text-amber-900 mb-2">⚠️ 合规说明</h3>
                <p className="text-sm text-amber-800">本功能仅适用于历史数据迁移。根据GB/T 39362-2020，导入后需补充四性检测记录。</p>
            </div>
        </div>
    </div>
);
