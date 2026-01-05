// src/components/settings/integration/components/__tests__/ErpConfigList.test.tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ErpConfigList } from '../ErpConfigList';
import { ErpConfig } from '@/types';

describe('ErpConfigList', () => {
  const mockConfigs: ErpConfig[] = [
    { id: 1, name: 'YonSuite', erpType: 'yonsuite', configJson: '{}', isActive: 1 },
    { id: 2, name: '金蝶云', erpType: 'kingdee', configJson: '{}', isActive: 1 },
  ];

  it('should render configs in grid layout', () => {
    render(<ErpConfigList configs={mockConfigs} />);
    // ErpConfigCard renders the connector name
    expect(screen.getByText('YonSuite')).toBeInTheDocument();
    expect(screen.getByText('金蝶云')).toBeInTheDocument();
  });

  it('should use responsive grid classes', () => {
    const { container } = render(<ErpConfigList configs={mockConfigs} />);
    const grid = container.querySelector('.grid-cols-1');
    expect(grid).toBeInTheDocument();
  });

  it('should show empty state when no configs', () => {
    render(<ErpConfigList configs={[]} />);
    expect(screen.getByText('还没有配置任何连接器')).toBeInTheDocument();
  });
});
