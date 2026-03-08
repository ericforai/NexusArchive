// Input: 无
// Output: GB/T 39784—2021《电子档案管理系统通用功能要求》 官方 PDF 预览版 (SEO 增强型)
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
        description="国家标准 GB/T 39784—2021 详细罗列了电子档案管理系统（EAMS）必须具备的收集、整理、保存、利用等通用功能模块要求。"
    />
  );
};

export default SystemFunctionalReq;
