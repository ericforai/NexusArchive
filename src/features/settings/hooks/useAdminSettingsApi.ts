// Input: settings api/admin
// Output: settings admin api hook
// Pos: src/features/settings/hooks/useAdminSettingsApi.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useMemo } from 'react';
import { adminApi } from '../api';

export const useAdminSettingsApi = () => {
    return useMemo(() => ({ adminApi }), []);
};
