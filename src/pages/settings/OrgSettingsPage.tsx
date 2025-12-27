// Input: React、OrgSettings、settings feature
// Output: OrgSettingsPage 组件
// Pos: src/pages/settings/OrgSettingsPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { OrgSettings } from '../../components/settings/OrgSettings';
import { useAdminSettingsApi } from '../../features/settings';

const OrgSettingsPage: React.FC = () => {
    const { adminApi } = useAdminSettingsApi();

    return <OrgSettings adminApi={adminApi} />;
};

export default OrgSettingsPage;
