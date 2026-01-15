// Input: SAP_INTERFACE_TYPES, React hooks
// Output: SapInterfaceTypes component displaying 4 interface type badges
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

// src/components/settings/integration/components/SapInterfaceTypes.tsx
import React from 'react';
import { Badge, Tooltip } from 'antd';
import { SAP_INTERFACE_TYPES } from '../types';

/**
 * SAP 接口类型状态徽章颜色映射
 */
const STATUS_COLOR_MAP: Record<string, string> = {
  implemented: 'success',
  reserved: 'default',
  planned: 'processing',
  deprecated: 'error',
} as const;

/**
 * SAP 接口类型状态文本映射
 */
const STATUS_TEXT_MAP: Record<string, string> = {
  implemented: '已实现',
  reserved: '产品能力预留',
  planned: '计划中',
  deprecated: '已废弃',
} as const;

/**
 * SAP 接口类型显示组件属性
 */
export interface SapInterfaceTypesProps {
  /**
   * 是否显示图标
   */
  showIcon?: boolean;
  /**
   * 是否显示描述
   */
  showDescription?: boolean;
  /**
   * 是否显示状态徽章
   */
  showStatus?: boolean;
  /**
   * 布局方向
   */
  direction?: 'horizontal' | 'vertical';
  /**
   * 自定义类名
   */
  className?: string;
  /**
   * 点击接口类型时的回调
   */
  onInterfaceClick?: (interfaceKey: string) => void;
}

/**
 * 单个接口类型项组件
 */
interface InterfaceTypeItemProps {
  type: typeof SAP_INTERFACE_TYPES[number];
  showIcon: boolean;
  showDescription: boolean;
  showStatus: boolean;
  onClick?: (key: string) => void;
}

const InterfaceTypeItem: React.FC<InterfaceTypeItemProps> = ({
  type,
  showIcon,
  showDescription: _showDescription,
  showStatus,
  onClick,
}) => {
  const IconComponent = type.icon;

  const handleClick = () => {
    if (onClick) {
      onClick(type.key);
    }
  };

  return (
    <Tooltip
      title={type.description}
      placement="top"
    >
      <div
        className={`interface-type-item ${onClick ? 'cursor-pointer' : ''}`}
        onClick={handleClick}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '8px 12px',
          borderRadius: '6px',
          backgroundColor: 'var(--bg-color-container, #f5f5f5)',
          border: '1px solid var(--border-color, #d9d9d9)',
          cursor: onClick ? 'pointer' : 'default',
          transition: 'all 0.2s',
        }}
        onMouseEnter={(e) => {
          if (onClick) {
            e.currentTarget.style.borderColor = '#1890ff';
            e.currentTarget.style.boxShadow = '0 2px 4px rgba(24, 144, 255, 0.1)';
          }
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.borderColor = 'var(--border-color, #d9d9d9)';
          e.currentTarget.style.boxShadow = 'none';
        }}
      >
        {showIcon && (
          <IconComponent
            size={16}
            style={{
              color: type.status === 'implemented' ? '#52c41a' : '#8c8c8c',
              flexShrink: 0,
            }}
          />
        )}
        <span
          style={{
            fontWeight: 500,
            fontSize: '14px',
            color: 'var(--text-color, rgba(0, 0, 0, 0.88))',
          }}
        >
          {type.name}
        </span>
        {showStatus && (
          <Badge
            status={STATUS_COLOR_MAP[type.status] as any}
            text={
              <span
                style={{
                  fontSize: '12px',
                  color: 'var(--text-color-secondary, rgba(0, 0, 0, 0.65))',
                }}
              >
                {STATUS_TEXT_MAP[type.status]}
              </span>
            }
          />
        )}
      </div>
    </Tooltip>
  );
};

/**
 * SAP 接口类型展示组件
 *
 * <p>展示 SAP S/4HANA 支持的四种接口类型：</p>
 * <ul>
 *   <li>OData 服务 - 已实现</li>
 *   <li>RFC/BAPI - 产品能力预留</li>
 *   <li>IDoc - 产品能力预留</li>
 *   <li>SAP Gateway - 产品能力预留</li>
 * </ul>
 *
 * @example
 * ```tsx
 * // 基础用法
 * <SapInterfaceTypes />
 *
 * // 带图标和状态
 * <SapInterfaceTypes showIcon showStatus />
 *
 * // 垂直布局
 * <SapInterfaceTypes direction="vertical" showDescription />
 *
 * // 可点击项
 * <SapInterfaceTypes onInterfaceClick={(key) => console.log(key)} />
 * ```
 */
export const SapInterfaceTypes: React.FC<SapInterfaceTypesProps> = ({
  showIcon = true,
  showDescription = false,
  showStatus = true,
  direction = 'horizontal',
  className,
  onInterfaceClick,
}) => {
  return (
    <div
      className={`sap-interface-types sap-interface-types-${direction} ${className || ''}`}
      style={{
        display: 'flex',
        flexDirection: direction === 'vertical' ? 'column' : 'row',
        gap: direction === 'vertical' ? '12px' : '16px',
        flexWrap: direction === 'horizontal' ? 'wrap' : 'nowrap',
      }}
    >
      {SAP_INTERFACE_TYPES.map((type) => (
        <InterfaceTypeItem
          key={type.key}
          type={type}
          showIcon={showIcon}
          showDescription={showDescription}
          showStatus={showStatus}
          onClick={onInterfaceClick}
        />
      ))}
    </div>
  );
};

/**
 * SAP 接口类型卡片组件
 * 用于在配置详情中展示 SAP 接口类型信息
 */
export const SapInterfaceTypesCard: React.FC<
  Omit<SapInterfaceTypesProps, 'direction' | 'showDescription'> & {
    title?: string;
  }
> = ({ title = 'SAP 集成接口类型', ...props }) => {
  return (
    <div
      style={{
        padding: '16px',
        borderRadius: '8px',
        backgroundColor: 'var(--bg-color-container, #fff)',
        border: '1px solid var(--border-color, #f0f0f0)',
      }}
    >
      <h4
        style={{
          marginBottom: '12px',
          fontSize: '14px',
          fontWeight: 600,
          color: 'var(--text-color, rgba(0, 0, 0, 0.88))',
        }}
      >
        {title}
      </h4>
      <SapInterfaceTypes {...props} />
    </div>
  );
};

export default SapInterfaceTypes;
