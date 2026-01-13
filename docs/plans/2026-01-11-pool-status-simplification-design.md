# 电子凭证池状态简化与仪表板设计

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将预归档状态从 10 个简化为 5 个核心状态，并添加可归档仪表板，让用户能快速识别和批量操作"可归档"状态的凭证。

**Architecture:** 后端使用 Flyway 迁移将 10 个状态映射为 5 个简化状态，前端新增 `PoolDashboard` 组件展示统计卡片，配合 `usePoolKanban` hook 的状态映射逻辑实现向下兼容。

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Flyway, React 19, TypeScript, Vitest, Ant Design

---

## 背景与问题

### 当前问题
1. **状态过多**：10 个预归档状态让用户难以理解
2. **不清晰**：无法快速识别"可归档"的凭证
3. **操作不便**：无法按状态分组进行批量操作

### 设计目标
1. 简化状态数量（10 → 5）
2. 添加可视化仪表板
3. 默认聚焦"可归档"状态

---

## 第 1 节：整体架构

### 1.1 状态映射关系

| 旧状态 (10个) | 新状态 (5个) | 说明 |
|--------------|-------------|------|
| `DRAFT`, `PENDING_CHECK` | `PENDING_CHECK` | 待检测（合并草稿） |
| `CHECK_FAILED`, `PENDING_METADATA` | `NEEDS_ACTION` | 待处理（需要人工介入） |
| `MATCH_PENDING`, `MATCHED` | `READY_TO_MATCH` | 可匹配（已就绪） |
| `PENDING_ARCHIVE` | `READY_TO_ARCHIVE` | 可归档（核心状态） |
| `PENDING_APPROVAL`, `ARCHIVING`, `ARCHIVED` | `COMPLETED` | 已完成（归档流程结束） |

### 1.2 前端架构

```
PoolPage
  ├── PoolDashboard (新增)
  │     ├── DashboardCard × 5
  │     └── 统计 + 快速操作按钮
  └── PoolKanbanView
        ├── KanbanColumn × 5
        └── 根据仪表板筛选显示
```

### 1.3 向下兼容策略

- **数据库迁移**：使用临时列过渡，保留原始数据
- **前端映射**：`STATUS_SIMPLIFICATION_MAP` 将旧状态转换为新状态
- **API 兼容**：后端枚举保留 `fromOldCode()` 方法处理旧值

---

## 第 2 节：简化状态设计

### 2.1 五个核心状态

```typescript
// SimplifiedPreArchiveStatus (5 states)
export enum SimplifiedPreArchiveStatus {
  PENDING_CHECK = 'PENDING_CHECK',    // 待检测
  NEEDS_ACTION = 'NEEDS_ACTION',      // 待处理
  READY_TO_MATCH = 'READY_TO_MATCH',  // 可匹配
  READY_TO_ARCHIVE = 'READY_TO_ARCHIVE', // 可归档（核心）
  COMPLETED = 'COMPLETED',            // 已完成
}
```

### 2.2 状态流转图

```
┌─────────────┐
│ PENDING_    │
│  CHECK      │  ← 新导入的凭证
└──────┬──────┘
       │
       ├──────────────┐
       │              │
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│ NEEDS_      │  │ READY_TO_   │
│  ACTION     │  │  MATCH      │
└──────┬──────┘  └──────┬──────┘
       │              │
       │              ▼
       │      ┌─────────────┐
       │      │ READY_TO_   │
       │      │  ARCHIVE    │ ← 核心状态，批量归档
       │      └──────┬──────┘
       │             │
       └─────────────┤
                     ▼
              ┌─────────────┐
              │  COMPLETED  │
              └─────────────┘
```

---

## 第 3 节：数据库迁移

### 3.1 Flyway 迁移脚本

**文件**: `nexusarchive-java/src/main/resources/db/migration/V96__simplify_pre_archive_status.sql`

```sql
-- Input: 10-state pre_archive_status column
-- Output: 5-state pre_archive_status column
-- Pos: src/main/resources/db/migration/
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ================================================================
-- Migration: V96__simplify_pre_archive_status.sql
-- Purpose: 简化预归档状态，从 10 个状态合并为 5 个核心状态
-- Author: Claude Code
-- Date: 2026-01-11
-- ================================================================

-- 步骤 1: 添加新的状态列（临时）
ALTER TABLE public.acc_archive
ADD COLUMN pre_archive_status_new VARCHAR(20);

COMMENT ON COLUMN public.acc_archive.pre_archive_status_new IS '简化后的预归档状态：PENDING_CHECK/NEEDS_ACTION/READY_TO_MATCH/READY_TO_ARCHIVE/COMPLETED';

-- 步骤 2: 将旧状态映射到新状态
UPDATE public.acc_archive
SET pre_archive_status_new = CASE pre_archive_status
    -- PENDING_CHECK: 合并 DRAFT + PENDING_CHECK
    WHEN 'DRAFT' THEN 'PENDING_CHECK'
    WHEN 'PENDING_CHECK' THEN 'PENDING_CHECK'

    -- NEEDS_ACTION: 合并 CHECK_FAILED + PENDING_METADATA
    WHEN 'CHECK_FAILED' THEN 'NEEDS_ACTION'
    WHEN 'PENDING_METADATA' THEN 'NEEDS_ACTION'

    -- READY_TO_MATCH: 合并 MATCH_PENDING + MATCHED
    WHEN 'MATCH_PENDING' THEN 'READY_TO_MATCH'
    WHEN 'MATCHED' THEN 'READY_TO_MATCH'

    -- READY_TO_ARCHIVE: 原名 PENDING_ARCHIVE
    WHEN 'PENDING_ARCHIVE' THEN 'READY_TO_ARCHIVE'

    -- COMPLETED: 合并 PENDING_APPROVAL + ARCHIVING + ARCHIVED
    WHEN 'PENDING_APPROVAL' THEN 'COMPLETED'
    WHEN 'ARCHIVING' THEN 'COMPLETED'
    WHEN 'ARCHIVED' THEN 'COMPLETED'

    -- 兜底：未知状态归到待检测
    ELSE 'PENDING_CHECK'
END
WHERE pre_archive_status IS NOT NULL;

-- 步骤 3: 删除旧列，重命名新列
ALTER TABLE public.acc_archive DROP COLUMN pre_archive_status;
ALTER TABLE public.acc_archive RENAME COLUMN pre_archive_status_new TO pre_archive_status;

-- 步骤 4: 添加 NOT NULL 约束和默认值
ALTER TABLE public.acc_archive
ALTER COLUMN pre_archive_status SET NOT NULL;

ALTER TABLE public.acc_archive
ALTER COLUMN pre_archive_status SET DEFAULT 'PENDING_CHECK';

-- 步骤 5: 创建索引
CREATE INDEX idx_acc_archive_pre_archive_status
ON public.acc_archive(pre_archive_status);

COMMENT ON INDEX public.acc_archive.idx_acc_archive_pre_archive_status
IS '预归档状态索引 - 用于仪表板统计和筛选';

-- 步骤 6: 更新现有数据的引用（如果其他表有外键引用）
-- 注意：此迁移假设没有其他表有外键引用 pre_archive_status
-- 如果有，需要先删除外键，迁移后重建
```

### 3.2 迁移验证

```sql
-- 验证迁移结果
SELECT
    pre_archive_status,
    COUNT(*) as count
FROM public.acc_archive
GROUP BY pre_archive_status
ORDER BY count DESC;

-- 预期结果：只有 5 个状态值
-- PENDING_CHECK
-- NEEDS_ACTION
-- READY_TO_MATCH
-- READY_TO_ARCHIVE
-- COMPLETED
```

---

## 第 4 节：后端枚举修改

### 4.1 PreArchiveStatus 枚举

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/entity/enums/PreArchiveStatus.java`

```java
// Input: Database pre_archive_status column (VARCHAR)
// Output: PreArchiveStatus enum with 5 states
// Pos: src/main/java/com/nexusarchive/entity/enums/

package com.nexusarchive.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 预归档状态枚举（简化版 - 5 个核心状态）
 *
 * <p>状态迁移说明：
 * <ul>
 *   <li>PENDING_CHECK: 合并 DRAFT + PENDING_CHECK</li>
 *   <li>NEEDS_ACTION: 合并 CHECK_FAILED + PENDING_METADATA</li>
 *   <li>READY_TO_MATCH: 合并 MATCH_PENDING + MATCHED</li>
 *   <li>READY_TO_ARCHIVE: 原 PENDING_ARCHIVE</li>
 *   <li>COMPLETED: 合并 PENDING_APPROVAL + ARCHIVING + ARCHIVED</li>
 * </ul>
 *
 * @see <a href="V96__simplify_pre_archive_status.sql">Flyway migration V96</a>
 */
@Getter
public enum PreArchiveStatus {

    /** 待检测 - 新导入或初始状态的凭证 */
    PENDING_CHECK("PENDING_CHECK", "待检测"),

    /** 待处理 - 检测失败或需要补全元数据 */
    NEEDS_ACTION("NEEDS_ACTION", "待处理"),

    /** 可匹配 - 可以进行凭证关联操作 */
    READY_TO_MATCH("READY_TO_MATCH", "可匹配"),

    /** 可归档 - 已就绪，可以提交归档（核心状态） */
    READY_TO_ARCHIVE("READY_TO_ARCHIVE", "可归档"),

    /** 已完成 - 归档流程已结束 */
    COMPLETED("COMPLETED", "已完成");

    /**
     * 旧状态到新状态的映射表（用于迁移兼容）
     */
    private static final java.util.Map<String, PreArchiveStatus> OLD_STATUS_MAP =
        java.util.Map.ofEntries(
            java.util.Map.entry("DRAFT", PENDING_CHECK),
            java.util.Map.entry("PENDING_CHECK", PENDING_CHECK),
            java.util.Map.entry("CHECK_FAILED", NEEDS_ACTION),
            java.util.Map.entry("PENDING_METADATA", NEEDS_ACTION),
            java.util.Map.entry("MATCH_PENDING", READY_TO_MATCH),
            java.util.Map.entry("MATCHED", READY_TO_MATCH),
            java.util.Map.entry("PENDING_ARCHIVE", READY_TO_ARCHIVE),
            java.util.Map.entry("PENDING_APPROVAL", COMPLETED),
            java.util.Map.entry("ARCHIVING", COMPLETED),
            java.util.Map.entry("ARCHIVED", COMPLETED)
        );

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    PreArchiveStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 从旧状态代码获取枚举值（迁移兼容）
     *
     * @param oldCode 旧状态代码（可能已废弃）
     * @return 对应的新状态枚举
     */
    public static PreArchiveStatus fromOldCode(String oldCode) {
        return OLD_STATUS_MAP.getOrDefault(oldCode, PENDING_CHECK);
    }

    /**
     * 从代码获取枚举值
     *
     * @param code 状态代码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果代码无效
     */
    public static PreArchiveStatus fromCode(String code) {
        for (PreArchiveStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PreArchiveStatus: " + code);
    }

    /**
     * 检查状态是否为可归档
     */
    public boolean isReadyToArchive() {
        return this == READY_TO_ARCHIVE;
    }

    /**
     * 检查状态是否需要人工处理
     */
    public boolean needsAction() {
        return this == NEEDS_ACTION;
    }

    /**
     * 检查状态是否已完成
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
```

### 4.2 需要修改的其他 Java 文件

| 文件 | 修改内容 |
|------|---------|
| `PoolServiceImpl.java` | 更新状态判断逻辑 |
| `PreArchiveSubmitService.java` | 使用新状态枚举 |
| `PreArchiveCheckService.java` | 更新检测后的状态设置 |
| `ProcessingListener.java` | 更新事件处理 |
| `OriginalVoucher.java` | 确认实体类使用 @EnumValue |
| `IngestServiceImpl.java` | 更新导入后的初始状态 |
| `BatchToArchiveServiceImpl.java` | 使用 READY_TO_ARCHIVE |
| `VoucherMatchingEngine.java` | 更新匹配后状态 |
| `VoucherCrudService.java` | 更新查询条件 |
| `VoucherVersionService.java` | 更新版本关联状态 |
| `CollectionBatchFile.java` | 批量文件的状态关联 |
| `MatchResultPersistenceService.java` | 匹配结果持久化 |

---

## 第 5 节：前端配置更新

### 5.1 pool-columns.config.ts

**文件**: `src/config/pool-columns.config.ts`

```typescript
// Input: SimplifiedPreArchiveStatus enum
// Output: Column groups configuration
// Pos: src/config/pool-columns.config.ts

import type { ColumnGroupConfig, SubStateConfig } from '@/types/pool';

/**
 * 简化后的预归档状态（5 个核心状态）
 */
export enum SimplifiedPreArchiveStatus {
  PENDING_CHECK = 'PENDING_CHECK',    // 待检测
  NEEDS_ACTION = 'NEEDS_ACTION',      // 待处理
  READY_TO_MATCH = 'READY_TO_MATCH',  // 可匹配
  READY_TO_ARCHIVE = 'READY_TO_ARCHIVE', // 可归档（核心）
  COMPLETED = 'COMPLETED',            // 已完成
}

/**
 * 状态显示配置（颜色、图标）
 */
export const STATUS_CONFIG: Record<SimplifiedPreArchiveStatus, {
  color: string;
  icon: string;
  label: string;
  description: string;
}> = {
  [SimplifiedPreArchiveStatus.PENDING_CHECK]: {
    color: '#94a3b8',
    icon: 'circle-dot',
    label: '待检测',
    description: '新导入的凭证，等待四性检测',
  },
  [SimplifiedPreArchiveStatus.NEEDS_ACTION]: {
    color: '#f59e0b',
    icon: 'alert-circle',
    label: '待处理',
    description: '检测失败或需要补全信息',
  },
  [SimplifiedPreArchiveStatus.READY_TO_MATCH]: {
    color: '#3b82f6',
    icon: 'link',
    label: '可匹配',
    description: '可以进行凭证关联',
  },
  [SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]: {
    color: '#10b981',
    icon: 'check-circle-2',
    label: '可归档',
    description: '已就绪，可以提交归档',
  },
  [SimplifiedPreArchiveStatus.COMPLETED]: {
    color: '#64748b',
    icon: 'check-circle',
    label: '已完成',
    description: '归档流程已完成',
  },
};

/**
 * 看板列配置（5 列）
 */
export const POOL_COLUMN_GROUPS: ColumnGroupConfig[] = [
  {
    id: 'pending',
    title: '待检测',
    subStates: [
      { value: SimplifiedPreArchiveStatus.PENDING_CHECK, label: '待检测' },
    ],
  },
  {
    id: 'needs-action',
    title: '待处理',
    subStates: [
      { value: SimplifiedPreArchiveStatus.NEEDS_ACTION, label: '待处理' },
    ],
  },
  {
    id: 'ready-to-match',
    title: '可匹配',
    subStates: [
      { value: SimplifiedPreArchiveStatus.READY_TO_MATCH, label: '可匹配' },
    ],
  },
  {
    id: 'ready-to-archive',
    title: '可归档',
    subStates: [
      { value: SimplifiedPreArchiveStatus.READY_TO_ARCHIVE, label: '可归档' },
    ],
    highlight: true, // 标记为重点列
  },
  {
    id: 'completed',
    title: '已完成',
    subStates: [
      { value: SimplifiedPreArchiveStatus.COMPLETED, label: '已完成' },
    ],
  },
];

/**
 * 默认筛选状态：可归档
 */
export const DEFAULT_DASHBOARD_FILTER = SimplifiedPreArchiveStatus.READY_TO_ARCHIVE;
```

---

## 第 6 节：仪表板组件设计

### 6.1 DashboardCard 组件

**文件**: `src/components/pool-dashboard/DashboardCard.tsx`

```typescript
// Input: count, status, isActive, onClick, actionLabel
// Output: Statistical card with action button
// Pos: src/components/pool-dashboard/DashboardCard.tsx

import React from 'react';
import { STATUS_CONFIG, SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';
import * as Icons from 'lucide-react';
import './DashboardCard.css';

interface DashboardCardProps {
  status: SimplifiedPreArchiveStatus;
  count: number;
  isActive: boolean;
  onClick: () => void;
  actionLabel?: string;
  showAction?: boolean;
}

/**
 * 仪表板统计卡片
 *
 * 显示每个状态的凭证数量，支持点击筛选
 */
export const DashboardCard: React.FC<DashboardCardProps> = ({
  status,
  count,
  isActive,
  onClick,
  actionLabel,
  showAction = false,
}) => {
  const config = STATUS_CONFIG[status];
  const IconComponent = Icons[config.icon as keyof typeof Icons] as React.ComponentType<{ className?: string }>;

  return (
    <div
      className={`dashboard-card ${isActive ? 'dashboard-card--active' : ''}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onClick();
        }
      }}
      aria-label={`${config.label}：${count} 条`}
      aria-pressed={isActive}
    >
      <div className="dashboard-card__header" style={{ '--card-color': config.color } as React.CSSProperties}>
        <div className="dashboard-card__icon">
          <IconComponent className="lucide-icon" size={24} />
        </div>
        <div className="dashboard-card__count">{count}</div>
      </div>

      <div className="dashboard-card__body">
        <div className="dashboard-card__title">{config.label}</div>
        <div className="dashboard-card__description">{config.description}</div>
      </div>

      {showAction && actionLabel && (
        <div className="dashboard-card__footer">
          <button
            className="dashboard-card__action"
            onClick={(e) => {
              e.stopPropagation();
              onClick();
            }}
          >
            {actionLabel}
          </button>
        </div>
      )}

      {isActive && (
        <div className="dashboard-card__indicator" style={{ backgroundColor: config.color }} />
      )}
    </div>
  );
};

export default DashboardCard;
```

### 6.2 DashboardCard 样式

**文件**: `src/components/pool-dashboard/DashboardCard.css`

```css
/* src/components/pool-dashboard/DashboardCard.css */
.dashboard-card {
  position: relative;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 120px;
  display: flex;
  flex-direction: column;
}

.dashboard-card:hover {
  border-color: var(--card-color, #64748b);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.dashboard-card--active {
  border-color: var(--card-color, #10b981);
  background: linear-gradient(to bottom, rgba(16, 185, 129, 0.05), white);
}

.dashboard-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.dashboard-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--card-color, #64748b);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.9;
}

.dashboard-card__count {
  font-size: 32px;
  font-weight: 700;
  color: var(--card-color, #64748b);
  line-height: 1;
}

.dashboard-card__body {
  flex: 1;
}

.dashboard-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 4px;
}

.dashboard-card__description {
  font-size: 13px;
  color: #64748b;
  line-height: 1.4;
}

.dashboard-card__footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.dashboard-card__action {
  width: 100%;
  padding: 8px 16px;
  background: var(--card-color, #10b981);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.dashboard-card__action:hover {
  opacity: 0.9;
}

.dashboard-card__indicator {
  position: absolute;
  top: 0;
  left: 16px;
  right: 16px;
  height: 3px;
  border-radius: 3px 3px 0 0;
}
```

### 6.3 PoolDashboard 组件

**文件**: `src/components/pool-dashboard/PoolDashboard.tsx`

```typescript
// Input: usePoolKanban hook, filter state
// Output: Dashboard with 5 statistical cards
// Pos: src/components/pool-dashboard/PoolDashboard.tsx

import React, { useMemo } from 'react';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { usePoolDashboard } from '@/hooks/usePoolDashboard';
import { DashboardCard } from './DashboardCard';
import {
  SimplifiedPreArchiveStatus,
  STATUS_CONFIG,
  DEFAULT_DASHBOARD_FILTER,
} from '@/config/pool-columns.config';
import './PoolDashboard.css';

interface PoolDashboardProps {
  /** 当前激活的筛选状态 */
  activeFilter: SimplifiedPreArchiveStatus | null;
  /** 筛选状态变更回调 */
  onFilterChange: (status: SimplifiedPreArchiveStatus | null) => void;
  /** 是否显示批量操作按钮 */
  showActions?: boolean;
  /** 批量归档回调 */
  onBatchArchive?: () => void;
}

/**
 * 电子凭证池仪表板
 *
 * 展示 5 个核心状态的统计卡片，支持点击筛选看板数据
 */
export const PoolDashboard: React.FC<PoolDashboardProps> = ({
  activeFilter,
  onFilterChange,
  showActions = false,
  onBatchArchive,
}) => {
  const { refetch } = usePoolKanban();
  const { stats, totalCount } = usePoolDashboard();

  // 计算每个状态的卡片配置
  const cards = useMemo(() => {
    return Object.values(SimplifiedPreArchiveStatus).map((status) => {
      const count = stats[status];
      const isActive = activeFilter === status;
      const config = STATUS_CONFIG[status];

      // "可归档"状态显示批量操作按钮
      const showAction = showActions && status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE && count > 0;
      const actionLabel = `批量归档 (${count})`;

      return {
        status,
        count,
        isActive,
        showAction,
        actionLabel: showAction ? actionLabel : undefined,
      };
    });
  }, [stats, activeFilter, showActions]);

  const handleCardClick = (status: SimplifiedPreArchiveStatus) => {
    // 切换筛选状态：如果点击当前激活状态，则取消筛选
    const newFilter = activeFilter === status ? null : status;
    onFilterChange(newFilter);
  };

  const handleBatchArchive = () => {
    onBatchArchive?.();
  };

  return (
    <div className="pool-dashboard">
      <div className="pool-dashboard__cards">
        {cards.map((card) => (
          <DashboardCard
            key={card.status}
            status={card.status}
            count={card.count}
            isActive={card.isActive}
            showAction={card.showAction}
            actionLabel={card.actionLabel}
            onClick={() =>
              card.showAction && card.status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE
                ? handleBatchArchive()
                : handleCardClick(card.status)
            }
          />
        ))}
      </div>

      <div className="pool-dashboard__summary">
        <span className="pool-dashboard__total">
          共 <strong>{totalCount}</strong> 条凭证
        </span>
        {activeFilter && (
          <button
            className="pool-dashboard__clear"
            onClick={() => onFilterChange(null)}
          >
            清除筛选
          </button>
        )}
      </div>
    </div>
  );
};

export default PoolDashboard;
```

### 6.4 PoolDashboard 样式

**文件**: `src/components/pool-dashboard/PoolDashboard.css`

```css
/* src/components/pool-dashboard/PoolDashboard.css */
.pool-dashboard {
  width: 100%;
  margin-bottom: 24px;
}

.pool-dashboard__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 1024px) {
  .pool-dashboard__cards {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 12px;
  }
}

@media (max-width: 640px) {
  .pool-dashboard__cards {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
}

.pool-dashboard__summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 14px;
  color: #64748b;
}

.pool-dashboard__total strong {
  color: #0f172a;
  font-size: 16px;
}

.pool-dashboard__clear {
  padding: 6px 12px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.pool-dashboard__clear:hover {
  background: #f1f5f9;
  color: #0f172a;
}
```

---

## 第 7 节：Hook 修改

### 7.1 usePoolKanban 修改

**文件**: `src/hooks/usePoolKanban.ts`

**新增内容**：

```typescript
// 旧状态到新状态的映射表
export const STATUS_SIMPLIFICATION_MAP: Record<string, SimplifiedPreArchiveStatus> = {
  // PENDING_CHECK
  'DRAFT': SimplifiedPreArchiveStatus.PENDING_CHECK,
  'PENDING_CHECK': SimplifiedPreArchiveStatus.PENDING_CHECK,

  // NEEDS_ACTION
  'CHECK_FAILED': SimplifiedPreArchiveStatus.NEEDS_ACTION,
  'PENDING_METADATA': SimplifiedPreArchiveStatus.NEEDS_ACTION,

  // READY_TO_MATCH
  'MATCH_PENDING': SimplifiedPreArchiveStatus.READY_TO_MATCH,
  'MATCHED': SimplifiedPreArchiveStatus.READY_TO_MATCH,

  // READY_TO_ARCHIVE
  'PENDING_ARCHIVE': SimplifiedPreArchiveStatus.READY_TO_ARCHIVE,

  // COMPLETED
  'PENDING_APPROVAL': SimplifiedPreArchiveStatus.COMPLETED,
  'ARCHIVING': SimplifiedPreArchiveStatus.COMPLETED,
  'ARCHIVED': SimplifiedPreArchiveStatus.COMPLETED,
};

/**
 * 将旧状态代码转换为简化状态
 */
export function toSimplifiedStatus(oldStatus: string): SimplifiedPreArchiveStatus {
  return STATUS_SIMPLIFICATION_MAP[oldStatus] || SimplifiedPreArchiveStatus.PENDING_CHECK;
}

// 扩展接口，添加 filter 属性
interface UsePoolKanbanOptions {
  /** 筛选特定状态（null 表示显示全部） */
  filter?: SimplifiedPreArchiveStatus | null;
}

export function usePoolKanban(options: UsePoolKanbanOptions = {}) {
  // ... 现有代码 ...

  // 修改 getCardsForColumn 以支持筛选
  const getCardsForColumn = useCallback((
    columnId: string,
    subState?: string
  ): PoolItem[] => {
    let cards = allCards.filter(card =>
      card._columnId === columnId &&
      (!subState || card.status === subState)
    );

    // 如果有筛选条件，只返回匹配的状态
    if (options.filter) {
      cards = cards.filter(card => {
        const simplifiedStatus = toSimplifiedStatus(card.status);
        return simplifiedStatus === options.filter;
      });
    }

    return cards;
  }, [allCards, options.filter]);

  return {
    // ... 现有返回值 ...
    getCardsForColumn,
  };
}
```

### 7.2 usePoolDashboard 新增 Hook

**文件**: `src/hooks/usePoolDashboard.ts`

```typescript
// Input: usePoolKanban hook
// Output: Dashboard statistics
// Pos: src/hooks/usePoolDashboard.ts

import { useMemo } from 'react';
import { usePoolKanban, toSimplifiedStatus } from './usePoolKanban';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';

/**
 * 仪表板统计数据接口
 */
export interface DashboardStats {
  [SimplifiedPreArchiveStatus.PENDING_CHECK]: number;
  [SimplifiedPreArchiveStatus.NEEDS_ACTION]: number;
  [SimplifiedPreArchiveStatus.READY_TO_MATCH]: number;
  [SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]: number;
  [SimplifiedPreArchiveStatus.COMPLETED]: number;
}

/**
 * 仪表板统计 Hook
 *
 * 计算每个简化状态的凭证数量
 */
export function usePoolDashboard(): {
  stats: DashboardStats;
  totalCount: number;
  readyToArchiveCount: number;
  needsActionCount: number;
} {
  const { cards } = usePoolKanban();

  const stats = useMemo<DashboardStats>(() => {
    const counts: Record<string, number> = {
      [SimplifiedPreArchiveStatus.PENDING_CHECK]: 0,
      [SimplifiedPreArchiveStatus.NEEDS_ACTION]: 0,
      [SimplifiedPreArchiveStatus.READY_TO_MATCH]: 0,
      [SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]: 0,
      [SimplifiedPreArchiveStatus.COMPLETED]: 0,
    };

    // 遍历所有卡片，将旧状态映射到新状态并计数
    cards.forEach(card => {
      const simplifiedStatus = toSimplifiedStatus(card.status);
      counts[simplifiedStatus]++;
    });

    return counts as DashboardStats;
  }, [cards]);

  const totalCount = useMemo(() => {
    return Object.values(stats).reduce((sum, count) => sum + count, 0);
  }, [stats]);

  return {
    stats,
    totalCount,
    readyToArchiveCount: stats[SimplifiedPreArchiveStatus.READY_TO_ARCHIVE],
    needsActionCount: stats[SimplifiedPreArchiveStatus.NEEDS_ACTION],
  };
}
```

---

## 第 8 节：PoolPage 集成

**文件**: `src/pages/pre-archive/PoolPage.tsx`

```typescript
// 添加仪表板集成
import { PoolDashboard } from '@/components/pool-dashboard';
import { SimplifiedPreArchiveStatus, DEFAULT_DASHBOARD_FILTER } from '@/config/pool-columns.config';

export const PoolPage: React.FC = () => {
  // ... 现有状态 ...

  // 仪表板筛选状态
  const [dashboardFilter, setDashboardFilter] = useState<SimplifiedPreArchiveStatus | null>(
    DEFAULT_DASHBOARD_FILTER // 默认显示"可归档"
  );

  // 处理批量归档
  const handleBatchArchive = useCallback(() => {
    // TODO: 实现批量归档逻辑
    console.log('Batch archive for READY_TO_ARCHIVE items');
  }, []);

  return (
    <div className="pool-page">
      {/* ... 现有头部代码 ... */}

      {/* 仪表板区域 */}
      <PoolDashboard
        activeFilter={dashboardFilter}
        onFilterChange={setDashboardFilter}
        showActions={true}
        onBatchArchive={handleBatchArchive}
      />

      {/* 看板区域 - 添加 filter prop */}
      <div className="pool-page__content">
        {viewMode === 'kanban' ? (
          <PoolKanbanView filter={dashboardFilter} />
        ) : (
          <ArchiveListPage routeConfig="pool" statusFilter={dashboardFilter} />
        )}
      </div>
    </div>
  );
};
```

---

## 第 9 节：测试策略

### 9.1 单元测试

**usePoolDashboard Hook 测试**

**文件**: `src/hooks/__tests__/usePoolDashboard.test.ts`

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { usePoolDashboard } from '../usePoolDashboard';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';

describe('usePoolDashboard', () => {
  let queryClient: QueryClient;
  let wrapper: React.FC<{ children: React.ReactNode }>;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  });

  it('should calculate correct counts for each simplified status', async () => {
    // Mock: 返回包含旧状态的数据
    const { result } = renderHook(() => usePoolDashboard(), { wrapper });

    await waitFor(() => {
      expect(result.current.stats).toBeDefined();
    });

    // 验证状态映射正确
    expect(result.current.stats[SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]).toBeGreaterThanOrEqual(0);
  });

  it('should map old statuses to new statuses correctly', async () => {
    const { result } = renderHook(() => usePoolDashboard(), { wrapper });

    await waitFor(() => {
      expect(result.current.totalCount).toBeGreaterThan(0);
    });

    // 验证总数正确
    const sum = Object.values(result.current.stats).reduce((a, b) => a + b, 0);
    expect(sum).toBe(result.current.totalCount);
  });
});
```

### 9.2 组件测试

**DashboardCard 组件测试**

**文件**: `src/components/pool-dashboard/__tests__/DashboardCard.test.tsx`

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { DashboardCard } from '../DashboardCard';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';

describe('DashboardCard', () => {
  it('should render status count and label', () => {
    render(
      <DashboardCard
        status={SimplifiedPreArchiveStatus.READY_TO_ARCHIVE}
        count={42}
        isActive={false}
        onClick={() => {}}
      />
    );

    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('可归档')).toBeInTheDocument();
  });

  it('should apply active style when isActive is true', () => {
    const { container } = render(
      <DashboardCard
        status={SimplifiedPreArchiveStatus.READY_TO_ARCHIVE}
        count={42}
        isActive={true}
        onClick={() => {}}
      />
    );

    const card = container.querySelector('.dashboard-card--active');
    expect(card).toBeInTheDocument();
  });

  it('should call onClick when clicked', () => {
    const handleClick = vi.fn();
    const { container } = render(
      <DashboardCard
        status={SimplifiedPreArchiveStatus.READY_TO_ARCHIVE}
        count={42}
        isActive={false}
        onClick={handleClick}
      />
    );

    const card = container.querySelector('.dashboard-card');
    fireEvent.click(card!);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('should show action button when showAction is true', () => {
    render(
      <DashboardCard
        status={SimplifiedPreArchiveStatus.READY_TO_ARCHIVE}
        count={10}
        isActive={false}
        onClick={() => {}}
        showAction={true}
        actionLabel="批量归档 (10)"
      />
    );

    expect(screen.getByText('批量归档 (10)')).toBeInTheDocument();
  });
});
```

### 9.3 数据库迁移测试

**手动验证步骤**：

1. **迁移前备份**
   ```bash
   pg_dump -h localhost -U postgres -d nexusarchive > backup_before_v96.sql
   ```

2. **执行迁移**
   ```bash
   cd nexusarchive-java
   mvn flyway:migrate
   ```

3. **验证结果**
   ```sql
   -- 检查列结构
   \d acc_archive

   -- 检查状态分布
   SELECT pre_archive_status, COUNT(*)
   FROM acc_archive
   GROUP BY pre_archive_status;

   -- 预期：只有 5 个状态值
   ```

4. **回滚测试**（如需要）
   ```sql
   -- 手动回滚脚本（开发环境）
   ALTER TABLE acc_archive DROP COLUMN pre_archive_status;
   ALTER TABLE acc_archive ADD COLUMN pre_archive_status VARCHAR(20);
   -- ... 恢复数据
   ```

---

## 第 10 节：实施计划

### 10.1 实施顺序

| 阶段 | 任务 | 预计时间 |
|------|------|---------|
| 1 | 后端枚举修改 (PreArchiveStatus.java) | 30 分钟 |
| 2 | 数据库迁移脚本 (V96) | 45 分钟 |
| 3 | 后端服务类更新 | 60 分钟 |
| 4 | 前端配置更新 (pool-columns.config.ts) | 30 分钟 |
| 5 | Hook 修改 (usePoolKanban, usePoolDashboard) | 45 分钟 |
| 6 | 仪表板组件 (DashboardCard, PoolDashboard) | 90 分钟 |
| 7 | PoolPage 集成 | 30 分钟 |
| 8 | 测试编写 | 60 分钟 |
| 9 | 手动验证与修复 | 60 分钟 |

### 10.2 验收标准

- [ ] 数据库迁移成功，只有 5 个状态值
- [ ] 仪表板显示 5 个统计卡片
- [ ] 点击卡片可以筛选看板数据
- [ ] 默认聚焦"可归档"状态
- [ ] "可归档"卡片显示批量操作按钮
- [ ] 旧状态代码正确映射到新状态
- [ ] 所有单元测试通过
- [ ] 架构检查通过 (`npm run check:arch`)

---

## 附录

### A. 文件清单

**后端修改**：
- `nexusarchive-java/src/main/resources/db/migration/V96__simplify_pre_archive_status.sql` (新建)
- `nexusarchive-java/src/main/java/com/nexusarchive/entity/enums/PreArchiveStatus.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/PoolServiceImpl.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/PreArchiveSubmitService.java`
- 其他相关 Service 类

**前端修改**：
- `src/config/pool-columns.config.ts`
- `src/hooks/usePoolKanban.ts`
- `src/hooks/usePoolDashboard.ts` (新建)
- `src/components/pool-dashboard/DashboardCard.tsx` (新建)
- `src/components/pool-dashboard/DashboardCard.css` (新建)
- `src/components/pool-dashboard/PoolDashboard.tsx` (新建)
- `src/components/pool-dashboard/PoolDashboard.css` (新建)
- `src/components/pool-kanban/index.ts` (导出新组件)
- `src/pages/pre-archive/PoolPage.tsx`

**测试文件**：
- `src/hooks/__tests__/usePoolDashboard.test.ts` (新建)
- `src/components/pool-dashboard/__tests__/DashboardCard.test.tsx` (新建)
- `src/components/pool-dashboard/__tests__/PoolDashboard.test.tsx` (新建)

### B. 相关文档

- `docs/guides/功能模块.md` - 需要更新功能说明
- `docs/plans/2026-01-11-pool-dual-view-design.md` - 双视图设计文档
