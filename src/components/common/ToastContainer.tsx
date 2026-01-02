// Input: React 与 Ant Design App 组件
// Output: Toast 容器组件
// Pos: 通用复用组件，为 Ant Design message hooks 提供 App 上下文
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { App } from 'antd';
import React from 'react';

interface ToastContainerProps {
    children: React.ReactNode;
}

/**
 * Toast container component
 * Must be wrapped in App.AppProvider for message hooks to work
 *
 * This component wraps the application with Ant Design's App context,
 * which is required for the message API (toast notifications) to function properly.
 */
export const ToastContainer: React.FC<ToastContainerProps> = ({ children }) => {
    return (
        <App>
            {children}
        </App>
    );
};
