// Input: API client
// Output: searchApi
// Pos: 全文检索 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';

export interface GlobalSearchDTO {
    id: string;
    archiveCode: string;
    title: string;
    matchType: 'ARCHIVE' | 'METADATA';
    matchDetail: string;
    score?: number;
}

export const searchApi = {
    search: async (query: string): Promise<GlobalSearchDTO[]> => {
        const response = await client.get<GlobalSearchDTO[]>(`/search?q=${encodeURIComponent(query)}`);
        return response.data;
    }
};
