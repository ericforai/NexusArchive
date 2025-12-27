// Input: React、RoleSettings、settings feature
// Output: RoleSettingsPage 组件
// Pos: src/pages/settings/RoleSettingsPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { RoleSettings } from '../../components/settings/RoleSettings';
import { useAdminSettingsApi } from '../../features/settings';

const RoleSettingsPage: React.FC = () => {
    const { adminApi } = useAdminSettingsApi();

    return <RoleSettings adminApi={adminApi} />;
};

export default RoleSettingsPage;
