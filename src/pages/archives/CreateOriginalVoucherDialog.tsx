// Input: React, Lucide Icons, originalVoucher API
// Output: CreateOriginalVoucherDialog 组件
// Pos: src/pages/archives/CreateOriginalVoucherDialog.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useRef } from 'react';
import { X, Upload, FileText, AlertCircle, Loader2 } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { createOriginalVoucher, addOriginalVoucherFile, getOriginalVoucherTypes } from '../../api/originalVoucher';
import { useFondsStore } from '../../store/useFondsStore';

interface CreateOriginalVoucherDialogProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess?: () => void;
    initialType?: string;
    category?: string;
}

export const CreateOriginalVoucherDialog: React.FC<CreateOriginalVoucherDialogProps> = ({
    isOpen,
    onClose,
    onSuccess,
    initialType,
    category
}) => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const currentFonds = useFondsStore((state) => state.currentFonds);

    // Form State
    const [voucherType, setVoucherType] = useState(initialType || '');
    const [amount, setAmount] = useState('');
    const [summary, setSummary] = useState('');
    const [file, setFile] = useState<File | null>(null);
    const [error, setError] = useState('');

    // Fetch types
    const { data: voucherTypes = [] } = useQuery({
        queryKey: ['originalVoucherTypes'],
        queryFn: getOriginalVoucherTypes,
        enabled: isOpen
    });

    const createMutation = useMutation({
        mutationFn: async () => {
            // 1. Create Voucher
            // Find category from selected type
            const selectedTypeObj = voucherTypes.find(t => t.typeCode === voucherType);
            const derivedCategory = selectedTypeObj?.categoryCode;

            const newVoucher = await createOriginalVoucher({
                voucherType,
                amount: amount ? parseFloat(amount) : undefined,
                summary,
                // Use prop category if present, otherwise use derived category, finally fallback to OTHER
                // Note: If both exist but mismatch, backend will catch it, but usually UI filters types if category is fixed.
                voucherCategory: category || derivedCategory || 'OTHER',
                currency: 'CNY',
                fondsCode: currentFonds?.fondsCode ?? '001',
                fiscalYear: new Date().getFullYear().toString()
            });

            // 2. Upload File if selected
            if (file && newVoucher.id) {
                await addOriginalVoucherFile(newVoucher.id, file, 'ORIGINAL');
            }
            return newVoucher;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['originalVouchers'] });
            if (onSuccess) onSuccess();
            onClose();
        },
        onError: (err: any) => {
            setError(err.message || '创建失败');
        }
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!voucherType) {
            setError('请选择凭证类型');
            return;
        }
        createMutation.mutate();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg border border-slate-100 dark:border-gray-700">
                {/* Header */}
                <div className="p-6 border-b border-slate-100 dark:border-gray-700 flex justify-between items-center bg-slate-50/50 dark:bg-gray-800/50 rounded-t-2xl">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800 dark:text-white">新建原始凭证</h3>
                        <p className="text-sm text-slate-500 dark:text-gray-400 mt-1">手动录入凭证元数据并上传扫描件</p>
                    </div>
                    <button onClick={onClose}><X size={20} className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200" /></button>
                </div>

                {/* Body */}
                <form onSubmit={handleSubmit} className="p-6 space-y-5">
                    {error && (
                        <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm flex items-center gap-2">
                            <AlertCircle size={16} />
                            {error}
                        </div>
                    )}

                    {/* Type Selection */}
                    <div>
                        <label className="block text-sm font-medium text-slate-700 dark:text-gray-300 mb-1">凭证类型 <span className="text-red-500">*</span></label>
                        <select
                            className="w-full border border-slate-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none"
                            value={voucherType}
                            onChange={(e) => setVoucherType(e.target.value)}
                        >
                            <option value="">请选择类型</option>
                            {voucherTypes.map(type => (
                                <option key={type.typeCode} value={type.typeCode}>{type.typeName}</option>
                            ))}
                        </select>
                    </div>

                    {/* Amount */}
                    <div>
                        <label className="block text-sm font-medium text-slate-700 dark:text-gray-300 mb-1">金额 (元)</label>
                        <input
                            type="number"
                            step="0.01"
                            className="w-full border border-slate-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none"
                            placeholder="0.00"
                            value={amount}
                            onChange={(e) => setAmount(e.target.value)}
                        />
                    </div>

                    {/* Summary */}
                    <div>
                        <label className="block text-sm font-medium text-slate-700 dark:text-gray-300 mb-1">摘要 / 说明</label>
                        <textarea
                            className="w-full border border-slate-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                            rows={3}
                            placeholder="请输入业务摘要..."
                            value={summary}
                            onChange={(e) => setSummary(e.target.value)}
                        />
                    </div>

                    {/* File Upload Area */}
                    <div>
                        <label className="block text-sm font-medium text-slate-700 dark:text-gray-300 mb-1">附件上传</label>
                        <div
                            className={`border-2 border-dashed rounded-xl p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-colors ${file ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20' : 'border-slate-300 dark:border-gray-600 hover:border-blue-400'}`}
                            onClick={() => fileInputRef.current?.click()}
                        >
                            <input
                                type="file"
                                ref={fileInputRef}
                                className="hidden"
                                onChange={handleFileChange}
                                accept=".pdf,.ofd,.jpg,.jpeg,.png"
                            />

                            {file ? (
                                <div className="flex items-center gap-3 text-blue-600 dark:text-blue-400">
                                    <FileText size={24} />
                                    <span className="font-medium text-sm truncate max-w-[200px]">{file.name}</span>
                                    <button
                                        type="button"
                                        onClick={(e) => { e.stopPropagation(); setFile(null); }}
                                        className="text-slate-400 hover:text-red-500 ml-2"
                                    >
                                        <X size={16} />
                                    </button>
                                </div>
                            ) : (
                                <>
                                    <div className="w-10 h-10 rounded-full bg-slate-100 dark:bg-gray-700 flex items-center justify-center mb-2 text-slate-500 dark:text-gray-400">
                                        <Upload size={20} />
                                    </div>
                                    <p className="text-sm font-medium text-slate-700 dark:text-gray-300">点击上传或是拖拽文件</p>
                                    <p className="text-xs text-slate-500 dark:text-gray-500 mt-1">支持 PDF, OFD, JPG, PNG</p>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Footer / Actions */}
                    <div className="pt-2 flex items-center justify-between">
                        <button
                            type="button"
                            onClick={() => { onClose(); navigate('/system/collection/upload'); }}
                            className="text-sm text-blue-600 hover:text-blue-700 hover:underline"
                        >
                            去批量上传 &rarr;
                        </button>

                        <div className="flex gap-3">
                            <button
                                type="button"
                                onClick={onClose}
                                className="px-4 py-2 text-sm text-slate-600 dark:text-gray-300 hover:bg-slate-100 dark:hover:bg-gray-700 rounded-lg transition"
                            >
                                取消
                            </button>
                            <button
                                type="submit"
                                disabled={createMutation.isPending}
                                className="px-4 py-2 text-sm bg-blue-600 hover:bg-blue-700 text-white rounded-lg shadow-sm shadow-blue-200 dark:shadow-none transition flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
                            >
                                {createMutation.isPending && <Loader2 size={16} className="animate-spin" />}
                                立即创建
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
};
