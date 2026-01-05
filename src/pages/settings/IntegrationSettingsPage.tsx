// Input: React、IntegrationSettingsPage、settings feature
// Output: IntegrationSettingsPage 组件
// Pos: src/pages/settings/IntegrationSettingsPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { IntegrationSettingsPage as IntegrationSettingsComponent } from '../../components/settings/integration';
import { useIntegrationSettingsApi } from '../../features/settings';

const IntegrationSettingsPage: React.FC = () => {
    const { erpApi } = useIntegrationSettingsApi();

    return <IntegrationSettingsComponent erpApi={erpApi} />;
};

export default IntegrationSettingsPage;
