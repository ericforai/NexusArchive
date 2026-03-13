// Input: React, previewApi
// Output: useFilePreview Hook
// Pos: 通用复用组件 - 预览数据获取 Hook

import { useState, useEffect, useCallback, useRef } from 'react';
import { previewApi, PreviewRequest, PreviewResourceType, WatermarkMetadata } from '../../api/preview';

export interface UseFilePreviewParams {
  /** 资源类型：档案主文件 / 文件附件 */
  resourceType?: PreviewResourceType;
  /** 档案ID（已归档档案使用） */
  archiveId?: string;
  /** 文件ID（记账凭证库或附件使用） */
  fileId?: string;
  /** 是否为记账凭证库模式（未归档） */
  isPool?: boolean;
  /** 预览模式 */
  mode?: 'stream' | 'presigned' | 'rendered';
  /** 是否自动加载（默认 true） */
  autoLoad?: boolean;
}

export interface UseFilePreviewReturn {
  /** Blob URL（用于 iframe/img src） */
  blobUrl: string | null;
  /** 预签名 URL（presigned 模式） */
  presignedUrl: string | null;
  /** 水印元数据 */
  watermark: WatermarkMetadata | null;
  /** 追踪ID */
  traceId: string | null;
  /** 加载状态 */
  loading: boolean;
  /** 错误信息 */
  error: string | null;
  /** 重试加载 */
  retry: () => void;
}

/**
 * 文件预览 Hook
 * <p>
 * 统一的预览数据获取逻辑，支持 stream/presigned/rendered 三种模式
 * 自动处理权限、水印、错误等
 * </p>
 */
export function useFilePreview(params: UseFilePreviewParams): UseFilePreviewReturn {
  const { resourceType, archiveId, fileId, isPool = false, mode = 'stream', autoLoad = true } = params;

  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [presignedUrl, setPresignedUrl] = useState<string | null>(null);
  const [watermark, setWatermark] = useState<WatermarkMetadata | null>(null);
  const [traceId, setTraceId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 用于存储 blob URL，避免重复创建
  const blobUrlRef = useRef<string | null>(null);

  // 清理 blob URL
  useEffect(() => {
    return () => {
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
    };
  }, []);

  // 提取错误信息
  const getErrorMessage = useCallback((err: any): string => {
    if (err?.response?.status === 404) {
      return '文件不存在或已被删除';
    }
    if (err?.response?.status === 403) {
      return '您没有权限访问此文件';
    }
    if (err?.response?.status === 410) {
      return '文件已过期';
    }
    if (err?.response?.status === 503) {
      return '预览服务暂时不可用，请稍后重试';
    }
    return err?.message || '加载失败，请重试';
  }, []);

  // 加载预览
  const loadPreview = useCallback(async () => {
    const normalizedRequest = previewApi.normalizeRequest({
      resourceType: resourceType ?? (isPool ? 'file' : undefined),
      archiveId,
      fileId,
      mode,
    });

    if ((isPool || normalizedRequest.resourceType === 'file') && !normalizedRequest.fileId) {
      setError('缺少文件ID');
      return;
    }
    if (!isPool && normalizedRequest.resourceType === 'archiveMain' && !normalizedRequest.archiveId) {
      setError('缺少档案ID');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      let result;

      if (isPool) {
        // 记账凭证库模式：使用 fileId 调用 pool preview API
        result = await previewApi.getPoolPreview(normalizedRequest.fileId!);
      } else {
        console.log("[useFilePreview] preview request:", normalizedRequest);
        const requestParams: PreviewRequest = normalizedRequest;
        result = await previewApi.getPreview(requestParams);
      }

      // 清理旧的 blob URL
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }

      if (result.mode === 'presigned') {
        // Presigned 模式
        setPresignedUrl(result.presignedUrl || null);
        setBlobUrl(null);
      } else {
        // Stream/Rendered 模式
        const url = URL.createObjectURL(result.blob);
        blobUrlRef.current = url;
        setBlobUrl(url);
        setPresignedUrl(null);
      }

      // 设置水印和追踪信息
      setWatermark(result.watermark || null);
      setTraceId(result.traceId || null);

    } catch (err: any) {
      console.error('Preview load error:', err);
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [resourceType, archiveId, fileId, isPool, mode, getErrorMessage]);

  // 自动加载
  useEffect(() => {
    if (autoLoad) {
      loadPreview();
    }
  }, [autoLoad, loadPreview]);

  // 重试
  const retry = useCallback(() => {
    loadPreview();
  }, [loadPreview]);

  return {
    blobUrl,
    presignedUrl,
    watermark,
    traceId,
    loading,
    error,
    retry,
  };
}

export default useFilePreview;
