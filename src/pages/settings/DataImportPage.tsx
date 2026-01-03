// Input: LegacyImportPage 组件
// Output: DataImportPage 组件（历史数据导入）
// Pos: src/pages/settings/DataImportPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { LegacyImportPage } from '../admin/LegacyImportPage';

/**
 * 数据导入页面
 * 
 * 集成历史数据导入功能到系统设置中
 */
const DataImportPage: React.FC = () => {
    return (
        <div className="-m-6">
            <LegacyImportPage />
        </div>
    );
};

export default DataImportPage;


