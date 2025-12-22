// Input: API client 与 ApiResponse 类型
// Output: notificationsApi
// Pos: 通知消息 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
