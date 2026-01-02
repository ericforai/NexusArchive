// Input: React、FilePreviewModal
// Output: PreviewWatermarkTestView 页面
// Pos: src/pages/debug/PreviewWatermarkTestView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { FilePreviewModal } from '@/components/preview';

const PreviewWatermarkTestView: React.FC = () => {
    const [open, setOpen] = useState(true);

    return (
        <div className="space-y-4 p-6">
            <div>
                <h1 className="text-2xl font-bold text-slate-900">预览水印链路验证</h1>
                <p className="text-sm text-slate-500 mt-1">
                    本页面用于验证预览接口返回的水印元数据是否被前端正确消费。
                </p>
            </div>

            <div>
                <button
                    type="button"
                    data-testid="preview-open"
                    onClick={() => setOpen(true)}
                    className="inline-flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-white shadow hover:bg-indigo-700"
                >
                    打开预览
                </button>
            </div>

            <FilePreviewModal
                isOpen={open}
                onClose={() => setOpen(false)}
                archiveId="ARCH-TEST-001"
                fileId="FILE-TEST-001"
            />
        </div>
    );
};

export default PreviewWatermarkTestView;
