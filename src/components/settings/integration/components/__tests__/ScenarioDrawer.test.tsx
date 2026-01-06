import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ScenarioDrawer } from '../ScenarioDrawer';
import { Scenario } from '@/types';

describe('ScenarioDrawer', () => {
  const mockScenarios: Scenario[] = [
    { id: 1, name: '凭证同步', status: 'idle', lastSyncTime: '2025-01-06T10:00:00' },
    { id: 2, name: '附件同步', status: 'running' },
  ];

  it('should not render when visible is false', () => {
    const { container } = render(
      <ScenarioDrawer
        visible={false}
        configName="测试连接器"
        scenarios={mockScenarios}
        onClose={() => {}}
      />
    );
    expect(container.querySelector('.ant-drawer-open')).toBeNull();
  });

  it('should render config name when visible', () => {
    render(
      <ScenarioDrawer
        visible={true}
        configName="用友 YonSuite"
        scenarios={mockScenarios}
        onClose={() => {}}
      />
    );
    expect(screen.getByText('用友 YonSuite 场景列表')).toBeInTheDocument();
  });

  it('should call onClose when close button clicked', () => {
    const onClose = vi.fn();
    render(
      <ScenarioDrawer
        visible={true}
        configName="测试"
        scenarios={mockScenarios}
        onClose={onClose}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /close/i }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should render all scenarios', () => {
    render(
      <ScenarioDrawer
        visible={true}
        configName="测试"
        scenarios={mockScenarios}
        onClose={() => {}}
      />
    );
    expect(screen.getByText('凭证同步')).toBeInTheDocument();
    expect(screen.getByText('附件同步')).toBeInTheDocument();
  });

  it('should call onSync with scenarioId when sync button clicked', () => {
    const onSync = vi.fn();
    render(
      <ScenarioDrawer
        visible={true}
        configName="测试"
        scenarios={mockScenarios}
        onClose={() => {}}
        onSync={onSync}
      />
    );

    const syncButtons = screen.getAllByText('立即同步');
    fireEvent.click(syncButtons[0]);

    expect(onSync).toHaveBeenCalledTimes(1);
    expect(onSync).toHaveBeenCalledWith(1);
  });

  it('should render status badges for different states', () => {
    const statusScenarios = [
      { id: 1, name: '空闲场景', status: 'idle' as const },
      { id: 2, name: '运行场景', status: 'running' as const },
      { id: 3, name: '成功场景', status: 'success' as const },
      { id: 4, name: '异常场景', status: 'error' as const },
    ];

    render(
      <ScenarioDrawer
        visible={true}
        configName="测试"
        scenarios={statusScenarios}
        onClose={() => {}}
      />
    );

    expect(screen.getByText('空闲')).toBeInTheDocument();
    expect(screen.getByText('运行中')).toBeInTheDocument();
    expect(screen.getByText('成功')).toBeInTheDocument();
    expect(screen.getByText('异常')).toBeInTheDocument();
  });

  it('should disable sync button when status is running', () => {
    const onSync = vi.fn();
    render(
      <ScenarioDrawer
        visible={true}
        configName="测试"
        scenarios={mockScenarios}
        onClose={() => {}}
        onSync={onSync}
      />
    );

    const runningSyncButton = screen.getByText('同步中...');
    expect(runningSyncButton).toBeInTheDocument();

    const idleSyncButton = screen.getByText('立即同步');
    expect(idleSyncButton).toBeInTheDocument();
  });
});
