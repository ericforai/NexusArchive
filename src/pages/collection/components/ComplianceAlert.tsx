// Input: React、Ant Design
// Output: ComplianceAlert 组件
// Pos: src/pages/collection/components/ComplianceAlert.tsx - 合规提示组件

/**
 * 批量上传合规提示组件
 *
 * 根据 DA/T 94-2022《电子会计档案管理规范》第14条要求：
 * 在电子会计资料归档和电子会计档案管理过程中应同时捕获、归档和管理元数据。
 *
 * 门类代码使用 DA/T 94 标准码：
 * - VOUCHER: 记账凭证
 * - AC02: 会计账簿
 * - AC03: 财务报告
 * - AC04: 其他会计资料
 */

import React from 'react';
import { Alert } from 'antd';

interface ComplianceAlertProps {
  /**
   * 档案门类 (DA/T 94 标准码)
   * @default 'VOUCHER'
   */
  category?: 'VOUCHER' | 'AC02' | 'AC03' | 'AC04';

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

/**
 * 根据档案门类获取合规提示内容
 */
const getComplianceMessage = (category: string) => {
  switch (category) {
    case 'VOUCHER':
      return (
        <span className="text-sm">
          <strong>合规提示：</strong>上传的文件将作为<strong>记账凭证</strong>处理，
          系统将自动关联 ERP 同步的分录数据。
        </span>
      );
    case 'AC02':
      return (
        <span className="text-sm">
          <strong>合规提示：</strong>上传的文件将作为<strong>会计账簿</strong>处理，
          请确保文件包含完整的账簿封面和账页，系统将自动校验文件完整性。
        </span>
      );
    case 'AC03':
      return (
        <span className="text-sm">
          <strong>合规提示：</strong>上传的文件将作为<strong>财务报告</strong>处理，
          请确保报告包含必要的签章和审计意见。财务报告需按永久或30年保管期限归档。
        </span>
      );
    case 'AC04':
      return (
        <span className="text-sm">
          <strong>合规提示：</strong>上传的文件将作为<strong>其他会计资料</strong>处理，
          包括银行对账单、纳税申报表等辅助性会计资料。请根据资料类型选择适当的保管期限。
        </span>
      );
    default:
      return (
        <span className="text-sm">
          <strong>合规提示：</strong>上传的文件将按照 DA/T 94-2022 规范进行归档处理。
        </span>
      );
  }
};

export const ComplianceAlert: React.FC<ComplianceAlertProps> = ({
  category = 'VOUCHER',
  closable = true,
  className,
  onClose
}) => {
  return (
    <Alert
      type="info"
      showIcon
      closable={closable}
      onClose={onClose}
      className={className}
      title={getComplianceMessage(category)}
    />
  );
};

export default ComplianceAlert;
