# 前端改动检查指南

> **创建日期**: 2025-01  
> **目的**: 指导如何检查和验证前端新功能的实现

---

## 📋 本次改动概览

### 新增页面
1. **全宗沿革管理页面** - `/system/admin/fonds-history`
2. **全宗沿革历史查看页面** - `/system/admin/fonds-history/list`
3. **授权票据申请页面** - `/system/security/auth-ticket/apply`
4. **授权票据列表和审批页面** - `/system/security/auth-ticket`

### 新增 API 文件
1. `src/api/fondsHistory.ts` - 全宗沿革管理 API
2. `src/api/authTicket.ts` - 授权票据管理 API

---

## ✅ 检查步骤

### 1. 确认文件存在

在项目根目录执行以下命令检查文件是否存在：

```bash
# 检查页面文件
ls -la src/pages/admin/FondsHistoryPage.tsx
ls -la src/pages/admin/FondsHistoryListPage.tsx
ls -la src/pages/security/AuthTicketApplyPage.tsx
ls -la src/pages/security/AuthTicketListPage.tsx

# 检查 API 文件
ls -la src/api/fondsHistory.ts
ls -la src/api/authTicket.ts
```

**预期结果**: 所有文件都应该存在

---

### 2. 检查路由配置

打开 `src/routes/index.tsx`，确认以下路由已配置：

```typescript
// 第 64-69 行：懒加载导入
const FondsHistoryPage = lazy(() => import('../pages/admin/FondsHistoryPage'));
const FondsHistoryListPage = lazy(() => import('../pages/admin/FondsHistoryListPage'));
const AuthTicketApplyPage = lazy(() => import('../pages/security/AuthTicketApplyPage'));
const AuthTicketListPage = lazy(() => import('../pages/security/AuthTicketListPage'));

// 第 187-193 行：路由定义（注意顺序：具体路由在通配符路由之前）
{ path: 'admin/fonds-history', element: withSuspense(FondsHistoryPage) },
{ path: 'admin/fonds-history/list', element: withSuspense(FondsHistoryListPage) },
{ path: 'security/auth-ticket/apply', element: withSuspense(AuthTicketApplyPage) },
{ path: 'security/auth-ticket', element: withSuspense(AuthTicketListPage) },
{ path: 'admin/*', element: withSuspense(AdminLayout) }, // 通配符路由必须在最后
```

**关键点**: 
- ✅ 具体路由必须在通配符路由 `admin/*` **之前**
- ✅ 路由路径必须正确

---

### 3. 重启开发服务器

**重要**: 修改路由配置后，必须重启开发服务器！

```bash
# 停止当前服务器（Ctrl+C）
# 然后重新启动
npm run dev
```

或者如果使用 Docker：

```bash
# 重启前端容器
docker-compose restart frontend
# 或
docker-compose down && docker-compose up -d
```

---

### 4. 直接访问 URL 测试

在浏览器中直接访问以下 URL（需要先登录）：

```
# 全宗沿革管理
http://localhost:15175/system/admin/fonds-history

# 全宗沿革历史查看
http://localhost:15175/system/admin/fonds-history/list

# 授权票据申请
http://localhost:15175/system/security/auth-ticket/apply

# 授权票据列表
http://localhost:15175/system/security/auth-ticket
```

**预期结果**: 
- ✅ 页面应该正常加载（可能显示"加载中..."然后显示页面内容）
- ❌ 如果显示 404 或空白页，检查浏览器控制台错误

---

### 5. 检查浏览器控制台

打开浏览器开发者工具（F12），检查：

#### Console 标签
- ❌ 是否有红色错误信息？
- ❌ 是否有路由相关的错误？
- ❌ 是否有模块加载失败的错误？

#### Network 标签
- ✅ 检查是否有请求 `/system/admin/fonds-history` 等路径
- ✅ 检查请求状态码（应该是 200 或 304）

---

### 6. 检查编译错误

在终端中检查是否有 TypeScript 或 ESLint 错误：

```bash
# 检查 TypeScript 编译
npm run type-check

# 检查 ESLint
npm run lint
```

**预期结果**: 应该没有错误

---

### 7. 检查 API 调用

打开浏览器开发者工具 → Network 标签，然后：

1. 访问全宗沿革管理页面
2. 查看是否有 API 请求：
   - `/api/fonds-history/*` - 全宗沿革相关
   - `/api/auth-ticket/*` - 授权票据相关
   - `/api/bas/fonds/list` - 全宗列表（用于下拉选择）

**预期结果**: 
- ✅ API 请求应该正常发送
- ⚠️ 如果后端未启动，会看到 500 或连接错误（这是正常的）

---

## 🔧 常见问题排查

### 问题 1: 页面显示 404

**可能原因**:
1. 路由配置顺序错误（通配符路由在具体路由之前）
2. 开发服务器未重启
3. 路由路径拼写错误

**解决方法**:
1. 检查 `src/routes/index.tsx` 中路由顺序
2. 重启开发服务器
3. 检查 URL 路径是否正确

---

### 问题 2: 页面显示空白

**可能原因**:
1. 组件导入路径错误
2. 组件有编译错误
3. 懒加载失败

**解决方法**:
1. 检查浏览器控制台错误信息
2. 检查 `src/routes/index.tsx` 中的导入路径
3. 检查组件文件是否有语法错误

---

### 问题 3: API 调用失败

**可能原因**:
1. 后端服务未启动
2. API 路径配置错误
3. 跨域问题

**解决方法**:
1. 确认后端服务已启动（`http://localhost:19090/api`）
2. 检查 `src/api/client.ts` 中的 `baseURL` 配置
3. 检查浏览器控制台的网络错误详情

---

### 问题 4: 导航菜单中没有入口

**当前状态**: ⚠️ 导航菜单中**尚未添加**新页面的入口

**临时访问方式**: 直接访问 URL（见步骤 4）

**后续工作**: 需要在 `src/constants.tsx` 的 `NAV_ITEMS` 中添加菜单项

---

## 📝 验证清单

- [ ] 所有文件都存在
- [ ] 路由配置正确（顺序正确）
- [ ] 开发服务器已重启
- [ ] 可以直接访问 URL
- [ ] 浏览器控制台无错误
- [ ] TypeScript 编译无错误
- [ ] API 请求正常发送（后端可能未实现，但请求应该发送）

---

## 🎯 下一步

1. **添加导航菜单入口** - 在 `src/constants.tsx` 中添加菜单项
2. **后端 API 实现** - 确认后端 API 已实现（Sprint 4 已完成）
3. **权限控制** - 添加页面级权限检查
4. **用户体验优化** - 添加加载状态、错误提示等

---

**维护说明**: 每次前端重大改动后，更新本指南。





