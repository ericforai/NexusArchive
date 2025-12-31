// Input: API client 与 ApiResponse
// Output: enterpriseArchitectureApi
// Pos: 集团架构 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 全宗节点
 */
export interface FondsNode {
    id: string;
    fondsCode: string;
    fondsName: string;
    archiveCount: number;
    sizeGB: number;
    archiveYearCount: number;
}

/**
 * 法人节点
 */
export interface EntityNode {
    id: string;
    name: string;
    taxId?: string;
    status: string;
    fondsCount: number;
    archiveCount: number;
    totalSizeGB: number;
    fonds: FondsNode[];
}

/**
 * 集团架构树
 */
export interface EnterpriseArchitectureTree {
    entities: EntityNode[];
}

export const enterpriseArchitectureApi = {
    /**
     * 获取完整的集团架构树
     */
    getTree: async () => {
        const response = await client.get<ApiResponse<EnterpriseArchitectureTree>>(
            '/enterprise-architecture/tree'
        );
        return response.data;
    },

    /**
     * 获取指定法人下的架构树
     */
    getTreeByEntity: async (entityId: string) => {
        const response = await client.get<ApiResponse<EnterpriseArchitectureTree>>(
            `/enterprise-architecture/tree/entity/${entityId}`
        );
        return response.data;
    },
};

