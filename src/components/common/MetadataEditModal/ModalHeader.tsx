// Input: React、lucide-react 图标
// Output: 模态框头部组件
// Pos: MetadataEditModal 子组件

import React from 'react';
import { FileText } from 'lucide-react';

interface ModalHeaderProps {
    fileName: string;
}

export const ModalHeader: React.FC<ModalHeaderProps> = ({ fileName }) => {
    return (
        <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                <FileText className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
                <h3 className="text-lg font-semibold text-slate-800 dark:text-white">元数据补录</h3>
                <p className="text-sm text-slate-500 dark:text-slate-400 truncate max-w-[280px]">{fileName}</p>
            </div>
        </div>
    );
};
