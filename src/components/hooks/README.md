一旦我所属的文件夹有所变化，请更新我。

# 自定义 React Hooks 模块

**作用**：通用 React Hooks，封装可复用的状态和副作用逻辑。

## 文件清单

| 文件 | 功能 |
|------|------|
| `useKeyboardShortcut.ts` | 键盘快捷键检测（支持 Ctrl/Meta 修饰键） |
| `useClickOutside.ts` | 检测点击组件外部区域 |
| `useSearchQuery.ts` | 搜索状态管理（含 300ms 防抖） |
| `index.ts` | 模块导出入口 |

## 依赖

- `react` - Hooks API
- `../../types.ts` - GlobalSearchDTO 类型
