# Agent C: 前端架构师任务书

> **角色**: 前端架构师
> **技术栈**: React 19, TypeScript, Zustand, React Query, Vite
> **负责阶段**: 第三阶段 - 前端架构优化
> **前置依赖**: 可与 Agent A 并行开始，但需等待 Agent A 完成登录相关 API 改动

---

## 📋 项目背景

NexusArchive 前端是一个 React 单页应用，使用 Vite 构建。

### 当前问题
- `SystemApp.tsx` 有大量 `useState`，状态管理混乱（~30 个状态变量）
- 无全局状态管理，组件间通信依赖 props 层层传递
- 没有服务端状态缓存，重复请求浪费资源
- 部分组件过于庞大，难以维护

### 项目结构
```
src/
├── App.tsx              # 路由入口
├── SystemApp.tsx        # 主应用（需重构）
├── api/                 # API 调用层
├── components/          # 组件（约 40 个）
├── hooks/               # 自定义 Hooks
├── routes/              # 路由配置
├── types.ts             # 类型定义
└── utils/               # 工具函数
```

---

## 🔐 必读规则

执行任务前，请阅读以下规则文件：

1. **[.agent/rules/general.md](file:///Users/user/nexusarchive/.agent/rules/general.md)** - 核心编码规范

---

## ✅ 任务清单

### 3.1.1 引入 Zustand 状态管理

| 任务 | 产出文件 | 说明 | 验收标准 |
|------|----------|------|----------|
| 安装依赖 | `package.json` | 安装 zustand | 依赖正确添加 |
| 创建 store 目录 | `src/store/` | 状态管理目录 | 目录结构合理 |
| 用户状态 Store | `src/store/useAuthStore.ts` | 登录/权限状态 | 登录状态全局可用 |
| 应用状态 Store | `src/store/useAppStore.ts` | 视图/导航状态 | 导航状态正确 |
| 主题状态 Store | `src/store/useThemeStore.ts` | 深色/浅色模式 | 主题切换正常 |

**代码示例：**
```typescript
// src/store/useAuthStore.ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface User {
  id: string
  username: string
  realName: string
  roles: string[]
  permissions: string[]
}

interface AuthState {
  token: string | null
  user: User | null
  isAuthenticated: boolean
  
  // Actions
  login: (token: string, user: User) => void
  logout: () => void
  hasPermission: (perm: string) => boolean
  hasRole: (role: string) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      
      login: (token, user) => set({ 
        token, 
        user, 
        isAuthenticated: true 
      }),
      
      logout: () => set({ 
        token: null, 
        user: null, 
        isAuthenticated: false 
      }),
      
      hasPermission: (perm) => {
        const { user } = get()
        if (!user) return false
        // 支持通配符匹配
        return user.permissions.some(p => 
          p === perm || p === '*' || perm.startsWith(p.replace('*', ''))
        )
      },
      
      hasRole: (role) => {
        const { user } = get()
        return user?.roles.includes(role) ?? false
      }
    }),
    { name: 'nexus-auth' }
  )
)
```

```typescript
// src/store/useAppStore.ts
import { create } from 'zustand'

interface AppState {
  // 侧边栏状态
  sidebarCollapsed: boolean
  toggleSidebar: () => void
  
  // 当前视图
  currentView: string
  setCurrentView: (view: string) => void
  
  // 加载状态
  globalLoading: boolean
  setGlobalLoading: (loading: boolean) => void
  
  // 通知
  notifications: Notification[]
  addNotification: (n: Notification) => void
  removeNotification: (id: string) => void
}

export const useAppStore = create<AppState>((set) => ({
  sidebarCollapsed: false,
  toggleSidebar: () => set(s => ({ sidebarCollapsed: !s.sidebarCollapsed })),
  
  currentView: 'dashboard',
  setCurrentView: (view) => set({ currentView: view }),
  
  globalLoading: false,
  setGlobalLoading: (loading) => set({ globalLoading: loading }),
  
  notifications: [],
  addNotification: (n) => set(s => ({ 
    notifications: [...s.notifications, n] 
  })),
  removeNotification: (id) => set(s => ({ 
    notifications: s.notifications.filter(n => n.id !== id) 
  }))
}))
```

---

### 3.1.2 重构 SystemApp.tsx

| 任务 | 说明 | 验收标准 |
|------|------|----------|
| 移除冗余 useState | 迁移到 Zustand Store | useState 减少 70% |
| 提取子组件 | 拆分大组件为小组件 | 每个组件 < 200 行 |
| 使用 Store | 组件使用全局状态 | 无 props drilling |
| 优化重渲染 | 使用选择器避免无效渲染 | 性能提升 |

**重构前后对比：**
```typescript
// 重构前 (SystemApp.tsx)
const [token, setToken] = useState(null)
const [user, setUser] = useState(null)
const [currentView, setCurrentView] = useState('dashboard')
const [permissions, setPermissions] = useState([])
const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
// ... 20+ 更多状态

// 重构后
const { user, hasPermission } = useAuthStore()
const { currentView, setCurrentView, sidebarCollapsed } = useAppStore()
```

---

### 3.1.3 添加 React Query（TanStack Query）

| 任务 | 产出文件 | 说明 | 验收标准 |
|------|----------|------|----------|
| 安装依赖 | `package.json` | @tanstack/react-query | 依赖正确 |
| 配置 QueryClient | `src/queryClient.ts` | 全局配置 | 配置合理 |
| 重构 API 调用 | `src/hooks/useArchives.ts` 等 | 使用 useQuery | 有缓存和加载状态 |

**代码示例：**
```typescript
// src/queryClient.ts
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,  // 5分钟内数据不重新请求
      gcTime: 10 * 60 * 1000,    // 10分钟后清理缓存
      retry: 1,
      refetchOnWindowFocus: false
    }
  }
})

// src/hooks/useArchives.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { archiveApi } from '@/api/archive'

export function useArchives(params: ArchiveQueryParams) {
  return useQuery({
    queryKey: ['archives', params],
    queryFn: () => archiveApi.list(params)
  })
}

export function useArchive(id: string) {
  return useQuery({
    queryKey: ['archive', id],
    queryFn: () => archiveApi.getById(id),
    enabled: !!id
  })
}

export function useCreateArchive() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: archiveApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['archives'] })
    }
  })
}
```

---

### 3.1.4 组件目录重组

| 任务 | 说明 | 验收标准 |
|------|------|----------|
| 按功能分组 | 创建 feature 目录 | 目录结构清晰 |
| 提取公共组件 | 常用 UI 组件抽离 | 复用性提高 |
| 添加 index 导出 | barrel exports | 导入路径简化 |

**推荐目录结构：**
```
src/
├── components/
│   ├── common/              # 通用组件
│   │   ├── Button.tsx
│   │   ├── Modal.tsx
│   │   ├── Table.tsx
│   │   └── index.ts
│   ├── layout/              # 布局组件
│   │   ├── Sidebar.tsx
│   │   ├── Header.tsx
│   │   └── index.ts
│   └── index.ts
├── features/                # 业务功能模块
│   ├── archives/
│   │   ├── ArchiveList.tsx
│   │   ├── ArchiveDetail.tsx
│   │   ├── hooks/
│   │   └── index.ts
│   ├── borrowing/
│   ├── compliance/
│   └── settings/
├── store/                   # 状态管理
├── hooks/                   # 通用 Hooks
└── api/                     # API 层
```

---

## 🧪 验证步骤

### 1. 构建验证
```bash
npm run build
# 应无 TypeScript 错误
```

### 2. 开发服务器
```bash
npm run dev
# 检查控制台无错误
```

### 3. 功能验证
- [ ] 登录后刷新页面，状态保持
- [ ] 侧边栏收起/展开正常
- [ ] 权限控制正常
- [ ] API 请求有缓存（重复请求不触发网络）

---

## 📝 完成标志

任务完成后，请在 `docs/优化计划.md` 中勾选第三阶段 3.1 的相关项目。

---

*Agent C 任务书 - 由 Claude 于 2025-12-07 生成*
