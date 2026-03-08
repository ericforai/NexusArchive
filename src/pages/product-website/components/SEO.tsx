// Input: title, description, keywords
// Output: 动态更新页面元数据（SEO 增强）
// Pos: src/pages/product-website/components/SEO.tsx

import React, { useEffect } from 'react';
import { useDocumentTitle } from '../../../hooks/useDocumentTitle';

interface SEOProps {
    title: string;
    description: string;
    keywords?: string;
    parentTitle?: string;
}

export const SEO: React.FC<SEOProps> = ({ title, description, keywords, parentTitle }) => {
    // 1. 设置浏览器标题
    useDocumentTitle(title, parentTitle);

    // 2. 更新 Meta 标签
    useEffect(() => {
        // 处理 Description
        let metaDescription = document.querySelector('meta[name="description"]');
        if (metaDescription) {
            metaDescription.setAttribute('content', description);
        } else {
            metaDescription = document.createElement('meta');
            metaDescription.setAttribute('name', 'description');
            metaDescription.setAttribute('content', description);
            document.head.appendChild(metaDescription);
        }

        // 处理 Keywords
        if (keywords) {
            let metaKeywords = document.querySelector('meta[name="keywords"]');
            if (metaKeywords) {
                metaKeywords.setAttribute('content', keywords);
            } else {
                metaKeywords = document.createElement('meta');
                metaKeywords.setAttribute('name', 'keywords');
                metaKeywords.setAttribute('content', keywords);
                document.head.appendChild(metaKeywords);
            }
        }

        // 处理 Open Graph Description (WeChat/Social)
        const ogDescription = document.querySelector('meta[property="og:description"]');
        if (ogDescription) {
            ogDescription.setAttribute('content', description);
        }

        const ogTitle = document.querySelector('meta[property="og:title"]');
        if (ogTitle) {
            ogTitle.setAttribute('content', `${title} | DigiVoucher 数凭`);
        }

        // Cleanup (可选：在组件卸载时恢复默认，或者由下一页面覆盖)
    }, [description, keywords, title]);

    return null; // SEO 组件不渲染任何 DOM
};
