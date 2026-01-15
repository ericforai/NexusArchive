// Input: React, api/abnormal
// Output: AbnormalEditModal Component
// Pos: src/pages/pre-archive/components/AbnormalEditModal.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { retryAbnormal } from '../../../api/abnormal';
import { updateAbnormalSip } from '../../../api/abnormal';

interface AbnormalEditModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    abnormalId: string;
    initialSipData: string;
}

const AbnormalEditModal: React.FC<AbnormalEditModalProps> = ({ isOpen, onClose, onSuccess, abnormalId, initialSipData }) => {
    const [jsonContent, setJsonContent] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (isOpen) {
            try {
                // Format JSON for readability
                const parsed = JSON.parse(initialSipData);
                setJsonContent(JSON.stringify(parsed, null, 4));
            } catch (e) {
                setJsonContent(initialSipData);
            }
            setError(null);
        }
    }, [isOpen, initialSipData]);

    const handleSave = async () => {
        setSaving(true);
        setError(null);
        try {
            // Validate JSON
            let parsed;
            try {
                parsed = JSON.parse(jsonContent);
            } catch (e) {
                setError('JSON 格式无效');
                setSaving(false);
                return;
            }

            // Update Backend
            await updateAbnormalSip(abnormalId, parsed);

            // Success
            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.message || '保存失败');
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-70 backdrop-blur-sm">
            <div className="bg-gray-800 rounded-xl shadow-2xl w-full max-w-4xl flex flex-col max-h-[90vh] border border-gray-700">
                {/* Header */}
                <div className="px-6 py-4 border-b border-gray-700 flex justify-between items-center bg-gray-900/50 rounded-t-xl">
                    <h3 className="text-xl font-semibold text-white flex items-center gap-2">
                        <svg className="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                        编辑 SIP 数据
                    </h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-white transition-colors">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Body */}
                <div className="flex-1 p-6 overflow-hidden flex flex-col">
                    <div className="mb-4 bg-yellow-900/20 border border-yellow-700/50 p-3 rounded text-yellow-200 text-sm flex items-start gap-2">
                        <svg className="w-5 h-5 text-yellow-400 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                        </svg>
                        <div>
                            <span className="font-bold">警告：</span> 手动修改 SIP 数据存在合规风险。所有更改都将被审计。请确保数据与原始凭证保持一致。
                        </div>
                    </div>

                    <div className="flex-1 relative">
                        <textarea
                            value={jsonContent}
                            onChange={(e) => setJsonContent(e.target.value)}
                            className="w-full h-full bg-gray-900 text-gray-300 font-mono text-sm p-4 rounded border border-gray-700 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none resize-none"
                            spellCheck={false}
                        />
                    </div>

                    {error && (
                        <div className="mt-4 text-red-400 text-sm flex items-center gap-2">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            {error}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="px-6 py-4 border-t border-gray-700 flex justify-end gap-3 bg-gray-900/50 rounded-b-xl">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 text-sm font-medium text-gray-300 hover:text-white bg-gray-700 hover:bg-gray-600 rounded transition-colors"
                        disabled={saving}
                    >
                        取消
                    </button>
                    <button
                        onClick={handleSave}
                        className="px-4 py-2 text-sm font-medium text-white bg-cyan-600 hover:bg-cyan-500 rounded transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                        disabled={saving}
                    >
                        {saving ? (
                            <>
                                <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                保存中...
                            </>
                        ) : '保存更改'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AbnormalEditModal;
