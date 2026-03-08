// Input: 无
// Output: DA/T 94—2022《电子会计档案管理规范》 官方 PDF 预览版 (SEO 增强型)
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
        description="详解 DA/T 94—2022《电子会计档案管理规范》内容，涵盖电子会计档案的元数据要求、归档、整理、保管及鉴定销毁全过程。DigiVoucher 助力企业 100% 遵从国标。"
    />
  );
};

export default DAT94Spec;
