# 预归档池列表页 Bug 复盘

**日期**: 2026-01-22
**Issue**: 预归档单据池列表页显示数量不一致、分页不工作、状态切换失效

## 一、问题表象

| 症状 | 用户描述 |
|------|----------|
| Dashboard 数字错误 | 显示 "95-0-0-0-0"（待检测95，其他都是0） |
| 状态切换无效 | 切换状态后列表不更新，始终显示可归档的数据 |
| 分页不工作 | 点击"下一页"页脚变了但列表没变化 |
| 不同状态表现不一 | 待检测可翻页，已完成状态不能翻页 |

## 二、根因分析

### 2.1 状态映射缺失

**文件**: `src/hooks/usePoolKanban.ts`

```typescript
// ❌ 问题：缺少数据库实际使用的状态映射
export const STATUS_SIMPLIFICATION_MAP = {
  'COMPLETED': SimplifiedPreArchiveStatus.COMPLETED,  // 缺失！
  'READY_TO_ARCHIVE': SimplifiedPreArchiveStatus.READY_TO_ARCHIVE,  // 缺失！
};

// 结果：toSimplifiedStatus('COMPLETED') 默认返回 PENDING_CHECK
// 导致 Dashboard 统计错误
```

**修复**:
```typescript
'COMPLETED': SimplifiedPreArchiveStatus.COMPLETED,
'READY_TO_ARCHIVE': SimplifiedPreArchiveStatus.READY_TO_ARCHIVE,
```

### 2.2 复杂的标志机制导致时序问题

**文件**: `src/features/archives/controllers/useArchiveDataLoader.ts`

```typescript
// ❌ 问题设计
const isPageChangeFromFilterRef = useRef(false);

// Filter effect:
isPageChangeFromFilterRef.current = true;  // 设置标志
setCurrentPage(1);                          // 触发 page change effect

// Page change effect:
if (isPageChangeFromFilterRef.current) {   // 检查标志
    isPageChangeFromFilterRef.current = false;
    return;  // 跳过加载
}
```

**问题**：当 `setCurrentPage(1)` 设置的值与当前值相同时，React 不触发 effect，标志位永远无法被重置。

```
用户在第1页查看"可归档" → page.currentPage = 1
用户点击"已完成" → poolStatusFilter = 'COMPLETED'
Filter effect: setCurrentPage(1) + isPageChangeFromFilterRef = true
React: 1 === 1，不触发 effect
用户点击"下一页" → isPageChangeFromFilterRef 仍是 true，加载被跳过
```

**修复**:
```typescript
// Filter effect: 直接调用 loadCurrentView，不依赖 effect 触发
loadCurrentView(1, poolStatusFilter);
```

### 2.3 闭包 vs Ref 的值捕获问题

```typescript
// ❌ 从 ref 读取（可能是旧值）
const { page, poolStatusFilter } = depsRef.current;
loadCurrentView(page.currentPage, poolStatusFilter);

// ✅ 使用依赖数组中的值（闭包捕获的最新值）
loadCurrentView(page.currentPage, poolStatusFilter);  // poolStatusFilter 来自依赖数组
```

**问题**: `depsRef.current` 中的值在 effect 执行时可能是旧值，导致使用错误的 `poolStatusFilter` 加载数据。

**修复**:
```typescript
// Effect 依赖数组中声明 poolStatusFilter
}, [page.currentPage, poolStatusFilter, loadCurrentView]);
// Effect 内部直接使用闭包变量
loadCurrentView(page.currentPage, poolStatusFilter);
```

## 三、修复方案

### 3.1 简化状态管理

移除 `isPageChangeFromFilterRef` 标志机制：

| 之前 | 之后 |
|------|------|
| Filter effect: 设置标志 + setCurrentPage + 直接加载 | Filter effect: 直接调用 loadCurrentView |
| Page change effect: 检查标志再决定是否加载 | Page change effect: 总是调用 loadCurrentView |
| 复杂的标志位同步 | 简单的函数调用 |

### 3.2 统一状态源

**问题**: 存在两套分页状态需要手动同步：
- `page.currentPage` (useArchivePagination)
- `data.pageInfo.page` (useArchiveData)

**当前方案**: 统一使用 `data.pageInfo` 作为列表页的数据源

**改进方向**: 考虑合并为一个状态源

## 四、调试技巧

### 4.1 洋葱模型调试

从表象层 → IO层 → 逻辑层 → 入口层，逐层回溯：

1. **Layer 3 (IO/数据层)**: API 返回的数据是否正确？
2. **Layer 2 (逻辑层)**: 状态映射、effect 触发时机
3. **Layer 1 (入口层)**: 传入的参数是否正确

### 4.2 最小验证法

```
"加一行 log 看变量值" > "重构整个模块"
```

关键日志点：
- Effect 触发时的参数值
- API 调用时的参数
- 状态变化前后的值

### 4.3 边界条件测试

- 第1页 → 第2页（值变化）
- 第1页 → 第1页（值不变，测试 effect 是否触发）
- 状态 A → 状态 B（filter 变化）

## 五、技术债务

1. **双分页状态**: `page.currentPage` 和 `data.pageInfo.page` 需要手动同步
2. **调试日志**: 生产环境需要统一的日志工具，避免 `console.log` 泄露

## 六、相关文件

| 文件 | 修改内容 |
|------|----------|
| `src/hooks/usePoolKanban.ts` | 添加 COMPLETED、READY_TO_ARCHIVE 状态映射 |
| `src/features/archives/controllers/useArchiveDataLoader.ts` | 简化 effect 逻辑，移除标志机制 |
| `src/pages/archives/ArchiveListView.tsx` | 统一使用 data.pageInfo 作为分页数据源 |
| `src/hooks/usePoolDashboard.ts` | 清理调试日志 |
