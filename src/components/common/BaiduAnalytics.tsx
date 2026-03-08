// Input: 百度统计 Site ID (via ENV)
// Output: 百度统计脚本加载组件
// Pos: src/components/common/BaiduAnalytics.tsx

import React, { useEffect } from 'react';

/**
 * Baidu Analytics Component
 * 
 * 职责：按需加载百度统计脚本。
 * 仅在 VITE_ENABLE_BAIDU_ANALYTICS=true 时生效。
 */
export const BaiduAnalytics: React.FC = () => {
    useEffect(() => {
        // 检查环境变量开关
        const isEnabled = import.meta.env.VITE_ENABLE_BAIDU_ANALYTICS === 'true';

        if (!isEnabled) {
            return;
        }

        const siteId = '500e253aad1de45a0d453b070aa98fc4';

        // 避免重复加载
        if (window._hmt) return;

        console.log('[Analytics] Initializing Baidu Analytics...');

        window._hmt = window._hmt || [];
        const script = document.createElement('script');
        script.src = `https://hm.baidu.com/hm.js?${siteId}`;
        script.async = true;

        const firstScript = document.getElementsByTagName('script')[0];
        if (firstScript && firstScript.parentNode) {
            firstScript.parentNode.insertBefore(script, firstScript);
        } else {
            document.head.appendChild(script);
        }

        return () => {
            // 注意：百度统计脚本通常不需要在组件卸载时手动移除，
            // 因为它会在全局作用域注册，且我们只在特定根路由加载。
        };
    }, []);

    return null; // 此组件不渲染任何 UI
};

declare global {
    interface Window {
        _hmt: any[];
    }
}
