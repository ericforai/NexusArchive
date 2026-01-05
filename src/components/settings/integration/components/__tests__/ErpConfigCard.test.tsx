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

  it('should call onDiagnose when clicking health check button', () => {
    const onDiagnose = vi.fn();
    render(<ErpConfigCard config={mockConfig} status="connected" onDiagnose={onDiagnose} />);
    fireEvent.click(screen.getByText('健康检查'));
    expect(onDiagnose).toHaveBeenCalledWith(1);
  });

  it('should call onReconcile when clicking reconcile button', () => {
    const onReconcile = vi.fn();
    render(<ErpConfigCard config={mockConfig} status="connected" onReconcile={onReconcile} />);
    fireEvent.click(screen.getByText('账务核对'));
    expect(onReconcile).toHaveBeenCalledWith(1);
  });

  it('should call onConfig when clicking config button', () => {
    const onConfig = vi.fn();
    render(<ErpConfigCard config={mockConfig} status="connected" onConfig={onConfig} />);
    fireEvent.click(screen.getByText('配置中心'));
    expect(onConfig).toHaveBeenCalledWith(mockConfig);
  });

  it('should call onDelete when clicking delete in more menu', () => {
    const onDelete = vi.fn();
    render(<ErpConfigCard config={mockConfig} status="connected" onDelete={onDelete} />);
    const buttons = screen.getAllByRole('button');
    const moreButton = buttons.find(btn => btn.className.includes('w-8'));
    fireEvent.click(moreButton!);
    fireEvent.click(screen.getByText('移除此连接器'));
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it('should render scenarios list', () => {
    const scenarios = [
      { id: 1, name: '凭证同步', lastSyncTime: '2026-01-01T00:00:00Z', recordCount: 100 }
    ];
    render(<ErpConfigCard config={mockConfig} status="connected" scenarios={scenarios} />);
    expect(screen.getByText('凭证同步')).toBeInTheDocument();
    expect(screen.getByText(/已同步 100 条/)).toBeInTheDocument();
  });
});
