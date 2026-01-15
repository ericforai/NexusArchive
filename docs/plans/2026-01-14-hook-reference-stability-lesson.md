# React Hook 引用稳定性问题 - 经验总结

**日期**: 2026-01-14
**严重程度**: 🔴 P0 - 导致多个页面完全无法使用
**影响范围**: 所有使用 `useArchiveListController` 的页面 (8+ 页面)

---

## 问题概述

### 症状
- 页面卡在"加载数据中..."状态
- 控制台日志疯狂增长，浏览器卡顿
- CPU 占用飙升

### 根本原因
**React Hook 返回值引用不稳定**，导致 `useEffect` 无限循环触发。

---

## 技术分析

### 错误代码示例

```javascript
// ❌ 错误：每次渲染都返回新对象引用
export function useArchiveData() {
    const [rows, setRows] = useState([]);
    const [isLoading, setIsLoading] = useState(false);

    return {
        rows,
        isLoading,
        setRows,
        setIsLoading,
        setPageInfo: () => {},    // 新函数！
        setCurrentPage: () => {}, // 新函数！
    }; // 新对象！每次渲染引用都不同
}

// ❌ 错误：依赖数组包含了不稳定的引用
useEffect(() => {
    // ... 数据加载逻辑
}, [setCurrentPage, loadPoolData]); // setCurrentPage 每次都是新引用！
```

### 循环触发链路

```
1. 组件渲染
   ↓
2. useArchiveData() 返回新对象 (新引用)
   ↓
3. useArchiveDataLoader 的 useEffect 检测到 setCurrentPage 引用变化
   ↓
4. 触发数据加载 → setIsLoading(true)
   ↓
5. 组件重新渲染
   ↓
6. 回到步骤 2 (无限循环)
```

---

## 修复方案

### 1. 使用 useMemo 稳定对象返回值

```javascript
// ✅ 正确：使用 useMemo 稳定返回值
import { useState, useMemo, useCallback } from 'react';

export function useArchiveData() {
    const [rows, setRows] = useState([]);
    const [isLoading, setIsLoading] = useState(false);

    // 使用 useCallback 稳定函数引用
    const setPageInfo = useCallback(() => {}, []);
    const setCurrentPage = useCallback(() => {}, []);

    // 使用 useMemo 稳定对象引用
    return useMemo(() => ({
        rows,
        isLoading,
        setRows,
        setIsLoading,
        setPageInfo,
        setCurrentPage,
    }), [rows, isLoading, setRows, setIsLoading, setPageInfo, setCurrentPage]);
}
```

### 2. 使用 useRef 绕过依赖检查

```javascript
// ✅ 正确：对于不需要触发 effect 的值，使用 ref
const depsRef = useRef({
    mode,
    query,
    page,
    // ...
});

// 同步更新 ref (不会触发 effect)
useEffect(() => {
    depsRef.current = { mode, query, page, ... };
});

// 在 useCallback 中使用 ref.current
const loadPoolData = useCallback(async () => {
    const { page, query } = depsRef.current; // 读取最新值
    // ...
}, []); // 空依赖数组，引用永远稳定
```

### 3. 移除不必要的依赖

```javascript
// ❌ 错误：refreshStats 依赖 isEnabled
const refreshStats = useCallback(async () => {
    if (!isEnabled) return; // isEnabled 变化导致函数重建
    // ...
}, [isEnabled]);

useEffect(() => {
    refreshStats();
}, [isEnabled, refreshStats]); // refreshStats 变化触发 effect

// ✅ 正确：使用 ref 存储 isEnabled
const isEnabledRef = useRef(isEnabled);
useEffect(() => {
    isEnabledRef.current = isEnabled;
}, [isEnabled]);

const refreshStats = useCallback(async () => {
    if (!isEnabledRef.current) return; // 从 ref 读取
    // ...
}, []); // 空依赖数组

useEffect(() => {
    refreshStats();
}, [isEnabled]); // 只依赖 isEnabled 的值变化
```

---

## 修复的文件清单

| 文件 | 修复内容 |
|------|---------|
| `src/features/archives/controllers/useArchiveData.ts` | 使用 useMemo + useCallback 稳定返回值 |
| `src/features/archives/controllers/useArchivePool.ts` | 使用 useRef + useMemo 稳定返回值 |
| `src/features/archives/controllers/useArchiveDataLoader.ts` | 使用 ref 存储依赖，避免循环依赖 |
| `src/features/archives/useArchiveListController.ts` | 使用 useMemo 稳定 controller 返回值 |

---

## 经验教训

### 1. Hook 返回值必须稳定

**原则**: 自定义 Hook 返回的对象、数组、函数，引用应该保持稳定。

```javascript
// ❌ 不好
function useData() {
    const [data, setData] = useState(null);
    return { data, setData }; // 每次都是新对象
}

// ✅ 好
function useData() {
    const [data, setData] = useState(null);
    return useMemo(() => ({ data, setData }), [data]);
}
```

### 2. useEffect 依赖数组要谨慎

**原则**: 只依赖真正需要触发 effect 的值，避免依赖频繁变化的引用。

```javascript
// ❌ 不好
useEffect(() => {
    loadData();
}, [loadData]); // loadData 可能每次都是新函数

// ✅ 好
useEffect(() => {
    loadData();
}, [dataId]); // 只依赖实际的数据 ID
```

### 3. 重构要验证引用稳定性

**原则**: 拆分 Hook 后，必须验证返回值的引用稳定性。

**检查清单**:
- [ ] 对象返回值是否用 useMemo 包装？
- [ ] 函数返回值是否用 useCallback 包装？
- [ ] useEffect 依赖数组是否包含可能变化的引用？
- [ ] 是否可以用 useRef 绕过依赖检查？

### 4. 使用 ESLint 规则辅助检测

```json
{
  "rules": {
    "react-hooks/exhaustive-deps": "warn",
    "react-hooks/rules-of-hooks": "error"
  }
}
```

### 5. 调试技巧

当遇到无限循环时：

```javascript
// 在可能变化的对象上添加日志
useEffect(() => {
    console.log('useArchiveData 返回值引用变化:', {
        rowsRef: rows === prevRows.current,
        timestamp: Date.now(),
    });
    prevRows.current = rows;
});
```

---

## 预防措施

### 1. 代码审查检查点

在审查自定义 Hook 时，检查：
- [ ] 返回值是否有 useMemo/useCallback 包装
- [ ] useEffect 依赖数组是否合理
- [ ] 是否有明显的循环触发路径

### 2. 单元测试覆盖

```javascript
describe('useArchiveData', () => {
    it('should return stable object references', () => {
        const { result, rerender } = renderHook(() => useArchiveData());
        const firstRef = result.current;

        rerender();
        const secondRef = result.current;

        expect(firstRef).toBe(secondRef); // 引用应该相同
    });
});
```

### 3. 架构原则

- **单一职责**: 一个 Hook 只做一件事
- **最小依赖**: Hook 之间的依赖要尽可能少
- **引用透明**: 相同的输入应该返回相同的引用

---

## 总结

这次 bug 是典型的**"重构引入的回归"**：
- 架构改进的目标是对的 (拆分巨型 Hook)
- 但忽略了 React Hook 的引用稳定性约束

**核心教训**: 在 React 中，**引用稳定性与代码正确性同等重要**。任何返回对象/函数的自定义 Hook，都必须考虑其引用的稳定性。

---

## 参考资料

- [React Hook FAQ - 为什么 useEffect 会在每次渲染时执行？](https://react.dev/reference/react/useEffect#why-is-my-effect-running-on-every-render)
- [UseCallback 和 UseMemo 完整指南](https://dmitripavlutin.com/react-usecallback-and-usememo/)
- [React Hook 性能优化最佳实践](https://www.patterns.dev/posts/react-patterns-memo-usememo-usecallback/)
