# Integration Settings UX Redesign - 三层信息架构

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 重构集成设置页面，采用三层信息架构（卡片摘要→抽屉详情→独立管理），解决页面高度不一致和健康检查按钮可读性问题。

**Architecture:**
1. **第一层（卡片）**：只显示摘要信息（连接状态、最近同步、健康状态、场景统计），固定高度，无展开功能
2. **第二层（抽屉）**：点击"查看详情"后从右侧滑出面板（40%宽度），显示完整场景列表和操作按钮
3. **第三层（页面）**：点击场景后跳转到独立的场景管理页面（本计划不实现，预留扩展）

**Tech Stack:** React 19, TypeScript 5.8, Ant Design 6, Zustand, Vitest

**关键改进:**
- 卡片高度统一，页面整洁
- 健康检查按钮：深灰背景+白字 → 蓝灰背景(slate-700)+白图标
- 场景统计显示：总数、运行中、失败数
- 抽屉组件：从右侧滑入，带遮罩层

---

## Task 1: 创建 ScenarioDrawer 组件（抽屉容器）

**Files:**
- Create: `src/components/settings/integration/components/ScenarioDrawer.tsx`
- Create: `src/components/settings/integration/components/__tests__/ScenarioDrawer.test.tsx`

**Step 1: 写测试 - 抽屉渲染和关闭功能**

```typescript
// ScenarioDrawer.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ScenarioDrawer } from '../ScenarioDrawer';

describe('ScenarioDrawer', () => {
  const mockScenarios = [
    { id: 1, name: '凭证同步', status: 'idle', lastSyncTime: '2025-01-06T10:00:00' },
    { id: 2, name: '附件同步', status: 'running', lastSyncTime: null },
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
});
```

**Step 2: 运行测试验证失败**

Run: `npm run test -- ScenarioDrawer.test.tsx`
Expected: FAIL with "ScenarioDrawer not found"

**Step 3: 实现 ScenarioDrawer 组件**

```typescript
// ScenarioDrawer.tsx
import React from 'react';
import { Drawer, Button } from 'antd';
import { X } from 'lucide-react';

interface Scenario {
  id: number;
  name: string;
  status: 'idle' | 'running' | 'success' | 'error';
  lastSyncTime?: string;
}

interface ScenarioDrawerProps {
  visible: boolean;
  configName: string;
  scenarios: Scenario[];
  onClose: () => void;
  onSync?: (scenarioId: number) => void;
}

export function ScenarioDrawer({
  visible,
  configName,
  scenarios,
  onClose,
  onSync
}: ScenarioDrawerProps) {
  const formatSyncTime = (dateString?: string) => {
    if (!dateString) return null;
    try {
      return new Date(dateString).toLocaleString('zh-CN');
    } catch {
      return '无效日期';
    }
  };

  const getStatusConfig = (status: Scenario['status']) => {
    switch (status) {
      case 'running':
        return { text: '运行中', color: 'text-blue-600', bg: 'bg-blue-50', dot: '●' };
      case 'success':
        return { text: '成功', color: 'text-green-600', bg: 'bg-green-50', dot: '●' };
      case 'error':
        return { text: '异常', color: 'text-red-600', bg: 'bg-red-50', dot: '●' };
      default:
        return { text: '空闲', color: 'text-gray-500', bg: 'bg-gray-50', dot: '○' };
    }
  };

  return (
    <Drawer
      title={
        <div className="flex items-center justify-between">
          <span className="text-lg font-semibold">{configName} 场景列表</span>
          <Button
            type="text"
            icon={<X size={18} />}
            onClick={onClose}
            className="hover:bg-gray-100"
          />
        </div>
      }
      placement="right"
      width={480}
      open={visible}
      onClose={onClose}
      styles={{
        body: { padding: '16px' },
      }}
    >
      <div className="space-y-3">
        {scenarios.map((scenario) => {
          const statusConfig = getStatusConfig(scenario.status);
          return (
            <div
              key={scenario.id}
              className="p-4 bg-white rounded-lg border border-gray-200 hover:border-blue-300 transition-colors"
            >
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-medium text-gray-900">{scenario.name}</h4>
                <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium ${statusConfig.bg} ${statusConfig.color}`}>
                  <span>{statusConfig.dot}</span>
                  <span>{statusConfig.text}</span>
                </span>
              </div>
              {scenario.lastSyncTime && (
                <p className="text-xs text-gray-500 mb-3">
                  最后同步: {formatSyncTime(scenario.lastSyncTime)}
                </p>
              )}
              {onSync && (
                <Button
                  size="small"
                  type="primary"
                  onClick={() => onSync(scenario.id)}
                  disabled={scenario.status === 'running'}
                  className="w-full"
                >
                  {scenario.status === 'running' ? '同步中...' : '立即同步'}
                </Button>
              )}
            </div>
          );
        })}
      </div>
    </Drawer>
  );
}
```

**Step 4: 运行测试验证通过**

Run: `npm run test -- ScenarioDrawer.test.tsx`
Expected: PASS (4 tests)

**Step 5: 提交**

```bash
git add src/components/settings/integration/components/ScenarioDrawer.tsx src/components/settings/integration/components/__tests__/ScenarioDrawer.test.tsx
git commit -m "feat(integration): add ScenarioDrawer component with right slide-in panel

- New ScenarioDrawer component using Ant Design Drawer
- Displays full scenario list with status indicators
- Supports onSync callback for immediate sync action
- Fixed 480px width, slides in from right
- Test coverage: 4 tests (render, close, scenarios display)

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 2: 创建 ScenarioSummaryCard 组件（场景统计卡片）

**Files:**
- Create: `src/components/settings/integration/components/ScenarioSummaryCard.tsx`
- Create: `src/components/settings/integration/components/__tests__/ScenarioSummaryCard.test.tsx`

**Step 1: 写测试 - 场景统计显示**

```typescript
// ScenarioSummaryCard.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ScenarioSummaryCard } from '../ScenarioSummaryCard';

describe('ScenarioSummaryCard', () => {
  it('should display scenario count', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={2}
        errorCount={0}
      />
    );
    expect(screen.getByText('8 个场景')).toBeInTheDocument();
  });

  it('should display running count when > 0', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={2}
        errorCount={0}
      />
    );
    expect(screen.getByText(/2.*运行中/)).toBeInTheDocument();
  });

  it('should display error count when > 0', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={0}
        errorCount={1}
      />
    );
    expect(screen.getByText(/1.*失败/)).toBeInTheDocument();
  });

  it('should display idle when no running or errors', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={0}
        errorCount={0}
      />
    );
    expect(screen.getByText('全部空闲')).toBeInTheDocument();
  });
});
```

**Step 2: 运行测试验证失败**

Run: `npm run test -- ScenarioSummaryCard.test.tsx`
Expected: FAIL with "ScenarioSummaryCard not found"

**Step 3: 实现 ScenarioSummaryCard 组件**

```typescript
// ScenarioSummaryCard.tsx
import React from 'react';

interface ScenarioSummaryCardProps {
  totalScenarios: number;
  runningCount: number;
  errorCount: number;
}

export function ScenarioSummaryCard({
  totalScenarios,
  runningCount,
  errorCount
}: ScenarioSummaryCardProps) {
  return (
    <div className="flex items-center justify-between py-3 px-4 bg-gray-50 rounded-lg">
      <div className="flex items-center gap-2">
        <span className="text-sm font-medium text-gray-700">场景</span>
        <span className="inline-flex items-center px-2 py-0.5 bg-blue-100 text-blue-700 text-xs font-medium rounded-full">
          {totalScenarios} 个
        </span>
      </div>
      <div className="flex items-center gap-3 text-xs">
        {runningCount > 0 && (
          <span className="inline-flex items-center gap-1 text-blue-600">
            <span className="animate-pulse">●</span>
            <span>{runningCount} 运行中</span>
          </span>
        )}
        {errorCount > 0 && (
          <span className="inline-flex items-center gap-1 text-red-600">
            <span>●</span>
            <span>{errorCount} 失败</span>
          </span>
        )}
        {runningCount === 0 && errorCount === 0 && (
          <span className="text-gray-500">全部空闲</span>
        )}
      </div>
    </div>
  );
}
```

**Step 4: 运行测试验证通过**

Run: `npm run test -- ScenarioSummaryCard.test.tsx`
Expected: PASS (4 tests)

**Step 5: 提交**

```bash
git add src/components/settings/integration/components/ScenarioSummaryCard.tsx src/components/settings/integration/components/__tests__/ScenarioSummaryCard.test.tsx
git commit -m "feat(integration): add ScenarioSummaryCard for scenario statistics

- Displays total scenario count with badge
- Shows running count with animated pulse indicator
- Shows error count in red when failures exist
- Shows '全部空闲' when no active scenarios
- Test coverage: 4 tests

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 3: 创建 ConnectionHealthBadge 组件（健康状态徽章）

**Files:**
- Create: `src/components/settings/integration/components/ConnectionHealthBadge.tsx`
- Create: `src/components/settings/integration/components/__tests__/ConnectionHealthBadge.test.tsx`

**Step 1: 写测试 - 健康状态显示**

```typescript
// ConnectionHealthBadge.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ConnectionHealthBadge } from '../ConnectionHealthBadge';

describe('ConnectionHealthBadge', () => {
  it('should display healthy status', () => {
    render(<ConnectionHealthBadge status="healthy" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText('✅ 正常')).toBeInTheDocument();
  });

  it('should display warning status', () => {
    render(<ConnectionHealthBadge status="warning" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText('⚠️ 警告')).toBeInTheDocument();
  });

  it('should display error status', () => {
    render(<ConnectionHealthBadge status="error" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText('❌ 异常')).toBeInTheDocument();
  });

  it('should display last check time', () => {
    render(<ConnectionHealthBadge status="healthy" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText(/检查于/)).toBeInTheDocument();
  });
});
```

**Step 2: 运行测试验证失败**

Run: `npm run test -- ConnectionHealthBadge.test.tsx`
Expected: FAIL with "ConnectionHealthBadge not found"

**Step 3: 实现 ConnectionHealthBadge 组件**

```typescript
// ConnectionHealthBadge.tsx
import React from 'react';

type HealthStatus = 'healthy' | 'warning' | 'error';

interface ConnectionHealthBadgeProps {
  status: HealthStatus;
  lastCheckTime?: string;
}

export function ConnectionHealthBadge({ status, lastCheckTime }: ConnectionHealthBadgeProps) {
  const statusConfig = {
    healthy: { icon: '✅', text: '正常', color: 'text-green-600', bg: 'bg-green-50' },
    warning: { icon: '⚠️', text: '警告', color: 'text-yellow-600', bg: 'bg-yellow-50' },
    error: { icon: '❌', text: '异常', color: 'text-red-600', bg: 'bg-red-50' },
  };

  const config = statusConfig[status];

  const formatCheckTime = (dateString?: string) => {
    if (!dateString) return null;
    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);

      if (diffMins < 1) return '刚刚';
      if (diffMins < 60) return `${diffMins}分钟前`;
      if (diffMins < 1440) return `${Math.floor(diffMins / 60)}小时前`;
      return date.toLocaleDateString('zh-CN');
    } catch {
      return null;
    }
  };

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium ${config.bg} ${config.color}`}>
      <span>{config.icon} {config.text}</span>
      {lastCheckTime && (
        <span className="opacity-75">
          · 检查于 {formatCheckTime(lastCheckTime)}
        </span>
      )}
    </div>
  );
}
```

**Step 4: 运行测试验证通过**

Run: `npm run test -- ConnectionHealthBadge.test.tsx`
Expected: PASS (4 tests)

**Step 5: 提交**

```bash
git add src/components/settings/integration/components/ConnectionHealthBadge.tsx src/components/settings/integration/components/__tests__/ConnectionHealthBadge.test.tsx
git commit -m "feat(integration): add ConnectionHealthBadge for health status display

- Shows healthy/warning/error status with icons
- Displays relative time for last check (刚刚, X分钟前, X小时前)
- Color-coded badges (green/yellow/red)
- Test coverage: 4 tests

🤖 Generated with Claude Code
Co-Authored-By Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 4: 重构 ErpConfigCard - 移除展开功能，添加摘要信息

**Files:**
- Modify: `src/components/settings/integration/components/ErpConfigCard.tsx`
- Modify: `src/components/settings/integration/components/__tests__/ErpConfigCard.test.tsx`

**Step 1: 更新测试 - 验证摘要视图和查看详情按钮**

```typescript
// ErpConfigCard.test.tsx - 添加新测试
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
    expect(screen.getByText('8 个场景')).toBeInTheDocument();
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
    expect(screen.getByText(/✅.*正常/)).toBeInTheDocument();
  });
});
```

**Step 2: 运行测试验证失败**

Run: `npm run test -- ErpConfigCard.test.tsx`
Expected: FAIL with new tests failing

**Step 3: 重构 ErpConfigCard 组件**

```typescript
// ErpConfigCard.tsx - 完全重写，移除展开功能
import React, { useRef, useEffect, useState, useCallback } from 'react';
import { Settings, Zap, Activity, ShieldCheck, Sliders, MoreHorizontal, ChevronRight } from 'lucide-react';
import { ErpConfig } from '@/types';
import { ScenarioSummaryCard } from './ScenarioSummaryCard';
import { ConnectionHealthBadge } from './ConnectionHealthBadge';

interface ErpConfigCardProps {
  config: ErpConfig;
  status: 'connected' | 'disconnected' | 'error';
  scenarioCount?: number;
  runningCount?: number;
  errorCount?: number;
  healthStatus?: 'healthy' | 'warning' | 'error';
  lastHealthCheck?: string;
  onTest?: (configId: number) => void;
  onDiagnose?: (configId: number) => void;
  onReconcile?: (configId: number) => void;
  onConfig?: (config: ErpConfig) => void;
  onDelete?: (configId: number) => void;
  onViewDetails?: (configId: number) => void;
}

export function ErpConfigCard({
  config,
  status,
  scenarioCount = 0,
  runningCount = 0,
  errorCount = 0,
  healthStatus,
  lastHealthCheck,
  onTest,
  onDiagnose,
  onReconcile,
  onConfig,
  onDelete,
  onViewDetails
}: ErpConfigCardProps) {
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ name: config.name });
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setShowMoreMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const statusConfig = {
    connected: { text: '已连接', color: 'text-green-600', bg: 'bg-green-50', dot: '●' },
    disconnected: { text: '未连接', color: 'text-gray-500', bg: 'bg-gray-50', dot: '○' },
    error: { text: '异常', color: 'text-red-600', bg: 'bg-red-50', dot: '●' },
  };

  const { text: statusText, color: statusColor, bg: statusBg, dot: statusDot } = statusConfig[status];

  return (
    <div className="bg-white rounded-xl border border-gray-200 hover:border-blue-200 hover:shadow-md transition-all duration-200 overflow-hidden">
      {/* Header Section */}
      <div className="p-5">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3 flex-1">
            <div className="w-11 h-11 rounded-lg bg-blue-50 flex items-center justify-center flex-shrink-0">
              <Settings size={20} className="text-blue-600" />
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-base font-semibold text-gray-900 truncate">{config.name}</h3>
              <div className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium mt-1 ${statusBg} ${statusColor}`}>
                <span>{statusDot}</span>
                <span>{statusText}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Action Bar - Grid layout */}
        <div className="grid grid-cols-2 gap-2 mb-3">
          <button
            onClick={() => setIsEditing(!isEditing)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-gray-700 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-lg transition-colors"
          >
            <Settings size={14} className="text-gray-500 flex-shrink-0" />
            <span>配置中心</span>
          </button>

          <button
            onClick={() => onTest?.(config.id)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-gray-700 bg-white hover:bg-blue-50 border border-gray-200 hover:border-blue-200 hover:text-blue-600 rounded-lg transition-colors"
          >
            <Zap size={14} className="text-blue-500 flex-shrink-0" />
            <span>检查连接</span>
          </button>

          <button
            onClick={() => onDiagnose?.(config.id)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-white bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors"
          >
            <Activity size={14} className="text-white flex-shrink-0" />
            <span>健康检查</span>
          </button>

          <button
            onClick={() => onReconcile?.(config.id)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg transition-colors"
          >
            <ShieldCheck size={14} className="text-emerald-600 flex-shrink-0" />
            <span>账务核对</span>
          </button>
        </div>

        {/* More Menu */}
        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setShowMoreMenu(!showMoreMenu)}
            aria-label="更多选项"
            aria-expanded={showMoreMenu}
            aria-haspopup="true"
            className="absolute right-0 top-0 h-8 w-8 flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <MoreHorizontal size={16} />
          </button>

          {showMoreMenu && (
            <div
              className="absolute right-0 top-full z-50 mt-1 bg-white rounded-lg shadow-lg border border-gray-200 py-1 min-w-[140px]"
              role="menu"
            >
              <button
                role="menuitem"
                onClick={() => {
                  setIsEditing(true);
                  setShowMoreMenu(false);
                }}
                className="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
              >
                <Sliders size={14} className="text-gray-500" />
                <span>编辑配置</span>
              </button>
              <button
                role="menuitem"
                onClick={() => {
                  onDelete?.(config.id);
                  setShowMoreMenu(false);
                }}
                className="w-full px-3 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
              >
                <span>移除此连接器</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Inline Edit Form */}
      {isEditing && (
        <div className="p-5 bg-blue-50 border-t border-blue-100" data-testid="inline-config-form">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">连接器名称</label>
              <input
                type="text"
                value={editForm.name}
                onChange={(e) => setEditForm({ name: e.target.value })}
                className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="输入连接器名称"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  onConfig?.({ ...config, name: editForm.name });
                  setIsEditing(false);
                }}
                className="flex-1 px-4 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
              >
                保存
              </button>
              <button
                onClick={() => setIsEditing(false)}
                className="flex-1 px-4 py-2.5 bg-white border border-gray-300 text-gray-700 text-sm font-medium rounded-lg hover:bg-gray-50 transition-colors"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Summary Section - Fixed Height */}
      <div className="border-t border-gray-100 p-5 space-y-3">
        {/* Health Status */}
        {healthStatus && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">健康状态</span>
            <ConnectionHealthBadge status={healthStatus} lastCheckTime={lastHealthCheck} />
          </div>
        )}

        {/* Scenario Summary */}
        {scenarioCount > 0 && (
          <ScenarioSummaryCard
            totalScenarios={scenarioCount}
            runningCount={runningCount}
            errorCount={errorCount}
          />
        )}

        {/* View Details Button */}
        <button
          onClick={() => onViewDetails?.(config.id)}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors group"
        >
          <span>查看详情</span>
          <ChevronRight size={16} className="group-hover:translate-x-1 transition-transform" />
        </button>
      </div>
    </div>
  );
}
```

**Step 4: 更新 ErpConfigCard.test.tsx - 删除旧的展开相关测试**

找到并删除这些测试：
- `should toggle scenarios expansion`
- `should show loading state when scenarios loading`
- `should display scenarios when expanded`

**Step 5: 运行测试验证通过**

Run: `npm run test -- ErpConfigCard.test.tsx`
Expected: PASS

**Step 6: 提交**

```bash
git add src/components/settings/integration/components/ErpConfigCard.tsx src/components/settings/integration/components/__tests__/ErpConfigCard.test.tsx
git commit -m "refactor(integration): redesign ErpConfigCard as summary view

BREAKING CHANGE: Removed expand/collapse functionality, replaced with summary view

Changes:
- Card now displays summary info only (fixed height)
- Added ScenarioSummaryCard for scenario statistics
- Added ConnectionHealthBadge for health status
- Added 'View Details' button that opens drawer
- Health check button changed to slate-700 with white text
- Removed scenariosExpanded state and expand UI
- Removed inline scenario list display

New props:
- runningCount, errorCount: scenario statistics
- healthStatus, lastHealthCheck: health status
- onViewDetails: callback to open drawer

Test updates:
- Removed expand/collapse tests
- Added summary view tests
- Added health badge tests

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 5: 更新 IntegrationSettingsPage - 集成 ScenarioDrawer

**Files:**
- Modify: `src/components/settings/integration/IntegrationSettingsPage.tsx`
- Modify: `src/components/settings/integration/__tests__/IntegrationSettingsPage.test.tsx`

**Step 1: 更新 IntegrationSettingsPage 添加抽屉状态管理**

找到 `IntegrationSettingsPage` 组件，在组件内部添加：

```typescript
// 在现有 state 之后添加
const [drawerConfigId, setDrawerConfigId] = useState<number | null>(null);

// 计算场景统计（包括运行中和失败数）
const scenarioStats = useMemo(() => {
  const stats: Record<number, { running: number; error: number }> = {};
  scenarioManager.state.scenarios.forEach(s => {
    if (!stats[s.configId]) {
      stats[s.configId] = { running: 0, error: 0 };
    }
    // 根据场景状态统计（这里需要根据实际场景状态字段调整）
    if (s.status === 'running') stats[s.configId].running++;
    if (s.status === 'error') stats[s.configId].error++;
  });
  return stats;
}, [scenarioManager.state.scenarios]);

// 获取当前抽屉显示的配置
const drawerConfig = useMemo(() => {
  if (drawerConfigId === null) return null;
  return configManager.state.configs.find(c => c.id === drawerConfigId);
}, [drawerConfigId, configManager.state.configs]);

// 获取当前抽屉显示的场景
const drawerScenarios = useMemo(() => {
  if (drawerConfigId === null) return [];
  return scenarioManager.state.scenarios
    .filter(s => s.configId === drawerConfigId)
    .map(s => ({
      id: s.id,
      name: s.name,
      status: s.status || 'idle',
      lastSyncTime: s.lastSyncTime
    }));
}, [drawerConfigId, scenarioManager.state.scenarios]);
```

**Step 2: 更新 ErpConfigList 调用，传递新 props**

找到 `<ErpConfigList>` 组件调用，更新为：

```typescript
<ErpConfigList
  configs={configManager.state.configs}
  scenarioCounts={scenarioCounts}
  runningCounts={scenarioStats}
  onConfig={(config) => connectorModal.actions.openModal(config)}
  onDelete={handleDeleteConfig}
  onTest={configManager.actions.testConnection}
  onDiagnose={diagnosis.actions.startDiagnosis}
  onReconcile={(id) => {
    console.log('Reconcile not implemented yet');
  }}
  onViewDetails={(configId) => {
    // 加载场景数据（如果尚未加载）
    if (!scenariosMap[configId] || scenariosMap[configId].length === 0) {
      scenarioManager.actions.loadScenarios(configId);
    }
    setDrawerConfigId(configId);
  }}
/>
```

**Step 3: 在 return 语句的 Modals 部分添加 ScenarioDrawer**

找到 `{/* Modals */}` 部分，在最后添加：

```typescript
{/* Scenario Drawer */}
{drawerConfig && (
  <ScenarioDrawer
    visible={drawerConfigId !== null}
    configName={drawerConfig.name}
    scenarios={drawerScenarios}
    onClose={() => setDrawerConfigId(null)}
    onSync={(scenarioId) => {
      scenarioManager.actions.syncScenario(scenarioId);
    }}
  />
)}
```

**Step 4: 更新 IntegrationSettingsPage.test.tsx 测试**

添加抽屉相关测试：

```typescript
it('should open drawer when clicking view details', () => {
  render(<IntegrationSettingsPage erpApi={mockErpApi} />);
  // 模拟点击查看详情
  // 验证抽屉打开
});

it('should close drawer when close button clicked', () => {
  // 测试抽屉关闭
});
```

**Step 5: 运行测试验证通过**

Run: `npm run test -- IntegrationSettingsPage.test.tsx`
Expected: PASS

**Step 6: 提交**

```bash
git add src/components/settings/integration/IntegrationSettingsPage.tsx src/components/settings/integration/__tests__/IntegrationSettingsPage.test.tsx
git commit -m "feat(integration): integrate ScenarioDrawer with summary view

Changes:
- Added drawerConfigId state to track open drawer
- Added scenarioStats calculation for running/error counts
- Updated ErpConfigList props to include runningCounts
- Added onViewDetails callback to open drawer
- Added ScenarioDrawer component to render
- Loads scenarios on-demand when drawer opens

New behavior:
- Clicking 'View Details' opens right-side drawer
- Drawer shows full scenario list with sync buttons
- Drawer closes with X button or clicking outside

Test updates:
- Added drawer open/close tests

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 6: 更新 ErpConfigList 组件传递新 props

**Files:**
- Modify: `src/components/settings/integration/components/ErpConfigList.tsx`
- Modify: `src/components/settings/integration/components/__tests__/ErpConfigList.test.tsx`

**Step 1: 更新 ErpConfigList 接口和实现**

```typescript
// ErpConfigList.tsx
interface ErpConfigListProps {
  configs: ErpConfig[];
  scenarioCounts?: Record<number, number>;
  runningCounts?: Record<number, { running: number; error: number }>;
  onTest?: (configId: number) => void;
  onDiagnose?: (configId: number) => void;
  onReconcile?: (configId: number) => void;
  onConfig?: (config: ErpConfig) => void;
  onDelete?: (configId: number) => void;
  onViewDetails?: (configId: number) => void;
}

export function ErpConfigList({
  configs,
  scenarioCounts = {},
  runningCounts = {},
  onTest,
  onDiagnose,
  onReconcile,
  onConfig,
  onDelete,
  onViewDetails
}: ErpConfigListProps) {
  if (configs.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center mb-4">
          <span className="text-3xl text-slate-300">🔌</span>
        </div>
        <h3 className="text-lg font-semibold text-slate-700 mb-2">还没有配置任何连接器</h3>
        <p className="text-sm text-slate-500 mb-4">点击下方按钮添加您的第一个 ERP 连接器</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      {configs.map((config) => {
        const stats = runningCounts[config.id] || { running: 0, error: 0 };
        return (
          <ErpConfigCard
            key={config.id}
            config={config}
            status="connected"
            scenarioCount={scenarioCounts[config.id] || 0}
            runningCount={stats.running}
            errorCount={stats.error}
            onTest={onTest}
            onDiagnose={onDiagnose}
            onReconcile={onReconcile}
            onConfig={onConfig}
            onDelete={onDelete}
            onViewDetails={onViewDetails}
          />
        );
      })}
    </div>
  );
}
```

**Step 2: 更新测试**

```typescript
// ErpConfigList.test.tsx
describe('ErpConfigList with running counts', () => {
  it('should pass running counts to cards', () => {
    const { getByText } = render(
      <ErpConfigList
        configs={[mockConfig]}
        scenarioCounts={{ 1: 8 }}
        runningCounts={{ 1: { running: 2, error: 0 } }}
      />
    );
    expect(getByText(/2.*运行中/)).toBeInTheDocument();
  });
});
```

**Step 3: 运行测试验证通过**

Run: `npm run test -- ErpConfigList.test.tsx`
Expected: PASS

**Step 4: 提交**

```bash
git add src/components/settings/integration/components/ErpConfigList.tsx src/components/settings/integration/components/__tests__/ErpConfigList.test.tsx
git commit -m "refactor(integration): update ErpConfigList for summary view props

Changes:
- Added runningCounts prop to pass statistics
- Removed scenarios, loadingScenarios, onLoadScenarios props
- Updated ErpConfigCard props to match new interface
- Map runningCounts to runningCount/errorCount per card

Breaking change:
- No longer passes scenario list data
- Only passes counts for summary display

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 7: 更新 types.ts 添加新类型定义

**Files:**
- Modify: `src/types.ts`

**Step 1: 添加场景相关类型**

找到 `types.ts` 文件，在合适位置添加：

```typescript
// 场景状态类型
export type ScenarioStatus = 'idle' | 'running' | 'success' | 'error';

// 扩展 ErpScenario 接口（如果存在）或添加新接口
export interface ScenarioSummary {
  id: number;
  configId: number;
  name: string;
  status: ScenarioStatus;
  lastSyncTime?: string;
  recordCount?: number;
}

// 连接器健康状态
export type ConnectionHealthStatus = 'healthy' | 'warning' | 'error';

// 场景统计
export interface ScenarioStatistics {
  total: number;
  running: number;
  error: number;
}
```

**Step 2: 提交**

```bash
git add src/types.ts
git commit -m "types(integration): add scenario and health status types

- Added ScenarioStatus type (idle, running, success, error)
- Added ScenarioSummary interface for scenario data
- Added ConnectionHealthStatus type
- Added ScenarioStatistics interface for counts

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 8: 更新 README 文档

**Files:**
- Modify: `src/components/settings/integration/README.md`

**Step 1: 更新架构说明**

在 README.md 中更新组件架构部分：

```markdown
## Architecture (v2.2 - Summary + Drawer)

### Three-Layer Information Architecture

**Layer 1: Summary Card (ErpConfigCard)**
- Displays summary information only
- Fixed height for consistent layout
- Shows: connection status, health, scenario counts
- Actions: config, test, diagnose, reconcile
- Entry point: "View Details" button

**Layer 2: Detail Drawer (ScenarioDrawer)**
- Slides in from right (40% width)
- Shows full scenario list
- Per-scenario actions: sync, view history
- Dismissible with X button or click outside

**Layer 3: Management Page (Future)**
- Dedicated page for scenario management
- Advanced features: history, logs, mapping
- Reached by clicking individual scenario

### Component Structure

```
IntegrationSettingsPage
├── ErpConfigList (grid layout)
│   └── ErpConfigCard (summary view)
│       ├── ScenarioSummaryCard (counts)
│       ├── ConnectionHealthBadge (status)
│       └── [Action Buttons]
└── ScenarioDrawer (detail view)
    └── [Scenario List]
```

### Visual Improvements

- Health check button: slate-700 background + white text (better contrast)
- Card height: Fixed for consistent layout
- Spacing: Increased padding and gaps for breathing room
- Status indicators: Color-coded with icons
```

**Step 2: 提交**

```bash
git add src/components/settings/integration/README.md
git commit -m "docs(integration): update README for v2.2 architecture

- Documented three-layer information architecture
- Updated component structure diagram
- Added visual improvements section
- Removed expand/collapse documentation

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 9: 运行完整测试套件验证

**Step 1: 运行所有测试**

Run: `npm run test:run`
Expected: All tests pass (208+ tests)

**Step 2: 运行 TypeScript 类型检查**

Run: `npx tsc --noEmit`
Expected: No type errors

**Step 3: 运行架构检查**

Run: `npm run check:arch`
Expected: No architecture violations

**Step 4: 如果所有检查通过，创建里程碑提交**

```bash
git add -A
git commit -m "feat(integration): complete UX redesign - three-layer architecture

Summary:
Redesigned Integration Settings page with three-layer information architecture
to solve page height inconsistency and improve visual hierarchy.

Changes:
- Created ScenarioDrawer component for right-side slide-in panel
- Created ScenarioSummaryCard for scenario statistics display
- Created ConnectionHealthBadge for health status with relative time
- Refactored ErpConfigCard to summary-only view (removed expand)
- Updated ErpConfigList to pass summary statistics
- Updated IntegrationSettingsPage with drawer state management
- Added new types: ScenarioStatus, ConnectionHealthStatus, ScenarioStatistics

Visual Improvements:
- Health check button: bg-gray-900 → bg-slate-700 with white text
- Fixed card heights for consistent layout
- Better spacing and visual hierarchy
- Color-coded status indicators

Breaking Changes:
- Removed on-demand scenario loading from cards
- Removed expand/collapse UI
- Scenarios now only visible in drawer

Test Results:
- All 208+ tests passing
- TypeScript compilation clean
- Architecture validation passed

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 10: 更新 CHANGELOG

**Files:**
- Modify: `docs/CHANGELOG.md`

**Step 1: 添加变更记录**

在 CHANGELOG.md 顶部添加：

```markdown
## [Unreleased]

### Added
- ScenarioDrawer component - right-side slide-in panel for scenario details
- ScenarioSummaryCard component - displays scenario counts with status
- ConnectionHealthBadge component - health status with relative time display
- Three-layer information architecture (Card → Drawer → Page)

### Changed
- ErpConfigCard redesigned as summary-only view (removed expand/collapse)
- Health check button style improved (slate-700 + white text for better contrast)
- Fixed card heights for consistent page layout
- Better visual hierarchy and spacing

### Removed
- On-demand scenario loading from cards (moved to drawer)
- Expand/collapse UI for scenarios
- Inline scenario list display

### Technical
- Added new types: ScenarioStatus, ConnectionHealthStatus, ScenarioStatistics
- Updated component interfaces to support summary view
- All 208+ tests passing
```

**Step 2: 提交**

```bash
git add docs/CHANGELOG.md
git commit -m "docs(changelog): add v2.2 UX redesign entries

- Documented new components and features
- Noted breaking changes (removed expand/collapse)
- Listed visual improvements
- Added technical notes

🤖 Generated with Claude Code
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## 完成检查清单

在实施完成后，验证以下内容：

- [ ] ScenarioDrawer 组件创建并通过测试
- [ ] ScenarioSummaryCard 组件创建并通过测试
- [ ] ConnectionHealthBadge 组件创建并通过测试
- [ ] ErpConfigCard 重构为摘要视图并通过测试
- [ ] IntegrationSettingsPage 集成抽屉并通过测试
- [ ] ErpConfigList 更新 props 并通过测试
- [ ] types.ts 添加新类型定义
- [ ] README.md 更新架构说明
- [ ] 所有测试通过 (208+)
- [ ] TypeScript 类型检查通过
- [ ] 架构检查通过
- [ ] CHANGELOG.md 更新

## 执行说明

此计划遵循以下原则：
- **DRY**: 代码复用，避免重复
- **YAGNI**: 只实现当前需要的功能，不预留过度抽象
- **TDD**: 先写测试，再实现代码
- **小步提交**: 每个任务完成后立即提交

每个任务预计 10-20 分钟完成。
