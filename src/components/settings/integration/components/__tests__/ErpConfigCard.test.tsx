import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ErpConfigCard } from '../ErpConfigCard';
import { ErpConfig } from '@/types';

describe('ErpConfigCard', () => {
  const mockConfig: ErpConfig = {
    id: 1,
    name: 'YonSuite',
    erpType: 'yonsuite',
    configJson: '{}',
    isActive: 1,
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

  it('should call onConfig when clicking config center button', () => {
    const mockOnConfig = vi.fn();
    const { getByRole } = render(
      <ErpConfigCard
        config={mockConfig}
        status="connected"
        onConfig={mockOnConfig}
      />
    );

    const configButton = getByRole('button', { name: /配置中心/ });
    fireEvent.click(configButton);

    expect(mockOnConfig).toHaveBeenCalledTimes(1);
    expect(mockOnConfig).toHaveBeenCalledWith(mockConfig);
  });

  describe('ErpConfigCard - Summary View', () => {
    it('should show scenario summary instead of expand button', () => {
      render(
        <ErpConfigCard
          config={mockConfig}
          status="connected"
          scenarioCount={8}
          runningCount={2}
          errorCount={0}
          onViewDetails={() => {}}
        />
      );
      // New compact inline format after Task 5: "场景: 8 / 运行2 / 错误0"
      expect(screen.getByText(/场景.*8.*运行2.*错误0/)).toBeInTheDocument();
      expect(screen.getByText('查看详情')).toBeInTheDocument();
      expect(screen.queryByText(/点击展开/)).toBeNull();
    });

    it('should call onViewDetails when details button clicked', () => {
      const onViewDetails = vi.fn();
      render(
        <ErpConfigCard
          config={mockConfig}
          status="connected"
          scenarioCount={8}
          onViewDetails={onViewDetails}
        />
      );
      fireEvent.click(screen.getByText('查看详情'));
      expect(onViewDetails).toHaveBeenCalledWith(1);
    });

    it('should display health badge when provided', () => {
      render(
        <ErpConfigCard
          config={mockConfig}
          status="connected"
          scenarioCount={8}
          healthStatus="healthy"
          lastHealthCheck="2025-01-06T10:00:00"
        />
      );
      expect(screen.getByText(/正常/)).toBeInTheDocument();
    });
  });
});
