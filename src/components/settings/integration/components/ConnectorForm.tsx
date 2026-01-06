// Input: ConnectorModal State/Actions interfaces
// Output: ConnectorForm drawer component (right-side slide-in)
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

// src/components/settings/integration/components/ConnectorForm.tsx

import React from 'react';
import { X, Plus, Trash2, CheckCircle } from 'lucide-react';
import { Drawer, Button, Input, Select, Space, Alert } from 'antd';
import { ConnectorModalState, ConnectorModalActions } from '../types';

interface ConnectorFormProps {
  state: ConnectorModalState;
  actions: ConnectorModalActions;
}

export function ConnectorForm({ state, actions }: ConnectorFormProps) {
  const {
    show,
    editingConfig,
    configForm,
    newAccbookCode,
    detectedType,
    testing,
  } = state;

  const isEdit = !!editingConfig?.id;
  const isTesting = testing;

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
      size="default"
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
            disabled={!configForm.name || !configForm.erpType || !configForm.baseUrl || !configForm.appKey || !configForm.appSecret}
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
          <Select.Option value="generic">通用 REST API</Select.Option>
        </Select>
      </div>

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
        <Input.Password
          value={configForm.appSecret}
          onChange={e => actions.updateForm('appSecret', e.target.value)}
          placeholder="您的应用密钥"
        />
      </div>

      {/* Accbook Codes */}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500 }}>
          账套编码
        </label>
        <Space direction="vertical" style={{ width: '100%' }} size="small">
          {configForm.accbookCodes.map(code => (
            <Space.Compact key={code} style={{ width: '100%' }}>
              <Input
                value={code}
                readOnly
                style={{ flex: 1 }}
              />
              <Button
                type="text"
                danger
                icon={<Trash2 size={16} />}
                onClick={() => actions.removeAccbookCode(code)}
              />
            </Space.Compact>
          ))}
          <Space.Compact style={{ width: '100%' }}>
            <Input
              value={newAccbookCode}
              onChange={e => actions.updateForm('newAccbookCode', e.target.value)}
              placeholder="输入账套编码"
              style={{ flex: 1 }}
              onPressEnter={() => newAccbookCode && actions.addAccbookCode(newAccbookCode)}
            />
            <Button
              type="primary"
              icon={<Plus size={16} />}
              onClick={() => actions.addAccbookCode(newAccbookCode)}
              disabled={!newAccbookCode}
            >
              添加
            </Button>
          </Space.Compact>
        </Space>
      </div>

      {/* Connection Test */}
      <div style={{ paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
        <Button
          type="primary"
          onClick={actions.testConnection}
          disabled={!configForm.baseUrl || !configForm.appKey || !configForm.appSecret || isTesting}
          loading={isTesting}
          icon={!isTesting && <CheckCircle size={16} />}
          size="large"
          block
          style={{ backgroundColor: '#52c41a', color: '#ffffff' }}
        >
          {isTesting ? '测试连接中...' : '测试连接'}
        </Button>
      </div>
    </Drawer>
  );
}
