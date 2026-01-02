// src/pages/archives/hooks/useVoucherData.ts
/**
 * 凭证数据获取 Hook
 *
 * 职责：管理凭证数据的获取、解析和状态
 * 变更理由：数据获取逻辑变化
 */

import { useState, useEffect } from 'react';
import type { VoucherDTO } from '../../../components/voucher';
import { archivesApi } from '../../../api/archives';
import { parseVoucherData } from '../utils/voucherDataParser';

interface UseVoucherDataOptions {
  row: any;
  enabled?: boolean;
}

export function useVoucherData({ row, enabled = true }: UseVoucherDataOptions) {
  const [voucherData, setVoucherData] = useState<VoucherDTO | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!enabled || !row?.id) {
      setVoucherData(null);
      return;
    }

    // 首先检查 row 是否已有 sourceData（来自电子凭证池视图）
    const existingSourceData = row.sourceData || row.source_data;
    if (existingSourceData) {
      const result = parseVoucherData(existingSourceData, row);
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
  }, [row?.id, enabled]);

  return {
    voucherData,
    isLoading,
    error,
  };
}
