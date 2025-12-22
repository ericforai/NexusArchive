// Input: React、lucide-react 图标、本地模块 types
// Output: React 组件 AddRecordModal
// Pos: 归档流程组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { X } from 'lucide-react';
import type { TableColumn } from '../../types';

interface AddRecordModalProps {
    isOpen: boolean;
    onClose: () => void;
    columns: TableColumn[];
    formData: Record<string, string>;
    onFormChange: (key: string, value: string) => void;
    onSubmit: (e: React.FormEvent) => void;
}

/**
 * 新增记录模态框组件
 * 
 * 从 ArchiveListView 拆分出来
 */
export const AddRecordModal: React.FC<AddRecordModalProps> = ({
    isOpen,
    onClose,
    columns,
    formData,
    onFormChange,
    onSubmit
}) => {
    if (!isOpen) return null;

    const editableColumns = columns.filter(c => c.type !== 'status' && c.type !== 'progress' && c.type !== 'action');

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <h3 className="text-lg font-bold text-slate-800">新增记录</h3>
                    <button onClick={onClose}><X size={20} className="text-slate-400" /></button>
                </div>
                <form onSubmit={onSubmit} className="p-6 space-y-4">
                    {editableColumns.map(col => (
                        <div key={col.key}>
                            <label className="text-sm font-medium">{col.header}</label>
                            <input
                                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                                value={formData[col.key] || ''}
                                onChange={(e) => onFormChange(col.key, e.target.value)}
                            />
                        </div>
                    ))}
                    <div className="pt-4 flex justify-end gap-2">
                        <button type="button" onClick={onClose} className="px-4 py-2 text-slate-600">取消</button>
                        <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg">确认</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default AddRecordModal;
