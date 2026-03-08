// Input: 无
// Output: DA/T 92—2022《电子档案单套管理一般要求》 官方 PDF 预览版
// Pos: src/pages/product-website/regulations/DAT92Spec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const DAT92Spec: React.FC = () => {
  return (
    <RegulationLayout 
        title="DA/T 92—2022《电子档案单套管理一般要求》"
        category="核心要求"
        source="国家标准/行业规范"
        effectiveDate="2022-07-01"
        pdfUrl="/regulations/dat-92-2022.pdf"
    />
  );
};

export default DAT92Spec;
