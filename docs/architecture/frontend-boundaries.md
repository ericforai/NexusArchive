# 前端架构边界规则 (Frontend Architecture Boundaries)

> **生效日期**: 2025-12-26  
> **版本**: 1.0  
> **状态**: ✅ 强制执行

> **更新说明** (2025-xx-xx): 当前强制规则以 `docs/architecture/module-boundaries.md` 与 `.eslintrc.cjs` 为准；
> `components` 已收紧为禁止依赖 `features/pages/api/store`，页面仅能从模块入口引入。

## 一、现状诊断

### 当前目录结构
```
src/
├── components/          # 页面 + UI 组件（混合层）
│   ├── common/         # 纯 UI 组件
│   ├── layout/         # 布局组件
│   ├── archive/        # 档案域页面组件
│   ├── matching/       # 匹配域页面组件
│   ├── settings/       # 设置域页面组件
│   └── admin/          # 管理域页面组件
├── features/           # 领域逻辑层（hooks + 业务逻辑）
│   ├── archives/
│   ├── borrowing/
│   ├── compliance/
│   └── settings/
├── routes/             # 路由配置
├── store/              # Zustand 状态管理
├── api/                # Axios HTTP 客户端
└── hooks/              # 通用 hooks
```

### 核心问题
`components/` 目录同时承担了"页面层"和"UI层"职责，长期存在边界腐蚀风险。

---

## 二、强制依赖规则

### A. 纯 UI 层 (`components/common` + `components/layout`)

**职责**: 无状态、可复用的 UI 组件

**✅ 允许依赖**:
- `types.ts` - 类型定义
- `utils/*` - 工具函数
- `hooks/*` - 通用 hooks（非业务逻辑）
- 其他 `components/common` 内的组件

**❌ 禁止依赖**:
- `features/*` - 领域逻辑
- `api/*` - API 调用
- `store/*` - 全局状态
- `components/archive|matching|settings|admin` - 页面组件

**示例**:
```tsx
// ✅ 正确
import { Button } from '../common/Button';
import { formatDate } from '../../utils/date';

// ❌ 错误
import { useArchives } from '../../features/archives/hooks';
import { archiveApi } from '../../api/archive';
```

---

### B. 页面/容器层 (`components/archive|matching|settings|admin`)

**职责**: 页面级组件，负责组装 UI + 调用业务逻辑

**✅ 允许依赖**:
- `features/*` - 领域逻辑 hooks
- `api/*` - API 调用
- `store/*` - 全局状态
- `components/common` - 纯 UI 组件
- `components/layout` - 布局组件
- `types.ts`, `utils/*`, `hooks/*`

**❌ 禁止行为**:
- ⚠️ 在组件内编写复杂业务逻辑（必须抽取到 `features/*`）
- ⚠️ 直接在组件内编写数据转换逻辑（应在 hooks 或 utils 内）

**示例**:
```tsx
// ✅ 正确：页面组件调用 features 提供的业务逻辑
import { useArchiveList } from '../../features/archives/hooks';
import { ArchiveTable } from '../common/ArchiveTable';

export function ArchiveListView() {
  const { archives, loading, refresh } = useArchiveList();
  return <ArchiveTable data={archives} loading={loading} />;
}

// ❌ 错误：在组件内直接写业务逻辑
export function ArchiveListView() {
  const [data, setData] = useState([]);
  useEffect(() => {
    fetch('/api/archives').then(res => {
      const processed = res.data.map(/* 复杂转换 */);
      setData(processed);
    });
  }, []);
  return <ArchiveTable data={data} />;
}
```

---

### C. 领域逻辑层 (`features/*`)

**职责**: 封装领域业务逻辑、数据转换、状态管理

**✅ 允许依赖**:
- `api/*` - API 调用
- `store/*` - 全局状态
- `types.ts`, `utils/*`, `hooks/*`
- 其他 `features/*` 内的模块

**❌ 禁止依赖**:
- `components/*` - **严禁反向依赖页面/UI组件**

**当前违规情况**:
```typescript
// ❌ features/*/index.ts 中存在 re-export components 的情况
export { ArchiveListView } from '../../components/ArchiveListView';
```

**修复方案**: 
- `features/*/index.ts` 应仅暴露 hooks 和业务逻辑
- 页面组件应从 `components/*` 直接导入

---

## 三、合规性检查结果

### ✅ 已合规
- `components/common/` 未依赖 `features/*`、`api/*`、`store/*`

### ⚠️ 待优化
- **features/*/index.ts re-export 问题**:
  - `features/archives/index.ts` re-export 了 `ArchiveListView` 等页面组件
  - **建议**: 移除这些 re-export，仅暴露 hooks 和业务逻辑

---

## 四、执行建议

### 立即执行
1. **更新 `features/*/index.ts`**: 移除对 `components/*` 的 re-export
2. **ESLint 规则配置**: 添加 `no-restricted-imports` 规则

### 中长期优化
1. **组件审计**: 定期检查 `components/archive|matching` 等目录，确保不含"胖组件"
2. **架构演进**: 当页面组件过于复杂时，拆分为：
   - `features/` - 业务逻辑
   - `components/common/` - 可复用UI
   - `components/domain/` - 页面容器

---

## 五、ESLint 配置示例

```json
{
  "rules": {
    "no-restricted-imports": ["error", {
      "patterns": [
        {
          "group": ["**/features/**"],
          "message": "components/common 禁止依赖 features/*"
        },
        {
          "group": ["**/api/**"],
          "message": "components/common 禁止依赖 api/*"
        },
        {
          "group": ["**/components/**"],
          "message": "features/* 禁止反向依赖 components/*"
        }
      ]
    }]
  },
  "overrides": [
    {
      "files": ["src/components/common/**/*.{ts,tsx}"],
      "rules": {
        "no-restricted-imports": ["error", {
          "patterns": ["**/features/**", "**/api/**", "**/store/**"]
        }]
      }
    },
    {
      "files": ["src/features/**/*.{ts,tsx}"],
      "rules": {
        "no-restricted-imports": ["error", {
          "patterns": ["**/components/**"]
        }]
      }
    }
  ]
}
```

---

## 六、常见问题

### Q: 如果页面组件需要复用业务逻辑怎么办？
**A**: 将逻辑抽取到 `features/` 提供的自定义 hook 中。

### Q: 如果 UI 组件需要状态管理怎么办？
**A**: 
- **局部状态**: 使用 `useState`（允许）
- **全局状态**: 通过 props 传入，不直接依赖 `store/*`

### Q: features 和 hooks 的区别是什么?
**A**:
- `features/*` - 领域业务逻辑（如档案管理、借阅管理）
- `hooks/*` - 通用技术 hooks（如 useDebounce、useLocalStorage）
