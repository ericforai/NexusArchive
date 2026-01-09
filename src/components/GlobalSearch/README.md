一旦我所属的文件夹有所变化，请更新我。

# GlobalSearch 全局搜索模块

**作用**：全局搜索功能，支持快捷键唤起(Ctrl+K)、实时搜索结果、点击外部关闭。

## 文件清单

| 文件 | 类型 | 功能 |
|------|------|------|
| `GlobalSearch.tsx` | 导出组件 | 主组件入口（原文件迁移） |
| `SearchInput.tsx` | 输入组件 | 搜索输入框，支持加载状态 |
| `SearchResults.tsx` | 结果组件 | 搜索结果列表，支持分类显示 |

## 依赖

- `../hooks/useSearchQuery.ts` - 搜索状态管理（防抖）
- `../hooks/useKeyboardShortcut.ts` - 快捷键处理
- `../hooks/useClickOutside.ts` - 外部点击检测
- `../../api/search.ts` - 搜索 API
- `../../types.ts` - GlobalSearchDTO 类型
