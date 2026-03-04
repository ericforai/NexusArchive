# NexusArchive API 文档

> **版本**: v2.0.0
> **更新日期**: 2026-01-14
> **目标受众**: AI (Claude Code, Cursor, Copilot) & 开发者

---

## 快速导航

| 文档 | 说明 |
|:-----|:-----|
| `openapi.yaml` | 机器可读的 OpenAPI 3.1 规格文件 |
| `modules/` | 分模块导读（可选）|
| `erp-sso-launch.md` | ERP 发起联查单点登录对接规范（签名、ticket、错误码） |
| `yonsuite-sso-launch.md` | YonSuite 两接口式 SSO 联查对接规范（token + urlPath） |

---

## 获取 OpenAPI 规格

### 方式 1: Maven 生成

```bash
cd nexusarchive-java
mvn clean verify
# 生成的文件位于: docs/api/openapi.yaml
```

### 方式 2: 运行时获取

```bash
# 启动后端服务
cd nexusarchive-java && mvn spring-boot:run

# 获取 OpenAPI JSON
curl http://localhost:19090/api/v3/api-docs

# 获取 OpenAPI YAML
curl http://localhost:19090/api/v3/api-docs.yaml
```

### 方式 3: Swagger UI

访问 http://localhost:19090/api/swagger-ui.html 查看交互式文档。

---

## 前端类型同步

```bash
# 安装依赖
npm install -D openapi-typescript @stoplight/spectral-cli

# 生成类型定义
npm run api:types

# 验证规格
npm run api:validate
```

---

## AI 使用指南

### 核心概念

| 概念 | 说明 |
|:-----|:-----|
| **全宗 (Fonds)** | 数据隔离单位，每个用户只能访问授权的全宗 |
| **档案 (Archive)** | 会计凭证/账簿/报表的数字化记录 |
| **案卷 (Volume)** | 按月/年组织的档案集合 |

### 权限代码

| 代码 | 说明 |
|:-----|:-----|
| `archive:read` | 档案查询 |
| `archive:manage` | 档案管理 |
| `archive:approve` | 档案审批 |
| `nav:all` | 跨全宗访问 |

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 模块分组

| 分组 | 路径前缀 | 说明 |
|:-----|:---------|:-----|
| 01-档案管理 | `/archives/**`, `/volumes/**` | 档案 CRUD、案卷组卷 |
| 02-归档审批 | `/approval/**`, `/batch/**` | 归档审批流程 |
| 03-四性检测 | `/compliance/**` | 真实性/完整性/可用性/安全性 |
| 04-ERP集成 | `/erp/**`, `/yonsuite/**` | 用友 YonSuite 集成 |
| 05-全宗管理 | `/fonds/**`, `/org/**` | 全宗与组织架构 |
| 06-用户权限 | `/auth/**`, `/users/**` | 认证授权 |
| 07-系统管理 | `/admin/**`, `/config/**` | 系统配置 |
| 08-审计日志 | `/audit/**` | 审计日志查询 |
| 09-搜索 | `/search/**` | 全局搜索 |
| 10-异步任务 | `/async/**`, `/tasks/**` | 异步任务监控 |
| 11-销毁管理 | `/destruction/**` | 档案销毁 |
| 12-扫描集成 | `/scan/**` | 扫描工作区 |
| 13-其他接口 | (其他) | 辅助功能 |

---

## 维护说明

### 修改 Controller 时

1. 更新 `@Operation` 的 `description`
2. 添加/更新 `@Parameter` 注解
3. 运行 `mvn clean verify` 重新生成 `openapi.yaml`
4. 提交变更

### 新增 DTO 时

1. 添加类级别 `@Schema(description = "...")`
2. 为每个字段添加 `@Schema` 注解
3. 包含 `example` 和 `required` 标记

---

## 相关文档

- [API 分页接口规范](pagination-api.md)
- [异步接口规范](async-api.md)
- [设计文档](../plans/2026-01-14-api-documentation-fix-plan.md)

---

**生成者**: SpringDoc OpenAPI v2.3.0
**规格版本**: OpenAPI 3.1.0
