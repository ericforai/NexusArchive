// src/components/settings/integration/hooks/useAiAdapterHandler.ts

import { useState, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import { AiAdapterState, AiAdapterActions } from '../types';

interface UseAiAdapterHandlerOptions {
  erpApi: {
    generateAiPreview: (file: File) => Promise<any>;
    adaptAiToConfig: (file: File, configId: number) => Promise<any>;
  };
  onAdapted?: () => void;
}

export function useAiAdapterHandler(options: UseAiAdapterHandlerOptions) {
  const { erpApi, onAdapted } = options;

  const [show, setShow] = useState(false);
  const [loading, setLoading] = useState(false);
  const [files, setFiles] = useState<File[]>([]);
  const [preview, setPreview] = useState<any>(null);
  const [selectedTargetConfigId, setSelectedTargetConfigId] = useState<number | null>(null);

  const openAiAdapter = useCallback(() => {
    setShow(true);
    setFiles([]);
    setPreview(null);
    setSelectedTargetConfigId(null);
  }, []);

  const closeAiAdapter = useCallback(() => {
    setShow(false);
    setFiles([]);
    setPreview(null);
    setSelectedTargetConfigId(null);
    setLoading(false);
  }, []);

  const uploadFiles = useCallback((uploadedFiles: File[]) => {
    setFiles(prev => [...prev, ...uploadedFiles]);
    setPreview(null);
  }, []);

  const removeFile = useCallback((index: number) => {
    setFiles(prev => prev.filter((_, i) => i !== index));
    setPreview(null);
  }, []);

  const generatePreview = useCallback(async () => {
    if (files.length === 0) {
      toast.error('请先上传文件');
      return;
    }

    setLoading(true);
    try {
      const firstFile = files[0];
      const response = await erpApi.generateAiPreview(firstFile);
      setPreview(response.data);
      toast.success('预览生成成功');
    } catch (error) {
      toast.error('预览生成失败');
      console.error('Generate preview error:', error);
    } finally {
      setLoading(false);
    }
  }, [files, erpApi]);

  const adaptToConfig = useCallback(async (configId: number) => {
    if (files.length === 0) {
      toast.error('请先上传文件');
      return;
    }

    setLoading(true);
    setSelectedTargetConfigId(configId);
    try {
      const firstFile = files[0];
      await erpApi.adaptAiToConfig(firstFile, configId);
      toast.success('适配成功');
      closeAiAdapter();
      onAdapted?.();
    } catch (error) {
      toast.error('适配失败');
      console.error('Adapt to config error:', error);
    } finally {
      setLoading(false);
    }
  }, [files, erpApi, closeAiAdapter, onAdapted]);

  const state: AiAdapterState = {
    show,
    loading,
    files,
    preview,
    selectedTargetConfigId,
  };

  const actions: AiAdapterActions = {
    openAiAdapter,
    closeAiAdapter,
    uploadFiles,
    removeFile,
    generatePreview,
    adaptToConfig,
  };

  return { state, actions };
}
