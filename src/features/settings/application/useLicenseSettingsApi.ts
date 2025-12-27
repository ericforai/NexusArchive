// Input: settings infrastructure/license
// Output: settings license api hook
// Pos: src/features/settings/application/useLicenseSettingsApi.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useMemo } from 'react';
import { licenseApi } from '../infrastructure';

export const useLicenseSettingsApi = () => {
    return useMemo(() => ({ licenseApi }), []);
};
