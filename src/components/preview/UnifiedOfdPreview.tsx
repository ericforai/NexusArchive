// Input: React、ofdPreviewApi、LiteOfdPreview
// Output: UnifiedOfdPreview 组件
// Pos: 通用复用组件 - 统一 OFD 预览决策层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, Download } from 'lucide-react';
import { ofdPreviewApi, OfdPreviewResource } from '@/api/ofdPreview';
import { LiteOfdPreview } from './LiteOfdPreview';

interface UnifiedOfdPreviewProps {
  fileId?: string;
  fileName?: string;
  sourceType?: 'ARCHIVE' | 'ORIGINAL' | null;
  originalDownloadUrl?: string;
  className?: string;
}

export function UnifiedOfdPreview({
  fileId,
  fileName,
  sourceType,
  originalDownloadUrl,
  className = '',
}: UnifiedOfdPreviewProps) {
  // Debug: 记录接收到的 props
  console.log('[UnifiedOfdPreview] Props:', { fileId, fileName, sourceType, originalDownloadUrl });

  const [resource, setResource] = useState<OfdPreviewResource | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fallbackDownloadUrl = useMemo(() => {
    if (originalDownloadUrl) {
      return originalDownloadUrl;
    }
    if (!fileId) {
      return '';
    }
    return sourceType === 'ARCHIVE'
      ? `/api/archive/files/download/${fileId}`
      : `/api/original-vouchers/files/download/${fileId}`;
  }, [fileId, originalDownloadUrl, sourceType]);

  console.log('[UnifiedOfdPreview] fallbackDownloadUrl:', fallbackDownloadUrl);

  useEffect(() => {
    let cancelled = false;

    async function loadResource() {
      console.log('[UnifiedOfdPreview] loadResource called, fileId:', fileId, 'fallbackDownloadUrl:', fallbackDownloadUrl);

      if (!fileId && !fallbackDownloadUrl) {
        console.error('[UnifiedOfdPreview] Missing both fileId and fallbackDownloadUrl!');
        setError('缺少 OFD 文件地址');
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        if (!fileId) {
          setResource({
            preferredMode: 'liteofd',
            originalFileId: '',
            originalDownloadUrl: fallbackDownloadUrl,
            convertedFileId: null,
            convertedMimeType: null,
            convertedPreviewUrl: null,
            fileName: fileName || null,
          });
          return;
        }

        const nextResource = await ofdPreviewApi.getResource(fileId);
        console.log('[UnifiedOfdPreview] API response:', nextResource);
        if (!cancelled) {
          setResource(nextResource);
        }
      } catch (loadError) {
        console.error('[UnifiedOfdPreview] API error:', loadError);
        if (!cancelled) {
          const status = extractStatusCode(loadError);
          if (status === 404) {
            setResource({
              preferredMode: 'liteofd',
              originalFileId: fileId,
              originalDownloadUrl: fallbackDownloadUrl,
              convertedFileId: null,
              convertedMimeType: null,
              convertedPreviewUrl: null,
              fileName: fileName || null,
            });
            setError(null);
          } else {
            setResource(null);
            setError(loadError instanceof Error ? loadError.message : 'OFD 预览资源加载失败');
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadResource();
    return () => {
      cancelled = true;
    };
  }, [fallbackDownloadUrl, fileId, fileName]);

  const effectiveDownloadUrl = resource?.originalDownloadUrl || fallbackDownloadUrl;
  const effectiveFileName = resource?.fileName || fileName || 'document.ofd';

  if (loading) {
    return (
      <div className={`flex h-full items-center justify-center bg-slate-50 ${className}`}>
        <div className="animate-pulse text-sm text-slate-500">正在解析 OFD 预览策略...</div>
      </div>
    );
  }

  if (resource?.preferredMode === 'converted' && resource.convertedPreviewUrl && resource.convertedMimeType) {
    if (resource.convertedMimeType.startsWith('image/')) {
      return (
        <div className={`flex h-full flex-col bg-slate-100 ${className}`}>
          <div className="flex items-center justify-end gap-2 border-b border-slate-200 bg-white px-3 py-2">
            <a
              href={effectiveDownloadUrl}
              download={effectiveFileName}
              className="inline-flex items-center gap-2 text-sm font-medium text-blue-700 hover:text-blue-900"
            >
              <Download size={16} />
              下载原始 OFD
            </a>
          </div>
          <div className="flex flex-1 items-center justify-center overflow-auto p-4">
            <img
              src={resource.convertedPreviewUrl}
              alt={effectiveFileName}
              className="max-h-full max-w-full rounded-lg bg-white shadow-sm"
            />
          </div>
        </div>
      );
    }

    return (
      <div className={`flex h-full flex-col bg-slate-100 ${className}`}>
        <div className="flex items-center justify-end gap-2 border-b border-slate-200 bg-white px-3 py-2">
          <a
            href={effectiveDownloadUrl}
            download={effectiveFileName}
            className="inline-flex items-center gap-2 text-sm font-medium text-blue-700 hover:text-blue-900"
          >
            <Download size={16} />
            下载原始 OFD
          </a>
        </div>
        <iframe
          src={resource.convertedPreviewUrl}
          className="h-full w-full border-0 bg-white"
          title="OFD Converted Preview"
        />
      </div>
    );
  }

  if (error) {
    return (
      <div className={`flex h-full items-center justify-center bg-slate-50 ${className}`}>
        <div className="max-w-md px-6 text-center text-slate-500">
          <AlertCircle size={36} className="mx-auto mb-3 text-rose-500" />
          <p className="font-medium text-slate-700">OFD 预览资源加载失败</p>
          <p className="mt-2 text-sm">{error}</p>
          <a
            href={effectiveDownloadUrl}
            download={effectiveFileName}
            className="mt-4 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
          >
            <Download size={16} />
            下载原始 OFD
          </a>
        </div>
      </div>
    );
  }

  return (
    <LiteOfdPreview
      fileUrl={effectiveDownloadUrl}
      fileName={effectiveFileName}
      downloadUrl={effectiveDownloadUrl}
      className={className}
    />
  );
}

function extractStatusCode(error: unknown): number | null {
  if (!error || typeof error !== 'object') {
    return null;
  }
  const maybeResponse = error as { response?: { status?: number } };
  return typeof maybeResponse.response?.status === 'number' ? maybeResponse.response.status : null;
}

export default UnifiedOfdPreview;
