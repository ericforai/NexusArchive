// Input: React、UserSettings、settings feature
// Output: UserSettingsPage 组件
// Pos: src/pages/settings/UserSettingsPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { UserSettings } from '../../components/settings/UserSettings';
import { useAdminSettingsApi } from '../../features/settings';

const UserSettingsPage: React.FC = () => {
    const { adminApi } = useAdminSettingsApi();

    return <UserSettings adminApi={adminApi} />;
};

export default UserSettingsPage;
