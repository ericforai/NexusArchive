// Input: settings infrastructure/erp
// Output: settings integration api hook
// Pos: src/features/settings/application/useIntegrationSettingsApi.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useMemo } from 'react';
import { erpApi } from '../infrastructure';

export const useIntegrationSettingsApi = () => {
    return useMemo(() => ({ erpApi }), []);
};
