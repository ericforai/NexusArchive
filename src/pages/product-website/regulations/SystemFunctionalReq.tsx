// Input: 无
// Output: GB/T 39784—2021《电子档案管理系统通用功能要求》 官方 PDF 预览版
// Pos: src/pages/product-website/regulations/SystemFunctionalReq.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const SystemFunctionalReq: React.FC = () => {
  return (
    <RegulationLayout 
        title="GB/T 39784—2021《电子档案管理系统通用功能要求》"
        category="通用标准"
        source="国家标准/行业规范"
        effectiveDate="2021-10-01"
        pdfUrl="/regulations/gbt-39784-2021.pdf"
    />
  );
};

export default SystemFunctionalReq;
