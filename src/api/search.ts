// Input: API client
// Output: searchApi
// Pos: 全文检索 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { GlobalSearchDTO } from '../types';

export const searchApi = {
    search: async (query: string): Promise<GlobalSearchDTO[]> => {
        const response = await client.get<GlobalSearchDTO[]>(`/search?q=${encodeURIComponent(query)}`);
        return response.data;
    }
};
