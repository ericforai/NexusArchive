// Input: 页面标题字符串
// Output: 副作用：更新浏览器的 document.title
// Pos: src/hooks/useDocumentTitle.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useEffect } from 'react';

/**
 * 动态更新文档标题 Hook
 * 
 * @param title 页面标题（不含后缀）
 * @param parentTitle 可选的父级标题（如：系统设置）
 */
export const useDocumentTitle = (title: string | undefined, parentTitle?: string) => {
    useEffect(() => {
        const baseTitle = 'DigiVoucher 数凭';
        if (!title) {
            document.title = baseTitle;
            return;
        }

        const displayTitle = parentTitle ? `${title} - ${parentTitle}` : title;
        document.title = `${displayTitle} | ${baseTitle}`;

        // Cleanup: 组件卸载时可以不做处理，由新页面 title 覆盖
    }, [title, parentTitle]);
};
