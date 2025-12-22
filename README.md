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

## ⚠️ 不可注释文件说明

当配置格式或二进制资源不支持注释（如 `package.json`、PDF/OFD/图片/JAR/Class、归档包与日志/数据库文件），保持文件原样，并在目录 MD 的文件清单中说明其角色与用途。

## ⚡ 快速开始

### 一键启动（推荐）

```bash
./scripts/restart-services.sh
```

### 手动启动

```bash
# 后端
cd nexusarchive-java && mvn clean package -DskipTests
java -jar target/nexusarchive-backend-2.0.0.jar

# 前端
npm install && npm run dev
```

- 后端: http://localhost:8080/api
- 前端: http://localhost:5173
- API 文档: http://localhost:8080/api/swagger-ui.html

👉 详细指南请参阅 [启动指南](/docs/deployment/启动指南.md)

---

## ✨ 核心功能

| 功能 | 说明 |
|------|------|
| **四性检测** | 真实性、完整性、可用性、安全性自动检测 |
| **审计防篡改** | SM3 哈希链保证日志不可篡改 |
| **AIP 导出** | 符合 GB/T 39674 标准的归档信息包 |
| **信创适配** | 支持SM2/SM3/SM4 国密 |
| **用友集成** | YonSuite 凭证自动同步 |
| **安全加固** | XSS防护/登录限流/密码策略/路径安全 |

👉 完整功能请参阅 [功能模块说明](/docs/guides/功能模块.md)

---

## 🏗 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.1.6 / Java 17 / MyBatis-Plus |
| **前端** | React 19 / TypeScript / Vite 6 / Tailwind CSS |
| **数据库** | PostgreSQL /
| **安全** | Spring Security / Argon2 / SM3 / ClamAV (防病毒) |

👉 详细技术说明请参阅 [技术栈](/docs/references/技术栈.md)

---

## 📚 文档导航

| 分类 | 文档 |
|------|------|
| **入门** | [启动指南](/docs/deployment/启动指南.md) · [用户手册](/docs/guides/用户使用手册.md) |
| **部署** | [部署指南](/docs/deployment/部署指南.md) · [故障排除](/docs/deployment/故障排除指南.md) |
| **功能** | [功能模块](/docs/guides/功能模块.md) · [权限管理](/docs/guides/权限管理.md) |
| **安全** | [安全指南](/docs/guides/安全指南.md) · [审计日志](/docs/guides/安全指南.md#审计日志防篡改) |
| **集成** | [用友集成](/docs/guides/用友集成.md) · [API 速查](/docs/api/接口速查.md) |
| **数据库** | [数据库设计](/docs/database/数据库设计.md) · [DDL 脚本](/docs/database/) |
| **规划** | [优化计划](/docs/planning/优化计划.md) · [更新日志](/docs/CHANGELOG.md) |

👉 完整文档目录请参阅 [docs/README.md](/docs/README.md)

---

## 🔗 快速链接

- 📄 [更新日志](/docs/CHANGELOG.md)
- 🔒 [安全指南](/docs/guides/安全指南.md)
- 🐛 [故障排除](/docs/deployment/故障排除指南.md)
- 📖 [API 文档](/docs/api/)
- 📋 [合规标准](/docs/references/)

---
