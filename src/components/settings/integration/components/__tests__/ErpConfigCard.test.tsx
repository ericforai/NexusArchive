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

  it('should toggle inline form when clicking config button', () => {
    const onConfig = vi.fn();
    render(<ErpConfigCard config={mockConfig} status="connected" onConfig={onConfig} />);
    // First click should open the inline form, not call onConfig
    fireEvent.click(screen.getByText('配置中心'));
    // onConfig should not be called yet
    expect(onConfig).not.toHaveBeenCalled();
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

  it('should toggle inline edit form when clicking config button', () => {
    const { container } = render(<ErpConfigCard config={mockConfig} status="connected" />);
    const configButton = screen.getByText('配置中心');

    // Initially, inline form should not be visible
    expect(container.querySelector('[data-testid="inline-config-form"]')).not.toBeInTheDocument();

    // Click config button to show inline form
    fireEvent.click(configButton);
    expect(container.querySelector('[data-testid="inline-config-form"]')).toBeInTheDocument();

    // Click again to hide
    fireEvent.click(configButton);
    expect(container.querySelector('[data-testid="inline-config-form"]')).not.toBeInTheDocument();
  });

  it('should show inline edit form with name input', () => {
    const { container } = render(<ErpConfigCard config={mockConfig} status="connected" />);
    fireEvent.click(screen.getByText('配置中心'));

    const inlineForm = container.querySelector('[data-testid="inline-config-form"]');
    expect(inlineForm).toBeInTheDocument();
    expect(screen.getByText('连接器名称')).toBeInTheDocument();
    // Service address field has been removed from the inline form
    expect(screen.getByText('保存')).toBeInTheDocument();
    expect(screen.getByText('取消')).toBeInTheDocument();
  });

  it('should call onConfig with updated name when saving inline form', () => {
    const onConfig = vi.fn();
    const { container } = render(
      <ErpConfigCard config={mockConfig} status="connected" onConfig={onConfig} />
    );

    // Open inline form
    fireEvent.click(screen.getByText('配置中心'));

    // Change name input
    const nameInput = container.querySelector('input[type="text"]') as HTMLInputElement;
    fireEvent.change(nameInput, { target: { value: 'Updated YonSuite' } });
    expect(nameInput.value).toBe('Updated YonSuite');

    // Click save
    fireEvent.click(screen.getByText('保存'));

    // Should call onConfig with updated name and close form
    expect(onConfig).toHaveBeenCalledWith({ ...mockConfig, name: 'Updated YonSuite' });
    expect(container.querySelector('[data-testid="inline-config-form"]')).not.toBeInTheDocument();
  });

  it('should close inline form when clicking cancel', () => {
    const { container } = render(<ErpConfigCard config={mockConfig} status="connected" />);

    // Open inline form
    fireEvent.click(screen.getByText('配置中心'));
    expect(container.querySelector('[data-testid="inline-config-form"]')).toBeInTheDocument();

    // Click cancel
    fireEvent.click(screen.getByText('取消'));
    expect(container.querySelector('[data-testid="inline-config-form"]')).not.toBeInTheDocument();
  });
});
