// src/pages/archives/components/VoucherUploadButton.tsx
/**
 * 凭证附件上传按钮
 *
 * 职责：处理附件上传的 UI 和逻辑
 * 变更理由：上传交互变化
 */

import React, { useRef } from 'react';
import { Upload } from 'lucide-react';

interface VoucherUploadButtonProps {
  onUpload: (file: File) => Promise<void>;
  isUploading?: boolean;
  accept?: string;
}

export const VoucherUploadButton: React.FC<VoucherUploadButtonProps> = ({
  onUpload,
  isUploading = false,
  accept = '.pdf,.ofd,.jpg,.jpeg,.png',
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    await onUpload(file);

    // 清空 input 以允许重复上传同一文件
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <>
      <input
        ref={fileInputRef}
        type="file"
        onChange={handleUpload}
        className="hidden"
        accept={accept}
      />
      <button
        onClick={() => fileInputRef.current?.click()}
        disabled={isUploading}
        className="px-3 py-1.5 text-xs bg-primary-50 text-primary-600 border border-primary-200 rounded hover:bg-primary-100 flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isUploading ? (
          <>
            <Upload size={12} className="animate-spin" />
            上传中...
          </>
        ) : (
          <>
            <Upload size={12} />
            添加附件
          </>
        )}
      </button>
    </>
  );
};
