// src/pages/archives/hooks/useVoucherData.ts
/**
 * 凭证数据获取 Hook
 *
 * 职责：管理凭证数据的获取、解析和状态
 * 变更理由：数据获取逻辑变化，添加关联附件支持
 */

import { useState, useEffect } from 'react';
import type { VoucherDTO } from '../../../components/voucher';
import { archivesApi, type VoucherDataResponse } from '../../../api/archives';
import { parseVoucherData } from '../utils/voucherDataParser';
import type { AttachmentDTO } from '../../../components/voucher/types';

interface UseVoucherDataOptions {
  row: any;
  enabled?: boolean;
}

// 将后端返回的附件信息转换为前端 AttachmentDTO 格式
// 注意：axios baseURL 已包含 /api，所以 fileUrl 不应再加 /api 前缀
const convertAttachments = (apiAttachments: VoucherDataResponse['attachments']): AttachmentDTO[] | undefined => {
  if (!apiAttachments || apiAttachments.length === 0) return undefined;
  return apiAttachments.map(att => ({
    id: att.id,
    fileName: att.fileName,
    fileUrl: `/archive/files/download/${att.id}`,  // 修复：移除重复的 /api 前缀
    type: att.fileType,
  }));
};

export function useVoucherData({ row, enabled = true }: UseVoucherDataOptions) {
  const [voucherData, setVoucherData] = useState<VoucherDTO | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!enabled || !row?.id) {
      setVoucherData(null);
      return;
    }

    // 首先检查 row 是否已有 sourceData（来自记账凭证库视图）
    const existingSourceData = row.sourceData || row.source_data;
    if (existingSourceData) {
      const result = parseVoucherData(existingSourceData, row);
      // 如果 row 中有 attachments，直接使用
      if (row.attachments && Array.isArray(row.attachments)) {
        result.voucherData = {
          ...result.voucherData!,
          voucherId: row.id ?? '',
          attachments: row.attachments
        };
      }
      setVoucherData(result.voucherData);
      setError(result.error || null);
      return;
    }

    // 否则调用 API 获取凭证数据（来自档案列表视图）
    const fetchVoucherData = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await archivesApi.getVoucherData(row.id);
        if (response?.sourceData) {
          const result = parseVoucherData(response.sourceData, row);
          // 添加关联的原始凭证附件
          const attachments = convertAttachments(response.attachments);
          if (attachments) {
            result.voucherData = {
              ...result.voucherData!,
              voucherId: row.id ?? '',
              attachments
            };
          }
          setVoucherData(result.voucherData);
          setError(result.error || null);
        } else {
          setVoucherData(null);
          setError('No voucher data available');
        }
      } catch (err) {
        console.warn('[useVoucherData] Failed to fetch:', err);
        setVoucherData(null);
        setError(String(err));
      } finally {
        setIsLoading(false);
      }
    };

    fetchVoucherData();
  }, [row?.id, row?.code, enabled]);

  return {
    voucherData,
    isLoading,
    error,
  };
}
