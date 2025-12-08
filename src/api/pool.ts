import { client } from './client';

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
   * 正式归档
   * 将凭证池中的记录转换为正式的 AIP 档案包
   */
  archiveItems: async (poolItemIds: string[]): Promise<void> => {
    const response = await client.post<ApiResponse<void>>('/v1/archive/sip/archive', {
      poolItemIds
    });
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '归档失败');
    }
  }
};
