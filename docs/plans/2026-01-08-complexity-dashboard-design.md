// Input: 复杂度仪表板与强制检查设计
// Output: 设计文档
// Pos: docs/plans/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

# 代码质量监控系统设计

**日期**: 2026-01-08
**状态**: 设计完成，待实施
**关联**: [复杂度规则](../architecture/complexity-rules.md)

---

## 一、概述

### 目标

基于现有的 ESLint 复杂度检查基础设施，构建完整的代码质量监控系统：

1. **强制检查** - Pre-commit hook 失败时阻止提交
2. **历史追踪** - 记录每次提交的复杂度快照
3. **可视化监控** - 独立的质量监控页面展示趋势和详情

### 设计约束

| 约束 | 说明 |
|------|------|
| 检查范围 | 仅 `src/**/*.{ts,tsx}`，跳过测试/配置/文档 |
| 数据存储 | 本地 JSON 文件 `docs/metrics/complexity-history.json` |
| 数据源 | Pre-commit 时预生成快照 |
| 模式 | 严格模式，失败直接阻止提交 |

---

## 二、架构设计

### 数据流

```
┌─────────────────────────────────────────────────────────────────┐
│                      开发者提交代码                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   pre-commit (阻塞式)                            │
│  1. 运行 ESLint 复杂度检查 (仅 src/**/*.{ts,tsx})               │
│  2. 失败 → 阻止提交，显示错误                                  │
│  3. 成功 → 生成快照 JSON                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              docs/metrics/complexity-history.json                │
│  累积历史数据，每次提交追加新记录                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  /system/quality 页面                            │
│  读取快照数据 → 渲染图表 + 详细列表                               │
└─────────────────────────────────────────────────────────────────┘
```

### 核心组件

| 组件 | 职责 | 位置 |
|------|------|------|
| `pre-commit-complexity-strict` | 阻塞式 pre-commit hook | `.husky/` |
| `complexity-snapshot.js` | 生成快照并写入历史 JSON | `scripts/` |
| `complexity-history.json` | 历史数据存储 | `docs/metrics/` |
| `QualityView.tsx` | 质量监控页面组件 | `src/pages/quality/` |
| `ComplexityChart.tsx` | 趋势图表组件 | `src/pages/quality/components/` |
| `ComplexityDetail.tsx` | 详细报告组件 | `src/pages/quality/components/` |

---

## 三、Pre-commit Hook 设计

### `.husky/pre-commit-complexity-strict`

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# 仅检查 src/ 目录的 TypeScript 文件
echo "🔍 Running complexity check (strict mode)..."

# 运行 ESLint 复杂度检查
npm run complexity:check -- --quiet --max-warnings=0

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Complexity check failed! Commit blocked."
    echo "   Run 'npm run complexity:report' for details."
    echo "   Fix issues or use 'git commit --no-verify' to bypass (not recommended)."
    exit 1
fi

# 检查通过，生成快照
echo "✅ Complexity check passed. Generating snapshot..."
node scripts/complexity-snapshot.js
```

### 快照脚本 `scripts/complexity-snapshot.js`

运行 ESLint 获取当前复杂度数据，追加到历史 JSON。

---

## 四、数据结构设计

### 历史数据格式

```json
{
  "metadata": {
    "formatVersion": "1.0",
    "createdAt": "2026-01-01T00:00:00Z",
    "lastUpdated": "2026-01-08T14:30:00Z"
  },
  "snapshots": [
    {
      "timestamp": "2026-01-08T14:30:00Z",
      "commit": "abc123",
      "branch": "main",
      "summary": {
        "total": 12,
        "high": 3,
        "medium": 7,
        "low": 2
      },
      "files": [
        {
          "path": "src/components/GlobalSearch.tsx",
          "lines": 134,
          "maxFunctionLines": 134,
          "complexity": 8,
          "violations": ["max-lines-per-function"]
        }
      ]
    }
  ]
}
```

### TypeScript 类型

```typescript
// src/pages/quality/types.ts
export interface ComplexitySnapshot {
    timestamp: string;
    commit: string;
    branch: string;
    summary: {
        total: number;
        high: number;
        medium: number;
        low: number;
    };
    files: FileViolation[];
}

export interface FileViolation {
    path: string;
    lines: number;
    maxFunctionLines: number;
    complexity: number;
    violations: string[];
}

export interface ComplexityHistory {
    metadata: {
        formatVersion: string;
        createdAt: string;
        lastUpdated: string;
    };
    snapshots: ComplexitySnapshot[];
}
```

---

## 五、质量监控页面设计

### 页面布局

```
┌──────────────────────────────────────────────────────────────────┐
│  代码质量监控                          [刷新] [导出报告]           │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  概览仪表板                                                 │ │
│  │  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐                  │ │
│  │  │  12  │  │   3  │  │   7  │  │   2  │  违规趋势图        │ │
│  │  │总违规│  │ 高   │  │ 中   │  │ 低   │  ┌─────────────┐  │ │
│  │  └──────┘  └──────┘  └──────┘  └──────┘  │   ╱╲    ╱╲  │  │ │
│  │                                            │  ╱  ╲  ╱  ╲ │  │ │
│  │  最严重文件 Top 5:                          │ ╱    ╲╱    ╲│  │ │
│  │  🔴 GlobalSearch.tsx        134 行          │             │  │ │
│  │  🟡 ProtectedRoute.tsx      61 行           └─────────────┘  │ │
│  │  🟡 api/client.ts           复杂度 15                        │ │
│  │  🟡 api/preview.ts          复杂度 17                        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  详细报告                                [筛选: 全部 ▼]     │ │
│  │                                                            │ │
│  │  文件名                    │ 行数  │ 复杂度 │ 违规项        │ │
│  │  ─────────────────────────────────────────────────────────  │ │
│  │  src/components/GlobalSearch.tsx    134    8   🔴 函数过长  │ │
│  │  src/auth/ProtectedRoute.tsx        61    12   🟡 函数过长  │ │
│  │  src/api/client.ts                 89    15   🟡 复杂度    │ │
│  │  ...                                      [展开全部]        │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### 路由集成

```typescript
// src/pages/system/viewConstants.ts
export const PATH_TO_VIEW: Record<string, ViewState> = {
    // ... 现有路由
    '/system/quality': ViewState.QUALITY,  // 新增
};
```

### 颜色规范

| 级别 | 阈值 | 颜色 | 图标 |
|------|------|------|------|
| High | 复杂度 >15 或 函数>100行 | `#ef4444` (red-500) | 🔴 |
| Medium | 复杂度 11-15 或 函数51-100行 | `#f59e0b` (amber-500) | 🟡 |
| Low | 复杂度 ≤10 或 函数≤50行 | `#22c55e` (green-500) | 🟢 |

---

## 六、组件设计

### 趋势图表 `ComplexityChart.tsx`

- 使用 Recharts（项目已依赖）绘制
- 显示最近 30 次提交的违规数趋势
- X轴：提交序号/日期
- Y轴：违规数量
- 多条线：总数(灰)、高严重度(红)、中严重度(黄)、低严重度(绿)

### 详细报告 `ComplexityDetail.tsx`

| 功能 | 实现方式 |
|------|----------|
| 排序 | 点击列头按行数/复杂度排序 |
| 筛选 | 下拉筛选：全部/高/中/低 |
| 搜索 | 按文件名模糊搜索 |
| 展开 | 点击行展开显示具体违规代码位置 |

### 数据 Hook

```typescript
// src/pages/quality/useComplexityData.ts
export const useComplexityData = () => {
    const [data, setData] = useState<ComplexityHistory | null>(null);
    // 从 docs/metrics/complexity-history.json 读取
    // 返回最新快照、趋势数据、文件列表
};
```

---

## 七、文件清单

### 新增文件

| 文件 | 行数估算 | 职责 |
|------|----------|------|
| `.husky/pre-commit-complexity-strict` | ~20 | 严格阻塞式 hook |
| `scripts/complexity-snapshot.js` | ~150 | 生成快照并追加历史 |
| `docs/metrics/complexity-history.json` | - | 历史数据（自动生成） |
| `docs/metrics/README.md` | ~50 | 指标目录说明 |
| `src/pages/quality/index.ts` | ~10 | 模块导出 |
| `src/pages/quality/QualityView.tsx` | ~120 | 质量监控主页面 |
| `src/pages/quality/types.ts` | ~40 | 类型定义 |
| `src/pages/quality/useComplexityData.ts` | ~60 | 数据读取 Hook |
| `src/pages/quality/components/ComplexityChart.tsx` | ~80 | 趋势图表 |
| `src/pages/quality/components/ComplexityDetail.tsx` | ~100 | 详细报告表格 |
| `src/pages/quality/README.md` | ~40 | 模块说明 |

### 修改文件

| 文件 | 变更 |
|------|------|
| `src/types/index.ts` | 添加 `ViewState.QUALITY` |
| `src/pages/system/viewConstants.ts` | 添加 QUALITY 路由映射 |
| `src/routes/index.tsx` | 添加 `/system/quality` 路由 |

---

## 八、实施顺序

1. **数据层** - 创建快照脚本、历史 JSON 结构
2. **Hook 层** - pre-commit 严格检查
3. **类型与数据** - types.ts、useComplexityData.ts
4. **组件** - Chart、Detail 组件
5. **页面** - QualityView 主页面
6. **集成** - 路由、导航菜单

---

## 九、验收标准

- [ ] Pre-commit 复杂度检查失败时阻止提交
- [ ] 每次成功提交后自动生成快照
- [ ] `/system/quality` 页面正常显示
- [ ] 概览仪表板显示总违规数和趋势图
- [ ] 详细报告支持排序、筛选、搜索
- [ ] 历史数据正确累积
