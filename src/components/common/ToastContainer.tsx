// Input: React 子节点
// Output: 简单的透传容器（HMR 修复版）
// Pos: src/components/common/ToastContainer.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { message as antdMessage, notification as antdNotification } from 'antd';

interface ToastContainerProps {
    children: React.ReactNode;
}

/**
 * Toast 容器组件
 *
 * HMR 修复: 使用 React 命名空间导入 (React.useEffect) 而非具名导入，
 * 以避免在 HMR 过程中出现 "Cannot read properties of null (reading 'useEffect')" 错误。
 */
export const ToastContainer: React.FC<ToastContainerProps> = ({ children }) => {
    React.useEffect(() => {
        try {
            // 安全配置 antd 全局提示
            if (antdMessage && typeof antdMessage.config === 'function') {
                antdMessage.config({
                    duration: 3,
                    maxCount: 3,
                });
            }
            if (antdNotification && typeof antdNotification.config === 'function') {
                antdNotification.config({
                    placement: 'topRight',
                    maxCount: 3,
                });
            }
        } catch (e) {
            console.warn('[ToastContainer] Config failed:', e);
        }
    }, []);

    return <>{children}</>;
};
