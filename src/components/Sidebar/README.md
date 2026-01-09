一旦我所属的文件夹有所变化，请更新我。

# Sidebar 组件模块

**作用**：主导航侧边栏，支持多层级菜单展开/收起、权限过滤、自动激活检测。

## 文件清单

| 文件 | 类型 | 功能 |
|------|------|------|
| `Sidebar.tsx` | 导航组件 | 侧边栏主容器，管理展开状态和权限过滤 |
| `NavNode.tsx` | 递归组件 | 单个导航节点渲染，支持无限层级嵌套 |

## 依赖

- `lucide-react` - 图标
- `react-router-dom` - 路由匹配
- `../constants.tsx` - 导航配置
- `../types.ts` - ViewState 类型
- `../hooks/usePermissions.ts` - 权限检查
