# 前端架构边界规则 - 团队培训文档

> **目标受众**: 前端开发团队成员  
> **培训时长**: 15-20 分钟  
> **前置要求**: 熟悉 React、TypeScript 基础

---

## 📌 为什么需要架构边界?

### 问题背景
在大型前端项目中，随着时间推移，代码会出现"边界腐蚀"：
- ❌ UI 组件直接调用 API
- ❌ 业务逻辑散落在各个组件
- ❌ 循环依赖导致难以重构

### 我们的解决方案
通过 ESLint 强制执行的分层架构，确保代码可演化。

---

## 🏛 三层架构模型

```
┌─────────────────────────────────────┐
│  components/archive|matching|...   │  页面/容器层
│  (可依赖 features/api/store)      │
└──────────────┬──────────────────────┘
               │ ✅ 允许
               ▼
┌─────────────────────────────────────┐
│  features/*                        │  业务逻辑层
│  (提供 hooks + 数据转换)           │
└──────────────┬──────────────────────┘
               │ ✅ 允许
               ▼
┌─────────────────────────────────────┐
│  api/* + store/*                   │  数据层
│  (HTTP 客户端 + 全局状态)          │
└─────────────────────────────────────┘

            ⬆ ❌ 禁止反向依赖
┌─────────────────────────────────────┐
│  components/common + layout        │  纯 UI 层
│  (仅依赖 types/utils/通用hooks)   │
└─────────────────────────────────────┘
```

---

## ✅ 规则速查表

| 层级 | 可以依赖 | 禁止依赖 |
|:---|:---|:---|
| `components/common`<br>`components/layout` | `types`, `utils`, 通用 hooks | `features/*`<br>`api/*`<br>`store/*` |
| `components/archive`<br>`components/matching`<br>`...其他页面` | `features/*`<br>`api/*`<br>`store/*`<br>`components/common` | _(无限制)_ |
| `features/*` | `api/*`<br>`store/*`<br>`utils` | `components/*` |

---

## 💡 常见场景示例

### 场景 1: 创建纯 UI 组件

**❌ 错误示例** (违反规则 A):
```tsx
// components/common/Button.tsx
import { useArchives } from '../../features/archives/hooks';

export function ArchiveButton() {
  const { archives } = useArchives(); // ❌ 纯 UI 层不能依赖 features
  return <button>{archives.length}</button>;
}
```

**✅ 正确示例**:
```tsx
// components/common/Button.tsx
interface ArchiveButtonProps {
  count: number; // ✅ 通过 props 传入数据
}

export function ArchiveButton({ count }: ArchiveButtonProps) {
  return <button>{count}</button>;
}
```

---

### 场景 2: 页面组件需要业务逻辑

**✅ 正确示例**:
```tsx
// components/archive/ArchiveListView.tsx
import { useArchiveList } from '../../features/archives/hooks'; // ✅ 页面层可以依赖 features
import { ArchiveTable } from '../common/ArchiveTable'; // ✅ 可以使用纯 UI 组件

export function ArchiveListView() {
  const { archives, loading, refresh } = useArchiveList();
  
  return <ArchiveTable data={archives} loading={loading} onRefresh={refresh} />;
}
```

---

### 场景 3: 抽取业务逻辑到 features

**❌ 错误示例** (胖组件):
```tsx
// components/archive/ArchiveForm.tsx
export function ArchiveForm() {
  const [data, setData] = useState([]);
  
  useEffect(() => {
    // ❌ 200+ 行复杂业务逻辑写在组件内
    fetch('/api/archives').then(/* ... */);
  }, []);
  
  const handleSubmit = () => {
    // ❌ 复杂验证逻辑
  };
  
  return <form>...</form>;
}
```

**✅ 正确示例**:
```tsx
// features/archives/hooks.ts
export function useArchiveForm() {
  const [data, setData] = useState([]);
  
  const loadData = async () => { /* ... */ };
  const validate = () => { /* ... */ };
  const submit = async () => { /* ... */ };
  
  return { data, loadData, validate, submit };
}

// components/archive/ArchiveForm.tsx
import { useArchiveForm } from '../../features/archives/hooks';

export function ArchiveForm() {
  const { data, submit } = useArchiveForm(); // ✅ 业务逻辑在 features
  return <form onSubmit={submit}>...</form>;
}
```

---

## 🔧 如何修复违规?

### 步骤 1: 运行 Lint
```bash
npm run lint
```

### 步骤 2: 查看错误
```
/src/components/common/Foo.tsx
  10:1  error  components/common 禁止依赖 features/* (违反架构边界规则 A)
```

### 步骤 3: 重构代码
| 违规类型 | 修复方案 |
|:---|:---|
| `common` 组件依赖 `api/*` | 通过 props 传入回调函数 |
| `common` 组件依赖 `features/*` | 提升数据到页面组件，通过 props 传递 |
| `features` 依赖 `components/*` | 移除 re-export，让路由直接导入组件 |
| 胖组件 | 抽取业务逻辑到 `features/*/hooks.ts` |

---

## 🎯 最佳实践

### DO ✅
1. **纯 UI 组件** - 只关注视觉呈现，通过 props 接收数据
2. **业务逻辑抽取** - 放在 `features/*/hooks.ts` 中
3. **合理分层** - 数据流从上往下（页面 → UI），事件从下往上（UI → 页面）

### DON'T ❌
1. **不要** 在 `components/common` 中使用 `useQuery`、`axios`
2. **不要** 在组件内编写复杂业务逻辑（超过 50 行）
3. **不要** 从 `features` 目录 re-export 组件

---

## ❓ 常见问题 (FAQ)

### Q1: 我可以在 `components/common` 使用 `useState` 吗?
**A**: ✅ 可以。局部 UI 状态（如 `isOpen`）是允许的，但不能依赖全局状态 (`store/*`)。

### Q2: 如果 UI 组件需要调用 API 怎么办?
**A**: 通过 props 传入回调函数。例如：
```tsx
interface ModalProps {
  onSave: (data: FormData) => Promise<void>; // ✅ 父组件提供
}
```

### Q3: `hooks/*` 和 `features/*` 有什么区别?
**A**:
- `hooks/*` - 通用技术 hooks (如 `useDebounce`, `useLocalStorage`)
- `features/*` - 领域业务逻辑 (如 `useArchiveList`, `useCompliance`)

### Q4: ESLint 报错但我确信没问题怎么办?
**A**: **不要轻易禁用规则**。优先考虑重构代码以符合架构。如果真的是边缘情况，在团队会议中讨论。

---

## 📚 延伸阅读
- [完整架构边界规则](file:///Users/user/nexusarchive/docs/architecture/frontend-boundaries.md)
- [ESLint 配置](file:///Users/user/nexusarchive/.eslintrc.cjs)

---

## 🤝 团队约定

1. **Code Review 重点**:
   - 检查新增组件是否放在正确目录
   - 验证是否有胖组件（超过 150 行）
   
2. **定期 Audit** (每季度):
   - 运行 `npm run lint`
   - 识别"胖组件"并重构

3. **新人 Onboarding**:
   - 阅读本文档
   - 完成一次"边界修复"练习

---

**记住**: 架构边界不是限制，而是让代码更容易理解和修改的护栏。
