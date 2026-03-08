// Input: 无
// Output: DA/T 104—2024《企业资源计划（ERP）系统电子文件归档和电子档案管理规范》 官方 PDF 预览版 (SEO 增强型)
// Pos: src/pages/product-website/regulations/ERPArchiveSpec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const ERPArchiveSpec: React.FC = () => {
  return (
    <RegulationLayout 
        title="DA/T 104—2024《企业资源计划（ERP）系统电子文件归档和电子档案管理规范》"
        category="行业标准"
        source="国家标准/行业规范"
        effectiveDate="2024-04-01"
        pdfUrl="/regulations/dat-104-2024.pdf"
        description="2024年新版 DA/T 104 规范。规定了 ERP 系统中电子文件的在线归档流程与数据接口标准，是企业 ERP 数字化转型的必备合规参考。"
    />
  );
};

export default ERPArchiveSpec;
