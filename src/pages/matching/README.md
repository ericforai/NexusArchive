// Input: matching 页面模块
// Output: 极简架构说明
// Pos: src/pages/matching/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 凭证匹配页面 (Matching Pages)

本目录包含凭证匹配引擎相关的页面容器组件。

## 文件清单

| 文件 | 功能 |
| --- | --- |
| `VoucherMatchingPage.tsx` | 凭证匹配页面容器 |

## 架构约束

- Page 层仅负责"胶水"逻辑
- 匹配业务逻辑应抽离至 `src/features/matching/`
