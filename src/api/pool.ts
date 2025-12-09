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

    if (response.data.code !== 200) {
      throw new Error(response.data.message || '归档失败');
    }
  }
};
