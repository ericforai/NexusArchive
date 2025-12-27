// Input: API client 与 auth store
// Output: poolApi
// Pos: 预归档池 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { useAuthStore } from '../store';

/**
 * 电子凭证池列表项
 */
export interface PoolItem {
  id: string;
  code: string;
  source: string;
  type: string;
  amount: string;
  date: string;
  status: string;
  /** 来源系统 (如 YonSuite, Kingdee, Manual) */
  sourceSystem?: string;
  /** 文件名 */
  fileName?: string;
  /** 凭证字号 (如"记-1") */
  voucherWord?: string;
  /** 摘要 (业务描述) */
  summary?: string;
  /** 业务日期 */
  docDate?: string;
}

/**
 * API响应格式
 */
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * 电子凭证池 API
 */
export const poolApi = {
  /**
   * 查询电子凭证池列表
   */
  getList: async (): Promise<PoolItem[]> => {
    try {
      const response = await client.get<ApiResponse<PoolItem[]>>('/pool/list');
      if (response.data.code === 200) {
        return response.data.data;
      } else {
        console.error('Failed to fetch pool list:', response.data.message);
        return [];
      }
    } catch (error) {
      console.error('Error fetching pool list:', error);
      return [];
    }
  },

  /**
   * 获取关联附件列表
   */
  getRelatedFiles: async (id: string): Promise<PoolItem[]> => {
    try {
      const response = await client.get<ApiResponse<PoolItem[]>>(`/pool/related/${id}`);
      if (response.data.code === 200) {
        return response.data.data;
      }
      return [];
    } catch (error) {
      console.error('Error fetching related files:', error);
      return [];
    }
  },

  /**
   * 正式归档 (批量提交归档申请)
   * 将凭证池中的记录提交归档审批
   */
  archiveItems: async (fileIds: string[]): Promise<void> => {
    const { user } = useAuthStore.getState();
    const applicantId = user?.id || 'admin';
    const applicantName = user?.username || 'Admin';

    const response = await client.post<ApiResponse<any>>('/pool/submit/batch', {
      fileIds,
      applicantId,
      applicantName,
      reason: '批量归档申请'
    });

    console.log('poolApi.archiveItems response:', response.data);

    if (response.data.code !== 200) {
      throw new Error(response.data.message || '归档失败');
    }
  },

  /**
   * 搜索候选凭证
   */
  searchCandidates: async (params: any): Promise<PoolItem[]> => {
    try {
      const response = await client.post<ApiResponse<PoolItem[]>>('/pool/candidates/search', params);
      if (response.data.code === 200) {
        return response.data.data;
      }
      return [];
    } catch (error) {
      console.error('Error searching candidates:', error);
      return [];
    }
  },

  /**
   * 删除电子凭证池记录
   */
  delete: async (id: string): Promise<void> => {
    const response = await client.post<ApiResponse<any>>(`/pool/delete/${id}`);
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '删除失败');
    }
  }
};
