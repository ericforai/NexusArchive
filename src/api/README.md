// Input: API 客户端定义
// Output: 极简架构说明
// Pos: src/api/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# API 调用层 (API)

本目录封装了所有与后端交互的 HTTP 请求。

## 目录结构

- `axios.ts`: Axios 实例配置（拦截器、Token 处理）。
- `archives.ts`: 档案操作接口。
- `system.ts`: 系统配置、日志接口。
- `auth.ts`: 登录与权限接口。

## 规范

1. **强类型**: 每个 API 调用都必须定义请求参数和响应数据的 TypeScript 类型。
2. **无副作用**: API 层仅负责请求发送，不应包含业务逻辑或 UI 弹窗处理。
