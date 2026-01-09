一旦我所属的文件夹有所变化，请更新我。

# FondsSwitcher 全宗切换器模块

**作用**：顶部导航栏全宗切换组件，自适应显示（单个全宗显示文本，多个全宗显示下拉）。

## 文件清单

| 文件 | 类型 | 功能 |
|------|------|------|
| `FondsSwitcher.tsx` | 主组件 | 全宗切换器入口 |
| `FondsDropdown.tsx` | 下拉组件 | 多全宗下拉菜单 |
| `SingleFondsDisplay.tsx` | 展示组件 | 单个全宗纯文本显示 |
| `LoadingState.tsx` | 状态组件 | 加载中状态 |
| `EmptyState.tsx` | 状态组件 | 无全宗权限提示 |
| `types.ts` | 类型定义 | Fonds 类型导出 |
| `index.ts` | 模块导出 | 统一导出入口 |

## 依赖

- `lucide-react` - 图标
- `../../store/useFondsStore.ts` - 全宗状态管理
