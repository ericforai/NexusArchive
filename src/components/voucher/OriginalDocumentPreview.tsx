// src/components/voucher/OriginalDocumentPreview.tsx
import React, { useState, useMemo, useEffect } from 'react';
import { FileText, Download, AlertCircle } from 'lucide-react';
import { message } from 'antd';
import { client } from '../../api/client';
import { archivesApi } from '../../api/archives';
import { SmartFilePreview } from '../preview';
import { OfdViewer } from '../preview/OfdViewer';

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
  archiveId?: string;
}

const normalizePreviewUrl = (url?: string): string | undefined => {
  if (!url) return undefined;
  if (/^https?:\/\//i.test(url)) return url;
  const legacyMatched = url.match(/^\/api\/archives\/([^/]+)\/download$/) || url.match(/^\/archives\/([^/]+)\/download$/);
  if (legacyMatched?.[1]) {
    return `/archive/${legacyMatched[1]}/content`;
  }
  if (url.startsWith('/api/')) {
    return url.replace(/^\/api/, '');
  }
  if (typeof window !== 'undefined' && url.startsWith('/') && !url.startsWith('/archive/') && !url.startsWith('/original-vouchers/')) {
    return `${window.location.origin}${url}`;
  }
  return url;
};

const extractArchiveIdFromContentUrl = (url?: string): string | null => {
  if (!url) return null;
  const matched = url.match(/^\/archive\/([^/]+)\/content$/);
  return matched?.[1] || null;
};

const detectPreviewFileType = (file: AttachmentDTO): 'pdf' | 'image' | 'ofd' | 'unknown' => {
  const normalizedType = file.type?.toLowerCase() || '';
  const normalizedName = (file.fileName || file.name || '').toLowerCase();

  if (normalizedType.includes('pdf') || normalizedName.endsWith('.pdf')) {
    return 'pdf';
  }
  if (
    normalizedType.startsWith('image/') ||
    /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(normalizedName)
  ) {
    return 'image';
  }
  if (
    normalizedType === 'ofd' ||
    normalizedType.includes('application/ofd') ||
    normalizedName.endsWith('.ofd')
  ) {
    return 'ofd';
  }

  return 'unknown';
};

export const OriginalDocumentPreview: React.FC<OriginalDocumentPreviewProps> = ({
  files,
  defaultFileIndex = 0,
  archiveId,
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
  const selectedFileType = selectedFile ? detectPreviewFileType(selectedFile) : 'unknown';
  const useSharedPreview = Boolean(archiveId && files.length === 1 && selectedFileType !== 'ofd');

  const previewFiles = useMemo(() => {
    return files.map(file => ({
      id: file.id,
      fileName: file.fileName || file.name || file.id,
      fileType: detectPreviewFileType(file),
    }));
  }, [files]);

  // 当外部文件列表切换（例如点击联查中的不同节点）时，重置当前选中文件
  useEffect(() => {
    setSelectedIndex(files.length > 0 ? Math.min(defaultFileIndex, files.length - 1) : -1);
  }, [files, defaultFileIndex]);

  // 获取文件并创建 blob URL
  useEffect(() => {
    if (useSharedPreview) return;
    files.forEach(file => {
      if (!fileUrls[file.id] && !loadingStates[file.id] && !errorStates[file.id]) {
        fetchFileWithAuth(file);
      }
    });
    // 依赖项：仅 files 变化时重新获取文件
  }, [errorStates, fileUrls, files, loadingStates, useSharedPreview]);

  const fetchFileWithAuth = async (file: AttachmentDTO) => {
    setLoadingStates(prev => ({ ...prev, [file.id]: true }));
    try {
      const normalizedFileUrl = normalizePreviewUrl(file.fileUrl);
      const archiveIdFromContentUrl = extractArchiveIdFromContentUrl(normalizedFileUrl);
      const candidateUrls: string[] = [];

      if (normalizedFileUrl) {
        candidateUrls.push(normalizedFileUrl);
      }

      if (archiveIdFromContentUrl) {
        // /archive/{archiveId}/content 失败时，回退到真实 fileId 下载链接
        try {
          const filesResp = await archivesApi.getArchiveFiles(archiveIdFromContentUrl);
          const archiveFiles = filesResp?.data || [];
          archiveFiles.forEach((af: any) => {
            if (af?.id) {
              candidateUrls.push(`/archive/files/download/${af.id}`);
            }
          });
        } catch {
          // 继续兜底
        }
      }

      // 最后兜底：按当前 id 当作 fileId 尝试下载
      candidateUrls.push(`/archive/files/download/${file.id}`);

      const dedupedUrls = Array.from(new Set(candidateUrls.filter(Boolean)));
      let lastError: unknown = null;

      for (const url of dedupedUrls) {
        try {
          const response = await client.get(url, {
            responseType: 'blob',
          });

          const contentType = response.headers['content-type'] || 'application/pdf';
          const blob = new Blob([response.data], { type: contentType });
          const blobUrl = URL.createObjectURL(blob);
          setFileUrls(prev => ({ ...prev, [file.id]: blobUrl }));
          lastError = null;
          break;
        } catch (error) {
          lastError = error;
        }
      }

      if (lastError) {
        throw lastError;
      }
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

  const handleSmartPreviewFileChange = (fileId: string) => {
    const nextIndex = files.findIndex(file => file.id === fileId);
    if (nextIndex >= 0) {
      setSelectedIndex(nextIndex);
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
      <div className="flex-1 overflow-hidden">
        {!selectedFile ? null : useSharedPreview ? (
          <div className="h-full p-4">
            <div className="h-full overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
              <SmartFilePreview
                key={`${archiveId}-${selectedFile.id}`}
                archiveId={archiveId}
                fileId={selectedFile.id}
                fileName={selectedFile.fileName || selectedFile.name}
                files={previewFiles}
                currentFileId={selectedFile.id}
                onFileChange={handleSmartPreviewFileChange}
                showFileNav={false}
                className="h-full"
              />
            </div>
          </div>
        ) : (
          <div className="h-full flex items-center justify-center p-4 overflow-hidden">
            {loadingStates[selectedFile.id] ? (
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
                {selectedFileType === 'image' ? (
                  <div className="flex-1 flex items-center justify-center bg-slate-200 overflow-auto p-4">
                    <img
                      src={fileUrls[selectedFile.id]}
                      alt={selectedFile.fileName || selectedFile.name}
                      className="max-w-full max-h-full object-contain shadow-2xl transition-transform hover:scale-105"
                    />
                  </div>
                ) : selectedFileType === 'ofd' ? (
                  <OfdViewer
                    url={fileUrls[selectedFile.id]}
                    fileName={selectedFile.fileName || selectedFile.name}
                    downloadUrl={fileUrls[selectedFile.id]}
                    className="h-full"
                  />
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
        )}
      </div>
    </div>
  );
};
