// src/components/voucher/OriginalDocumentPreview.tsx
import React, { useState, useMemo } from 'react';
import { FileText, Download } from 'lucide-react';

interface AttachmentDTO {
  id: string;
  fileName?: string;
  name?: string;
  fileUrl?: string;
  type?: string;
}

interface OriginalDocumentPreviewProps {
  files: AttachmentDTO[];
  defaultFileIndex?: number;
}

export const OriginalDocumentPreview: React.FC<OriginalDocumentPreviewProps> = ({
  files,
  defaultFileIndex = 0,
}) => {
  const [selectedIndex, setSelectedIndex] = useState(
    files.length > 0 ? Math.min(defaultFileIndex, files.length - 1) : -1
  );

  const selectedFile = useMemo(() => {
    return selectedIndex >= 0 ? files[selectedIndex] : null;
  }, [selectedIndex, files]);

  // 如果没有文件
  if (files.length === 0) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-slate-400 bg-slate-50">
        <FileText size={48} className="mb-4 opacity-20" />
        <p>暂无关联附件</p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* 文件列表 */}
      {files.length > 1 && (
        <div className="border-b border-slate-200 p-2 bg-slate-50">
          <div className="flex gap-2 overflow-x-auto">
            {files.map((file, index) => (
              <button
                key={file.id}
                onClick={() => setSelectedIndex(index)}
                className={`px-3 py-1.5 text-sm rounded whitespace-nowrap transition-colors ${
                  selectedIndex === index
                    ? 'bg-blue-50 text-blue-700 border border-blue-200'
                    : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
                }`}
              >
                {file.fileName || file.name || `文件${index + 1}`}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 预览区域 */}
      <div className="flex-1 overflow-auto bg-slate-100 p-4">
        {selectedFile?.fileUrl ? (
          <div className="h-full flex flex-col">
            {/* iframe PDF 预览 */}
            <iframe
              src={selectedFile.fileUrl}
              title={selectedFile.fileName || selectedFile.name}
              className="flex-1 w-full rounded border-0 bg-white shadow-sm"
            />
          </div>
        ) : (
          <div className="h-full flex flex-col items-center justify-center text-slate-400">
            <FileText size={48} className="mb-4 opacity-20" />
            <p>该文件无法预览</p>
            <a
              href={selectedFile?.fileUrl || '#'}
              download={selectedFile?.fileName || selectedFile?.name}
              className="mt-4 flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              <Download size={16} />
              下载文件
            </a>
          </div>
        )}
      </div>
    </div>
  );
};
