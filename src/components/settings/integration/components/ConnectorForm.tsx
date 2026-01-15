// Input: ConnectorModal State/Actions interfaces
// Output: ConnectorForm drawer component (right-side slide-in)
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

// src/components/settings/integration/components/ConnectorForm.tsx

import React from 'react';
import { X, Plus, Trash2, CheckCircle, ArrowRight, Building2, ShieldAlert } from 'lucide-react';
import { Drawer, Button, Input, Select, Space, Alert, Table, Tag, Switch, Collapse } from 'antd';
import { ConnectorModalState, ConnectorModalActions, SAP_INTERFACE_TYPES } from '../types';
import { SapInterfaceTypesCard } from './SapInterfaceTypes';
import { SapInterfaceConfigForm } from './SapInterfaceConfigForm';

interface ConnectorFormProps {
  state: ConnectorModalState;
  actions: ConnectorModalActions;
}

export function ConnectorForm({ state, actions }: ConnectorFormProps) {
  const {
    show,
    editingConfig,
    configForm,
    newMappingEntry,
    detectedType,
    testing,
  } = state;

  const isEdit = !!editingConfig?.id;
  const isTesting = testing;

  // 转换映射为表格数据
  const mappingData = Object.entries(configForm.accbookMapping || {}).map(
    ([accbookCode, fondsCode]) => ({
      key: accbookCode,
      accbookCode,
      fondsCode,
    })
  );

  const columns = [
    {
      title: '账套编码',
      dataIndex: 'accbookCode',
      key: 'accbookCode',
      render: (code: string) => <code className="px-2 py-1 bg-blue-50 text-blue-700 rounded">{code}</code>,
    },
    {
      title: '',
      key: 'arrow',
      width: 40,
      render: () => <ArrowRight size={16} className="text-gray-400 mx-auto" />,
    },
    {
      title: '全宗编码',
      dataIndex: 'fondsCode',
      key: 'fondsCode',
      render: (code: string) => <Tag color="green">{code}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 60,
      render: (_: unknown, record: { accbookCode: string }) => (
        <Button
          type="text"
          danger
          icon={<Trash2 size={14} />}
          onClick={() => actions.removeMappingEntry(record.accbookCode)}
        />
      ),
    },
  ];

  return (
    <Drawer
      title={
        <div className="flex items-center justify-between">
          <span className="text-lg font-semibold">
            {isEdit ? '编辑连接器配置' : '创建连接器配置'}
          </span>
          <Button
            type="text"
            icon={<X size={18} />}
            onClick={actions.closeModal}
          />
        </div>
      }
      placement="right"
      styles={{ wrapper: { width: 800 } }}
      open={show}
      onClose={actions.closeModal}
      destroyOnClose={true}
      footer={
        <div className="flex justify-end gap-3">
          <Button onClick={actions.closeModal}>
            取消
          </Button>
          <Button
            type="primary"
            onClick={actions.saveConfig}
            disabled={
              !configForm.name ||
              !configForm.erpType ||
              !configForm.baseUrl ||
              !configForm.appKey ||
              (!isEdit && !configForm.appSecret)
            }
          >
            {isEdit ? '保存' : '创建'}
          </Button>
        </div>
      }
    >
      {/* Config Name */}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          配置名称 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <Input
          value={configForm.name}
          onChange={e => actions.updateForm('name', e.target.value)}
          placeholder="例如: 用友YonSuite生产环境"
        />
      </div>

      {/* ERP Type */}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          ERP类型 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <Select
          value={configForm.erpType || undefined}
          onChange={value => actions.updateForm('erpType', value)}
          placeholder="请选择ERP类型"
          style={{ width: '100%' }}
        >
          <Select.Option value="yonsuite">用友 YonSuite</Select.Option>
          <Select.Option value="kingdee">金蝶云星空</Select.Option>
          <Select.Option value="weaver">泛微 OA</Select.Option>
          <Select.Option value="SAP">SAP S/4HANA</Select.Option>
          <Select.Option value="generic">通用 REST API</Select.Option>
        </Select>
      </div>

      {/* SAP 接口类型选择（仅当 ERP 类型为 SAP 时显示） */}
      {configForm.erpType === 'SAP' && (
        <div style={{ marginBottom: 16 }}>
          <Collapse
            defaultActiveKey={['sap-interface']}
            items={[
              {
                key: 'sap-interface',
                label: (
                  <div className="flex items-center gap-2">
                    <ShieldAlert size={16} className="text-blue-600" />
                    <span className="font-medium">SAP 集成接口类型</span>
                    {configForm.sapInterfaceType && (
                      <Tag color="blue">{configForm.sapInterfaceType}</Tag>
                    )}
                  </div>
                ),
                children: (
                  <div className="space-y-4">
                    {/* 接口类型卡片 */}
                    <SapInterfaceTypesCard
                      title="选择集成接口类型"
                      showIcon
                      showStatus
                      onInterfaceClick={(key) => {
                        const selectedType = SAP_INTERFACE_TYPES.find(t => t.key === key);
                        if (selectedType?.status === 'implemented') {
                          actions.selectSapInterfaceType?.(key.toUpperCase() as any);
                          actions.updateSapConfig?.({});
                        } else {
                          // 预留类型显示提示
                          // 使用静态提示，避免 Alert.info 的类型问题
                          console.log(`${selectedType?.name || key} 接口类型目前为产品能力预留，暂未实现。`);
                        }
                      }}
                    />

                    {/* 选中接口类型的配置表单 */}
                    {configForm.sapInterfaceType && configForm.sapInterfaceType === 'ODATA' && (
                      <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                        <h5 className="text-sm font-semibold mb-3">OData 服务配置</h5>
                        <SapInterfaceConfigForm
                          interfaceType={configForm.sapInterfaceType}
                          config={configForm.sapConfig || {}}
                          onChange={(config) => actions.updateSapConfig?.(config)}
                        />
                      </div>
                    )}
                  </div>
                ),
              },
            ]}
          />
        </div>
      )}

      {/* Base URL */}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          服务地址 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <Space.Compact style={{ width: '100%' }}>
          <Input
            type="url"
            value={configForm.baseUrl}
            onChange={e => actions.updateForm('baseUrl', e.target.value)}
            placeholder="https://api.example.com"
            style={{ flex: 1 }}
          />
          <Button
            onClick={() => actions.detectErpType(configForm.baseUrl)}
            disabled={!configForm.baseUrl}
          >
            自动检测
          </Button>
        </Space.Compact>
        {detectedType && (
          <Alert
            message={`检测到: ${detectedType}`}
            type="success"
            showIcon
            icon={<CheckCircle size={14} />}
            style={{ marginTop: 8 }}
          />
        )}
      </div>

      {/* App Key */}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          应用ID (App Key) <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <Input
          value={configForm.appKey}
          onChange={e => actions.updateForm('appKey', e.target.value)}
          placeholder="您的应用ID"
        />
      </div>

      {/* App Secret */}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          应用密钥 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        {isEdit && !configForm.appSecret && (
          <div style={{ marginBottom: 8, padding: '6px 12px', backgroundColor: '#e6f7ff', border: '1px solid #91d5ff', borderRadius: 4, fontSize: 12 }}>
            📝 应用密钥已保存，无需重新填写（留空则保持原值）
          </div>
        )}
        <Input.Password
          value={configForm.appSecret}
          onChange={e => actions.updateForm('appSecret', e.target.value)}
          placeholder={isEdit ? "留空保持原密钥，或输入新密钥更新" : "您的应用密钥"}
        />
      </div>

      {/* Close Check Mode (仅 YonSuite 显示) */}
      {configForm.erpType === 'yonsuite' && (
        <div style={{ marginBottom: 16, padding: 16, backgroundColor: '#f9fafb', borderRadius: 8 }}>
          <label style={{ display: 'block', marginBottom: 8, fontSize: 14, fontWeight: 500 }}>
            <div className="flex items-center gap-2">
              <ShieldAlert size={16} />
              <span>关账检查模式</span>
            </div>
          </label>
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm">
              {configForm.requireClosedPeriod ? (
                <span className="text-red-600">强制模式：未关账期间将无法同步凭证</span>
              ) : (
                <span className="text-amber-600">提醒模式：未关账时警告但允许继续同步</span>
              )}
            </span>
            <Switch
              checked={configForm.requireClosedPeriod ?? false}
              onChange={(checked) => actions.updateForm('requireClosedPeriod', checked)}
              checkedChildren="强制"
              unCheckedChildren="提醒"
            />
          </div>
          <div className="text-xs text-gray-500">
            用友 YonSuite 凭证同步前会检查期间关账状态
          </div>
        </div>
      )}

      {/* Accbook-Fonds Mapping */}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          <div className="flex items-center gap-2">
            <Building2 size={16} />
            <span>账套-全宗映射</span>
            <span className="text-xs text-gray-500 font-normal">(一个账套对应一个全宗)</span>
          </div>
        </label>

        {/* 映射列表 */}
        {mappingData.length > 0 && (
          <Table
            columns={columns}
            dataSource={mappingData}
            size="small"
            pagination={false}
            className="mb-3"
            style={{ marginBottom: 12 }}
          />
        )}

        {/* 添加新映射 */}
        <Space.Compact style={{ width: '100%' }}>
          <Input
            placeholder="账套编码 (如: BR01)"
            value={newMappingEntry.accbookCode}
            onChange={e => actions.updateNewMappingEntry('accbookCode', e.target.value)}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="全宗编码 (如: FONDS_A)"
            value={newMappingEntry.fondsCode}
            onChange={e => actions.updateNewMappingEntry('fondsCode', e.target.value)}
            style={{ flex: 1 }}
          />
          <Button
            type="primary"
            icon={<Plus size={16} />}
            onClick={() => actions.addMappingEntry(newMappingEntry.accbookCode, newMappingEntry.fondsCode)}
            disabled={!newMappingEntry.accbookCode || !newMappingEntry.fondsCode}
          >
            添加映射
          </Button>
        </Space.Compact>

        {/* 合规性提示 */}
        <div className="mt-2 p-2 bg-amber-50 text-amber-700 text-xs rounded">
          合规性要求：一个全宗只能关联一个ERP账套 (1:1严格对应)
        </div>
      </div>

      {/* Connection Test */}
      <div style={{ paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
        <Button
          type="primary"
          onClick={actions.testConnection}
          disabled={!configForm.baseUrl || !configForm.appKey || (!isEdit && !configForm.appSecret) || isTesting}
          loading={isTesting}
          icon={!isTesting && <CheckCircle size={16} />}
          size="small"
          block
          style={{ backgroundColor: '#52c41a', color: '#ffffff' }}
        >
          {isTesting ? '测试连接中...' : '测试连接'}
        </Button>
      </div>
    </Drawer>
  );
}
