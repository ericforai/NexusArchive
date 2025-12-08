import { client } from './client';
import { ApiResponse } from '../types';

export interface NotificationItem {
  id: string;
  title: string;
  time: string;
  type: 'info' | 'warning' | 'success';
}

export const notificationsApi = {
  list: async () => {
    const res = await client.get<ApiResponse<NotificationItem[]>>('/notifications');
    return res.data;
  }
};
