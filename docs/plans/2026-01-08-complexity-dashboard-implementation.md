# 代码质量监控系统实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 构建代码复杂度监控仪表板，包含阻塞式 pre-commit 检查和历史趋势可视化

**架构:** Pre-commit hook 运行 ESLint 复杂度检查 → 失败则阻止提交 → 成功则生成快照追加到本地 JSON → React 页面读取 JSON 渲染图表和详细报告

**技术栈:** ESLint 9、Recharts、React 19、TypeScript、Node.js

---

## 前置检查

### 验证现有基础设施

**Step 1: 确认 ESLint 复杂度配置存在**

运行: `cat .eslintrc.complexity.cjs | head -20`
Expected: 输出包含 `max-lines-per-function`、`complexity` 等规则的配置

**Step 2: 确认现有检查脚本**

运行: `cat scripts/complexity-report.sh | head -10`
Expected: 输出包含 ESLint 命令的脚本

**Step 3: 验证 Recharts 依赖**

运行: `grep recharts package.json`
Expected: `"recharts": "^2.x.x"` 已存在

---

## Task 1: 数据层 - 快照脚本

**Files:**
- Create: `scripts/complexity-snapshot.js`
- Create: `docs/metrics/complexity-history.json` (初始结构)

**Step 1: 创建快照脚本**

```javascript
// scripts/complexity-snapshot.js
// Input: ESLint 复杂度检查输出
// Output: 追加到 docs/metrics/complexity-history.json
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const HISTORY_FILE = path.join(__dirname, '../docs/metrics/complexity-history.json');
const SRC_DIR = path.join(__dirname, '../src');

/**
 * 解析 ESLint JSON 输出，提取复杂度违规
 */
function parseEslintOutput(eslintJson) {
    const results = JSON.parse(eslintJson);
    const files = [];
    let totalViolations = 0;
    let highSeverity = 0;
    let mediumSeverity = 0;
    let lowSeverity = 0;

    for (const result of results) {
        if (!result.filePath.startsWith(SRC_DIR)) continue;

        const fileViolations = [];
        let maxFunctionLines = 0;
        let maxComplexity = 0;

        for (const message of result.messages) {
            if (message.ruleId === 'max-lines-per-function') {
                const lines = parseInt(message.message.match(/(\d+) lines/)?.[1] || '0');
                maxFunctionLines = Math.max(maxFunctionLines, lines);
                fileViolations.push({
                    rule: 'max-lines-per-function',
                    line: message.line,
                    message: message.message
                });
            }
            if (message.ruleId === 'complexity') {
                const complexity = parseInt(message.message.match(/(\d+)/)?.[1] || '0');
                maxComplexity = Math.max(maxComplexity, complexity);
                fileViolations.push({
                    rule: 'complexity',
                    line: message.line,
                    message: message.message
                });
            }
        }

        if (fileViolations.length > 0) {
            const relativePath = path.relative(path.join(__dirname, '../'), result.filePath);
            const severity = getSeverity(maxFunctionLines, maxComplexity);

            files.push({
                path: relativePath,
                lines: result.source?.split('\n').length || 0,
                maxFunctionLines,
                complexity: maxComplexity,
                violations: fileViolations.map(v => v.rule)
            });

            totalViolations += fileViolations.length;
            if (severity === 'high') highSeverity++;
            else if (severity === 'medium') mediumSeverity++;
            else lowSeverity++;
        }
    }

    return { files, totalViolations, highSeverity, mediumSeverity, lowSeverity };
}

/**
 * 根据指标确定严重程度
 */
function getSeverity(maxFunctionLines, complexity) {
    if (complexity > 15 || maxFunctionLines > 100) return 'high';
    if (complexity > 10 || maxFunctionLines > 50) return 'medium';
    return 'low';
}

/**
 * 获取当前 Git 信息
 */
function getGitInfo() {
    try {
        const commit = execSync('git rev-parse --short HEAD', { encoding: 'utf-8' }).trim();
        const branch = execSync('git rev-parse --abbrev-ref HEAD', { encoding: 'utf-8' }).trim();
        return { commit, branch };
    } catch {
        return { commit: 'unknown', branch: 'unknown' };
    }
}

/**
 * 主函数
 */
function main() {
    // 运行 ESLint 获取 JSON 输出
    const eslintCmd = 'npx eslint src --ext .ts,.tsx -c .eslintrc.complexity.cjs --format json';
    let eslintOutput;
    try {
        eslintOutput = execSync(eslintCmd, { encoding: 'utf-8' });
    } catch (error) {
        // ESLint 返回非零退出码但仍输出 JSON
        eslintOutput = error.stdout;
    }

    const { files, totalViolations, highSeverity, mediumSeverity, lowSeverity } = parseEslintOutput(eslintOutput);

    // 读取现有历史
    let history = { metadata: {}, snapshots: [] };
    if (fs.existsSync(HISTORY_FILE)) {
        history = JSON.parse(fs.readFileSync(HISTORY_FILE, 'utf-8'));
    }

    // 初始化 metadata
    if (!history.metadata.createdAt) {
        history.metadata = {
            formatVersion: '1.0',
            createdAt: new Date().toISOString(),
            lastUpdated: new Date().toISOString()
        };
    } else {
        history.metadata.lastUpdated = new Date().toISOString();
    }

    // 创建新快照
    const { commit, branch } = getGitInfo();
    const snapshot = {
        timestamp: new Date().toISOString(),
        commit,
        branch,
        summary: {
            total: totalViolations,
            high: highSeverity,
            medium: mediumSeverity,
            low: lowSeverity
        },
        files
    };

    // 追加快照
    history.snapshots.push(snapshot);

    // 保留最近 100 条快照
    if (history.snapshots.length > 100) {
        history.snapshots = history.snapshots.slice(-100);
    }

    // 写入文件
    fs.writeFileSync(HISTORY_FILE, JSON.stringify(history, null, 2));
    console.log(`✅ Complexity snapshot saved: ${totalViolations} violations`);
}

main();
```

**Step 2: 测试快照脚本**

运行: `node scripts/complexity-snapshot.js`
Expected: `✅ Complexity snapshot saved: X violations`

运行: `cat docs/metrics/complexity-history.json | head -30`
Expected: JSON 格式包含 `metadata` 和 `snapshots` 数组

**Step 3: 提交**

```bash
git add scripts/complexity-snapshot.js docs/metrics/complexity-history.json
git commit -m "feat: add complexity snapshot script"
```

---

## Task 2: Pre-commit 严格检查

**Files:**
- Create: `.husky/pre-commit-complexity-strict`

**Step 1: 创建严格检查 hook**

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

echo "🔍 Running complexity check (strict mode)..."

# 运行 ESLint 复杂度检查，仅 src/ 目录
npm run complexity:check -- --quiet --max-warnings=0

RESULT=$?

if [ $RESULT -ne 0 ]; then
    echo ""
    echo "❌ Complexity check failed! Commit blocked."
    echo ""
    echo "Run 'npm run complexity:report' for detailed report."
    echo "Fix issues before committing, or use 'git commit --no-verify' to bypass."
    exit 1
fi

echo "✅ Complexity check passed. Generating snapshot..."
node scripts/complexity-snapshot.js
```

**Step 2: 设置可执行权限**

运行: `chmod +x .husky/pre-commit-complexity-strict`

**Step 3: 测试 hook（故意创建违规文件）**

创建测试文件:
```typescript
// src/test-complexity.ts
export const testFunction = () => {
    let a = 1;
    let b = 2;
    let c = 3;
    let d = 4;
    let e = 5;
    let f = 6;
    let g = 7;
    let h = 8;
    let i = 9;
    let j = 10;
    if (a === 1) {
        if (b === 2) {
            if (c === 3) {
                if (d === 4) {
                    if (e === 5) {
                        return f;
                    }
                }
            }
        }
    }
    return 0;
};
```

运行: `git add src/test-complexity.ts && git commit -m "test: should fail"`

Expected: `❌ Complexity check failed! Commit blocked.`

**Step 4: 清理测试文件**

运行: `git restore --staged src/test-complexity.ts && rm src/test-complexity.ts`

**Step 5: 提交**

```bash
git add .husky/pre-commit-complexity-strict
git commit -m "feat: add strict complexity pre-commit hook"
```

---

## Task 3: TypeScript 类型定义

**Files:**
- Create: `src/pages/quality/types.ts`
- Create: `src/pages/quality/index.ts`
- Create: `src/pages/quality/README.md`

**Step 1: 创建类型文件**

```typescript
// src/pages/quality/types.ts
// Input: 复杂度快照数据结构
// Output: TypeScript 类型定义
// Pos: src/pages/quality/ 类型定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 文件违规详情
 */
export interface FileViolation {
    /** 文件路径（相对于项目根目录） */
    path: string;
    /** 文件总行数 */
    lines: number;
    /** 最大函数行数 */
    maxFunctionLines: number;
    /** 圈复杂度 */
    complexity: number;
    /** 违规规则列表 */
    violations: string[];
}

/**
 * 快照摘要
 */
export interface SnapshotSummary {
    /** 总违规数 */
    total: number;
    /** 高严重度违规数 */
    high: number;
    /** 中严重度违规数 */
    medium: number;
    /** 低严重度违规数 */
    low: number;
}

/**
 * 复杂度快照
 */
export interface ComplexitySnapshot {
    /** 快照时间戳 */
    timestamp: string;
    /** Git commit hash */
    commit: string;
    /** Git 分支名 */
    branch: string;
    /** 违规摘要 */
    summary: SnapshotSummary;
    /** 违规文件列表 */
    files: FileViolation[];
}

/**
 * 历史元数据
 */
export interface HistoryMetadata {
    /** 格式版本 */
    formatVersion: string;
    /** 创建时间 */
    createdAt: string;
    /** 最后更新时间 */
    lastUpdated: string;
}

/**
 * 完整历史数据
 */
export interface ComplexityHistory {
    /** 元数据 */
    metadata: HistoryMetadata;
    /** 快照数组 */
    snapshots: ComplexitySnapshot[];
}

/**
 * 严重程度
 */
export type SeverityLevel = 'high' | 'medium' | 'low';

/**
 * 路由状态（扩展 ViewState）
 */
export enum QualityViewState {
    DASHBOARD = 'dashboard',
    DETAILS = 'details'
}
```

**Step 2: 创建模块入口**

```typescript
// src/pages/quality/index.ts
// Input: 模块子文件
// Output: 统一导出接口
// Pos: src/pages/quality/ 模块入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export * from './types';
export * from './useComplexityData';
export * from './QualityView';
export * from './components/ComplexityChart';
export * from './components/ComplexityDetail';
```

**Step 3: 创建模块 README**

```markdown
// Input: 无
// Output: 质量监控模块说明
// Pos: src/pages/quality/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

本目录存放代码质量监控相关模块。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `index.ts` | 模块入口 | 统一导出接口 |
| `types.ts` | 类型定义 | 复杂度数据类型 |
| `useComplexityData.ts` | Hook | 复杂度数据读取 |
| `QualityView.tsx` | 页面 | 质量监控主页面 |
| `components/` | 组件目录 | 图表和详情组件 |

## 重构说明

本模块为新增功能，用于可视化展示代码复杂度趋势和详细报告。
```

**Step 4: 提交**

```bash
git add src/pages/quality/
git commit -m "feat: add quality module types and structure"
```

---

## Task 4: 数据读取 Hook

**Files:**
- Create: `src/pages/quality/useComplexityData.ts`

**Step 1: 创建数据 Hook**

```typescript
// src/pages/quality/useComplexityData.ts
// Input: complexity-history.json
// Output: 复杂度数据 Hook
// Pos: src/pages/quality/ 数据读取
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useEffect } from 'react';
import type { ComplexityHistory, ComplexitySnapshot, FileViolation } from './types';

const HISTORY_URL = '/docs/metrics/complexity-history.json';

/**
 * 复杂度数据 Hook
 * 读取并处理历史快照数据
 */
export const useComplexityData = () => {
    const [data, setData] = useState<ComplexityHistory | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const response = await fetch(HISTORY_URL);
                if (!response.ok) {
                    throw new Error('Failed to load complexity history');
                }
                const history: ComplexityHistory = await response.json();
                setData(history);
                setError(null);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Unknown error');
                console.error('[useComplexityData] Failed to load data:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    /**
     * 获取最新快照
     */
    const latestSnapshot = data?.snapshots[data.snapshots.length - 1] || null;

    /**
     * 获取最近 N 条快照
     */
    const getRecentSnapshots = (count: number): ComplexitySnapshot[] => {
        if (!data) return [];
        return data.snapshots.slice(-count);
    };

    /**
     * 获取所有违规文件
     */
    const getAllViolations = (): FileViolation[] => {
        if (!latestSnapshot) return [];
        return [...latestSnapshot.files].sort((a, b) => {
            // 按严重程度排序
            const severityOrder = { high: 0, medium: 1, low: 2 };
            const aSeverity = getSeverityLevel(a);
            const bSeverity = getSeverityLevel(b);
            if (severityOrder[aSeverity] !== severityOrder[bSeverity]) {
                return severityOrder[aSeverity] - severityOrder[bSeverity];
            }
            // 相同严重程度按复杂度排序
            return b.complexity - a.complexity;
        });
    };

    /**
     * 按严重程度筛选
     */
    const getBySeverity = (severity: 'high' | 'medium' | 'low'): FileViolation[] => {
        return getAllViolations().filter(file => getSeverityLevel(file) === severity);
    };

    return {
        data,
        loading,
        error,
        latestSnapshot,
        getRecentSnapshots,
        getAllViolations,
        getBySeverity
    };
};

/**
 * 获取文件严重程度
 */
function getSeverityLevel(file: FileViolation): 'high' | 'medium' | 'low' {
    if (file.complexity > 15 || file.maxFunctionLines > 100) return 'high';
    if (file.complexity > 10 || file.maxFunctionLines > 50) return 'medium';
    return 'low';
}
```

**Step 2: 确保 Vite 配置允许访问 docs 目录**

检查 `vite.config.ts` 包含:
```typescript
server: {
    fs: {
        allow: ['..'], // 允许访问父级目录
    },
},
```

运行: `grep -A3 "fs:" vite.config.ts`
Expected: 输出包含 `allow: ['..']` 或类似配置

如果没有，需要添加。

**Step 3: 提交**

```bash
git add src/pages/quality/useComplexityData.ts
git commit -m "feat: add useComplexityData hook"
```

---

## Task 5: 趋势图表组件

**Files:**
- Create: `src/pages/quality/components/ComplexityChart.tsx`
- Create: `src/pages/quality/components/index.ts`

**Step 1: 创建图表组件**

```typescript
// src/pages/quality/components/ComplexityChart.tsx
// Input: Recharts、复杂度快照数据
// Output: 趋势图表组件
// Pos: src/pages/quality/components/ 图表组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';
import type { ComplexitySnapshot } from '../types';

interface ComplexityChartProps {
    /** 快照数据 */
    snapshots: ComplexitySnapshot[];
}

const COLORS = {
    total: '#94a3b8',   // slate-400
    high: '#ef4444',    // red-500
    medium: '#f59e0b',  // amber-500
    low: '#22c55e'      // green-500
};

/**
 * 复杂度趋势图表
 */
export const ComplexityChart: React.FC<ComplexityChartProps> = ({ snapshots }) => {
    // 转换数据为图表格式
    const chartData = snapshots.map((snapshot, index) => ({
        index: index + 1,
        date: new Date(snapshot.timestamp).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }),
        total: snapshot.summary.total,
        high: snapshot.summary.high,
        medium: snapshot.summary.medium,
        low: snapshot.summary.low
    }));

    if (chartData.length === 0) {
        return (
            <div className="flex items-center justify-center h-64 text-slate-400">
                暂无历史数据
            </div>
        );
    }

    return (
        <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="index"
                    tickFormatter={(value) => `#${value}`}
                    stroke="#64748b"
                />
                <YAxis stroke="#64748b" />
                <Tooltip
                    contentStyle={{
                        backgroundColor: 'white',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px'
                    }}
                    labelFormatter={(value) => `提交 #${value}`}
                />
                <Legend />
                <Line
                    type="monotone"
                    dataKey="total"
                    stroke={COLORS.total}
                    name="总违规"
                    strokeWidth={2}
                    dot={false}
                />
                <Line
                    type="monotone"
                    dataKey="high"
                    stroke={COLORS.high}
                    name="高严重度"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                />
                <Line
                    type="monotone"
                    dataKey="medium"
                    stroke={COLORS.medium}
                    name="中严重度"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                />
                <Line
                    type="monotone"
                    dataKey="low"
                    stroke={COLORS.low}
                    name="低严重度"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                />
            </LineChart>
        </ResponsiveContainer>
    );
};
```

**Step 2: 创建组件入口**

```typescript
// src/pages/quality/components/index.ts
// Input: 子组件
// Output: 统一导出
// Pos: src/pages/quality/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export * from './ComplexityChart';
export * from './ComplexityDetail';
```

**Step 3: 测试组件渲染（手动验证）**

先创建占位的 Detail 组件:
```typescript
// src/pages/quality/components/ComplexityDetail.tsx
// Placeholder - Task 6 实现完整组件
import React from 'react';
export const ComplexityDetail: React.FC = () => <div>Details coming soon</div>;
```

**Step 4: 提交**

```bash
git add src/pages/quality/components/
git commit -m "feat: add complexity trend chart component"
```

---

## Task 6: 详细报告组件

**Files:**
- Modify: `src/pages/quality/components/ComplexityDetail.tsx`

**Step 1: 实现详细报告组件**

```typescript
// src/pages/quality/components/ComplexityDetail.tsx
// Input: 文件违规列表
// Output: 详细报告表格组件
// Pos: src/pages/quality/components/ 详情组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useMemo } from 'react';
import type { FileViolation } from '../types';

interface ComplexityDetailProps {
    /** 违规文件列表 */
    violations: FileViolation[];
}

type SortField = 'path' | 'lines' | 'maxFunctionLines' | 'complexity';
type FilterType = 'all' | 'high' | 'medium' | 'low';

/**
 * 获取严重程度
 */
const getSeverity = (file: FileViolation): 'high' | 'medium' | 'low' => {
    if (file.complexity > 15 || file.maxFunctionLines > 100) return 'high';
    if (file.complexity > 10 || file.maxFunctionLines > 50) return 'medium';
    return 'low';
};

/**
 * 严重程度颜色映射
 */
const severityColor = {
    high: 'text-red-500',
    medium: 'text-amber-500',
    low: 'text-green-500'
};

const severityBg = {
    high: 'bg-red-50',
    medium: 'bg-amber-50',
    low: 'bg-green-50'
};

const severityIcon = {
    high: '🔴',
    medium: '🟡',
    low: '🟢'
};

/**
 * 详细报告表格组件
 */
export const ComplexityDetail: React.FC<ComplexityDetailProps> = ({ violations }) => {
    const [sortField, setSortField] = useState<SortField>('complexity');
    const [sortAsc, setSortAsc] = useState(false);
    const [filter, setFilter] = useState<FilterType>('all');
    const [searchQuery, setSearchQuery] = useState('');

    /** 筛选和排序后的数据 */
    const filteredData = useMemo(() => {
        let result = [...violations];

        // 筛选
        if (filter !== 'all') {
            result = result.filter(v => getSeverity(v) === filter);
        }

        // 搜索
        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            result = result.filter(v => v.path.toLowerCase().includes(query));
        }

        // 排序
        result.sort((a, b) => {
            const aVal = a[sortField];
            const bVal = b[sortField];
            const comparison = aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
            return sortAsc ? comparison : -comparison;
        });

        return result;
    }, [violations, sortField, sortAsc, filter, searchQuery]);

    /** 切换排序 */
    const toggleSort = (field: SortField) => {
        if (sortField === field) {
            setSortAsc(!sortAsc);
        } else {
            setSortField(field);
            setSortAsc(false);
        }
    };

    /** 获取排序图标 */
    const getSortIcon = (field: SortField) => {
        if (sortField !== field) return '⇅';
        return sortAsc ? '↑' : '↓';
    };

    if (violations.length === 0) {
        return (
            <div className="flex items-center justify-center h-64 text-slate-400">
                暂无违规数据
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {/* 工具栏 */}
            <div className="flex flex-wrap gap-4 items-center">
                <div className="flex items-center gap-2">
                    <label className="text-sm text-slate-600">筛选:</label>
                    <select
                        value={filter}
                        onChange={(e) => setFilter(e.target.value as FilterType)}
                        className="border border-slate-300 rounded px-3 py-1.5 text-sm"
                    >
                        <option value="all">全部 ({violations.length})</option>
                        <option value="high">高严重度</option>
                        <option value="medium">中严重度</option>
                        <option value="low">低严重度</option>
                    </select>
                </div>
                <div className="flex items-center gap-2">
                    <label className="text-sm text-slate-600">搜索:</label>
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="文件名..."
                        className="border border-slate-300 rounded px-3 py-1.5 text-sm w-48"
                    />
                </div>
                <div className="ml-auto text-sm text-slate-500">
                    显示 {filteredData.length} / {violations.length} 条
                </div>
            </div>

            {/* 表格 */}
            <div className="border border-slate-200 rounded-lg overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-slate-50 border-b border-slate-200">
                        <tr>
                            <th
                                className="px-4 py-3 text-left cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('path')}
                            >
                                文件名 {getSortIcon('path')}
                            </th>
                            <th
                                className="px-4 py-3 text-right cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('lines')}
                            >
                                行数 {getSortIcon('lines')}
                            </th>
                            <th
                                className="px-4 py-3 text-right cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('maxFunctionLines')}
                            >
                                最大函数 {getSortIcon('maxFunctionLines')}
                            </th>
                            <th
                                className="px-4 py-3 text-right cursor-pointer hover:bg-slate-100"
                                onClick={() => toggleSort('complexity')}
                            >
                                复杂度 {getSortIcon('complexity')}
                            </th>
                            <th className="px-4 py-3 text-left">违规项</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredData.map((file, index) => {
                            const severity = getSeverity(file);
                            return (
                                <tr
                                    key={file.path}
                                    className={index % 2 === 0 ? 'bg-white' : 'bg-slate-50'}
                                >
                                    <td className="px-4 py-3 font-mono text-xs truncate max-w-md">
                                        {file.path}
                                    </td>
                                    <td className="px-4 py-3 text-right text-slate-600">
                                        {file.lines}
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        <span className={severityColor[severity]}>
                                            {file.maxFunctionLines}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        <span className={severityColor[severity]}>
                                            {file.complexity}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3">
                                        {file.violations.map(v => (
                                            <span
                                                key={v}
                                                className={`inline-block px-2 py-0.5 rounded text-xs mr-1 ${severityBg[severity]} ${severityColor[severity]}`}
                                            >
                                                {severityIcon[severity]} {v}
                                            </span>
                                        ))}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};
```

**Step 2: 更新组件导出**

确认 `src/pages/quality/components/index.ts` 包含:
```typescript
export * from './ComplexityChart';
export * from './ComplexityDetail';
```

**Step 3: 提交**

```bash
git add src/pages/quality/components/ComplexityDetail.tsx
git commit -m "feat: add complexity detail table component"
```

---

## Task 7: 主质量监控页面

**Files:**
- Create: `src/pages/quality/QualityView.tsx`

**Step 1: 创建主页面组件**

```typescript
// src/pages/quality/QualityView.tsx
// Input: 复杂度数据、图表和详情组件
// Output: 质量监控主页面
// Pos: src/pages/quality/ 主页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { RefreshCw, Download } from 'lucide-react';
import { useComplexityData } from './useComplexityData';
import { ComplexityChart } from './components/ComplexityChart';
import { ComplexityDetail } from './components/ComplexityDetail';

const COLORS = {
    high: 'text-red-500 bg-red-50 border-red-200',
    medium: 'text-amber-500 bg-amber-50 border-amber-200',
    low: 'text-green-500 bg-green-50 border-green-200'
};

/**
 * 代码质量监控页面
 */
export const QualityView: React.FC = () => {
    const {
        data,
        loading,
        error,
        latestSnapshot,
        getRecentSnapshots,
        getAllViolations
    } = useComplexityData();

    const [refreshKey, setRefreshKey] = useState(0);

    /** 刷新数据 */
    const handleRefresh = () => {
        setRefreshKey(prev => prev + 1);
        window.location.reload();
    };

    /** 导出报告 */
    const handleExport = () => {
        if (!data) return;
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `complexity-report-${new Date().toISOString().split('T')[0]}.json`;
        a.click();
        URL.revokeObjectURL(url);
    };

    // 加载状态
    if (loading) {
        return (
            <div className="flex items-center justify-center h-96">
                <div className="text-slate-500">加载中...</div>
            </div>
        );
    }

    // 错误状态
    if (error) {
        return (
            <div className="flex flex-col items-center justify-center h-96 gap-4">
                <div className="text-red-500">加载失败: {error}</div>
                <button
                    onClick={handleRefresh}
                    className="px-4 py-2 bg-slate-100 rounded hover:bg-slate-200"
                >
                    重试
                </button>
            </div>
        );
    }

    // 无数据状态
    if (!latestSnapshot) {
        return (
            <div className="flex flex-col items-center justify-center h-96 gap-4 text-slate-500">
                <div>暂无复杂度数据</div>
                <div className="text-sm">提交代码后会自动生成快照</div>
            </div>
        );
    }

    const violations = getAllViolations();
    const recentSnapshots = getRecentSnapshots(30);

    return (
        <div className="p-6 space-y-6 bg-white min-h-screen">
            {/* 页头 */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">代码质量监控</h1>
                    <p className="text-slate-500 text-sm mt-1">
                        最后更新: {new Date(latestSnapshot.timestamp).toLocaleString('zh-CN')}
                    </p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={handleRefresh}
                        className="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded hover:bg-slate-50"
                    >
                        <RefreshCw size={16} />
                        刷新
                    </button>
                    <button
                        onClick={handleExport}
                        className="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded hover:bg-slate-50"
                    >
                        <Download size={16} />
                        导出报告
                    </button>
                </div>
            </div>

            {/* 概览仪表板 */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 统计卡片 */}
                <div className="space-y-4">
                    {/* 总违规 */}
                    <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
                        <div className="text-sm text-slate-500 mb-1">总违规数</div>
                        <div className="text-3xl font-bold text-slate-900">
                            {latestSnapshot.summary.total}
                        </div>
                    </div>

                    {/* 高严重度 */}
                    <div className={`border rounded-lg p-4 ${COLORS.high}`}>
                        <div className="text-sm opacity-80 mb-1">高严重度</div>
                        <div className="text-3xl font-bold">
                            {latestSnapshot.summary.high}
                        </div>
                    </div>

                    {/* 中严重度 */}
                    <div className={`border rounded-lg p-4 ${COLORS.medium}`}>
                        <div className="text-sm opacity-80 mb-1">中严重度</div>
                        <div className="text-3xl font-bold">
                            {latestSnapshot.summary.medium}
                        </div>
                    </div>

                    {/* 低严重度 */}
                    <div className={`border rounded-lg p-4 ${COLORS.low}`}>
                        <div className="text-sm opacity-80 mb-1">低严重度</div>
                        <div className="text-3xl font-bold">
                            {latestSnapshot.summary.low}
                        </div>
                    </div>
                </div>

                {/* 趋势图表 */}
                <div className="lg:col-span-2 bg-white border border-slate-200 rounded-lg p-4">
                    <h2 className="text-lg font-semibold text-slate-900 mb-4">违规趋势</h2>
                    <ComplexityChart snapshots={recentSnapshots} />
                </div>
            </div>

            {/* Top 5 最严重文件 */}
            {violations.length > 0 && (
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
                    <h2 className="text-lg font-semibold text-slate-900 mb-3">最严重文件 Top 5</h2>
                    <div className="space-y-2">
                        {violations.slice(0, 5).map((file, index) => {
                            const severity = file.complexity > 15 || file.maxFunctionLines > 100 ? 'high' : 'medium';
                            const icon = severity === 'high' ? '🔴' : '🟡';
                            return (
                                <div
                                    key={file.path}
                                    className="flex items-center justify-between bg-white border border-slate-200 rounded p-3"
                                >
                                    <div className="flex items-center gap-3">
                                        <span className="text-lg">{icon}</span>
                                        <span className="font-mono text-sm">{file.path}</span>
                                    </div>
                                    <div className="text-sm text-slate-600">
                                        {file.maxFunctionLines > 50 ? `${file.maxFunctionLines} 行` : `复杂度 ${file.complexity}`}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            {/* 详细报告 */}
            <div className="bg-white border border-slate-200 rounded-lg p-4">
                <h2 className="text-lg font-semibold text-slate-900 mb-4">详细报告</h2>
                <ComplexityDetail violations={violations} />
            </div>
        </div>
    );
};
```

**Step 2: 更新模块导出**

确认 `src/pages/quality/index.ts` 包含 `QualityView`:
```typescript
export * from './QualityView';
```

**Step 3: 提交**

```bash
git add src/pages/quality/QualityView.tsx
git commit -m "feat: add quality monitoring main page"
```

---

## Task 8: 路由集成

**Files:**
- Modify: `src/types/index.ts`
- Modify: `src/pages/system/viewConstants.ts`
- Modify: `src/routes/index.tsx`

**Step 1: 添加 ViewState 枚举**

查找 `ViewState` 定义位置:
运行: `grep -n "enum ViewState" src/types/*.ts`

在 `ViewState` 枚举中添加:
```typescript
QUALITY = 'quality',
```

**Step 2: 添加路由映射**

在 `src/pages/system/viewConstants.ts` 的 `PATH_TO_VIEW` 中添加:
```typescript
'/system/quality': ViewState.QUALITY,
```

**Step 3: 添加路由**

在 `src/routes/index.tsx` 中添加路由:
```typescript
{
    path: '/system/quality',
    element: <QualityView />,
}
```

**Step 4: 添加导航菜单项**

查找 Sidebar 组件的菜单定义，添加"代码质量"菜单项。

**Step 5: 测试路由**

运行: `npm run dev`
访问: `http://localhost:15175/system/quality`

Expected: 质量监控页面正常显示

**Step 6: 提交**

```bash
git add src/types/index.ts src/pages/system/viewConstants.ts src/routes/index.tsx
git commit -m "feat: integrate quality monitoring route"
```

---

## Task 9: 验收测试

**Step 1: 完整流程测试**

1. 创建测试文件触发复杂度违规
2. 尝试提交，验证 pre-commit 阻止
3. 修复违规，验证提交成功
4. 验证快照生成
5. 访问质量页面，验证数据显示

**Step 2: 边界测试**

1. 空数据状态
2. 大量数据（100+ 快照）性能
3. 筛选和排序功能

**Step 3: 文档检查**

1. `docs/metrics/README.md` 存在且正确
2. `src/pages/quality/README.md` 存在且正确
3. 所有文件有 4 行头注释

**Step 4: 最终提交**

```bash
git add .
git commit -m "docs: update documentation for quality monitoring feature"
```

---

## 附录：故障排查

| 问题 | 解决方案 |
|------|----------|
| Fetch 404 错误 | 检查 Vite `fs.allow` 配置 |
| Hook 未执行 | 检查 `.husky/pre-commit-complexity-strict` 可执行权限 |
| 图表不显示 | 确认 Recharts 依赖已安装 |
| TypeScript 错误 | 检查类型导入路径 |

---

**实施完成后，更新 `docs/plans/2026-01-08-complexity-dashboard-design.md` 的验收状态。**
