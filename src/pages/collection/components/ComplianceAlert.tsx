// Input: React、Ant Design
// Output: ComplianceAlert 组件
// Pos: src/pages/collection/components/ComplianceAlert.tsx - 合规提示组件

/**
 * 批量上传合规提示组件
 *
 * 根据 DA/T 94-2022《电子会计档案管理规范》第14条要求：
 * 在电子会计资料归档和电子会计档案管理过程中应同时捕获、归档和管理元数据。
 *
 * 本功能用于上传原始凭证附件（发票、合同、回单等），需要在凭证关联页面匹配到记账凭证后方可归档。
 */

import React from 'react';
import { Alert } from 'antd';

interface ComplianceAlertProps {
  /**
   * 是否显示关闭按钮
   * @default true
   */
  closable?: boolean;

  /**
   * 自定义样式类名
   */
  className?: string;

  /**
   * 关闭时的回调
   */
  onClose?: () => void;
}

export const ComplianceAlert: React.FC<ComplianceAlertProps> = ({
  closable = true,
  className,
  onClose
}) => {
  return (
    <Alert
      type="warning"
      showIcon
      closable={closable}
      onClose={onClose}
      className={className}
      message={
        <span className="text-sm">
          <strong>合规提示：</strong>上传的文件将作为<strong>原始凭证附件</strong>处理，
          需要在凭证关联页面匹配到对应记账凭证后方可归档。
          未关联的文件将保持<strong>待补录</strong>状态，可手动归档。
        </span>
      }
    />
  );
};

export default ComplianceAlert;
