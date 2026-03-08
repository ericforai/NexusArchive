// Input: 无
// Output: DA/T 95—2022《行政事业单位一般公共预算支出财务报销电子会计凭证档案管理技术规范》 官方 PDF 预览版
// Pos: src/pages/product-website/regulations/DAT95Spec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const DAT95Spec: React.FC = () => {
  return (
    <RegulationLayout 
        title="DA/T 95—2022《行政事业单位一般公共预算支出财务报销电子会计凭证档案管理技术规范》"
        category="技术规范"
        source="国家标准/行业规范"
        effectiveDate="2022-07-01"
        pdfUrl="/regulations/dat-95-2022.pdf"
    />
  );
};

export default DAT95Spec;
