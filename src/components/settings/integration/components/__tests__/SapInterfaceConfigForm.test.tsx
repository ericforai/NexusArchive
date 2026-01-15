// Input: SapInterfaceConfigForm component, test utilities
// Output: Component tests for SAP interface configuration form
// Pos: src/components/settings/integration/components/__tests__/

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SapInterfaceConfigForm } from '../SapInterfaceConfigForm';

describe('SapInterfaceConfigForm', () => {
  const mockOnChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('ODATA interface type', () => {
    it('should render OData configuration fields', () => {
      render(
        <SapInterfaceConfigForm
          interfaceType="ODATA"
          config={{
            odata: {
              serverUrl: '/sap/opu/odata4/sap/api_journal_entry',
              authType: 'Basic',
              username: 'TEST_USER',
              password: 'test_password',
            },
          }}
          onChange={mockOnChange}
        />
      );

      expect(screen.getByText('服务端点 URL')).toBeInTheDocument();
      expect(screen.getByText('认证方式')).toBeInTheDocument();
      expect(screen.getByText('技术用户名')).toBeInTheDocument();
      expect(screen.getByText('密码')).toBeInTheDocument();
      expect(screen.getByText('客户端编号')).toBeInTheDocument();
    });

    it('should show "即将推出" alert for RFC interface type', () => {
      render(
        <SapInterfaceConfigForm
          interfaceType="RFC"
          config={{}}
          onChange={mockOnChange}
        />
      );

      expect(screen.getByText('即将推出')).toBeInTheDocument();
      expect(screen.getByText(/RFC\/BAPI 接口类型目前为产品能力预留/)).toBeInTheDocument();
    });

    it('should show "即将推出" alert for IDOC interface type', () => {
      render(
        <SapInterfaceConfigForm
          interfaceType="IDOC"
          config={{}}
          onChange={mockOnChange}
        />
      );

      expect(screen.getByText('即将推出')).toBeInTheDocument();
      expect(screen.getByText(/IDoc 接口类型目前为产品能力预留/)).toBeInTheDocument();
    });

    it('should show "即将推出" alert for GATEWAY interface type', () => {
      render(
        <SapInterfaceConfigForm
          interfaceType="GATEWAY"
          config={{}}
          onChange={mockOnChange}
        />
      );

      expect(screen.getByText('即将推出')).toBeInTheDocument();
      expect(screen.getByText(/SAP Gateway 接口类型目前为产品能力预留/)).toBeInTheDocument();
    });

    it('should disable all fields when disabled prop is true', () => {
      render(
        <SapInterfaceConfigForm
          interfaceType="ODATA"
          config={{
            odata: {
              serverUrl: '/sap/opu/odata4/sap/api_journal_entry',
              authType: 'Basic',
              username: 'TEST_USER',
              password: 'test_password',
            },
          }}
          onChange={mockOnChange}
          disabled
        />
      );

      const inputs = screen.getAllByRole('textbox');
      inputs.forEach(input => {
        expect(input).toBeDisabled();
      });
    });

    it('should display help text for client number field', () => {
      render(
        <SapInterfaceConfigForm
          interfaceType="ODATA"
          config={{
            odata: {
              serverUrl: '/sap/opu/odata4/sap/api_journal_entry',
              authType: 'Basic',
              username: 'TEST_USER',
              password: 'test_password',
            },
          }}
          onChange={mockOnChange}
        />
      );

      // 客户端编号字段的帮助文本包含 "on-premise"
      expect(screen.getByText(/on-premise/i)).toBeInTheDocument();
    });
  });
});
