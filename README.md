一旦我所属的文件夹有所变化，请更新我。
# NexusArchive 电子会计档案系统

> **DA/T 94-2022 标准合规 · 电子会计档案全生命周期管理**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.6-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.0-blue.svg)](https://react.dev/)
[![Compliance](https://img.shields.io/badge/Compliance-DA%2FT%2094--2022-red.svg)](http://www.saac.gov.cn/)

---

## 📖 简介

**NexusArchive** 是一款专为企业级财务归档设计的私有化部署解决方案。系统严格遵循国家档案局 **DA/T 94-2022《电子会计档案管理规范》**，深度适配国密算法，提供从凭证采集、四性检测、归档整理到长期保存的全链路管理能力。

---

## 📌 文档自洽规则（强制）

- 任何功能/架构/写法更新，工作结束后必须同步更新相关目录的子文档（目录 MD）。
- 目录 MD 必须包含：开头固定声明、1~3 行目录作用、完整文件清单（含角色/能力）。
- 源码与关键配置文件需保持三行头注释（Input/Output/Pos）与维护声明一致。
- 以下白名单目录为自动生成/第三方/运行产物，不要求目录 MD 或文件头注释（见下一节）。

最小流程：
1. 完成改动
2. 识别受影响目录/文件
3. 更新对应目录 MD
4. 更新根 README.md 记录（或 docs/CHANGELOG.md 中的文档变更）

## 🚫 文档忽略目录（白名单）

- .git/（版本库元数据）
- node_modules/（第三方依赖）
- dist/（构建产物）
- coverage/（测试覆盖率产物）
- playwright-report/（E2E 报告）
- test-results/（测试输出）
- perf/（性能测试产物）
- backups/（备份数据）
- logs/（运行日志）
- deploy/offline/frontend/（离线前端构建产物）
- nexusarchive-java/target/（后端构建产物）
- nexusarchive-java/logs/（后端运行日志）
- nexusarchive-java/data/（后端运行数据/附件）
- nexusarchive-java/lib/（第三方 Jar）
- nexusarchive-java/keystore/（密钥材料）
- src/data/archives/（前端演示归档数据树）

根目录文件放置规则见：[Root Directory Whitelist](docs/standards/ROOT_DIRECTORY_WHITELIST.md)

## ⚠️ 不可注释文件说明

当配置格式或二进制资源不支持注释（如 `package.json`、PDF/OFD/图片/JAR/Class、归档包与日志/数据库文件），保持文件原样，并在目录 MD 的文件清单中说明其角色与用途。

## ⚡ 快速开始

### 开发环境

#### 一键启动（推荐）

```bash
npm run dev
```

自动完成：
- 启动 PostgreSQL + Redis（Docker）
- 启动后端（本地，支持热重载）
- 启动前端（本地，支持 HMR）

访问地址：
- 前端: http://localhost:15175
- 后端: http://localhost:19090/api
- 数据库: localhost:54321
- Redis: localhost:16379

停止服务：
```bash
npm run dev:stop
```

#### 数据同步（多台 Mac 间）

```bash
# 离开公司前 - 导出数据
npm run db:dump

# 回到家后 - 导入数据
npm run db:load
```

### 生产环境

#### 服务器部署（一键）

```bash
npm run deploy
```

或手动部署：
```bash
# 服务器上启动所有服务（DB + Redis + Backend + Frontend）
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d
```

#### 离线安装包

```bash
cd deploy/offline && ./install.sh
```

👉 详细指南：
- 开发环境：[Docker 开发指南](docs/deployment/docker-development.md)
- 生产部署：[Docker 生产部署](docs/deployment/docker-production.md)
- 离线部署：[离线部署手册](docs/guides/离线部署简易手册.md)

---

## ✅ 测试

### UI 冒烟（Playwright）

```bash
BASE_URL=http://localhost:15175 PW_USER=zhangsan PW_PASS=admin123 npm run test:smoke
```

- 默认 headless 运行
- 如需系统 Chrome：设置 `PW_CHANNEL=chrome`
- 如需可视化：设置 `PW_HEADLESS=false`

## ✨ 核心功能

| 功能 | 说明 |
|------|------|
| **四性检测** | 真实性、完整性、可用性、安全性自动检测 |
| **审计防篡改** | SM3 哈希链保证日志不可篡改 |
| **AIP 导出** | 符合 GB/T 39674 标准的归档信息包 |
| **读写分离** | 数据库主从分离，读操作路由到从库，提升性能 |
| **信创适配** | 支持SM2/SM3/SM4 国密 |
| **用友集成** | YonSuite 凭证自动同步与分录映射 |
| **凭证关联** | 支持按金额、日期、发票号进行多维度精准手动关联 |
| **原始凭证** | 独立的原始凭证（发票/单据）采集与版本管理 |
| **安全加固** | XSS防护/登录限流/密码策略/路径安全 |
| **License** | 基于非对称加密的授权控制与节点限制 |


👉 完整功能请参阅 [功能模块说明](docs/guides/功能模块.md)

---

## 🏗 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.1.6 / Java 17 / MyBatis-Plus |
| **前端** | React 19 / TypeScript / Vite 6 / Tailwind CSS |
| **数据库** | PostgreSQL /
| **安全** | Spring Security / Argon2 / SM3 / ClamAV (防病毒) |

👉 详细技术说明请参阅 [技术栈](docs/references/技术栈.md)

---

## 📚 文档导航

| 分类 | 文档 |
|------|------|
| **入门** | [启动指南](docs/deployment/启动指南.md) · [用户手册](docs/guides/用户使用手册.md) · [新人接手指南](docs/guides/新人接手指南.md) |
| **部署** | [部署指南](docs/guides/系统部署手册.md) · [故障排除](docs/guides/系统部署手册.md#5-故障排除) |
| **功能** | [功能模块](docs/guides/功能模块.md) · [权限管理](docs/guides/权限管理.md) · [读写分离](docs/features/read-write-splitting.md) |
| **产品** | [PRD v1.0](docs/product/prd-v1.0.md) · [产品架构](docs/product/architecture.md) |
| **安全** | [安全指南](docs/guides/安全指南.md) · [审计日志](docs/guides/安全指南.md#审计日志防篡改) |
| **集成** | [用友集成](docs/guides/用友集成.md) · [AI 适配器生成](docs/guides/ai-adapter-generation.md) · [API 速查](docs/api/接口速查.md) |
| **数据库** | [数据库设计](docs/database/数据库设计.md) · [DDL 脚本](docs/database/) |
| **规划** | [优化计划](docs/planning/优化计划.md) · [更新日志](docs/CHANGELOG.md) · [模块边界试点成果](docs/implementation/2025-12-27-module-boundary-pilot.md) |

👉 完整文档目录请参阅 [docs/README.md](docs/README.md)

---

## 🔗 快速链接

- 📄 [更新日志](docs/CHANGELOG.md)
- 🔒 [安全指南](docs/guides/安全指南.md)
- 🐛 [故障排除](docs/guides/系统部署手册.md#5-故障排除)
- 📖 [API 文档](docs/api/)
- 📋 [合规标准](docs/references/)

---
