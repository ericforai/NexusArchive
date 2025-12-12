# NexusArchive 电子会计档案系统

> **DA/T 94-2022 标准合规 · 信创适配 · 电子会计档案全生命周期管理**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.6-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.0-blue.svg)](https://react.dev/)
[![Compliance](https://img.shields.io/badge/Compliance-DA%2FT%2094--2022-red.svg)](http://www.saac.gov.cn/)

---

## 📖 简介

**NexusArchive** 是一款专为企业级财务归档设计的私有化部署解决方案。系统严格遵循国家档案局 **DA/T 94-2022《电子会计档案管理规范》**，深度适配 **信创环境**（国产数据库、国密算法），提供从凭证采集、四性检测、归档整理到长期保存的全链路管理能力。

---

## ⚡ 快速开始

### 一键启动（推荐）

```bash
./restart-services.sh
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
| **信创适配** | 支持达梦、人大金仓 + SM2/SM3/SM4 国密 |
| **用友集成** | YonSuite 凭证自动同步 |
| **安全加固** | XSS防护/登录限流/密码策略/路径安全 |

👉 完整功能请参阅 [功能模块说明](/docs/guides/功能模块.md)

---

## 🏗 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.1.6 / Java 17 / MyBatis-Plus |
| **前端** | React 19 / TypeScript / Vite 6 / Tailwind CSS |
| **数据库** | PostgreSQL / 达梦 / 人大金仓 |
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
| **规划** | [优化计划](/docs/planning/优化计划.md) · [更新日志](/CHANGELOG.md) |

👉 完整文档目录请参阅 [docs/README.md](/docs/README.md)

---

## 🔗 快速链接

- 📄 [更新日志](/CHANGELOG.md)
- 🔒 [安全指南](/docs/guides/安全指南.md)
- 🐛 [故障排除](/docs/deployment/故障排除指南.md)
- 📖 [API 文档](/docs/api/)
- 📋 [合规标准](/docs/references/)

---

## 📄 许可证

本项目采用 [MIT 许可证](./LICENSE)。
