import { render, screen, fireEvent } from '@testing-library/react';
import { ErpConfigCard } from '../ErpConfigCard';
import { ErpConfig } from '@/types';

describe('ErpConfigCard', () => {
  const mockConfig: ErpConfig = {
    id: 1,
    name: 'YonSuite',
    erpType: 'yonsuite',
    configJson: '{}',
    createdAt: '2026-01-01T00:00:00Z',
  };

  it('should render connector name and status', () => {
    render(<ErpConfigCard config={mockConfig} status="connected" />);
    expect(screen.getByText('YonSuite')).toBeInTheDocument();
    expect(screen.getByText('已连接')).toBeInTheDocument();
  });

  it('should render action bar with all buttons', () => {
    render(<ErpConfigCard config={mockConfig} status="connected" />);
    expect(screen.getByText('配置中心')).toBeInTheDocument();
    expect(screen.getByText('检查连接')).toBeInTheDocument();
    expect(screen.getByText('健康检查')).toBeInTheDocument();
    expect(screen.getByText('账务核对')).toBeInTheDocument();
  });

  it('should call onTest when clicking test button', () => {
    const onTest = vi.fn();
    render(<ErpConfigCard config={mockConfig} status="connected" onTest={onTest} />);
    fireEvent.click(screen.getByText('检查连接'));
    expect(onTest).toHaveBeenCalledWith(1);
  });
});
