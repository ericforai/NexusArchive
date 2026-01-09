一旦我所属的文件夹有所变化，请更新我。

# TopBar 顶部导航栏模块

**作用**：系统顶部导航栏，包含全宗切换、全局搜索、用户信息。

## 文件清单

| 文件 | 类型 | 功能 |
|------|------|------|
| `TopBar.tsx` | 主组件 | 顶部导航栏入口 |
| `UserProfile.tsx` | 用户组件 | 用户头像和信息显示 |
| `FondsSection.tsx` | 区域组件 | 全宗切换器区域 |
| `SearchSection.tsx` | 区域组件 | 全局搜索区域 |
| `useTopBarActions.ts` | Hook | 用户菜单操作处理 |
| `index.ts` | 模块导出 | 统一导出入口 |

## 依赖

- `antd` - Avatar 组件
- `../GlobalSearch/` - 全局搜索
- `../common/FondsSwitcher/` - 全宗切换器
- `../../store/useAuthStore.ts` - 用户状态
- `../../store/useFondsStore.ts` - 全宗状态
- `../layout/ProfileDrawer.tsx` - 用户资料抽屉
