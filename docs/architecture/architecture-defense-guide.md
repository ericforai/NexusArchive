# 架构防御机制实施指南

**版本**: v1.0
**更新日期**: 2026-01-01
**状态**: ✅ 生产就绪

---

## 概述

本项目已建立完整的架构防御机制，确保代码架构稳定性和可维护性。

### 核心原则

**Architecture = Code** - 架构即代码，结构由代码本身定义，而非外部文档

### 四大支柱 (J1-J4)

| 支柱 | 说明 | 实现状态 |
|------|------|----------|
| **J1: Self-Description** | 每个模块有自描述清单 | ✅ 23 个模块已记录 |
| **J2: Self-Check** | 内置检查工具持续验证 | ✅ 前后端工具就绪 |
| **J3: Closed Rules** | 规则应用于构建流程 | ✅ Pre-commit + CI |
| **J4: Reflex** | 违规被 CI 阻止 | ✅ error 级别规则 |

---

## 前端架构防御

### 工具: dependency-cruiser

**配置文件**: `.dependency-cruiser.cjs`

**核心规则**:
```javascript
{
  name: 'no-circular',
  severity: 'error',  // 阻止提交
  comment: '循环依赖检测'
}
```

**使用方式**:
```bash
# 本地检查
npm run check:arch

# 生成依赖图
npm run check:arch:graph
```

**当前状态**: ✅ **0 violations** (240 modules, 806 dependencies)

### 已解决的循环依赖

**问题**: `client.ts → store → fonds.ts → client.ts`

**解决方案**: 依赖倒置模式
```typescript
// client.types.ts - 定义接口
export interface HttpClientStateProvider {
  getState(): Partial<HttpClientState>;
}

// client.ts - 使用接口
const state = getHttpClientState();

// useAuthStore.ts - 注册提供者
registerAuthProvider({ getState: () => ({ token }) });
```

---

## 后端架构防御

### 工具: ArchUnit

**测试文件**: `nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java`

**活跃规则** (7 条):

| 规则 | 验证内容 | 状态 |
|------|----------|------|
| `noCyclicDependencies` | 无循环依赖 | ✅ 已启用 |
| `controllersShouldOnlyDependOnServiceInterfaces` | 控制器不依赖实现类 | ✅ 已启用 |
| `persistenceAnnotationsOnlyInEntities` | 持久化注解位置正确 | ✅ 已启用 |
| `modularServicesShouldBeIndependent` | 模块化服务独立性 | ✅ 已启用 |
| `facadeShouldBeTheOnlyPublicEntry` | Facade 作为唯一入口 | ✅ 已启用 |
| `pluginsShouldNotDependOnServiceImpl` | 插件不依赖实现类 | ✅ 已启用 |
| `configClassesShouldBeInConfigPackage` | 配置类位置正确 | ✅ 已启用 |

**已禁用规则** (待修复):

| 规则 | 违规数量 | 说明 |
|------|----------|------|
| `controllersShouldNotDependOnMappers` | 39 处 | 7 个控制器直接依赖 Mapper |
| `controllersShouldNotThrowBusinessExceptions` | 9 处 | 3 个控制器抛出业务异常 |

**运行方式**:
```bash
# 运行架构测试
mvn test -Dtest=ArchitectureTest

# 运行特定规则
mvn test -Dtest=ArchitectureTest#noCyclicDependencies
```

**当前状态**: ✅ **7/7 tests passed**

---

## 自动化机制

### 模块治理 API

**基础路径**: `/api/governance`

| 端点 | 方法 | 说明 |
|------|------|------|
| `/modules` | GET | 获取所有模块信息 |
| `/modules/discover` | GET | 自动发现新模块 |
| `/modules/validate` | GET | 验证清单一致性 |
| `/modules/export` | GET | 导出 JSON 格式 |
| `/modules/dependencies` | GET | 获取依赖关系 |
| `/modules/metrics` | GET | 获取度量指标 |

### 使用示例

```bash
# 发现新模块
curl http://localhost:19090/api/governance/modules/discover

# 验证清单
curl http://localhost:19090/api/governance/modules/validate

# 导出 JSON
curl http://localhost:19090/api/governance/modules/export
```

---

## CI/CD 集成

### Pre-commit Hook

**文件**: `.husky/pre-commit`

```bash
#!/bin/sh
echo "🔍 Running architecture checks..."

# 前端架构检查
npm run check:arch || {
  echo "❌ Frontend architecture check failed!"
  exit 1
}

# 后端架构测试
mvn test -Dtest=ArchitectureTest -q || {
  echo "❌ Backend architecture check failed!"
  exit 1
}

echo "✅ All architecture checks passed!"
```

### GitHub Actions

**文件**: `.github/workflows/architecture.yml`

```yaml
name: Architecture Check

on:
  pull_request:
  push:
    branches: [main]

jobs:
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm run check:arch

  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      - working-directory: ./nexusarchive-java
        run: mvn test -Dtest=ArchitectureTest
```

---

## 模块清单

### 模块总览

**前端**: 3 个模块 (FE.SYS, FE.ADMIN, FE.SHARED)

**后端**: 23 个模块

| 分类 | 数量 | 模块 |
|------|------|------|
| 核心架构层 | 7 | CONTROLLER, SERVICE, MAPPER, ENTITY, DTO, CONFIG, SECURITY |
| 模块化组件 | 4 | BORROWING, INGEST, VOUCHER, MATCHING |
| 集成层 | 2 | INTEGRATION, ERP_PLUGINS |
| 基础设施 | 10 | COMMON, UTIL, ANNOTATION, ASPECT, EVENT, LISTENER, SERIALIZER, EXCEPTION, REPOSITORY, INFRASTRUCTURE |

### 清单文件

**文档**: `docs/architecture/module-manifest.md`

**版本**: v2.1.0

**导出 API**: `/api/governance/modules/export`

---

## 常见问题

### Q1: 如何处理架构违规？

**步骤**:
1. 运行 `npm run check:arch` 或 `mvn test -Dtest=ArchitectureTest` 查看详细错误
2. 根据错误信息修复代码
3. 重新运行测试验证
4. 提交时 pre-commit hook 会自动验证

### Q2: 添加新模块需要做什么？

1. **创建模块**（遵循模块化原则）
2. **更新清单** (`docs/architecture/module-manifest.md`)
3. **运行验证** (`GET /api/governance/modules/validate`)
4. **添加架构规则** (如需要，更新 `ArchitectureTest.java`)

### Q3: 如何添加新的架构规则？

**前端**: 编辑 `.dependency-cruiser.cjs`
```javascript
{
  name: 'my-new-rule',
  severity: 'error',
  from: { path: '^src/my-module/' },
  to: { path: '^src/forbidden/' }
}
```

**后端**: 编辑 `ArchitectureTest.java`
```java
@Test
void myNewRule() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..my-module..")
        .should().dependOnClassesThat()
        .resideInAPackage("..forbidden..");

    rule.check(importedClasses);
}
```

---

## 维护指南

### 日常开发

- ✅ **提交前**: 本地运行 `npm run check:arch` 和架构测试
- ✅ **代码审查**: 检查是否违反架构规则
- ✅ **添加模块**: 更新 `module-manifest.md`

### 定期维护

- 📅 **每月**: 运行 `/api/governance/modules/discover` 发现新模块
- 📅 **每季度**: 审查架构规则，调整阈值
- 📅 **每半年**: 评估模块化质量，重构熵增模块

### 紧急情况

如果需要紧急绕过架构检查：
```bash
# 跳过 pre-commit hook
git commit --no-verify -m "emergency fix"

# 注意：必须事后跟进修复！
```

---

## 成功指标

| 指标 | 目标 | 当前 |
|------|------|------|
| 前端违规数 | 0 | ✅ 0 |
| 后端测试通过率 | 100% | ✅ 100% (7/7) |
| 模块清单覆盖率 | >90% | ✅ 100% |
| CI 架构检查通过率 | 100% | ✅ 通过 |

---

## 参考资料

- [dependency-cruiser 文档](https://github.com/sverweij/dependency-cruiser)
- [ArchUnit 文档](https://www.archunit.org/)
- [模块清单文档](./module-manifest.md)
- [架构审查报告](./module-catalog-2025-12-31.md)
