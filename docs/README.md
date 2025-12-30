一旦我所属的文件夹有所变化，请更新我。
本目录是项目文档总入口。
用于汇总部署、功能、接口与排障说明。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `CHANGELOG.md` | 变更记录 | 文档与版本变更记录 |
| `change-list.md` | 变更记录 | 本次文档与代码变更清单 |
| `README.md` | 入口文档 | 文档中心索引与导航 |
| `agents/` | 目录入口 | Agent 协作说明 |
| `ai-brain/` | 目录入口 | AI 相关记录与资料 |
| `api/` | 目录入口 | API 文档集合 |
| `architecture/` | 目录入口 | 架构设计说明 |
| `database/` | 目录入口 | 数据库设计与脚本 |
| `deployment/` | 目录入口 | 部署与运维指南 |
| `guides/` | 目录入口 | 功能与使用指南 |
| `images/` | 目录入口 | 文档配图资源 |
| `implementation/` | 目录入口 | 实施与落地说明 |
| `knowledge/` | 目录入口 | 复盘与知识沉淀 |
| `planning/` | 目录入口 | 规划与路线文档 |
| `plans/` | 目录入口 | 专项评估与临时方案 |
| `product/` | 目录入口 | 产品需求与产品架构 |
| `references/` | 目录入口 | 规范与参考资料 |
| `troubleshooting/` | 目录入口 | 故障排除与案例 |

# NexusArchive 文档中心

## ⚖️ 真源规则 (True Source Rules)
- **Guides 为真源**：面向使用/交付的指南统一放 `docs/guides/`
- **Deployment 为历史/脚本库**：只保留脚本与“历史说明”，不再写新的操作指南
- **Troubleshooting 为真源**：故障处理只放 `docs/troubleshooting/`
- **Bug-reports 为单据**：缺陷单据只放 `docs/bug-reports/`，并必须链接到对应 troubleshooting 条目（如果形成可复用结论）

欢迎使用 **NexusArchive 电子会计档案管理系统**文档。

> 符合 DA/T 94-2022 和等保 2.0 三级标准的企业级电子会计档案管理解决方案

---

## NexusArchive (信创电子档案系统)
## 简介 (Introduction)
NexusArchive 是一个符合中国国家标准 (DA/T 94-2022) 的电子会计档案管理系统，专为信创环境（私有化部署）设计。它支持从 ERP 系统自动采集凭证，并进行四性检测（其中 SM3 哈希算法用于确保数据完整性）。

## 核心架构 (Architecture)
系统采用 **通用连接器 (Universal Connector)** 架构，实现了核心归档逻辑与上游业务系统的解耦。
- **SourceConnector**: 标准化接口，适配用友 YonSuite、金蝶 Cloud 等多种 ERP。
- **UniversalSyncEngine**: 通用同步引擎，统一处理凭证获取、格式转换、PDF 生成及四性检测调用。
- **UnifiedDocumentDTO**: 统一数据模型，屏蔽不同系统的字段差异。

## 核心功能 (Features)
- **多源采集**: 支持多种 ERP 系统自动同步。
- **四性检测**: 严格遵循 DA/T 92-2022 标准（真实性、完整性、可用性、安全性）。
- **长期保存**: 支持 OFD/PDF 版式文件生成与归档。
- **信创适配**: 支持国产数据库、操作系统及 SM3 国密算法。

## 🎯 系统概述

NexusArchive 是一款专为中国企业设计的电子会计档案管理系统，具备以下特性：

- ✅ **合规性** - 符合《会计档案管理办法》、DA/T 94-2022、等保 2.0 三级
- ✅ **信创适配** - 支持国产 CPU、操作系统、数据库
- ✅ **私有化部署** - 完全内网运行，数据自主可控
- ✅ **安全可靠** - 三员分立、审计追溯、防篡改
- ✅ **OFD 版式** - PDF 转 OFD 长期保存格式
- ✅ **全文检索** - Elasticsearch 高亮搜索
- ✅ **多 ERP 对接** - 用友/金蝶/通用适配器

---

## 🚀 快速开始

| 场景 | 推荐文档 |
|:---|:---|
| 首次部署 | [启动指南](deployment/启动指南.md) |
| 了解功能 | [功能模块](guides/功能模块.md) |
| API 开发 | [接口速查](api/接口速查.md) |
| 遇到问题 | [故障排除指南](deployment/故障排除指南.md) |

**在线 API 文档**: 启动服务后访问 `/api/swagger-ui.html`

---

## 📘 用户指南

| 文档 | 说明 |
|:---|:---|
| [用户使用手册](guides/用户使用手册.md) | 完整使用指南，包含所有功能操作说明 |
| [功能模块](guides/功能模块.md) | 各功能模块详细说明 |
| [权限管理](guides/权限管理.md) | 用户、角色、权限配置（三员分立） |
| [安全指南](guides/安全指南.md) | 安全配置与最佳实践 |
| [数据库治理规范](guides/database-governance.md) | Schema 迁移规范、SM4 加密字段长度规范 |
| [用友集成](guides/用友集成.md) | YonSuite 凭证同步配置 |
| [OFD 版式文件](guides/OFD版式文件.md) | PDF 转 OFD 长期保存格式 |
| [全文检索](guides/全文检索.md) | Elasticsearch 高亮搜索 |
| [ERP 集成配置](guides/ERP集成配置.md) | 多 ERP 系统对接配置 |

---

## 📒 产品文档

| 文档 | 说明 |
|:---|:---|
| [产品需求(PRD)](product/prd-v1.0.md) | 集团私有化版 PRD v1.0 |
| [产品架构](product/architecture.md) | 产品架构与核心对象说明 |
| [功能清单](product/功能清单.md) | 全量功能模块清单 |

---

## 🔧 部署运维

| 文档 | 说明 |
|:---|:---|
| [启动指南](deployment/启动指南.md) | 快速启动服务 |
| [部署指南](guides/系统部署手册.md) | 本地开发与生产环境部署 |
| [离线安装包](guides/系统部署手册.md#4-离线安装包) | 离线安装包结构与使用 |
| [健康监控](deployment/health_and_monitoring.md) | 系统健康检查与监控 |
| [故障排除](guides/系统部署手册.md#5-故障排除) | 常见问题诊断与解决 |

---

---

## 🔧 故障排除案例

| 文档 | 说明 |
|:---|:---|
| [归档审批功能修复](knowledge/2025-12-10-approval-ui-fix.md) | 2025-12-10 技术复盘：UI 统计不同步、SM4 加密字段长度溢出 |
| [登录问题修复记录](troubleshooting/登录问题修复记录.md) | Token 存储不一致导致的登录失败问题 |
| [提交归档流程修复复盘](knowledge/2025-12-09-submission-flow-fix.md) | 2025-12-09 技术复盘：提交静默失败、DB约束、审批流程修复 |

> 💡 **提示**: 如遇到新问题，请在 `docs/troubleshooting/` 目录下添加案例记录。

---

## 📡 API 接口

| 文档 | 说明 |
|:---|:---|
| [接口速查](api/接口速查.md) | 常用 API 快速参考 |
| [API 接口文档](api/API接口文档) | 完整接口说明 |
| [组卷归档 API](api/组卷归档API接口文档.md) | 组卷归档接口详情 |

### 核心 API 端点

| 模块 | 端点 | 说明 |
|:---|:---|:---|
| 认证 | `POST /api/auth/login` | 用户登录 |
| 用户 | `GET /api/admin/users` | 用户管理 |
| 角色 | `GET /api/admin/roles` | 角色管理 |
| 档案 | `GET /api/archives` | 档案查询 |
| 预览 | `POST /api/archive/preview` | 档案流式预览（含水印元数据） |
| 文件 | `GET /api/archive/{id}/content` | 档案文件下载 (PDF/OFD) |
| OFD 转换 | `POST /api/archive/{id}/convert-to-ofd` | PDF 转 OFD |
| 全文检索 | `GET /api/search` | Elasticsearch 全文搜索 |
| 借阅 | `POST /api/borrowing` | 借阅申请 |
| ERP 配置 | `GET /api/erp/config` | ERP 对接配置管理 |
| 审计 | `GET /api/audit-logs` | 审计日志 |
| 健康 | `GET /api/health` | 健康检查 |

---

## 🗄️ 数据库设计

| 文档 | 说明 |
|:---|:---|
| [数据库设计](database/数据库设计.md) | 架构设计与四性检测逻辑 |
| [DDL 脚本](database/) | PostgreSQL/达梦/人大金仓 建表脚本 |

### 核心表结构

| 表名 | 说明 |
|:---|:---|
| `sys_user` | 用户表 |
| `sys_role` | 角色表 |
| `sys_permission` | 权限表 |
| `acc_archive` | 档案主表 |
| `arc_file_content` | 档案文件表 |
| `sys_audit_log` | 审计日志表 |

---

## 📋 规划设计

| 文档 | 说明 |
|:---|:---|
| [优化计划](planning/优化计划.md) | 总体优化计划与进度 |
| [规划文档](planning/) | 系统设计、需求分析、技术方案 |

---

## 🤖 AI 开发指南

本项目采用多 Agent 协作开发模式，详见：

| 文档 | 说明 |
|:---|:---|
| [Agent 协作指南](agents/README.md) | 多 Agent 任务分配与协作机制 |
| [Agent A 任务](agents/task-agent-a.md) | 后端安全加固（JWT/密码策略/限流） |
| [Agent B 任务](agents/task-agent-b.md) | 合规性增强（四性检测/签章/国密） |
| [Agent C 任务](agents/task-agent-c.md) | 前端架构重构（Zustand/React Query） |
| [Agent D 任务](agents/task-agent-d.md) | 基础设施（OFD/ES/ERP 集成） |
| [Agent E 任务](agents/task-agent-e.md) | 质量保障（测试/性能/覆盖率） |

---

## 📚 参考资料

| 文档 | 说明 |
|:---|:---|
| [技术栈](references/技术栈.md) | 技术选型说明 |
| [合规标准](references/) | 法规标准、技术规范 |

### 技术栈概览

| 层级 | 技术 |
|:---|:---|
| 前端 | React 19 + TypeScript + Vite + Zustand |
| 后端 | Spring Boot 3.1.6 + MyBatis Plus |
| 数据库 | PostgreSQL / 达梦 / 人大金仓 |
| 安全 | Spring Security + JWT + SM3/SM4 |
| 测试 | JUnit 5 + Mockito + Vitest |

---

## 🔒 安全特性

- **三员分立** - 系统管理员、安全管理员、审计管理员互斥
- **权限控制** - 基于 RBAC 的细粒度权限管理
- **审计日志** - 全操作审计追溯，哈希链防篡改
- **XSS 防护** - 用户输入过滤
- **登录限流** - 防暴力破解（5次失败锁定）
- **国密支持** - SM2/SM3/SM4 算法支持

---

## 🧪 测试覆盖

系统已通过全面体检，共 **123+ 测试用例**：

| 测试类型 | 测试类 | 用例数 | 状态 |
|:---|:---|:---|:---|
| 后端 Service | 13 | 74+ | ✅ |
| 后端 Controller | 9 | 40+ | ✅ |
| 前端组件 | 3 | 36 | ✅ |
| 集成测试 | 3 | 10+ | ✅ |

---

## 📝 文档更新记录

| 日期 | 更新内容 |
|:---|:---|
| 2025-12-10 | 新增 SM4 加密字段长度规范、修复归档审批 500 错误 (V33 迁移) |
| 2025-12-09 | 新增故障排除案例、AI 协作文档、多 Agent 任务分工章节 |
| 2025-12-09 | 添加 AI 开发指南，更新测试覆盖统计，完善技术栈说明 |
| 2025-12-08 | 系统全面体检，完成 123 个测试用例，修复 XSS 漏洞 |
| 2025-12-07 | 重构文档目录，新增功能模块、权限管理、用友集成文档 |
| 2025-12-06 | 新增离线安装包文档 |
| 2025-12-05 | 新增故障排除指南 |

---

## 📞 技术支持

如有问题，请参考 [故障排除指南](deployment/故障排除指南.md) 或联系技术支持。
