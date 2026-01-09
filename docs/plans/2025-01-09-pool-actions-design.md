# 电子凭证池操作流程重构设计

**日期**: 2025-01-09
**状态**: 设计中
**作者**: Claude (AI Assistant)

## 1. 背景与问题

### 1.1 当前问题

电子凭证池 (`/system/pre-archive/pool`) 页面当前操作设计存在以下问题：

1. **操作不明确** - 页面仅有"提交归档"和"批量删除"两个按钮，用户不清楚文件需要经过什么流程才能提交

2. **状态盲区** - 文件有10种状态（DRAFT、PENDING_CHECK、CHECK_FAILED等），但用户不知道下一步该做什么

3. **流程割裂** - 检测、补录、匹配、归档等操作分散在不同位置，需要用户自己探索

4. **反馈缺失** - 执行操作后不知道进度和结果

### 1.2 当前状态分布

根据数据库查询，当前文件状态分布：

| 状态 | 数量 | 说明 |
|------|------|------|
| ARCHIVED | 114 | 已归档 |
| CHECK_FAILED | 3 | 检测失败 |
| PENDING_CHECK | 11 | 待检测 |
| PENDING_METADATA | 4 | 待补录 |
| PENDING_ARCHIVE | 0 | 准备归档 |

**关键发现**：没有处于"准备归档"状态的文件，因此无法提交归档申请。用户需要先执行检测、补录等操作。

### 1.3 设计目标

1. **清晰的操作引导** - 用户一眼就能看出当前文件支持哪些操作
2. **状态驱动交互** - 操作按钮根据文件状态动态显示/隐藏
3. **统一的操作模式** - 复用核心能力，保持不同页面的交互一致性
4. **完善的反馈机制** - 操作前确认、操作中进度、操作后结果

## 2. 核心架构设计

### 2.1 设计原则

经过头脑风暴讨论，确定以下原则：

1. **能力复用，视图自由** - 核心逻辑（hooks、工具函数）复用，UI 实现各页面自主
2. **页面控制状态** - 选择状态由页面管理，组件只消费数据
3. **声明式配置** - 操作通过配置定义，而非硬编码
4. **渐进式增强** - 基础功能先实现，复杂功能按需添加

### 2.2 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                    页面组件层                             │
│  ArchiveListView、ApprovalView 等                       │
│  • 管理选择状态 (selectedIds)                           │
│  • 管理业务状态 (loading, error)                        │
│  • 自由决定布局和交互                                    │
├─────────────────────────────────────────────────────────┤
│                   复用能力层 (hooks)                      │
│  • useBatchActions - 计算可用操作                       │
│  • useBatchExecutor - 执行批量操作                      │
│  • useActionResult - 处理操作结果                       │
├─────────────────────────────────────────────────────────┤
│                   配置层 (definitions)                   │
│  • poolActionConfigs - 凭证池操作定义                   │
│  • approvalActionConfigs - 审批操作定义                 │
│  • ActionConfig 类型定义                                │
├─────────────────────────────────────────────────────────┤
│                   UI 组件层 (components)                 │
│  • ActionPanel - 操作面板组件                           │
│  • ActionButton - 操作按钮组件                          │
│  • BatchResultModal - 结果弹窗                          │
│  • ConfirmDialog - 确认弹窗                            │
└─────────────────────────────────────────────────────────┘
```

### 2.3 核心类型定义

```typescript
// 动作配置接口
interface ActionConfig<T = any> {
  key: string;                    // 唯一标识
  label: string;                  // 显示文本
  icon: ComponentType;            // 图标
  description?: string;           // 提示文本

  // 可用性判断
  isAvailable: (items: T[]) => boolean;

  // 执行逻辑
  handler: (items: T[]) => Promise<BatchResult>;

  // UI 配置
  variant?: 'primary' | 'danger' | 'default';
  size?: 'small' | 'medium' | 'large';

  // 确认配置
  confirm?: {
    title: (count: number) => string;
    description?: (count: number) => string;
    confirmText?: string;
    cancelText?: string;
  };

  // 执行配置
  execution?: {
    mode: 'parallel' | 'sequential';  // 并行/串行
    progress?: boolean;               // 是否显示进度
  };
}
```

## 3. 页面布局设计

### 3.1 电子凭证池页面布局

采用**左右分栏布局**，左侧文件列表，右侧动态操作面板：

```
┌─────────────────────────────────────────────────────────────────────────┐
│  电子凭证池                                    [状态筛选 ▼] [搜索框]       │
├───────────────────────────────────────┬─────────────────────────────────┤
│  文件列表                              │  操作面板                        │
│  ┌─────────────────────────────────┐  │  ┌───────────────────────────┐  │
│  │ ☐ INV-001.pdf    待检测        │  │  │ 已选择 3 个文件             │  │
│  │ ☐ INV-002.pdf    检测失败      │  │  │                           │  │
│  │ ☑ INV-003.pdf    待补录        │◄─┼──┤  待处理状态分布：           │  │
│  │ ☑ INV-004.pdf    待补录        │  │  │  • 待检测: 2              │  │
│  │ ☑ INV-005.pdf    准备归档      │  │  │  • 待补录: 3              │  │
│  │    ...                        │  │  │  • 准备归档: 1            │  │
│  └─────────────────────────────────┘  │  │                           │  │
│                                       │  │  可执行操作：               │  │
│                                       │  │  ┌─────────────────────┐  │  │
│                                       │  │  │ 🔍 四性检测 (2)     │  │  │
│                                       │  │  │ ✏️ 补录元数据 (3)   │  │  │
│                                       │  │  │ 📎 智能匹配 (1)     │  │  │
│                                       │  │  │ ✓ 提交归档 (1)      │  │  │
│                                       │  │  │ 🗑️ 删除 (5)         │  │  │
│                                       │  │  └─────────────────────┘  │  │
│                                       │  └───────────────────────────┘  │
└───────────────────────────────────────┴─────────────────────────────────┘
```

**布局特点**：
- 左侧：文件列表，支持勾选，点击高亮
- 右侧：操作面板，显示选中数量、状态分布、可用操作
- 操作面板根据选中文件的状态动态更新
- 操作按钮后的数字表示可执行该操作的文件数量

### 3.2 档案审批页面布局

采用**传统表格 + 底部操作栏**布局（保持现有风格）：

```
┌─────────────────────────────────────────────────────────────────────────┐
│  档案审批管理        [待审批 5] [已批准 23] [已拒绝 2]                   │
├─────────────────────────────────────────────────────────────────────────┤
│  ☐  档号      题名              申请人   申请时间       状态    操作  │
│  ────────────────────────────────────────────────────────────────────  │
│  ☑  A001      发票XXX          张三     2025-01-09    待审批  详情  │
│  ☐  A002      收据XXX          李四     2025-01-09    待审批  详情  │
│  ...                                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│  [✓ 批准 2] [✗ 拒绝] [清除选择]                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.3 响应式处理

- **宽屏 (>1280px)**：显示完整左右分栏
- **中屏 (768px - 1280px)**：操作面板变为可折叠侧边栏
- **窄屏 (<768px)**：操作面板变为底部抽屉

## 4. 操作配置定义

### 4.1 电子凭证池操作配置

```typescript
// src/configs/actions/pool-actions.ts
import { ShieldCheck, Edit3, Link, CheckCircle, Trash2 } from 'lucide-react';
import { poolApi } from '@/api/pool';

export const poolActionConfigs = {
  // 四性检测
  check: {
    key: 'check',
    label: '四性检测',
    icon: ShieldCheck,
    description: '对文件进行真实性、完整性、可用性、安全性检测',
    isAvailable: (items) =>
      items.some(i => i.preArchiveStatus === 'PENDING_CHECK' ||
                       i.preArchiveStatus === 'CHECK_FAILED'),
    handler: async (items) => {
      const targetItems = items.filter(i =>
        i.preArchiveStatus === 'PENDING_CHECK' ||
        i.preArchiveStatus === 'CHECK_FAILED'
      );
      const ids = targetItems.map(i => i.id);
      return await poolApi.checkMultiple(ids);
    },
    variant: 'primary',
    confirm: {
      title: (n) => `确认检测 ${n} 个文件？`,
      description: (n) => '检测过程可能需要较长时间，请耐心等待',
    },
    execution: { mode: 'parallel', progress: true }
  },

  // 补录元数据
  fillMetadata: {
    key: 'fillMetadata',
    label: '补录元数据',
    icon: Edit3,
    description: '补充完善档案元数据信息',
    isAvailable: (items) =>
      items.some(i => i.preArchiveStatus === 'PENDING_METADATA'),
    handler: async (items) => {
      // 打开补录弹窗
      return { type: 'modal', items };
    },
    variant: 'default'
  },

  // 智能匹配
  match: {
    key: 'match',
    label: '智能匹配',
    icon: Link,
    description: '自动匹配关联的原始凭证',
    isAvailable: (items) =>
      items.some(i => i.preArchiveStatus === 'MATCH_PENDING'),
    handler: async (items) => {
      const targetItems = items.filter(i =>
        i.preArchiveStatus === 'MATCH_PENDING'
      );
      return await poolApi.matchVouchers(targetItems.map(i => i.id));
    },
    variant: 'primary',
    confirm: {
      title: (n) => `匹配 ${n} 个凭证的原始单据？`,
    }
  },

  // 提交归档
  submitArchive: {
    key: 'submitArchive',
    label: '提交归档',
    icon: CheckCircle,
    description: '提交归档审批申请',
    isAvailable: (items) =>
      items.some(i => i.preArchiveStatus === 'PENDING_ARCHIVE'),
    handler: async (items) => {
      const targetItems = items.filter(i =>
        i.preArchiveStatus === 'PENDING_ARCHIVE'
      );
      return await poolApi.archiveItems(targetItems.map(i => i.id));
    },
    variant: 'primary',
    confirm: {
      title: (n) => `提交 ${n} 个文件归档？`,
      description: (n) => '提交后将进入审批流程，请确认信息无误',
      confirmText: '确认提交',
    }
  },

  // 删除
  delete: {
    key: 'delete',
    label: '删除',
    icon: Trash2,
    description: '删除选中的文件',
    isAvailable: (items) => items.length > 0,
    handler: async (items) => {
      const results = await Promise.allSettled(
        items.map(i => poolApi.delete(i.id))
      );
      return {
        success: results.filter(r => r.status === 'fulfilled').length,
        failed: results.filter(r => r.status === 'rejected').length,
      };
    },
    variant: 'danger',
    confirm: {
      title: (n) => `确认删除 ${n} 个文件？`,
      description: (n) => '删除后无法恢复，请谨慎操作',
      confirmText: '确认删除',
    }
  },
};
```

### 4.2 档案审批操作配置

```typescript
// src/configs/actions/approval-actions.ts
import { CheckCircle2, XCircle, Eye } from 'lucide-react';
import { archiveApprovalApi } from '@/api/archiveApproval';

export const approvalActionConfigs = {
  // 批准
  approve: {
    key: 'approve',
    label: '批准',
    icon: CheckCircle2,
    isAvailable: (items) =>
      items.every(i => i.status === 'PENDING'),
    handler: async (items, { user }) => {
      return await archiveApprovalApi.batchApprove({
        ids: items.map(i => i.id),
        approverId: user?.id || 'system',
        approverName: user?.realName || user?.username || '系统',
        comment: '批准归档',
      });
    },
    variant: 'primary',
  },

  // 拒绝
  reject: {
    key: 'reject',
    label: '拒绝',
    icon: XCircle,
    isAvailable: (items) =>
      items.every(i => i.status === 'PENDING'),
    handler: async (items, { user }, { comment }) => {
      if (!comment) {
        throw new Error('拒绝必须填写审批意见');
      }
      return await archiveApprovalApi.batchReject({
        ids: items.map(i => i.id),
        approverId: user?.id || 'system',
        approverName: user?.realName || user?.username || '系统',
        comment,
      });
    },
    variant: 'danger',
    requireComment: true,
  },

  // 查看详情
  detail: {
    key: 'detail',
    label: '详情',
    icon: Eye,
    isAvailable: (items) => items.length === 1,
    handler: async (items) => {
      return { type: 'navigate', to: `/archive/${items[0].id}` };
    },
    variant: 'default',
  },
};
```

## 5. 核心 Hooks 实现

### 5.1 useBatchActions Hook

```typescript
// src/hooks/useBatchActions.ts
import { useMemo, useCallback } from 'react';
import { ActionConfig, BatchResult } from '@/types/actions';

interface UseBatchActionsOptions<T> {
  items: T[];
  configs: Record<string, ActionConfig<T>>;
  onSuccess?: (result: BatchResult) => void;
  onError?: (error: Error) => void;
}

export function useBatchActions<T>({
  items,
  configs,
  onSuccess,
  onError,
}: UseBatchActionsOptions<T>) {
  // 计算每个操作的可执行数量
  const actionCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    Object.entries(configs).forEach(([key, config]) => {
      const applicableItems = items.filter(item => {
        // 检查单个项目是否适用
        const singleCheck = { ...config, isAvailable: (i: T[]) =>
          config.isAvailable([item])
        };
        return singleCheck.isAvailable([item]);
      });
      counts[key] = applicableItems.length;
    });
    return counts;
  }, [items, configs]);

  // 计算可用的操作列表
  const availableActions = useMemo(() => {
    return Object.entries(configs)
      .filter(([_, config]) => config.isAvailable(items))
      .map(([key, config]) => ({
        key,
        ...config,
        count: actionCounts[key] || 0,
      }))
      .sort((a, b) => {
        // 按可用数量降序排列
        return (b.count || 0) - (a.count || 0);
      });
  }, [items, configs, actionCounts]);

  // 执行操作
  const execute = useCallback(async (
    actionKey: string,
    options?: { comment?: string }
  ) => {
    const config = configs[actionKey];
    if (!config) {
      throw new Error(`操作 "${actionKey}" 不存在`);
    }

    if (!config.isAvailable(items)) {
      throw new Error(`当前选中文件不支持 "${config.label}" 操作`);
    }

    try {
      const result = await config.handler(items, options);
      onSuccess?.(result);
      return result;
    } catch (error) {
      onError?.(error as Error);
      throw error;
    }
  }, [items, configs, onSuccess, onError]);

  return {
    availableActions,
    actionCounts,
    execute,
    hasActions: availableActions.length > 0,
  };
}
```

### 5.2 useBatchExecutor Hook

```typescript
// src/hooks/useBatchExecutor.ts
import { useState, useCallback } from 'react';
import { ActionConfig } from '@/types/actions';

interface ExecutionState {
  loading: boolean;
  progress: number;
  total: number;
  current: string;
}

export function useBatchExecutor() {
  const [state, setState] = useState<ExecutionState>({
    loading: false,
    progress: 0,
    total: 0,
    current: '',
  });

  const execute = useCallback(async <T>(
    items: T[],
    config: ActionConfig<T>,
    options?: { comment?: string }
  ) => {
    setState({
      loading: true,
      progress: 0,
      total: items.length,
      current: '',
    });

    try {
      // 根据执行模式选择策略
      if (config.execution?.mode === 'parallel') {
        // 并行执行
        const results = await Promise.allSettled(
          items.map(async (item, index) => {
            setState(prev => ({
              ...prev,
              progress: index,
              current: `处理 ${index + 1}/${items.length}`,
            }));
            return config.handler([item], options);
          })
        );

        const success = results.filter(r => r.status === 'fulfilled').length;
        const failed = results.filter(r => r.status === 'rejected').length;

        setState(prev => ({ ...prev, loading: false, progress: items.length }));
        return { success, failed, results };
      } else {
        // 串行执行
        const results = [];
        let success = 0;
        let failed = 0;

        for (let i = 0; i < items.length; i++) {
          setState(prev => ({
            ...prev,
            progress: i,
            current: `处理 ${i + 1}/${items.length}`,
          }));

          try {
            const result = await config.handler([items[i]], options);
            results.push({ status: 'fulfilled', value: result });
            success++;
          } catch (error) {
            results.push({ status: 'rejected', reason: error });
            failed++;
          }
        }

        setState(prev => ({ ...prev, loading: false, progress: items.length }));
        return { success, failed, results };
      }
    } catch (error) {
      setState(prev => ({ ...prev, loading: false }));
      throw error;
    }
  }, []);

  const reset = useCallback(() => {
    setState({
      loading: false,
      progress: 0,
      total: 0,
      current: '',
    });
  }, []);

  return {
    ...state,
    execute,
    reset,
    progressPercent: state.total > 0 ? (state.progress / state.total) * 100 : 0,
  };
}
```

### 5.3 useActionConfirm Hook

```typescript
// src/hooks/useActionConfirm.ts
import { useState, useCallback } from 'react';

interface ConfirmOptions {
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  requireComment?: boolean;
}

export function useActionConfirm() {
  const [confirmState, setConfirmState] = useState<{
    visible: boolean;
    options: ConfirmOptions | null;
    resolve: ((value: { confirmed: boolean; comment?: string }) => void) | null;
  }>({
    visible: false,
    options: null,
    resolve: null,
  });

  const confirm = useCallback((options: ConfirmOptions): Promise<{ confirmed: boolean; comment?: string }> => {
    return new Promise((resolve) => {
      setConfirmState({
        visible: true,
        options,
        resolve,
      });
    });
  }, []);

  const handleConfirm = useCallback((comment?: string) => {
    confirmState.resolve?.({ confirmed: true, comment });
    setConfirmState({ visible: false, options: null, resolve: null });
  }, [confirmState.resolve]);

  const handleCancel = useCallback(() => {
    confirmState.resolve?.({ confirmed: false });
    setConfirmState({ visible: false, options: null, resolve: null });
  }, [confirmState.resolve]);

  return {
    confirm,
    confirmVisible: confirmState.visible,
    confirmOptions: confirmState.options,
    onConfirm: handleConfirm,
    onCancel: handleCancel,
  };
}
```

## 6. UI 组件与页面集成

### 6.1 ActionPanel 组件

```typescript
// src/components/actions/ActionPanel.tsx
import { ActionConfig } from '@/types/actions';

interface ActionPanelProps<T> {
  selectedCount: number;
  statusDistribution?: Record<string, number>;
  actions: Array<ActionConfig<T> & { count: number }>;
  onActionClick: (key: string) => void;
  loading?: boolean;
  className?: string;
}

export function ActionPanel<T>({
  selectedCount,
  statusDistribution,
  actions,
  onActionClick,
  loading = false,
  className = '',
}: ActionPanelProps<T>) {
  if (selectedCount === 0) {
    return (
      <div className={`p-6 text-center text-slate-400 ${className}`}>
        <p>请选择文件以查看可用操作</p>
      </div>
    );
  }

  return (
    <div className={`p-4 space-y-4 ${className}`}>
      {/* 选中数量 */}
      <div className="text-sm font-medium text-slate-700">
        已选择 <span className="text-lg text-indigo-600">{selectedCount}</span> 个文件
      </div>

      {/* 状态分布 */}
      {statusDistribution && Object.keys(statusDistribution).length > 0 && (
        <div className="border-t border-slate-200 pt-4">
          <div className="text-xs text-slate-500 mb-2">待处理状态分布：</div>
          <div className="space-y-1">
            {Object.entries(statusDistribution).map(([status, count]) => (
              <div key={status} className="flex justify-between text-sm">
                <span className="text-slate-600">{statusLabels[status]}</span>
                <span className="font-medium text-slate-800">{count}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 可执行操作 */}
      <div className="border-t border-slate-200 pt-4">
        <div className="text-xs text-slate-500 mb-3">可执行操作：</div>
        <div className="space-y-2">
          {actions.map((action) => (
            <ActionButton
              key={action.key}
              config={action}
              count={action.count}
              onClick={() => onActionClick(action.key)}
              disabled={loading}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

const statusLabels: Record<string, string> = {
  PENDING_CHECK: '待检测',
  CHECK_FAILED: '检测失败',
  PENDING_METADATA: '待补录',
  PENDING_ARCHIVE: '准备归档',
  // ...
};
```

### 6.2 ActionButton 组件

```typescript
// src/components/actions/ActionButton.tsx
import { ActionConfig } from '@/types/actions';

interface ActionButtonProps<T> {
  config: ActionConfig<T>;
  count?: number;
  onClick: () => void;
  disabled?: boolean;
}

export function ActionButton<T>({
  config,
  count,
  onClick,
  disabled,
}: ActionButtonProps<T>) {
  const Icon = config.icon;

  const variantStyles = {
    primary: 'bg-indigo-600 hover:bg-indigo-700 text-white',
    danger: 'bg-rose-600 hover:bg-rose-700 text-white',
    default: 'bg-slate-100 hover:bg-slate-200 text-slate-700',
  };

  return (
    <button
      onClick={onClick}
      disabled={disabled || count === 0}
      className={`
        w-full flex items-center justify-between px-4 py-3 rounded-lg
        font-medium text-sm transition-all
        ${variantStyles[config.variant || 'default']}
        ${disabled || count === 0 ? 'opacity-50 cursor-not-allowed' : ''}
      `}
      title={config.description}
    >
      <span className="flex items-center gap-2">
        <Icon size={16} />
        {config.label}
      </span>
      {count !== undefined && (
        <span className={`px-2 py-0.5 rounded-full text-xs ${
          config.variant === 'primary'
            ? 'bg-indigo-500 text-white'
            : config.variant === 'danger'
            ? 'bg-rose-500 text-white'
            : 'bg-slate-200 text-slate-600'
        }`}>
          {count}
        </span>
      )}
    </button>
  );
}
```

### 6.3 页面使用示例

```typescript
// src/pages/system/PoolPage.tsx
import { useState } from 'react';
import { useBatchActions } from '@/hooks/useBatchActions';
import { useActionConfirm } from '@/hooks/useActionConfirm';
import { poolActionConfigs } from '@/configs/actions/pool-actions';
import { ActionPanel } from '@/components/actions/ActionPanel';
import { toast } from '@/utils/notification';

export function PoolPage() {
  // 页面管理自己的状态
  const [files, setFiles] = useState<PoolFile[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // 计算选中的文件
  const selectedFiles = files.filter(f => selectedIds.has(f.id));

  // 使用复用的 hooks
  const { availableActions, execute } = useBatchActions({
    items: selectedFiles,
    configs: poolActionConfigs,
    onSuccess: (result) => {
      toast.success(`操作完成：成功 ${result.success}，失败 ${result.failed}`);
      // 刷新列表
      loadFiles();
    },
    onError: (error) => {
      toast.error(`操作失败：${error.message}`);
    },
  });

  const { confirm, ...confirmState } = useActionConfirm();

  // 处理操作点击
  const handleActionClick = async (actionKey: string) => {
    const config = poolActionConfigs[actionKey];
    if (!config) return;

    // 确认
    if (config.confirm) {
      const result = await confirm({
        title: config.confirm.title(selectedFiles.length),
        description: config.confirm.description?.(selectedFiles.length),
        confirmText: config.confirm.confirmText,
        cancelText: config.confirm.cancelText,
        requireComment: config.requireComment,
      });

      if (!result.confirmed) return;
    }

    // 执行
    try {
      await execute(actionKey, { comment: confirmState.comment });
      setSelectedIds(new Set()); // 清除选择
    } catch (error) {
      // 错误已在 hook 中处理
    }
  };

  // 计算状态分布
  const statusDistribution = useMemo(() => {
    const distribution: Record<string, number> = {};
    selectedFiles.forEach(f => {
      distribution[f.preArchiveStatus] = (distribution[f.preArchiveStatus] || 0) + 1;
    });
    return distribution;
  }, [selectedFiles]);

  return (
    <div className="flex h-screen">
      {/* 左侧文件列表 */}
      <FileList
        files={files}
        selectedIds={selectedIds}
        onSelect={setSelectedIds}
      />

      {/* 右侧操作面板 */}
      <ActionPanel
        selectedCount={selectedIds.size}
        statusDistribution={statusDistribution}
        actions={availableActions}
        onActionClick={handleActionClick}
      />

      {/* 确认弹窗 */}
      <ConfirmDialog {...confirmState} />
    </div>
  );
}
```

## 7. 实施计划

### 7.1 实施阶段

| 阶段 | 内容 | 产出 |
|------|------|------|
| **阶段一：类型与配置** | 定义核心类型、创建操作配置 | `types/actions.ts`, `configs/actions/*.ts` |
| **阶段二：核心 Hooks** | 实现 useBatchActions、useBatchExecutor、useActionConfirm | `hooks/useBatchActions.ts` 等 |
| **阶段三：UI 组件** | 实现 ActionPanel、ActionButton、ConfirmDialog | `components/actions/*.tsx` |
| **阶段四：页面集成** | 改造电子凭证池页面 | `PoolPage.tsx` 重构 |
| **阶段五：其他页面** | 改造档案审批等页面 | `ArchiveApprovalView.tsx` 重构 |

### 7.2 文件结构

```
src/
├── types/
│   └── actions/
│       └── index.ts          # ActionConfig, BatchResult 等类型定义
├── configs/
│   └── actions/
│       ├── pool-actions.ts    # 电子凭证池操作配置
│       ├── approval-actions.ts # 档案审批操作配置
│       └── index.ts           # 导出所有配置
├── hooks/
│   ├── useBatchActions.ts     # 核心操作 hook
│   ├── useBatchExecutor.ts   # 执行器 hook
│   └── useActionConfirm.ts   # 确认弹窗 hook
├── components/
│   └── actions/
│       ├── ActionPanel.tsx    # 操作面板组件
│       ├── ActionButton.tsx   # 操作按钮组件
│       ├── ConfirmDialog.tsx  # 确认弹窗组件
│       └── BatchResultModal.tsx # 结果弹窗组件
└── pages/
    └── system/
        └── pool/
            └── PoolPage.tsx   # 改造后的页面
```

### 7.3 技术要点

1. **类型安全** - 使用泛型确保操作配置与数据类型匹配
2. **性能优化** - 使用 useMemo 缓存可用操作计算
3. **错误处理** - 统一的错误处理和用户反馈
4. **可测试性** - hooks 和组件易于单元测试

### 7.4 向后兼容

- 保留现有的批量操作组件
- 新旧方式可以并存
- 逐步迁移，避免大规模重构

## 8. 总结

本设计方案通过**能力复用 + 视图自由**的原则，解决了电子凭证池操作流程不清晰的问题：

1. **用户视角**：选中文件后，右侧面板直接显示可执行的操作，操作后数字变化，直观清晰

2. **开发视角**：通过配置定义操作，通过 hooks 复用逻辑，新页面只需定义配置即可

3. **扩展性**：新增操作只需添加配置，新增页面只需复用 hooks

4. **一致性**：所有页面使用相同的操作定义方式，交互体验统一

