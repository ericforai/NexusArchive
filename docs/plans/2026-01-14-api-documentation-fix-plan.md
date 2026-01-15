# API 文档修复计划

> **版本**: v1.0.0
> **创建日期**: 2026-01-14
> **目标受众**: AI (Claude Code, Cursor, Copilot)
> **目的**: 为 AI 提供精确、结构化的 API 规格，避免幻觉和上下文丢失

---

## 执行摘要

| 维度 | 现状 | 目标 | 差距 |
|:-----|:-----|:-----|:-----|
| 后端 Controller | 64 个 | 64 个 | ✅ 数量一致 |
| 前端 API 客户端 | 41 个 | 同步生成 | 🔴 需自动化 |
| OpenAPI 规格 | 部分覆盖 | 100% 覆盖 | 🔴 需补全注解 |
| AI 元数据 | 无 | 有 | 🔴 需添加 |

**核心策略**: 手动标注优先 → OpenAPI 导出 → AI 友好增强

---

## 一、架构设计

### 1.1 单源真理体系

```
┌─────────────────────────────────────────────────────────┐
│  Controller 层 (Swagger 注解 - 手动维护)                 │
│  @Tag, @Operation, @Parameter, @ApiResponse             │
└─────────────────────────────────────────────────────────┘
           ↓ springdoc-openapi (已安装 v2.3.0)
┌─────────────────────────────────────────────────────────┐
│  openapi.yaml (机器可读规格 - Maven 生成)                │
│  - 完整类型签名                                          │
│  - AI 元数据扩展                                         │
└─────────────────────────────────────────────────────────┘
           ↓ openapi-typescript
┌─────────────────────────────────────────────────────────┐
│  src/api/types.ts (前端类型同步 - 自动生成)              │
└─────────────────────────────────────────────────────────┘
```

### 1.2 现有基础设施

项目已具备以下组件，无需额外安装：

- ✅ `springdoc-openapi-starter-webmvc-ui:2.3.0`
- ✅ `OpenApiConfig.java` - 13 个 API 分组配置
- ✅ JWT Bearer 认证配置
- ✅ Swagger UI: `http://localhost:19090/api/swagger-ui.html`

**需要添加**：
1. Maven 插件导出 `openapi.yaml`
2. Controller 补全 Swagger 注解
3. AI 专用元数据扩展

---

## 二、Swagger 注解规范

### 2.1 类级别注解模板

```java
@Tag(
    name = "Archive Management",
    description = """
    电子会计档案核心管理接口，符合 DA/T 94-2022。

    **权限要求**: 所有接口需要 archive:read 或更高权限
    **全宗隔离**: 自动按用户的全宗权限过滤数据
    """,
    externalDocs = @ExternalDocumentation(
        description = "完整文档",
        url = "/docs/api/archives.md"
    )
)
public class ArchiveController { }
```

### 2.2 接口级别注解模板

```java
@Operation(
    summary = "分页查询档案",
    description = """
    根据条件分页检索档案列表。

    - 支持标题/档号模糊搜索
    - 自动按用户全宗过滤
    - 返回 DTO，不暴露 Entity
    """,
    operationId = "listArchives",  // 重要：AI 生成客户端时使用
    tags = {"Archives"}
)
@Parameters({
    @Parameter(
        name = "page",
        description = "页码（从1开始）",
        example = "1",
        schema = @Schema(minimum = "1", defaultValue = "1")
    ),
    @Parameter(
        name = "limit",
        description = "每页条数",
        example = "10",
        schema = @Schema(maximum = "100", defaultValue = "10")
    )
})
@ApiResponse(
    responseCode = "200",
    description = "成功",
    content = @Content(schema = @Schema(implementation = PageResponse.class))
)
@ApiResponse(responseCode = "401", description = "未认证")
@ApiResponse(responseCode = "403", description = "权限不足")
public Result<Page<ArchiveResponse>> list(...) { }
```

### 2.3 DTO 注解模板

```java
@Schema(description = "档案响应 DTO")
public class ArchiveResponse {

    @Schema(description = "档案ID", example = "123456", required = true)
    private String id;

    @Schema(description = "档案编号", example = "COMP001-2024-10Y-FIN-AC01-V9900")
    private String archiveCode;

    @Schema(description = "档案标题", example = "2024年度会计凭证")
    private String title;

    // ...
}
```

---

## 三、分阶段实施路线图

### 3.1 阶段划分

| 阶段 | 模块 | Controller 数量 | 优先级 | 预计工作量 |
|:-----|:-----|:---------------|:-------|:-----------|
| **Phase 1** | Auth + Archives + Volumes | 3 | P0 | 1-2 天 |
| **Phase 2** | Destruction + AuditLog + Scan | 3 | P1 | 1 天 |
| **Phase 3** | Admin + User + Role + Permission | 4 | P1 | 1-2 天 |
| **Phase 4** | ERP Integration + YonSuite | 5 | P2 | 1-2 天 |
| **Phase 5** | 其他 49 个 Controller | 49 | P3 | 持续 |

### 3.2 Phase 1 (POC) 详细拆解

#### AuthController
- `POST /auth/login` - 用户登录
- `POST /auth/logout` - 用户登出
- `GET /auth/me` - 获取当前用户信息
- `POST /auth/refresh` - 刷新 Token

#### ArchiveController
- `GET /archives` - 分页查询档案
- `GET /archives/{id}` - 获取档案详情
- `GET /archives/{id}/files` - 获取档案关联文件
- `GET /archives/recent` - 获取最近档案
- `POST /archives` - 创建档案
- `PUT /archives/{id}` - 更新档案
- `DELETE /archives/{id}` - 删除档案

#### VolumeController
- `GET /volumes` - 分页查询案卷
- `GET /volumes/{id}` - 获取案卷详情
- `POST /volumes/assemble` - 按月组卷

---

## 四、工具链配置

### 4.1 Maven 插件 (pom.xml)

```xml
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.4</version>
    <goals>
        <goal>generate</goal>
    </goals>
    <configuration>
        <apiDocsUrl>http://localhost:19090/api/v3/api-docs</apiDocsUrl>
        <outputFileName>openapi.yaml</outputFileName>
        <outputDir>${project.basedir}/../docs/api</outputDir>
    </configuration>
</plugin>
```

### 4.2 前端自动化 (package.json)

```json
{
  "scripts": {
    "api:types": "openapi-typescript docs/api/openapi.yaml -o src/api/types.ts",
    "api:validate": "spectral lint docs/api/openapi.yaml"
  },
  "devDependencies": {
    "openapi-typescript": "^7.4.0",
    "@stoplight/spectral-cli": "^6.11.0"
  }
}
```

### 4.3 CI 检查 (.github/workflows/api-doc-check.yml)

```yaml
name: API Documentation Check
on: [pull_request]
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Backend
        run: cd nexusarchive-java && mvn clean compile
      - name: Generate OpenAPI
        run: cd nexusarchive-java && mvn springdoc:generate
      - name: Check for Changes
        run: |
          git diff --exit-code docs/api/openapi.yaml \
            || echo "::error::API changed, please update documentation!"
```

---

## 五、AI 友好增强

### 5.1 AI 元数据扩展

在 `openapi.yaml` 中添加：

```yaml
openapi: 3.1.0
info:
  title: NexusArchive API
  version: 2.0.0
  x-ai-context: |
    # 系统背景
    电子会计档案管理系统，符合 DA/T 94-2022 标准。

    # 核心概念
    - 全宗(Fonds): 数据隔离单位，每个用户只能访问授权的全宗
    - 档案(Archive): 会计凭证/账簿/报表的数字化记录
    - 案卷(Volume): 按月/年组织的档案集合

    # 权限体系
    - archive:read, archive:manage, nav:all
    - 三员分立: system_admin, security_admin, audit_admin

    # AI 使用指南
    1. 所有接口返回统一的 Result<T> 结构
    2. 分页参数: page (从1开始), limit (最大100)
    3. 时间格式: ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)
    4. 金额类型: DECIMAL(18,2)，字符串表示
```

### 5.2 输出文件结构

```
docs/api/
├── openapi.yaml           # 机器可读规格（AI 主要读取）
├── openapi.json           # JSON 格式
├── README.md              # 人类导读
└── modules/               # 分模块导读（可选）
    ├── auth.md
    ├── archives.md
    └── ...
```

---

## 六、质量保障

### 6.1 文档同步检查清单

每次变更 Controller 时必须检查：

- [ ] `operationId` 是否唯一且语义化
- [ ] 所有 `@Parameter` 有 `description` 和 `example`
- [ ] 所有 `@ApiResponse` 定义了错误场景
- [ ] DTO 的 `@Schema` 包含字段说明
- [ ] 构建后 `openapi.yaml` 已更新

### 6.2 维护节奏

| 触发条件 | 动作 |
|:---------|:-----|
| 新增 Controller | 立即添加完整注解 |
| 修改接口签名 | 同步更新 `@Operation` |
| 新增 DTO | 添加 `@Schema` 注解 |
| 每周五 | CI 自动验证文档一致性 |

---

## 七、成功标准

1. **覆盖率**: 100% 的 Controller 有完整 Swagger 注解
2. **机器可读**: `openapi.yaml` 可被 openapi-typescript 解析
3. **类型同步**: 前端可自动生成类型定义
4. **AI 友好**: Claude/Cursor 可准确理解接口签名

---

## 八、下一步行动

1. ✅ 设计已完成
2. ⏳ 添加 Maven 插件配置
3. ⏳ 为 Phase 1 Controller 添加完整注解
4. ⏳ 生成初始 `openapi.yaml`
5. ⏳ 添加 AI 元数据扩展
6. ⏳ 配置 CI 检查

---

**文档生成者**: Claude Opus 4.5
**头脑风暴方法**: superpowers:brainstorming
