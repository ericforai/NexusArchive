// Input: api/search
// Output: 全文检索 API Hook
// Pos: src/hooks/useGlobalSearchApi.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useCallback } from 'react';
import { searchApi } from '../api/search';
import { GlobalSearchDTO } from '../types';

export const useGlobalSearchApi = () => {
    const search = useCallback((query: string): Promise<GlobalSearchDTO[]> => {
        return searchApi.search(query);
    }, []);

    return { search };
};
