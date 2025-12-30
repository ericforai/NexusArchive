# NexusArchive 开发成果记录

> 电子会计档案管理系统 - 开发历程与成果汇总

---

## 2025-12-10 增量更新与环境管理体系

### 🚀 四层增量更新机制
为解决客户环境频繁更新的问题，设计并实施了分层更新策略：

| 更新类型 | 命名示例 | 适用场景 | 构建脚本 |
|:---|:---|:---|:---|
| **热补丁** | `*-p1.tar.gz` | 紧急 Bug 修复 (仅变更文件) | `build_patch.sh` |
| **安全补丁** | `*-sec1.tar.gz` | CVE 漏洞修复 (含安全公告) | `build_security_patch.sh` |
| **增量更新** | `*-to-*.tar.gz` | 常规版本迭代 (周/月) | `build_incremental.sh` |
| **完整升级** | `*-installer.tar.gz` | 首次安装/架构变更 | `build_offline_package.sh` |

**交付物**:
- 客户端文档: `docs/deployment/增量更新手册.md`
- 客户端文档: `docs/deployment/环境管理手册.md`

### 🌍 多环境与版本基线
确立了清晰的环境和版本管理规范：

| 环境 | 用途 | 更新频率 | 版本基线 |
|:---|:---|:---|:---|
| **开发环境** | 日常开发 | 实时 | `Latest` |
| **测试环境** | 功能验证 | 每周 | Tag: `v2.0.0` |
| **客户环境** | 生产运行 | 2周~1月 | Tag: `v2.0.0-customer-baseline` |

**Tag 管理**:
- 使用 `git tag` 标记客户环境基线
- 补丁构建脚本自动基于 `*-customer-baseline` 计算变更差异

---

## 2025-12-09 ~ 2025-12-10 归档审批与加密修复

### 🔧 归档审批流程修复

**问题链**:
1. 前端提交归档申请返回"已提交 0 个申请"
2. 数据库约束报错 `null value in column "fiscal_year/org_name/created_at"`
3. 审批"批准"操作无法闭环，文件卡在中间状态

**解决方案**:
- 引入 `BatchOperationResult<T>` 泛型类，区分成功/失败项
- 显式设置 `createdTime`/`lastModifiedTime`，绕过 FieldFill 失效问题
- 新增 `PreArchiveStatus.PENDING_APPROVAL` 状态，完善状态流转链路
- 审批通过时同步更新 `ArcFileContent.preArchiveStatus = ARCHIVED`

详见: [归档流程修复复盘](../knowledge/2025-12-09-submission-flow-fix.md)

### 🔐 SM4 加密字段长度修复

**问题**: 审批操作报 500 错误 `value too long for type character varying(255)`

**数据库迁移** (`V33__increase_archive_column_lengths.sql`):
| 字段 | 原长度 | 新长度 |
|------|--------|--------|
| `title` | 255 | 1000 |
| `summary` | 255 | 2000 |
| `creator` | 255 | 500 |
| `org_name` | 255 | 500 |

### 🛡️ 401 认证拦截器增强

- `useAuthStore` 添加 `VERSION` 版本控制，自动清理旧版 Token
- `client.ts` 拦截 401 响应，自动登出并跳转登录页

### 📂 临时文件路径优化

**问题**: 上传文件存储在 `/tmp`，系统重启后丢失导致四性检测失败

**修复**: 修改 `application.yml`，使用持久化目录 `./data/temp`

详见: [临时文件存储路径问题排查](../knowledge/临时文件存储路径问题排查.md)

### 📊 数据库迁移脚本 (V29~V33)
- `V29`: 添加 `pre_archive_status` 列
- `V30`: 签名相关列
- `V31`: 缺失实体列补全
- `V32`: Schema 验证修复
- `V33`: 加密字段长度扩展

---

## 2025-12-09 代码同步与跨设备协作

### 📦 GitHub 仓库同步
- **仓库**: `git@github.com:ericforai/NexusArchive.git` / `main` 分支
- 创建 `docs/ai-brain/` 目录支持 AI 记忆跨设备同步
- 优化 `.gitignore` 排除构建产物

---

## 2025-12-08 系统健康检查与安全加固

### 🏥 系统全面体检

完成 **123+ 测试用例** 的健康检查：

| 测试类型 | 测试类数 | 用例数 | 状态 |
|----------|---------|--------|------|
| 后端 Service | 13 | 74+ | ✅ |
| 后端 Controller | 9 | 40+ | ✅ |
| 前端组件 | 3 | 36 | ✅ |
| 集成测试 | 9 | 10+ | ✅ |

### 🛡️ XSS 漏洞修复

- 实现 `XssFilter.java` 过滤用户输入
- 在 `UserService` 等服务层应用 XSS 过滤

### 🔗 健康检查端点

- 白名单豁免 `/health` 和 `/health/**` 端点（无需认证）
- 修改 `SecurityConfig` 添加健康检查白名单

### 👤 用户管理 API 完善

- 实现 `GET /admin/users/{id}` 单用户查询接口
- 完善 `AdminUserController` 和 `UserService`

---

## 2025-12-07 ~ 2025-12-08 登录问题修复

### 🔐 Token 存储一致性问题

**问题现象**:
- 登录 API 返回 200 OK，前端显示"登录成功"
- 但页面不跳转，后续 API 调用返回 401

**根因**: 
- `LoginView` 使用 `useAuthStore` (Zustand) 存储 token
- `ProtectedRoute` 使用 `safeStorage.getItem('token')` 检查登录
- 两者存储位置不一致

**解决方案**:
- 修改 `ProtectedRoute.tsx`，统一使用 `useAuthStore` 获取认证状态
- 详见: [登录问题修复记录](../troubleshooting/登录问题修复记录.md)

### 🧰 浏览器存储安全封装

- 实现 `safeStorage` 工具类处理受限上下文中的 `localStorage` 访问
- 解决 "Access to storage is not allowed from this context" 错误

---

## 2025-12-07 权限系统完善

### 🔐 三员分立权限模型

- 实现 **系统管理员、安全管理员、审计管理员** 角色互斥
- `RoleValidationService` 校验角色互斥规则
- `UserService.createUserWithRoles()` 事务性创建

### 🧪 权限测试框架

| 测试类 | 覆盖内容 |
|--------|---------|
| `ThreeRoleExclusionIntegrationTest` | 三员互斥规则 |
| `PermissionIntegrationTest` | 权限校验 |
| `MenuPermission.test.ts` | 前端菜单权限 |

- CI/CD 配置: `.github/workflows/permission-tests.yml`

---

## 2025-12-07 前端架构重构

### 🎨 状态管理迁移到 Zustand

| Store | 用途 |
|-------|------|
| `useAuthStore` | 认证状态（token、user） |
| `useAppStore` | 应用状态（菜单、全局配置） |
| `useThemeStore` | 主题状态（暗黑模式） |

### 📂 组件目录重组

```
src/components/
├── common/      # 通用组件（Button, Modal）
├── layout/      # 布局组件（Header, Sidebar）
├── features/    # 功能组件（Dashboard）
├── admin/       # 管理组件（UserManagement）
├── archive/     # 档案组件（ArchiveList）
└── settings/    # 设置组件
```

### 🔄 React Query 集成

- `queryClient.ts` 全局配置
- `useArchives.ts` 档案数据 Hook
- 实现数据缓存和后台自动刷新

---

## 2025-12-07 基础设施增强

### 📄 OFD 转换服务

- `OfdConvertService` - PDF/图片转 OFD 版式文件
- `OfdViewer.tsx` - 前端 OFD 预览组件
- 支持长期保存格式要求

### 🔍 Elasticsearch 全文搜索

- `ArchiveSearchService` - 索引与搜索服务
- `ArchiveDocument` - ES 文档实体
- `ArchiveSearchRepository` - 搜索仓库
- **支持高亮显示**搜索关键词

### 🔗 ERP 多系统集成

| 适配器 | 用途 |
|--------|------|
| `ErpAdapter` | 统一接口定义 |
| `YonSuiteErpAdapter` | 用友实现 |
| `KingdeeAdapter` | 金蝶实现 |
| `GenericErpAdapter` | 通用适配器 |
| `ErpAdapterFactory` | 适配器工厂 |

- `ErpConfig` 实体及 `ErpConfigController` API

---

## 2025-12-07 合规增强

### ✍️ 电子签名集成

- `DigitalSignatureService` - SM2/RSA 签名验证
- `SignatureAdapter` 接口支持多厂商对接

### 📋 审计日志防篡改

- **哈希链机制**: `previous_hash` 字段链接上一条日志
- SM3 算法计算日志摘要
- MAC 地址采集优化

### 🔐 SM4 加密增强

- 敏感字段加密存储（`title`, `summary`, `creator`）
- 密钥轮换机制

### ✅ 四性检测完善

| 检测项 | 内容 |
|--------|------|
| 真实性 | Hash 校验、数字签名验证 |
| 完整性 | 元数据完整性、附件数量 |
| 可用性 | 文件格式校验、可打开性 |
| 安全性 | 病毒扫描、权限校验 |

---

## 2025-12-07 开发环境迁移

### 💻 跨设备迁移

- PostgreSQL 数据库备份与恢复
- 重建 Maven/npm 依赖（适配新机器架构）
- 解决 Flyway 迁移冲突

### 🧪 测试用例修复

- 修复 `ArchiveControllerTest` 失败（Mock 配置问题）
- 解决 `HttpServletRequest.getAttribute("userId")` 在 `@WebMvcTest` 中的处理

---

## 2025-12-06 ~ 2025-12-07 部署与文档

### 📦 离线安装包

- `deploy/offline/` - 完整离线部署方案
- 支持静默安装、升级、卸载
- 环境检测脚本

### 📚 文档整理

- `docs/` 目录结构重组（api, guides, deployment, knowledge 等）
- `用户使用手册.md` 完善
- `docs/README.md` 作为文档索引

### ⚙️ 部署脚本

| 文件 | 用途 |
|------|------|
| `deploy/build.sh` | 构建脚本 |
| `deploy/docker-compose.yml` | Docker 编排 |
| `deploy/helm/` | Kubernetes Helm Charts |
| `deploy/dev-start.sh` | 开发环境启动 |

---

## 技术栈总览

| 层级 | 技术 |
|------|------|
| 前端 | React 19 + TypeScript + Vite + Zustand + React Query |
| 后端 | Spring Boot 3.1.6 + MyBatis Plus |
| 数据库 | PostgreSQL (信创: 达梦/人大金仓) |
| 安全 | SM2/SM3/SM4 国密算法 + Spring Security + JWT |
| 搜索 | Elasticsearch |
| 部署 | Docker + Kubernetes + Helm |
| 测试 | JUnit 5 + Mockito + Vitest |

---

*最后更新: 2025-12-10*
*此文档由 AI 助手自动维护，用于跨设备记忆同步*
