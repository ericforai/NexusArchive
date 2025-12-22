// Input: API client 与 ApiResponse 类型
// Output: warehouseApi
// Pos: 库房管理 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

export interface Shelf {
  id: string;
  name: string;
  code: string;
  path?: string;
  capacity?: number;
  usedCount?: number;
  status?: string;
}

export interface WarehouseEnvironment {
  temperature?: number;
  humidity?: number;
  status?: string;
  lastUpdated?: string;
}

export type WarehouseCommand = 'open' | 'close' | 'vent' | 'lock' | 'unlock';

export const warehouseApi = {
  async getShelves() {
    const res = await client.get<ApiResponse<Shelf[]>>('/warehouse/shelves');
    return res.data;
  },
  async getEnvironment() {
    const res = await client.get<ApiResponse<WarehouseEnvironment>>('/warehouse/environment');
    return res.data;
  },
  async sendCommand(rackId: string, action: WarehouseCommand) {
    const res = await client.post<ApiResponse<any>>(`/warehouse/racks/${rackId}/command`, { action: action.toUpperCase() });
    return res.data;
  }
};
