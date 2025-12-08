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
