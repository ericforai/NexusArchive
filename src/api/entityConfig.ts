// Input: API client 与 ApiResponse
// Output: entityConfigApi
// Pos: 法人配置 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 法人配置
 */
export interface EntityConfig {
    id?: string;
    entityId: string;
    configType: 'ERP_INTEGRATION' | 'BUSINESS_RULE' | 'COMPLIANCE_POLICY';
    configKey: string;
    configValue?: string; // JSON格式
    description?: string;
    createdBy?: string;
    createdTime?: string;
    updatedTime?: string;
}

export const entityConfigApi = {
    /**
     * 查询指定法人的所有配置
     */
    getConfigsByEntityId: async (entityId: string) => {
        const response = await client.get<ApiResponse<EntityConfig[]>>(
            `/entity-config/entity/${entityId}`
        );
        return response.data;
    },

    /**
     * 查询指定法人和配置类型的配置
     */
    getConfigsByEntityIdAndType: async (entityId: string, configType: string) => {
        const response = await client.get<ApiResponse<EntityConfig[]>>(
            `/entity-config/entity/${entityId}/type/${configType}`
        );
        return response.data;
    },

    /**
     * 查询指定法人的配置（按类型分组）
     */
    getConfigsGroupedByType: async (entityId: string) => {
        const response = await client.get<ApiResponse<Record<string, EntityConfig[]>>>(
            `/entity-config/entity/${entityId}/grouped`
        );
        return response.data;
    },

    /**
     * 保存或更新配置
     */
    saveOrUpdate: async (config: EntityConfig) => {
        const response = await client.post<ApiResponse<{ configId: string }>>(
            '/entity-config',
            config
        );
        return response.data;
    },

    /**
     * 删除指定法人的所有配置
     */
    deleteByEntityId: async (entityId: string) => {
        const response = await client.delete<ApiResponse<void>>(
            `/entity-config/entity/${entityId}`
        );
        return response.data;
    },

    /**
     * 删除指定法人和配置类型的配置
     */
    deleteByEntityIdAndType: async (entityId: string, configType: string) => {
        const response = await client.delete<ApiResponse<void>>(
            `/entity-config/entity/${entityId}/type/${configType}`
        );
        return response.data;
    },
};






