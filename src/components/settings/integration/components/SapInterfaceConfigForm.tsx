// Input: SapInterfaceType, SapInterfaceConfig, form state/actions
// Output: SAP 接口配置表单组件（OData/RFC/IDoc/Gateway）
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { Alert, Input, InputNumber, Select, Space } from 'antd';
import { Info, Lock, Server, ShieldAlert } from 'lucide-react';
import { SapInterfaceType, SapInterfaceConfig } from '../types';

interface SapInterfaceConfigFormProps {
  interfaceType: SapInterfaceType;
  config: SapInterfaceConfig;
  onChange: (config: Partial<SapInterfaceConfig>) => void;
  disabled?: boolean;
}

/**
 * OData 配置表单
 */
const ODataConfigForm: React.FC<{
  config: Required<Pick<SapInterfaceConfig, 'odata'>>['odata'];
  onChange: (odata: SapInterfaceConfig['odata']) => void;
  disabled?: boolean;
}> = ({ config, onChange, disabled }) => {
  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      {/* Server URL */}
      <div>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          <div className="flex items-center gap-2">
            <Server size={14} />
            <span>服务端点 URL</span>
            <span style={{ color: '#ef4444' }}>*</span>
          </div>
        </label>
        <Input
          type="url"
          value={config.serverUrl}
          onChange={(e) => onChange({ ...config, serverUrl: e.target.value })}
          placeholder="/sap/opu/odata4/sap/api_journal_entry/srvd_a2x/sap/journal_entry/0001"
          disabled={disabled}
        />
        <div className="text-xs text-gray-500 mt-1">
          SAP S/4HANA OData 服务完整路径
        </div>
      </div>

      {/* 认证方式（固定为 Basic） */}
      <div>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          <div className="flex items-center gap-2">
            <ShieldAlert size={14} />
            <span>认证方式</span>
          </div>
        </label>
        <Select value="Basic" disabled style={{ width: '100%' }}>
          <Select.Option value="Basic">Basic Authentication</Select.Option>
        </Select>
      </div>

      {/* 用户名 */}
      <div>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          技术用户名 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <Input
          value={config.username}
          onChange={(e) => onChange({ ...config, username: e.target.value })}
          placeholder="SAP 系统技术用户"
          disabled={disabled}
        />
      </div>

      {/* 密码 */}
      <div>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          <div className="flex items-center gap-2">
            <Lock size={14} />
            <span>密码</span>
            <span style={{ color: '#ef4444' }}>*</span>
          </div>
        </label>
        <Input.Password
          value={config.password}
          onChange={(e) => onChange({ ...config, password: e.target.value })}
          placeholder="技术用户密码"
          disabled={disabled}
        />
      </div>

      {/* Client Number（仅 on-premise） */}
      <div>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          <div className="flex items-center gap-2">
            <Info size={14} />
            <span>客户端编号</span>
            <span className="text-xs text-gray-500 font-normal">(仅 on-premise S/4HANA)</span>
          </div>
        </label>
        <Input
          value={config.clientNumber || ''}
          onChange={(e) => onChange({ ...config, clientNumber: e.target.value || undefined })}
          placeholder="例如: 800"
          disabled={disabled}
        />
      </div>

      {/* Test Connection Service（可选） */}
      <div>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          测试连接服务 <span className="text-xs text-gray-500 font-normal">(可选)</span>
        </label>
        <Input
          value={config.testService || ''}
          onChange={(e) => onChange({ ...config, testService: e.target.value || undefined })}
          placeholder="用于测试连接的服务名"
          disabled={disabled}
        />
      </div>
    </Space>
  );
};

/**
 * RFC/BAPI 配置表单（预留）
 */
const RfcConfigForm: React.FC<{
  config?: SapInterfaceConfig['rfc'];
  onChange: (rfc: SapInterfaceConfig['rfc']) => void;
  disabled?: boolean;
}> = ({ config, onChange, disabled }) => {
  return (
    <Alert
      message="即将推出"
      description="RFC/BAPI 接口类型目前为产品能力预留，暂未实现。如需此功能，请联系产品团队。"
      type="info"
      showIcon
    />
  );
};

/**
 * IDoc 配置表单（预留）
 */
const IdocConfigForm: React.FC<{
  config?: SapInterfaceConfig['idoc'];
  onChange: (idoc: SapInterfaceConfig['idoc']) => void;
  disabled?: boolean;
}> = ({ config, onChange, disabled }) => {
  return (
    <Alert
      message="即将推出"
      description="IDoc 接口类型目前为产品能力预留，暂未实现。如需此功能，请联系产品团队。"
      type="info"
      showIcon
    />
  );
};

/**
 * SAP Gateway 配置表单（预留）
 */
const GatewayConfigForm: React.FC<{
  config?: SapInterfaceConfig['gateway'];
  onChange: (gateway: SapInterfaceConfig['gateway']) => void;
  disabled?: boolean;
}> = ({ config, onChange, disabled }) => {
  return (
    <Alert
      message="即将推出"
      description="SAP Gateway 接口类型目前为产品能力预留，暂未实现。如需此功能，请联系产品团队。"
      type="info"
      showIcon
    />
  );
};

/**
 * SAP 接口配置表单组件
 *
 * 根据选中的接口类型动态渲染对应的配置表单：
 * - OD DATA: 完整的 OData 服务配置表单
 * - RFC/IDOC/Gateway: 显示"即将推出"提示
 */
export const SapInterfaceConfigForm: React.FC<SapInterfaceConfigFormProps> = ({
  interfaceType,
  config,
  onChange,
  disabled = false,
}) => {
  const handleOdataChange = (odata: SapInterfaceConfig['odata']) => {
    onChange({ odata });
  };

  const handleRfcChange = (rfc: SapInterfaceConfig['rfc']) => {
    onChange({ rfc });
  };

  const handleIdocChange = (idoc: SapInterfaceConfig['idoc']) => {
    onChange({ idoc });
  };

  const handleGatewayChange = (gateway: SapInterfaceConfig['gateway']) => {
    onChange({ gateway });
  };

  switch (interfaceType) {
    case 'ODATA':
      return (
        <ODataConfigForm
          config={config.odata || {
            serverUrl: '',
            authType: 'Basic',
            username: '',
            password: '',
          }}
          onChange={handleOdataChange}
          disabled={disabled}
        />
      );

    case 'RFC':
      return <RfcConfigForm config={config.rfc} onChange={handleRfcChange} disabled={disabled} />;

    case 'IDOC':
      return <IdocConfigForm config={config.idoc} onChange={handleIdocChange} disabled={disabled} />;

    case 'GATEWAY':
      return <GatewayConfigForm config={config.gateway} onChange={handleGatewayChange} disabled={disabled} />;

    default:
      return null;
  }
};

export default SapInterfaceConfigForm;
