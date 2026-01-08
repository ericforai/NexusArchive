一旦我所属的文件夹有所变化，请更新我。
本目录存放 SystemApp 相关模块。
用于主应用入口的逻辑拆分与复用。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `index.ts` | 模块入口 | 统一导出接口 |
| `viewConstants.ts` | 常量配置 | 视图路径映射、类型常量 |
| `viewRenderers.tsx` | 视图渲染器 | 根据 ViewState 渲染对应页面组件 |
| `useAuthVerification.ts` | Hook | Token 验证逻辑 |
| `useUrlSync.ts` | Hook | URL 与 AppStore 同步逻辑 |
| `useSystemHandlers.ts` | Hook | 登录、登出、导航事件处理 |

## 重构说明

原 `SystemApp.tsx` (339 行) 已拆分为本模块：
- 主组件: 91 行 (-73%)
- 逻辑分离到独立 Hooks 和渲染器
- 降低圈复杂度: 34 → 15
