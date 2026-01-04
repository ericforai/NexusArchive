// Input: React、SecuritySettings、settings feature
// Output: SecuritySettingsPage 组件
// Pos: src/pages/settings/SecuritySettingsPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { SecuritySettings } from '../../components/settings';
import { useAdminSettingsApi } from '../../features/settings';

const SecuritySettingsPage: React.FC = () => {
    const { adminApi } = useAdminSettingsApi();

    return <SecuritySettings adminApi={adminApi} />;
};

export default SecuritySettingsPage;
