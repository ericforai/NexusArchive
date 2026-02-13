// src/components/voucher/OriginalDocumentPreview.tsx
import React, { useState, useMemo, useEffect } from 'react';
import { FileText, Download, AlertCircle } from 'lucide-react';
import { message } from 'antd';
import { client } from '../../api/client';

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
  const [fileUrls, setFileUrls] = useState<Record<string, string>>({});
  const [loadingStates, setLoadingStates] = useState<Record<string, boolean>>({});
  const [errorStates, setErrorStates] = useState<Record<string, boolean>>({});

  const selectedFile = useMemo(() => {
    return selectedIndex >= 0 ? files[selectedIndex] : null;
  }, [selectedIndex, files]);

  // 当外部文件列表切换（例如点击联查中的不同节点）时，重置当前选中文件
  useEffect(() => {
    setSelectedIndex(files.length > 0 ? Math.min(defaultFileIndex, files.length - 1) : -1);
  }, [files, defaultFileIndex]);

  // 获取文件并创建 blob URL
  useEffect(() => {
    files.forEach(file => {
      if (!fileUrls[file.id] && !loadingStates[file.id] && !errorStates[file.id]) {
        fetchFileWithAuth(file);
      }
    });
    // 依赖项：仅 files 变化时重新获取文件
  }, [files]);

  const fetchFileWithAuth = async (file: AttachmentDTO) => {
    setLoadingStates(prev => ({ ...prev, [file.id]: true }));
    try {
      const url = file.fileUrl || `/archive/files/download/${file.id}`;
      const response = await client.get(url, {
        responseType: 'blob',
      });

      // 从响应头中获取内容类型，如果没有则回退
      const contentType = response.headers['content-type'] || 'application/pdf';

      // 创建带有正确类型的 blob URL
      const blob = new Blob([response.data], { type: contentType });
      const blobUrl = URL.createObjectURL(blob);

      setFileUrls(prev => ({ ...prev, [file.id]: blobUrl }));
    } catch (error) {
      console.error('Failed to fetch file:', error);
      setErrorStates(prev => ({ ...prev, [file.id]: true }));
      message.error(`无法加载文件: ${file.fileName || file.name}`);
    } finally {
      setLoadingStates(prev => ({ ...prev, [file.id]: false }));
    }
  };

  // 清理 blob URLs
  useEffect(() => {
    return () => {
      Object.values(fileUrls).forEach(url => URL.revokeObjectURL(url));
    };
  }, [fileUrls]);

  // 下载文件
  const handleDownload = (file: AttachmentDTO) => {
    const blobUrl = fileUrls[file.id];
    if (blobUrl) {
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = file.fileName || file.name || 'download';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } else if (file.fileUrl) {
      window.open(file.fileUrl, '_blank');
    }
  };

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
    <div className="h-full flex flex-col bg-slate-100">
      {/* 文件列表 */}
      {files.length > 1 && (
        <div className="shrink-0 border-b border-slate-200 p-2 bg-white">
          <div className="flex gap-2 overflow-x-auto">
            {files.map((file, index) => (
              <button
                key={file.id}
                onClick={() => setSelectedIndex(index)}
                className={`px-3 py-1.5 text-sm rounded whitespace-nowrap transition-colors ${selectedIndex === index
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

      {/* PDF 预览区域 */}
      <div className="flex-1 flex items-center justify-center p-4 overflow-hidden">
        {!selectedFile ? null : loadingStates[selectedFile.id] ? (
          <div className="text-slate-500">加载中...</div>
        ) : errorStates[selectedFile.id] ? (
          <div className="flex flex-col items-center text-slate-400">
            <AlertCircle size={48} className="mb-4 text-amber-400" />
            <p>文件加载失败</p>
            <button
              onClick={() => {
                setErrorStates(prev => ({ ...prev, [selectedFile.id]: false }));
                fetchFileWithAuth(selectedFile);
              }}
              className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              重试
            </button>
          </div>
        ) : fileUrls[selectedFile.id] ? (
          <div className="w-full h-full flex flex-col relative overflow-hidden">
            {selectedFile.type?.startsWith('image/') ||
              selectedFile.fileName?.match(/\.(jpg|jpeg|png|gif|webp)$/i) ? (
              <div className="flex-1 flex items-center justify-center bg-slate-200 overflow-auto p-4">
                <img
                  src={fileUrls[selectedFile.id]}
                  alt={selectedFile.fileName || selectedFile.name}
                  className="max-w-full max-h-full object-contain shadow-2xl transition-transform hover:scale-105"
                />
              </div>
            ) : (
              <iframe
                src={fileUrls[selectedFile.id] + '#view=FitH'}
                title={selectedFile.fileName || selectedFile.name}
                className="w-full h-full rounded border-0 bg-white shadow-lg"
              />
            )}
            {/* 浮动下载按钮 */}
            <div className="absolute bottom-6 right-6">
              <button
                onClick={() => handleDownload(selectedFile)}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg shadow-lg hover:bg-blue-700 transition-all"
              >
                <Download size={16} />
                下载
              </button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center text-slate-400">
            <FileText size={48} className="mb-4 opacity-20" />
            <p>该文件无法预览</p>
            <button
              onClick={() => fetchFileWithAuth(selectedFile)}
              className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              下载文件
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
