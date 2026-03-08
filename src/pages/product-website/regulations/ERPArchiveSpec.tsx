// Input: 无
// Output: DA/T 104—2024《企业资源计划（ERP）系统电子文件归档和电子档案管理规范》 官方 PDF 预览版
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
    />
  );
};

export default ERPArchiveSpec;
