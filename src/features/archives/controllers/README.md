一旦我所属的文件夹有所变化，请更新我。

// Input: 档案控制器模块
// Output: 极简架构说明
// Pos: src/features/archives/controllers/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 档案控制器 (Archives Controllers)

本目录包含档案管理的控制器逻辑（状态管理、数据加载、操作处理）。

## 目录结构

本目录遵循 **控制器模式**，将档案列表的复杂逻辑拆分为独立的控制器模块：

| 文件 | 功能 |
| --- | --- |
| `index.ts` | 控制器入口（组合所有控制器） |
| `types.ts` | 类型定义（ControllerData、ControllerQuery 等） |
| `useArchiveControllerActions.ts` | 操作控制器（删除、导出、查看详情等） |
| `useArchiveData.ts` | 数据控制器（行数据管理） |
| `useArchiveDataLoader.ts` | 数据加载控制器（API 调用、分页） |
| `useArchiveMode.ts` | 模式控制器（凭证/账簿/原始凭证视图模式） |
| `useArchivePagination.ts` | 分页控制器（页码、每页条数） |
| `utils.ts` | 工具函数（数据映射、格式化） |

## 架构说明

### 控制器分层

```
┌─────────────────────────────────────────────────────────┐
│                    ArchiveListView                       │
│                   (页面组件层)                           │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│                   controller (组合层)                    │
│  ┌─────────────┬──────────────┬──────────────────────┐  │
│  │  query      │ data         │ actions              │  │
│  │ (筛选状态)  │ (数据管理)   │ (操作处理)           │  │
│  └─────────────┴──────────────┴──────────────────────┘  │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│                   API / Services                         │
│                   (数据服务层)                           │
└─────────────────────────────────────────────────────────┘
```

### Query 控制器

负责筛选和搜索状态管理：

```typescript
interface ControllerQuery {
    searchTerm: string;           // 搜索关键词
    statusFilter: string;         // 状态筛选
    orgFilter: string;            // 组织筛选
    subTypeFilter: string;        // 子类型筛选
    orgOptions: Array<...>;       // 组织选项
}
```

注意：全宗筛选通过顶部全局 FondsSwitcher 控制，
数据加载层 (useArchiveDataLoader) 自动从 useFondsStore 获取当前全宗。

### Data 控制器

负责列表数据管理：

```typescript
interface ControllerData {
    rows: Row[];                  // 当前行数据
    selectedIds: string[];        // 已选中 ID
    pageInfo: PageInfo;           // 分页信息
    isLoading: boolean;           // 加载状态
    errorMessage: string | null;  // 错误信息
}
```

### Actions 控制器

负责用户操作处理：

- `handleDelete`: 删除档案
- `handleExport`: 导出数据
- `handleViewDetail`: 查看详情
- `handleResetSelection`: 重置选择

## 导航集成

控制器支持从其他页面接收导航参数，全宗切换通过全局状态管理：

```typescript
// 从集团架构页面跳转时，全局 FondsSwitcher 会自动切换到目标全宗
// useArchiveDataLoader 会自动从 useFondsStore 获取当前全宗
// 无需在控制器中手动处理全宗筛选逻辑

// 其他导航参数示例（非全宗相关）：
useEffect(() => {
    const state = location.state as { status?: string; searchTerm?: string } | null;
    if (state?.status) {
        setStatusFilter(state.status);
    }
    if (state?.searchTerm) {
        setSearchTerm(state.searchTerm);
    }
}, [location.state]);
```

## 规范

1. **单一职责**: 每个控制器只负责一个方面的逻辑
2. **无副作用**: 控制器不应直接操作 DOM，只管理状态
3. **类型安全**: 所有状态和函数都必须有明确的类型定义
4. **可测试性**: 控制器逻辑应易于单元测试
