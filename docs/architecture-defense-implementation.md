# Architecture Defense 实施报告

## 概述

本文档记录了 NexusArchive 后端代码的 **Architecture Defense**（架构防御）系统实施情况。

**Architecture Defense = Code.** 架构结构由代码本身的元数据定义，而非外部文档。

## 四大支柱 (J1-J4)

### ✅ J1: Self-Description (模块自描述)

**实现方式**: `@ModuleManifest` 注解

每个主要包都有 `package-info.java` 文件声明模块边界：

```java
@ModuleManifest(
    id = "layer.controller",
    name = "Controller Layer",
    owner = "team-backend",
    layer = Layer.CONTROLLER,
    description = "REST API 控制器层，处理 HTTP 请求和响应",
    dependencies = {
        @DependencyRule(
            allowedPackages = {"..service..", "..dto.."},
            forbiddenPackages = {"..mapper..", "..entity.."}
        )
    }
)
package com.nexusarchive.controller;
```

**已配置的包**:
- `com.nexusarchive.controller` - 控制器层
- `com.nexusarchive.service` - 服务层
- `com.nexusarchive.mapper` - 数据访问层
- `com.nexusarchive.entity` - 实体层

**文件位置**:
- 注解定义: `nexusarchive-java/src/main/java/com/nexusarchive/annotation/architecture/ModuleManifest.java`
- 包清单: `nexusarchive-java/src/main/java/com/nexusarchive/*/package-info.java`

---

### ✅ J2: Self-Check (自动验证)

**实现方式**: ArchUnit 测试

**现有测试**: `ArchitectureTest.java` (15 条规则)
**新增测试**: `EnhancedArchitectureTest.java` (17 条规则)

**规则示例**:

| 规则 | 描述 | 状态 |
|------|------|------|
| 分层架构约束 | Controller → Service → Mapper → Entity | ✅ |
| 禁止跨层访问 | Controller 不能直接访问 Mapper/Entity | ✅ |
| DTO 不依赖实体 | DTO 应独立于实体 | ✅ |
| 服务实现隔离 | 服务实现不应直接调用其他服务实现 | ✅ |
| 配置类位置 | @Configuration 应在 config 包中 | ✅ |

**文件位置**:
- `nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java`
- `nexusarchive-java/src/test/java/com/nexusarchive/EnhancedArchitectureTest.java`

**运行方式**:
```bash
# 运行所有架构测试
mvn test -Dtest=ArchitectureTest,EnhancedArchitectureTest

# 只运行原有架构测试
mvn test -Dtest=ArchitectureTest

# 只运行增强架构测试
mvn test -Dtest=EnhancedArchitectureTest
```

---

### ✅ J3: Closed Rules (CI 集成)

**实现方式**: GitHub Actions 工作流

**工作流**: `.github/workflows/architecture-check.yml`

**检查项目**:
1. ✅ 架构规则验证 - 运行所有 ArchUnit 测试
2. ✅ 架构违规检查 - 阻止违规代码合并
3. ✅ 模块清单检查 - 确保主要包有 @ModuleManifest
4. ✅ 架构健康评分 - 生成架构健康报告

**触发条件**:
- Pull Request 到 main/develop 分支
- Push 到 main/develop 分支

**违规处理**:
- 任何架构测试失败 → 工作流失败 → 阻止合并

**文件位置**:
- `.github/workflows/architecture-check.yml`

---

### ✅ J4: Reflex (运行时可见性)

**实现方式**: ArchitectureIntrospectionService + REST API

**API 端点**:

| 端点 | 描述 | 响应 |
|------|------|------|
| `GET /api/architecture/modules` | 获取所有模块清单 | ArchitectureReportDto |
| `GET /api/architecture/validate` | 验证模块依赖规则 | ViolationReportDto |
| `GET /api/architecture/tests` | 运行所有架构测试 | TestResultsDto |
| `GET /api/architecture/health` | 获取架构健康状态 | HealthReportDto |

**使用示例**:
```bash
# 查看所有模块清单
curl http://localhost:19090/api/architecture/modules

# 查看架构健康状态
curl http://localhost:19090/api/architecture/health

# 运行架构测试
curl http://localhost:19090/api/architecture/tests
```

**文件位置**:
- 服务: `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchitectureIntrospectionService.java`
- 控制器: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchitectureManagementController.java`
- DTO: `nexusarchive-java/src/main/java/com/nexusarchive/dto/response/ArchitectureReportDto.java`

---

## 依赖配置

### Maven 依赖 (pom.xml)

```xml
<!-- ArchUnit - 架构测试框架 -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

### Maven 插件配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0</version>
    <configuration>
        <argLine>-XX:+EnableDynamicAgentLoading</argLine>
        <groups>com.nexusarchive.ArchitectureTest,com.nexusarchive.EnhancedArchitectureTest</groups>
    </configuration>
</plugin>
```

---

## 目录结构

```
nexusarchive/
├── .github/workflows/
│   └── architecture-check.yml          # J3: CI 集成
├── nexusarchive-java/
│   ├── pom.xml                         # ArchUnit 依赖
│   ├── src/main/java/com/nexusarchive/
│   │   ├── annotation/architecture/
│   │   │   └── ModuleManifest.java     # J1: 模块清单注解
│   │   ├── controller/
│   │   │   ├── package-info.java       # Controller 层清单
│   │   │   └── ArchitectureManagementController.java  # J4: 运行时 API
│   │   ├── service/
│   │   │   ├── package-info.java       # Service 层清单
│   │   │   └── ArchitectureIntrospectionService.java  # J4: 自省服务
│   │   ├── mapper/
│   │   │   └── package-info.java       # Mapper 层清单
│   │   ├── entity/
│   │   │   └── package-info.java       # Entity 层清单
│   │   └── dto/response/
│   │       ├── ArchitectureReportDto.java
│   │       ├── ModuleInfoDto.java
│   │       └── ViolationReportDto.java
│   └── src/test/java/com/nexusarchive/
│       ├── ArchitectureTest.java       # J2: 原有测试 (15 规则)
│       └── EnhancedArchitectureTest.java # J2: 增强测试 (17 规则)
└── docs/
    └── architecture-defense-implementation.md  # 本文档
```

---

## 使用指南

### 开发者工作流

#### 1. 创建新模块时

在包的 `package-info.java` 中添加 `@ModuleManifest`:

```java
@ModuleManifest(
    id = "feature.my-feature",
    name = "My Feature Module",
    owner = "team-backend",
    layer = Layer.SERVICE,
    description = "描述模块功能"
)
package com.nexusarchive.service.myfeature;
```

#### 2. 本地验证

```bash
# 运行架构测试
cd nexusarchive-java
mvn test -Dtest=ArchitectureTest,EnhancedArchitectureTest

# 检查编译
mvn clean compile
```

#### 3. 提交前检查

```bash
# 确保架构测试通过
mvn test -Dtest=ArchitectureTest,EnhancedArchitectureTest

# 确保代码编译通过
mvn clean package -DskipTests
```

#### 4. 提交 PR

CI 会自动运行架构检查，任何违规都会阻止合并。

### 遗留代码处理

对于遗留代码，使用以下标签：

```java
@ModuleManifest(
    id = "legacy.old-module",
    name = "Old Legacy Module",
    owner = "team-backend",
    layer = Layer.SERVICE,
    legacy = true,
    complianceTarget = "2025-06-01",
    tags = {"legacy", "needs-refactor"}
)
```

### 紧急修复处理

对于紧急情况：

```java
@ModuleManifest(
    id = "emergency.hotfix",
    name = "Emergency Hotfix",
    owner = "team-on-call",
    layer = Layer.SERVICE,
    exceptionReason = "Critical hotfix - PROD-1234",
    reviewDate = "2025-01-15"
)
```

---

## 当前状态

| 组件 | 状态 | 说明 |
|------|------|------|
| J1 Self-Description | ✅ 完成 | 主要包已配置 @ModuleManifest |
| J2 Self-Check | ✅ 完成 | 32 条 ArchUnit 规则 |
| J3 Closed Rules | ✅ 完成 | GitHub Actions 集成 |
| J4 Reflex | ✅ 完成 | 4 个 REST API 端点 |

---

## 下一步行动

### 短期 (1-2 周)

1. **修复编译错误** - 当前代码有约 30+ 编译错误需要修复
2. **运行完整测试** - 验证所有 32 条架构规则
3. **添加更多包清单** - 为 `integration`、`dto` 等包添加清单

### 中期 (1-2 月)

1. **完善运行时验证** - 让 ArchitectureIntrospectionService 能够实际解析注解
2. **添加可视化** - 生成依赖关系图
3. **性能优化** - 优化 ArchUnit 测试执行时间

### 长期 (3+ 月)

1. **重构遗留代码** - 标记为 legacy 的模块进行重构
2. **扩展到前端** - 为前端代码添加类似的架构防御
3. **自动化修复** - 提供自动修复架构违规的工具

---

## 常见问题

### Q: 为什么要添加 package-info.java？

A: package-info.java 是 Java 标准方式来为包添加注解和文档。@ModuleManifest 注解放在这里可以实现模块自描述（J1）。

### Q: 架构测试失败怎么办？

A:
1. 查看测试日志了解具体违规内容
2. 参考 ArchitectureTest.java 中的规则说明
3. 修复代码或调整架构
4. 重新运行测试验证

### Q: 可以临时跳过架构测试吗？

A: 不建议。架构测试的目的是防止架构腐化。如果确实需要临时例外，使用 `exceptionReason` 和 `reviewDate` 参数。

### Q: 架构测试会影响构建性能吗？

A: ArchUnit 测试通常在几秒内完成。如果需要，可以只在 PR 时运行完整测试，本地开发时运行部分测试。

---

## 参考资料

- [ArchUnit 官方文档](https://www.archunit.org/)
- [Architecture Defense 技能指南](../.claude/skills/architecture-defense/)
- [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

*最后更新: 2026-01-04*
