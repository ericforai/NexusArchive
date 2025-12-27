// Input: React、BasicSettings、settings feature
// Output: BasicSettingsPage 组件
// Pos: src/pages/settings/BasicSettingsPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { BasicSettings } from '../../components/settings/BasicSettings';
import { useAdminSettingsApi } from '../../features/settings';

const BasicSettingsPage: React.FC = () => {
    const { adminApi } = useAdminSettingsApi();

    return <BasicSettings adminApi={adminApi} />;
};

export default BasicSettingsPage;
