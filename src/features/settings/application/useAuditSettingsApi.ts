// Input: settings infrastructure/audit
// Output: settings audit api hook
// Pos: src/features/settings/application/useAuditSettingsApi.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useMemo } from 'react';
import { auditApi } from '../infrastructure';

export const useAuditSettingsApi = () => {
    return useMemo(() => ({ auditApi }), []);
};
