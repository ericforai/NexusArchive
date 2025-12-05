# Electronic Accounting Archives (EAA) / 数凭电子会计档案系统

> **DA/T 94-2022 标准合规 · 信创适配 · 电子会计档案全生命周期管理**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.6-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.0-blue.svg)](https://react.dev/)
[![Compliance](https://img.shields.io/badge/Compliance-DA%2FT%2094--2022-red.svg)](http://www.saac.gov.cn/)

## 📖 项目简介

**数凭电子会计档案系统 (EAA)** 是一款专为企业级财务归档设计的私有化部署解决方案。系统严格遵循国家档案局 **DA/T 94-2022《电子会计档案管理规范》**，深度适配 **信创环境**（国产数据库、国密算法），提供从凭证采集、四性检测、归档整理到长期保存的全链路管理能力。

### 核心价值
- **合规性 (Compliance)**: 内置 "四性检测"（真实性、完整性、可用性、安全性）逻辑，确保档案法律效力。
- **信创适配 (Xinchuang)**: 支持达梦 (Dameng)、人大金仓 (Kingbase) 数据库，集成 BouncyCastle 实现 SM3 国密摘要。
- **数据完整 (Integrity)**: 采用 `java.math.BigDecimal` 确保金额零误差，全链路哈希校验防止篡改。
- **自动化 (Automation)**: 自动关联凭证与附件、定时归档任务。

---

## 🏗 技术架构

系统采用前后端分离架构，确保高性能与高扩展性。

### 后端 (Backend)
位于 `nexusarchive-java/` 目录。

- **核心框架**: Spring Boot 3.1.6
- **语言版本**: Java 17 (LTS)
- **ORM 框架**: MyBatis-Plus 3.5.7
- **数据库支持**:
  - **开发**: PostgreSQL
  - **生产 (信创)**: 达梦 (Dameng) 8, 人大金仓 (Kingbase) 8
- **安全体系**:
  - Spring Security + JWT (0.12.3)
  - Argon2 密码哈希
  - SM3 国密摘要 (BouncyCastle)
- **工具组件**:
  - Hutool (通用工具)
  - Apache Tika (文件格式检测)
  - Apache PDFBox (版式文件解析)

### 前端 (Frontend)
位于根目录 `src/`。

- **核心框架**: React 19 + TypeScript
- **构建工具**: Vite 6
- **UI 框架**: Tailwind CSS + Lucide React
- **图表库**: Recharts
- **HTTP 客户端**: Axios

---

## 📦 功能模块

### 1. 预归档管理 (Pre-Archival)
- **电子凭证池**: 统一接收来自 ERP、报销系统的原始凭证。
- **自动关联**: 
  - **智能匹配预演**: 提供"高置信度"与"疑似匹配"的分级预览，人工确认后一键应用。
  - **关联全景**: 可视化展示"原始凭证-记账凭证-依据文件"的完整业务链条。
  - **深度穿透**: 支持从列表和图谱直接穿透查看单据详情。

### 2. 档案管理 (Archives Management)
- **全宗管理 (`bas_fonds`)**: 支持多全宗体系，适应集团化管理。
- **案卷管理 (`arc_account_item`)**: 严格遵循档号规则 `[全宗]-[年度]-[保管期限]-[机构]-[分类]-[件号]`。
- **四性检测**: 归档时自动执行真实性、完整性、可用性、安全性检测。
- **AIP 导出**: 支持导出符合 **GB/T 39674** 及 **DA/T 94-2022** 标准的 **结构化 AIP (Archival Information Package)** 归档包。
  - 包含 `index.xml` (总索引)、`accounting.xml` (结构化数据) 及分区的版式文件 (`content/`) 与附件 (`attachment/`)。
- **正式归档**: 提供从凭证池直接触发的正式归档流程，自动生成档号并封装 AIP 包。
- **增强预览**: 支持多种档案类型（凭证、账簿、报表）的专业化预览。
  - **会计账簿**: 还原三栏式/多栏式账页结构。
  - **财务报告**: 智能识别利润表/资产负债表，展示标准报表格式。
  - **电子凭证翻阅器 (Voucher Player)**: 
    - **分屏对照**: 左凭证、右附件的沉浸式审阅体验。
    - **X光透视**: 鼠标悬停金额/科目，自动高亮发票对应区域。
    - **安全水印**: 全屏动态水印，防止截屏泄密。

### 3. 系统设置 (System Settings)
### 3. 系统设置 (System Settings)
- **定时任务**: 
  - **自动关联**: 每日凌晨 2:00 自动关联凭证与附件。
  - **健康巡检**: 每日凌晨 3:00 对全量已归档数据进行"四性检测"，确保长期保存安全。
- **审计日志**: 记录所有关键操作（归档、销毁、下载），满足 GB/T 39784-2021 审计要求。

### 5. 全库检索 (Global Search)
- **全文检索**: 支持通过凭证号、摘要、金额、发票代码等关键词进行全库搜索。
- **智能导航**: 搜索结果区分"档案匹配"与"元数据匹配"，点击直接跳转至档案全景视图。
- **快捷键支持**: 
  - **Mac**: `Cmd + K` 唤起搜索。
  - **Windows/Linux**: `Ctrl + K` 唤起搜索。

### 6. 资料收集 (Data Collection)
- **在线接收**: 集成监控台，实时监控 SAP、金蝶、泛微OA 等外部系统的凭证同步状态。
- **扫描集成**: 支持高速扫描仪对接，实现纸质凭证数字化。

### 7. 演示特性 (Demo Features)
- **真实数据模拟**: 演示数据包含真实的金额（与 PDF 内容一致）和多样的来源系统（用友、SAP 等）。
- **全景视图增强**: 
  - **多维关联**: 完整展示"凭证-发票-回单-合同"的证据链条。
  - **真实感预览**: 内置采购合同、增值税发票、银行回单等高保真模拟预览，还原真实业务场景。
- **AIP 导出**: 演示数据支持完整的 AIP 导出，包含 metadata.xml 和版式文件，可直接用于合规性验证。
- **双入口模式**: 
  - 官网首页 (`/`): 产品介绍与营销展示。
  - 业务系统 (`/system`): 核心业务操作平台。

### 8. 用友云集成 (YonSuite Integration)
- **凭证同步**: 自动从用友云 YonSuite 拉取会计凭证数据。
- **配置项**:
  - `yonsuite.base-url`: 用友 API 网关地址
  - `yonsuite.app-key`: 应用密钥
  - `yonsuite.app-secret`: 应用密钥密码
- **同步能力**:
  - 按账簿代码 (`accbookCode`) 筛选
  - 按会计期间范围同步 (`periodStart` ~ `periodEnd`)
  - 支持增量同步和全量同步
- **数据映射**: 自动映射用友凭证字段到 EAA 档案结构，生成标准档号。
- **凭证分录完整同步** (2025-12-05 新增):
  - 自动同步凭证的完整分录信息，包括 **摘要 (description)** 和 **科目 (accsubject)**
  - 分录数据存储于 `custom_metadata` 字段 (PostgreSQL JSONB 类型)
  - 导出 AIP 包时，PDF 凭证将展示真实的业务摘要（如"内部交易出库核算"）和科目名称（如"6401 主营业务成本"）

#### 已解决的技术问题 (2025-12-05)

1. **Connection Refused 问题**
   - **症状**: Java 应用无法连接 `https://dbox.yonyoucloud.com`，报 `ConnectException: Connection refused`
   - **根因**: Java 默认使用系统代理设置 (`java.net.useSystemProxies`)，而代理服务器未运行
   - **解决**: 启动时添加 JVM 参数 `-Djava.net.useSystemProxies=false` 禁用系统代理

2. **JSONB 类型不匹配问题**
   - **症状**: 保存凭证分录时报错 `column "custom_metadata" is of type jsonb but expression is of type character varying`
   - **根因**: MyBatis-Plus 默认将 String 类型直接插入 JSONB 列，PostgreSQL 拒绝隐式转换
   - **解决**: 创建 `PostgresJsonTypeHandler.java`，使用 `Types.OTHER` 正确处理 JSONB 类型转换

### 9. 档案组卷管理 (Volume Management)
**符合 DA/T 104-2024《ERP系统电子文件归档和电子档案管理规范》**

- **按月组卷**: 自动将同一会计期间的凭证组成案卷。
- **案卷编号**: 自动生成符合规范的案卷号 `[全宗号]-[分类号]-[期间]`。
- **案卷标题**: 按 `[责任者]+[年度]+[月份]+[档案类型]` 格式自动生成。
- **保管期限**: 自动取卷内最长保管期限作为案卷保管期限。
- **归档审核流程**:
  1. **组卷** (draft): 凭证按期间自动分组
  2. **提交审核** (pending): 案卷提交给档案管理员审核
  3. **审批归档** (archived): 审核通过，案卷和卷内凭证同时归档
  4. **驳回** (draft): 退回草稿状态，可继续修改
- **归档登记表**: 生成符合 GB/T 18894 附录 A 格式的归档登记表。

---

## 🚀 快速开始

### 环境要求
- **Node.js**: v18+
- **JDK**: 17+
- **Maven**: 3.8+
- **Database**: PostgreSQL 14+ (或达梦/金仓)

### 🔥 一键启动 (推荐)

我们提供了一个脚本来自动处理端口冲突并启动所有服务：

```bash
./restart-services.sh
```

该脚本会自动：
1. 清理占用 **8080** (后端) 和 **5173** (前端) 端口的进程。
2. 启动后端服务 (http://localhost:8080/api)。
3. 启动前端服务 (http://localhost:5173)。

### 手动启动

```bash
cd nexusarchive-java

# 1. 配置数据库连接
# 编辑 src/main/resources/application.yml 修改 spring.datasource 相关配置

# 2. 编译打包
mvn clean package -DskipTests

# 3. 运行
java -jar target/nexusarchive-backend-2.0.0.jar
```

### 2. 前端启动

```bash
# 根目录下
npm install
npm run dev

# 访问 http://localhost:5173
```

---

## 🧪 快速冒烟（鉴权）

后端启动后可运行脚本验证登录/登出、黑名单是否生效（默认 BASE_URL=http://localhost:8080/api，用户名/密码自行覆盖）：

```bash
BASE_URL=http://localhost:8080/api USERNAME=admin PASSWORD=pass ./scripts/auth_smoke.sh
```

依赖 `curl` + `jq`。

---

## 🖥️ 前端登录与鉴权行为

- 进入 `/system` 时会自动调用 `/auth/me` 校验 token，无效/过期 token 将清除并回到登录页。
- 任意接口返回 401 时自动跳转回 `/system` 登录（见 `src/api/client.ts`）。
- 登出调用 `/auth/logout`，同时清理本地存储的 token/user。
- 登录页增加加载态与错误提示，直接显示后端返回的 message（见 `src/components/LoginView.tsx`）。
- `src/api/auth.ts` 同步后端返回的用户字段（roles、permissions、status 等），便于后续按权限控制菜单/路由。

---

## 👥 用户与角色管理（迭代 2 & 3）

后端（需 `manage_users` 权限或 `system_admin` 角色）：
- 分页查询用户：`GET /api/admin/users?page=1&limit=10&search=xx&status=active`
- 创建 / 更新 / 删除：`POST /api/admin/users`、`PUT /api/admin/users/{id}`、`DELETE /api/admin/users/{id}`
- 重置密码：`POST /api/admin/users/{id}/reset-password` body `{ "newPassword": "xxx" }`
- 状态切换：`PUT /api/admin/users/{id}/status` body `{ "status": "active|disabled|locked" }`

前端绑定（Settings 页）：
- 位置：`/system` → “系统设置”
  - “创建用户”：填写基本信息、选择角色，提交后自动刷新用户列表。
  - “角色管理”：创建/编辑/删除角色（系统角色禁删），选择类别、类型、描述和权限；权限从 `/admin/permissions` 动态加载，分组复选。
  - “用户管理”：分页列表、下拉修改状态、重置密码（弹框输入）、刷新。
  - “组织/部门”：创建组织/部门、查看组织树、列表删除，排序（上下调 orderNum），支持批量导入 JSON / CSV / Excel（`.csv/.xls/.xlsx` 调用 `/admin/org/import`），模板说明 `/admin/org/import/template`，排序接口 `/admin/org/{id}/order`。
- “系统设置”：读取/保存系统名称、档号前缀、存储类型/路径、默认保管期限（对接 `/admin/settings`）。
- API 对齐：`src/api/admin.ts` 已调用上述接口，状态更新/重置密码/创建用户/角色/组织/系统设置 CRUD 即时反馈。

组织/部门/权限/设置：已提供 `sys_org`、`sys_role`、`sys_user`、`sys_user_role`、`sys_permission`、`sys_setting` 表结构与接口，示例 DDL 见：
- PostgreSQL：`docs/db/auth_schema.sql`
- MySQL：`docs/db/auth_schema_mysql.sql`
- Kingbase：`docs/db/auth_schema_kingbase.sql`
- 达梦：`docs/db/auth_schema_dameng.sql`

前端数据绑定与体验（迭代 6）：
- 系统设置：已绑定后端 `/admin/settings`，保存按钮直接落库。
- 角色/权限：动态拉取 `/admin/permissions`，角色创建/编辑/删除即时生效。
- 用户管理：状态切换、重置密码、创建用户均调真实接口。
- 组织管理：树/列表/排序/导入均调真实接口（支持 CSV/Excel）。
- 归档视图：筛选支持组织下拉，避免完全依赖静态 mock。

---

## 🗄️ 鉴权/组织参考表结构

参考 DDL 文件：`docs/db/auth_schema.sql`（PostgreSQL 语法，可按需调整）包含：
- `sys_org`：组织/部门（id/name/code/parent_id/type/order_num/逻辑删）。
- `sys_role`：角色（code 唯一，role_category、permissions JSON 字符串、type system/custom）。
- `sys_user`：用户（status active/disabled/locked 等）。
- `sys_user_role`：用户-角色关联。
- `sys_permission`：权限配置（键/分组/排序/启用标记），角色权限优先读取此表；为空时回落默认权限。
- `sys_setting`：系统配置（键/值/描述/分类）。
- 并附带默认三员角色注释示例。

### 组织批量导入与排序（CSV/Excel 建议）
- 已实现：JSON/CSV/Excel 导入（后端 `/admin/org/import`，POI 解析 Excel；前端上传按钮支持 `.csv/.xls/.xlsx`），模板说明接口 `/admin/org/import/template`。
- 校验：必填字段（name）、类型/排序解析，返回成功/失败统计与错误行列表。
- 排序：前端“上/下”调整 orderNum 调用 `/admin/org/{id}/order`；可根据需求改为拖拽批量提交。

---

## 🛡️ 审计与安全（迭代 4）
- 审计注解 `@ArchivalAudit` 应用于用户 CRUD、角色 CRUD、系统配置更新；日志落表 `sys_audit_log`（切面负责写入）。
- Token 黑名单/登录失败锁定：见 AuthService / LoginAttemptService / TokenBlacklistService。
- 权限动态加载：角色管理优先读取 `sys_permission` 表（空表时回落默认权限）。

---

## 🧾 License 与自检（迭代 5 部分）
- License 接口：`/api/license/load`（Base64 JSON 简化，需 system_admin），`/api/license` 获取当前缓存。实际生产可替换为 RSA 签名/离线验证。
- 健康自检：`/api/health`、`/api/health/self-check` 返回运行状态、uptime、license 信息（可扩展 DB/磁盘检测、存储可写性）。

---

## ⚙️ 配置说明

### 环境变量
推荐在根目录创建 `.env.local` 文件管理敏感配置：

```env
# 后端 API 地址
VITE_API_BASE_URL=http://localhost:8080/api

# 外部服务密钥 (如需)
GEMINI_API_KEY=your_api_key_here
```

### 数据库设计规范 (Database Standards)
- **设计理念**: 电子会计档案系统的数据库本质是 **"法律证据库"**，合规性 > 性能。
- **金额字段**: 必须使用 `DECIMAL(18, 2)` 类型，禁止使用 `FLOAT/DOUBLE`。
- **哈希算法**: 默认使用 **SM3** (国密)，如环境不支持自动降级为 SHA-256。
- **文件存储**: 数据库仅存储元数据 (`arc_file_content`)，实体文件存储于 OSS 或 NAS。

### 核心数据库架构 (Core Schema)
系统基于 **DA/T 94-2022** 标准构建了四大核心支柱表：

1.  **单据总表 (`acc_archive`) - "户口本"**
    - 核心身份: `unique_biz_id` (唯一业务ID，解决多源异构关联)
    - 关键元数据: `amount`, `doc_date`, `volume_id`
    - 扩展性: `metadata_json` (NoSQL设计，适应未来单据类型扩展)

2.  **文件实体表 (`arc_file_content`) - "金库"**
    - **双哈希机制**: `original_hash` (接收时) + `current_hash` (巡检时)，实现自动健康巡检。
    - **防篡改**: 存储 `timestamp_token` (时间戳) 和 `sign_value` (电子签名)。

3.  **案卷表 (`acc_archive_volume`) - "文件夹"**
    - 虚拟装订: `volume_code`, `retention_period`, `status` (封卷状态)。
    - 合规凭证: 存储 `validation_report_path` (四性检测报告)。

4.  **审计日志表 (`sys_audit_log`) - "黑匣子"**
    - 证据保全: 只增不改 (Append Only)。
    - 关键字段: `data_before`, `data_after`, `ip_address`, `mac_address`。

### 四性检测逻辑详情 (Four-Nature Check Logic)
系统内置的健康巡检服务 (`ArchiveHealthCheckService`) 会对已归档数据执行以下严格检查：

1.  **真实性 (Authenticity)**
    - **逻辑**: 读取磁盘上的物理文件 -> 重新计算哈希值 (SM3/SHA256) -> 与数据库中的 `original_hash` 比对。
    - **作用**: 确保文件未被篡改，且磁盘存储完好。

2.  **完整性 (Integrity)**
    - **逻辑**: 校验元数据中关键合规字段 (`unique_biz_id`, `amount`, `doc_date`) 是否缺失；校验物理文件与档案记录的关联关系。
    - **作用**: 确保档案元数据符合 DA/T 94-2022 标准。

3.  **可用性 (Usability)**
    - **逻辑**: 检查物理文件是否存在、是否可读；校验文件头 (Magic Number) 是否与扩展名匹配。
    - **作用**: 防止文件损坏或格式错误导致无法打开。

4.  **安全性 (Safety)**
    - **逻辑**: 调用病毒扫描接口对文件内容进行扫描。
    - **作用**: 确保存储环境安全，防止恶意软件潜伏。

---

## 📂 目录结构

```
nexusarchive/
├── .agent/                      # Agent 工作流配置
├── .env.local                   # 本地环境变量 (需自行创建)
├── .gitignore                   # Git 忽略配置
├── .vercel/                     # Vercel 部署缓存
├── backups/                     # 备份文件
│   └── nexusarchive---future-accounting.zip # 初始项目备份
├── deploy/                      # 部署相关脚本
│   ├── deploy.sh                # 自动化部署脚本 (包含后端/前端构建、DB迁移、演示数据注入)
│   ├── build.sh                 # 构建脚本
│   ├── nginx.conf               # Nginx 配置示例
│   └── nexusarchive.service     # Systemd 服务配置
├── dist/                        # 前端构建产物 (npm run build 生成)
├── docs/                        # 项目文档
│   ├── DEPLOY.md                # 详细部署文档
│   ├── api_test_plan.md         # API 测试计划
│   ├── 启动指南.md               # 快速启动说明
│   ├── 技术栈.md                 # 技术选型说明
│   └── 部署指南.md               # 生产环境部署指南
├── nexusarchive-java/           # Java 后端工程 (Spring Boot)
│   ├── src/main/java/com/nexusarchive/
│   │   ├── config/              # 配置类 (Security, MyBatis, Swagger)
│   │   ├── controller/          # 控制器 (REST API)
│   │   ├── service/             # 业务逻辑层
│   │   ├── entity/              # 数据库实体 (JPA/MyBatis)
│   │   ├── dto/                 # 数据传输对象
│   │   ├── mapper/              # 数据访问层 (Mapper接口)
│   │   ├── util/                # 工具类
│   │   ├── aspect/              # AOP 切面 (审计日志)
│   │   └── NexusArchiveApplication.java # 启动类
│   └── pom.xml                  # Maven 依赖配置
├── node_modules/                # Node.js 依赖库
├── public/                      # 静态资源 (favicon, robots.txt)
├── src/                         # React 前端源码
│   ├── api/                     # API 接口定义
│   ├── components/              # 业务组件
│   ├── data/                    # 模拟数据
│   ├── hooks/                   # 自定义 Hooks
│   ├── utils/                   # 前端工具函数
│   ├── App.tsx                  # 主应用组件
│   ├── types.ts                 # TypeScript 类型定义
│   └── constants.tsx            # 全局常量
├── index.html                   # 前端入口 HTML
├── package.json                 # 前端项目配置
├── package-lock.json            # 依赖版本锁定
├── tsconfig.json                # TypeScript 配置
├── vite.config.ts               # Vite 构建配置
└── README.md                    # 项目说明文档
```

---

## 🛡 版权与许可

本项目采用 **MIT 许可证**。
详细信息请参阅 [LICENSE](./LICENSE) 文件。
