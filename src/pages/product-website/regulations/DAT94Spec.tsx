// Input: 无
// Output: DA/T 94—2022《电子会计档案管理规范》 官方 PDF 预览版
// Pos: src/pages/product-website/regulations/DAT94Spec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const DAT94Spec: React.FC = () => {
  return (
    <RegulationLayout 
        title="DA/T 94—2022《电子会计档案管理规范》"
        category="核心规范"
        source="国家标准/行业规范"
        effectiveDate="2022-07-01"
        pdfUrl="/regulations/dat-94-2022.pdf"
    />
  );
};

export default DAT94Spec;
