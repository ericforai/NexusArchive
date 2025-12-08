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
