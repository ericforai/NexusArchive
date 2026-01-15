# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

> **NexusArchive** - Electronic Accounting Archive Management System (电子会计档案管理系统)
>
> 符合 **DA/T 94-2022**《电子会计档案管理规范》· 等保 2.0 三级 · 信创适配

---

## Project Overview

**NexusArchive** is an enterprise-grade electronic accounting archive management system designed for private deployment, fully compliant with Chinese national standards:

| Standard | Description |
|----------|-------------|
| **DA/T 94-2022** | Electronic Accounting Archive Management Specification |
| **DA/T 92-2022** | Four-Property Testing (Authenticity, Integrity, Usability, Safety) |
| **GB/T 39674** | AIP Archive Information Package Export Format |
| **GB/T 39784-2021** | Electronic Archive Management System Requirements |
| **等保 2.0 三级** | Classified Information Security Protection |

### Technology Stack

| Layer | Technology | Location |
|-------|------------|----------|
| **Backend** | Spring Boot 3.1.6, Java 17, MyBatis-Plus 3.5.7 | `nexusarchive-java/` |
| **Frontend** | React 19.2, TypeScript 5.8, Vite 6, Ant Design 6 | `src/` |
| **Database** | PostgreSQL (primary), DM8/Kingbase (optional) | Flyway migrations |
| **Cache** | Redis | Spring Data Redis |
| **Auth** | JWT (jjwt 0.12.3), Spring Security, Argon2 | `security/` package |
| **Cryptography** | SM2/SM3/SM4 (国密), BouncyCastle | `util/crypto/` |
| **Testing** | JUnit 5, ArchUnit, Vitest, Playwright | `src/__tests__`, `tests/` |

## Repository Layout

```
nexusarchive/
├── nexusarchive-java/              # Backend (Maven project)
│   ├── pom.xml                     # Maven dependencies
│   ├── src/main/java/com/nexusarchive/
│   │   ├── annotation/             # Custom annotations
│   │   ├── aspect/                 # AOP aspects
│   │   ├── collection/             # Collection batch processing (批量审批)
│   │   ├── common/                 # Shared utilities
│   │   │   └── exception/          # Custom exceptions
│   │   ├── config/                 # Spring configuration
│   │   ├── controller/             # REST endpoints
│   │   │   └── scan/               # 扫描工作区控制器
│   │   ├── dto/                    # Request/Response DTOs
│   │   │   ├── destruction/        # 销毁相关 DTO
│   │   │   └── scan/               # 扫描相关 DTO
│   │   ├── entity/                 # JPA/MyBatis entities
│   │   ├── event/                  # Domain events
│   │   ├── exception/              # Global exception handlers
│   │   ├── infrastructure/         # Infrastructure layer
│   │   ├── integration/            # External system integrations (YonSuite, etc.)
│   │   ├── listener/               # Event listeners
│   │   ├── mapper/                 # MyBatis-Plus mappers
│   │   ├── modules/                # Feature modules
│   │   ├── repository/             # Data repositories
│   │   ├── security/               # Auth & JWT
│   │   ├── serializer/             # Custom serializers
│   │   ├── service/                # Business logic
│   │   │   ├── impl/               # Service implementations
│   │   │   └── pdf/                # PDF 处理服务
│   │   └── util/                   # Utility classes
│   └── src/main/resources/
│       ├── application.yml         # Main config
│       ├── application-dev.yml     # Dev profile
│       ├── application.properties  # Properties override
│       └── db/migration/           # Flyway SQL scripts
├── src/                            # Frontend (React/Vite)
│   ├── api/                        # API client (axios)
│   ├── auth/                       # Auth utilities
│   ├── components/                 # React components
│   │   ├── operations/             # 批量操作组件 (BatchOperationBar, BatchApprovalDialog, etc.)
│   │   └── scan/                   # 扫描集成组件 (OCR 识别、文件监控等)
│   ├── data/                       # Mock data / fixtures
│   ├── e2e/                        # E2E test utilities
│   ├── features/                   # Feature modules
│   ├── hooks/                      # Custom React hooks
│   ├── layouts/                    # Layout components
│   ├── lib/                        # Third-party library wrappers
│   ├── pages/                      # Page components
│   │   ├── operations/             # 业务操作页面 (审批、批次、销毁)
│   │   └── scan/                   # 扫描工作区页面
│   ├── routes/                     # React Router config
│   ├── store/                      # Zustand state
│   ├── types/                      # TypeScript type definitions
│   ├── utils/                      # Utility functions
│   └── __tests__/                  # Vitest tests & setup
├── tests/                          # Playwright E2E tests
├── package.json                    # Frontend dependencies
├── vite.config.ts                  # Vite configuration
├── tsconfig.json                   # TypeScript config
├── playwright.config.ts            # Playwright E2E config
├── docker-compose.infra.yml        # Infrastructure (DB + Redis)
├── docker-compose.app.yml          # Application services (Docker deployment)
└── docker-compose.prod.yml         # Production deployment config
```

## Development Commands

### Backend (run from `nexusarchive-java/`)

```bash
# Build
mvn clean compile                   # Compile
mvn clean package -DskipTests       # Package JAR (skip tests)
mvn clean package                   # Package JAR (with tests)

# Run
mvn spring-boot:run                 # Start dev server
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # With dev profile

# Test
mvn test                            # Run all tests
mvn test -Dtest=ClassName           # Run specific test class
mvn test -Dtest=ClassName#methodName # Run specific test method

# Dependencies
mvn dependency:tree                 # Show dependency tree
mvn versions:display-dependency-updates  # Check for updates

# Module (DDD)
./scripts/create-module.sh <ModuleName>  # Generate new DDD module (4-layer structure)
```

### Frontend (run from project root)

```bash
# Install
npm install                         # Install dependencies

# Development
npm run dev                         # Start Vite dev server (port 15175)
npm run dev:vite                    # Start Vite directly (port 15175)
npm run build                       # Production build to dist/
npm run preview                     # Preview production build

# Test
npm run test                        # Run Vitest in watch mode
npm run test:run                    # Run tests once
npm run test:coverage               # Run with coverage report
npm run test:smoke                  # Run Playwright smoke tests
```

### Docker

```bash
# Development (DB + Redis only)
docker-compose -f docker-compose.infra.yml up -d

# Production (DB + Redis + Backend + Frontend)
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d

# Alternative: Production with docker-compose.prod.yml
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### NPM Scripts (project root)

```bash
npm run dev              # 启动开发环境 (Docker 跑 DB，本地跑前后端)
npm run dev:stop         # 停止本地环境 (停止应用进程，可选停止 Docker)
npm run db:dump          # 导出数据快照 (离开公司前执行，保存到 db/seed-data.sql)
npm run db:load          # 导入数据快照 (回到家后执行，从 db/seed-data.sql 恢复)
npm run db:reset         # 重置数据库 (清空并重新初始化)
npm run deploy           # 部署到预发/生产服务器

# Code Quality
npm run typecheck        # TypeScript type checking
npm run lint             # ESLint check
npm run lint:fix         # ESLint auto-fix
npm run check:arch       # dependency-cruiser architecture check

# Module Discovery (Architecture Defense)
npm run modules:discover # Discover all frontend modules
npm run modules:validate # Validate module boundaries
npm run modules:update   # Update module manifest
```

## Code Conventions

### Backend (Java)

- **Package structure**: `com.nexusarchive.<layer>` (controller, service, mapper, entity, dto, config, security, common, integration, util, etc.)
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **MyBatis-Plus**: **强制使用 `LambdaQueryWrapper`，禁止使用字符串方式的 `QueryWrapper`**
  - ✅ 正确: `LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(); wrapper.eq(User::getName, "John");`
  - ❌ 错误: `QueryWrapper<User> wrapper = new QueryWrapper<>(); wrapper.eq("name", "John");`
  - **理由**: 编译期类型检查，IDE 重构支持，避免字段名拼写错误
  - **例外**: 动态字段场景需添加 `// ALLOW-QUERYWRAPPER` 注释
  - **强制检测**: 运行 `mvn test -Dtest=ComplexityRulesTest#shouldUseLambdaQueryWrapper`
- **Entities**: Use Lombok (`@Data`, `@Builder`), MyBatis-Plus annotations
- **Services**: Interface in `service/`, implementation in `service/impl/`
- **DTOs**: Separate request/response DTOs, use validation annotations
- **Exceptions**: Custom exceptions in `common/exception/` and `exception/`, global handler in config
- **AOP**: Aspects in `aspect/` for cross-cutting concerns
- **Events**: Domain events in `event/`, listeners in `listener/`
- **Architecture Tests**: ArchUnit for architecture validation (test group: `architecture`)

### Frontend (TypeScript/React)

- **Components**: Functional components with hooks, PascalCase filenames
- **State**: Zustand for global state, React Query (@tanstack/react-query) for server state
- **API calls**: Centralized in `src/api/`, use axios instance
- **Styling**: Ant Design components, lucide-react icons
- **Tests**: Colocate in `__tests__/` directories, use Vitest + Testing Library
- **Routing**: React Router v7
- **E2E**: Playwright for end-to-end tests
- **Architecture**: Module manifests (manifest.config.ts) for boundary enforcement
- **Path aliases**: `@/`, `@api/`, `@components/`, `@features/`, `@pages/`, `@hooks/`, `@store/`, `@utils/`

### React Hook 引用稳定性 (Critical)

**🔴 P0 级规则**: 自定义 Hook 返回的对象、函数、数组，引用必须保持稳定。

#### 错误示例 ❌

```javascript
// ❌ 每次渲染返回新对象引用
export function useData() {
    const [data, setData] = useState(null);
    return { data, setData };  // 新对象！
}

// ❌ useEffect 依赖不稳定的引用
useEffect(() => {
    loadData();
}, [setData]);  // setData 每次都是新引用 → 无限循环
```

#### 正确示例 ✅

```javascript
// ✅ 使用 useMemo 稳定对象引用
export function useData() {
    const [data, setData] = useState(null);

    return useMemo(() => ({
        data,
        setData,
        extra: () => {},  // useCallback 包装
    }), [data]);
}

// ✅ 使用 useRef 绕过依赖检查
const valueRef = useRef(value);
useEffect(() => {
    valueRef.current = value;
}, [value]);

const callback = useCallback(() => {
    console.log(valueRef.current);  // 从 ref 读取
}, []);  // 空依赖数组，引用永远稳定
```

#### 检查清单

创建/修改自定义 Hook 时，必须检查：
- [ ] 返回对象是否用 `useMemo` 包装？
- [ ] 返回函数是否用 `useCallback` 包装？
- [ ] useEffect 依赖数组是否包含可能变化的引用？
- [ ] 是否可以用 `useRef` 绕过依赖检查？

**详细案例**: `docs/plans/2026-01-14-hook-reference-stability-lesson.md`

## Key Configuration Files

| File | Purpose | Caution Level |
|------|---------|---------------|
| `application.yml` | Spring Boot config | HIGH - contains DB/Redis connection |
| `application-dev.yml` | Dev profile config | HIGH |
| `application.properties` | Properties override | MEDIUM |
| `pom.xml` | Maven dependencies | MEDIUM - version changes can break build |
| `src/main/resources/db/migration/*.sql` | Flyway migrations | HIGH - never modify existing migrations |
| `package.json` | Frontend dependencies | MEDIUM |
| `vite.config.ts` | Vite build configuration | LOW |
| `tsconfig.json` | TypeScript config | LOW |
| `playwright.config.ts` | E2E test config | LOW |
| `.dependency-cruiser.cjs` | Architecture dependency rules | MEDIUM |

## Core Features

| Feature | Description |
|---------|-------------|
| **四性检测 (Four-Property Testing)** | 真实性 (数字签名验证)、完整性 (SM3哈希)、可用性 (格式验证)、安全性 (ClamAV病毒扫描) |
| **审计防篡改** | SM3 哈希链保证日志不可篡改，每条日志记录 `log_hash` 和 `prev_log_hash` |
| **AIP 导出** | 符合 GB/T 39674 标准的归档信息包，含 index.xml 和结构化数据 |
| **信创适配** | 支持 SM2/SM3/SM4 国密算法，适配达梦/人大金仓数据库 |
| **用友集成** | YonSuite 凭证自动同步、组织架构同步，支持凭证/退款单/分录映射，账套-全宗强制路由 |
| **组织同步** | ERP 组织架构自动同步到 `sys_entity` 表，支持树形结构和增量同步 |
| **凭证关联** | 支持按金额、日期、发票号进行多维度精准手动关联 |
| **批量审批** | 归档审批/批次管理/销毁申请的批量批准/拒绝，单次最多 100 条 |
| **扫描集成** | 扫描工作区、OCR 智能识别、移动端扫码上传、文件夹监控 |
| **三员分立** | 系统管理员、安全保密员、安全审计员互斥 (GB/T 39784-2021) |
| **全宗隔离** | 多全宗体系，数据按法人边界隔离，全宗切换器自适应显示 |
| **License 控制** | 基于非对称加密的授权控制与节点限制 |

### Module Boundaries (架构防御)

本项目采用 **模块化单体架构**，强制执行以下边界规则：

**Frontend Boundaries**:
- `src/pages/*` 只能从 `src/features/<module>/index.ts` 引入模块能力 (禁止 deep import)
- `src/components/**` 禁止引入 `features/pages/api/store`
- `src/routes/**` 仅允许引入 `pages` + `layouts` + `components/common` + `auth`
- 配置: `.dependency-cruiser.cjs` (dependency-cruiser)

**Backend Boundaries**:
- 模块对外仅暴露 `app` 层 (Facade/ApplicationService) 和 `api.dto`
- 禁止外部访问 `domain` 和 `infra` 层
- 守卫规则: `src/test/java/com/nexusarchive/architecture/ModuleBoundaryTest.java`
- 架构测试组: `mvn test -Dgroups=architecture`

详见: `docs/architecture/module-boundaries.md`, `docs/architecture/self-review-sop.md`

### Batch Operations (批量操作)

批量操作功能提供高效的审批处理能力：

| 功能 | 描述 | 限制 |
|------|------|------|
| **归档审批批量** | `ArchiveApprovalView` 支持批量批准/拒绝归档申请 | 单次最多 100 条 |
| **批次批量操作** | `ArchiveBatchView` 支持批量批准/拒绝归档批次 | 单次最多 100 条 |
| **销毁批量操作** | `DestructionApprovalPage` 支持批量批准/拒绝销毁申请 | 单次最多 100 条 |

**组件**:
- `BatchOperationBar` - 批量操作工具栏
- `BatchApprovalDialog` - 批量审批确认对话框
- `BatchResultModal` - 批量操作结果展示

**交互规则**:
- 选择 10 条以上: 显示确认提示
- 选择 50 条以上: 提示后台任务处理

详见: `docs/plans/2026-01-07-batch-operations-design.md`

### Scan Integration (扫描集成)

扫描集成模块提供从纸质到数字化的完整链路：

| 功能 | 描述 |
|------|------|
| **扫描工作区** | `/system/collection/scan` - 统一的扫描文件管理界面 |
| **OCR 智能识别** | 自动识别增值税发票、合同协议、银行回单、身份/资质证件 |
| **移动端扫码** | 二维码会话 (30 分钟有效期)，手机扫码自动同步 |
| **监控文件夹** | Java NIO WatchService 实时监控，支持文件类型过滤 |

**API 端点**:
- `/api/scan/workspace` - 工作区管理
- `/api/scan/ocr` - OCR 识别
- `/api/scan/mobile/session` - 移动端会话
- `/api/scan/folder-monitor` - 文件夹监控

详见: `docs/guides/扫描集成使用指南.md`

### Accbook-Fonds Mapping (账套-全宗映射)

ERP 集成实现后端强制路由机制：

| 规则 | 说明 |
|------|------|
| **1:1 严格映射** | 一个全宗只能关联一个 ERP 账套 |
| **后端强制路由** | 同步接口根据用户当前全宗上下文自动路由到对应账套 |
| **配置界面** | 系统管理员可在集成配置中心维护映射关系 |
| **合规性校验** | 后端自动检测并防止重复全宗映射 |

**技术实现**:
- `ErpConfig.accbookMapping` 字段 (JSON 格式存储映射)
- `ErpConfigDtoBuilder` 注入当前全宗上下文
- `YonSuiteErpAdapter` 后端强制路由，不信任前端参数

**数据库变更**: V95 迁移添加 `sys_erp_config.accbook_mapping` 字段

### YonSuite 组织同步

ERP 组织架构同步功能实现从 YonSuite 到本地 `sys_entity` 表的自动同步：

| 功能 | 描述 |
|------|------|
| **树版本同步** | 调用 `treeversionsync` API 获取最新的组织树版本 |
| **成员同步** | 调用 `treemembersync` API 获取组织成员数据 |
| **增量同步** | 基于 `pubts` 时间戳增量获取变更数据 |
| **树形结构** | 支持通过 `parentId` 和 `orderNum` 构建层级关系 |

**技术实现**:
- `ErpOrgSyncService` - 组织同步服务，调用 YonSuite API
- `YonSuiteOrgClient` - YonSuite 组织架构 API 客户端
- `EntityService` - 法人服务，提供树形操作方法
- `SysEntity.parentId` - 父节点 ID，支持树形结构
- `SysEntity.orderNum` - 排序号，控制同级节点顺序

**API 端点**:
- `/yonbip/digitalModel/openapi/treedatasync/treeversionsync` - 树版本查询
- `/yonbip/digitalModel/openapi/treedatasync/treemembersync` - 组织成员查询

**数据库变更**: V101 迁移添加 `sys_entity.parent_id` 字段

### Three-Role Management (三员分立)

系统实现严格的三员分立 (GB/T 39784-2021):

| 角色 | 代码 | 互斥 | 职责 |
|------|------|-----|------|
| 系统管理员 | `system_admin` | ✅ | 系统运维、配置管理、组织架构管理 |
| 安全保密员 | `security_admin` | ✅ | 用户权限分配、角色管理、密钥管理 |
| 安全审计员 | `audit_admin` | ✅ | 查看审计日志、操作追踪、合规检查 |

**特殊角色**:
- **超级管理员** (`super_admin`): 拥有所有权限，仅用于开发/测试环境
- **业务操作员** (`business_user`): 普通业务用户 (档案员、会计等)

**关键约束**:
- 三员角色不能分配给同一用户 (后端自动拦截)
- 任何角色都不会自动拥有全宗权限，必须手动分配
- 超级管理员分配全宗后可跨全宗查询所有数据

### Fonds Permission Mechanism (全宗权限)

**两层控制**:
1. `FondsContextFilter` - HTTP 过滤器层：检查用户是否分配了至少一个全宗
2. `DataScopeService` - 业务数据层：根据权限决定数据访问范围

**关键规则**:
- 任何用户必须分配至少一个全宗，否则无法访问系统
- 超级管理员分配全宗后，可访问所有全宗数据 (`nav:all` 权限生效)
- 普通用户只能访问分配的全宗数据

## Safe Change Guidelines

1. **文档自洽规则 (强制)**:
   - 任何功能/架构/写法更新，工作结束后必须同步更新相关目录的子文档 (目录 MD)
   - 源码与关键配置文件需保持三行头注释 (`Input/Output/Pos`)
   - 目录 MD 必须包含：开头固定声明、目录作用、完整文件清单 (含角色/能力)

2. **Small diffs preferred**: Make incremental changes, not sweeping refactors
3. **Never modify existing Flyway migrations**: Create new migration files instead
4. **Test after changes**: Run `mvn test` / `npm run test:run` to verify
5. **Preserve existing patterns**: Follow conventions already in the codebase
6. **No mass refactors**: Avoid renaming across many files without explicit request

---

## Debugging 经验教训

### "数据查不到"类问题的洋葱模型

当遇到"查询结果为空但预期有数据"时，问题往往是**多层不一致叠加**的结果。按以下顺序排查：

```
第一层：API 契约一致性
   └─ 前端发送的参数 vs 后端接收的参数名是否匹配？

第二层：状态模型语义
   └─ 前端状态值 vs 后端状态值 vs 数据库存储值

第三层：类型代码历史遗留
   └─ 新代码 vs 旧代码（如 BANK_RECEIPT vs BANK_SLIP）

第四层：关联数据完整性
   └─ 外键引用的实体是否存在（如全宗代码、类型代码）

第五层：业务状态合理性
   └─ 页面筛选条件与数据实际状态是否矛盾

第六层：缓存失效
   └─ @Cacheable 是否导致修改不生效
```

### 核心预防原则

1. **单一事实来源 (SSOT)**
   - 枚举值（类型代码、状态值）应该只有一处定义
   - 前后端共享 OpenAPI spec 作为契约来源

2. **类型别名策略（向后兼容）**
   ```java
   // 正确：支持新旧两种类型代码
   private List<String> getTypeAliases(String typeCode) {
       return switch (typeCode) {
           case "BANK_RECEIPT" -> List.of("BANK_RECEIPT", "BANK_SLIP");
           default -> List.of(typeCode);
       };
   }
   ```

3. **缓存修改必须同步失效**
   - 修改基础数据（如全宗权限）时，必须有配套的缓存清理
   - 使用 `@CacheEvict` 或手动清除 Redis key

4. **数据迁移检查清单**
   - [ ] 外键引用的实体是否存在？
   - [ ] 类型/状态代码是否需要迁移？
   - [ ] 缓存是否已清除？
   - [ ] 前后端 API 是否同步更新？

### 典型案例：单据池空数据问题 (2026-01-15)

**问题现象**：预归档单据池页面点击具体类型后看不到单据

**根本原因**（6 层叠加）：
1. 后端不接收 `poolStatus` 参数
2. `poolStatus` 映射到错误的 `archiveStatus`
3. 类型代码不匹配（BANK_RECEIPT ≠ BANK_SLIP）
4. 全宗代码不存在（BR01 不在法人表中）
5. 单据状态与页面筛选矛盾（已归档 vs 预归档）
6. Redis 缓存未失效

**详见**：`docs/plans/2026-01-15-doc-pool-empty-bug-postmortem.md`

---
## Security Features

### Cryptography (国密算法)

| Algorithm | Usage | Implementation |
|-----------|-------|----------------|
| **SM3** | 审计日志哈希链、文件完整性校验 | `Sm3Utils.java` |
| **SM4** | 敏感字段加密 (title, 等) | `Sm4TypeHandler.java` |
| **SM2** | 电子签章验签 | `Sm2SignatureService.java` |

### Security Components

| Component | File | Purpose |
|-----------|------|---------|
| XSS 防护 | `XssFilter.java` | 跨站脚本攻击防护 |
| 请求限流 | `RateLimitFilter.java` | 令牌桶算法防 DDoS (100次/分钟, 登录10次/分钟) |
| 登录锁定 | `LoginAttemptService.java` | 5次失败后锁定15分钟 (Redis分布式) |
| 密码策略 | `PasswordPolicyValidator.java` | ≥8位，含大小写字母、数字、特殊字符 |
| 路径安全 | `PathSecurityUtils.java` | 防止路径遍历攻击 |
| 病毒扫描 | `ClamAvAdapter.java` | 流式病毒扫描 (zINSTREAM 模式) |
| 配置验证 | `SecurityConfigValidator.java` | 生产环境安全配置检查 |

### Audit Log (审计日志防篡改)

SM3 哈希链机制:
```
日志1 → SM3(内容+NULL) → Hash1
日志2 → SM3(内容+Hash1) → Hash2
日志3 → SM3(内容+Hash2) → Hash3
```

每条日志记录:
- `log_hash`: 当前日志的 SM3 哈希值
- `prev_log_hash`: 前一条日志的哈希值

验证: `auditLogService.verifyLogChain(startDate, endDate)`

### Environment Variables (必需)

| Variable | Description | Security Level |
|----------|-------------|----------------|
| `JWT_SECRET` | JWT 签名密钥 (≥32字符) | 🔴 Critical |
| `DB_PASSWORD` | 数据库密码 | 🔴 Critical |
| `SM4_KEY` | SM4 加密密钥 (32位Hex) | 🔴 Critical |
| `AUDIT_LOG_HMAC_KEY` | 审计日志 HMAC 密钥 | 🔴 Critical |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的源 | 🟡 Medium |

---

## Database Design

### Core Tables (四大核心支柱)

| Table | Description | Key Fields |
|-------|-------------|------------|
| `acc_archive` | 单据总表 ("户口本") | `unique_biz_id`, `amount`, `doc_date`, `volume_id`, `metadata_json` |
| `arc_file_content` | 文件实体表 ("金库") | `original_hash`, `current_hash`, `timestamp_token`, `sign_value` |
| `acc_archive_volume` | 案卷表 ("文件夹") | `volume_code`, `retention_period`, `status`, `validation_report_path` |
| `sys_audit_log` | 审计日志表 ("黑匣子") | `data_before`, `data_after`, `ip_address`, `log_hash`, `prev_log_hash` |

### Schema Validation (Entity-Database 一致性)

`EntitySchemaValidator` 在应用启动时自动验证:

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `SCHEMA_VALIDATION_ENABLED` | `true` | 是否启用验证 |
| `SCHEMA_VALIDATION_FAIL` | `true` | 发现问题时是否阻止启动 |

详见: `docs/schema-validation-guide.md`

### Naming Conventions

| 类型 | 规范 | 示例 |
|------|------|------|
| 金额字段 | `DECIMAL(18, 2)` | 禁止 FLOAT/DOUBLE |
| 时间字段 | `TIMESTAMP` / `TIMESTAMPTZ` | `created_at`, `updated_at` |
| 哈希字段 | `VARCHAR(64)` | SM3/SHA256 输出 |
| 布尔字段 | `BOOLEAN` / `TINYINT(1)` | `is_deleted`, `is_active` |

### Flyway Migrations

- 位置: `src/main/resources/db/migration/`
- 命名: `V{version}__{description}.sql`
- 规则: 永不修改已存在的迁移文件，只能创建新迁移

---

## Common Issues

### Backend won't start
1. Check PostgreSQL is running: `docker ps | grep postgres`
2. Verify `application.yml` datasource config
3. Check Redis connection if caching enabled
4. Check Schema Validation output: `tail -f backend.log | grep -i "schema validator"`
5. Verify JWT keys exist in `keystore/` directory

### Frontend build fails
1. Clear node_modules: `rm -rf node_modules && npm install`
2. Check TypeScript errors: `npm run typecheck`
3. Verify import paths match actual file locations
4. Check architecture violations: `npm run check:arch`

### Tests fail
- **Backend**: Check `application-test.yml` config, H2 in-memory DB used for tests
- **Frontend**: Check jsdom environment in vitest config
- **Architecture Tests**: Run `mvn test -Dgroups=architecture`

### Schema Validation Errors
```
MISSING: Entity 'Archive' field 'createdTime' -> Column 'created_time' not found in table 'acc_archive'
```
**Solution**: Create Flyway migration or fix `@TableField` annotation

详见: `docs/schema-validation-guide.md`

---

## Documentation Navigation

| 分类 | 文档 |
|:---|:---|
| **入门** | [启动指南](docs/deployment/启动指南.md) · [用户手册](docs/guides/用户使用手册.md) · [新人接手指南](docs/guides/新人接手指南.md) |
| **部署** | [部署指南](docs/guides/系统部署手册.md) · [Docker 构建](docs/DOCKER_BUILD_GUIDE.md) · [环境部署快速配置手册](docs/deployment/环境部署快速配置手册.md) |
| **功能** | [功能模块](docs/guides/功能模块.md) · [权限管理](docs/guides/权限管理.md) |
| **集成** | [扫描集成使用指南](docs/guides/扫描集成使用指南.md) · [用友集成](docs/guides/用友集成.md) · [AI 适配器生成](docs/guides/ai-adapter-generation.md) |
| **架构** | [模块边界](docs/architecture/module-boundaries.md) · [自审清单](docs/architecture/self-review-sop.md) · [手把手集成化应用指南](docs/guides/手把手集成化应用指南.md) |
| **安全** | [安全指南](docs/guides/安全指南.md) · [审计日志](docs/guides/安全指南.md#审计日志防篡改) |
| **数据库** | [数据库设计](docs/database/数据库设计.md) · [DDL 脚本](docs/database/) |
| **更新日志** | [CHANGELOG](docs/CHANGELOG.md) |

---
