// Input: React 与 Ant Design ConfigProvider
// Output: Toast 容器组件
// Pos: 通用复用组件，为 Ant Design message 提供配置上下文
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import React from 'react';

interface ToastContainerProps {
    children: React.ReactNode;
}

/**
 * Toast container component
 * Uses ConfigProvider instead of App to avoid React 19 compatibility issues
 * while still providing Ant Design context for message/notification APIs.
 */
export const ToastContainer: React.FC<ToastContainerProps> = ({ children }) => {
    return (
        <ConfigProvider locale={zhCN}>
            {children}
        </ConfigProvider>
    );
};
