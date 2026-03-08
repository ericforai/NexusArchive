// Input: 无
// Output: GB/T 18894—2016《电子文件归档与电子档案管理规范》 官方 PDF 预览版 (SEO 增强型)
// Pos: src/pages/product-website/regulations/GBT18894Spec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const GBT18894Spec: React.FC = () => {
  return (
    <RegulationLayout 
        title="GB/T 18894—2016《电子文件归档与电子档案管理规范》"
        category="国家标准"
        source="国家标准/行业规范"
        effectiveDate="2017-03-01"
        pdfUrl="/regulations/gbt-18894-2016.pdf"
        description="通用国标 GB/T 18894。规定了电子文件从产生到归档的全流程管理规范，适用于各级各类机构开展电子档案标准化建设。"
    />
  );
};

export default GBT18894Spec;
