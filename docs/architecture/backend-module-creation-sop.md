# 后端 DDD 模块创建 SOP

> 本文档描述如何在 NexusArchive 后端创建一个新的 DDD 模块。

## 前置条件

1. 模块有明确的业务边界（单一职责）
2. 模块不会与现有服务产生大量耦合
3. 已阅读 `modules/_template/README.md`

## 步骤 1：复制模板

```bash
cd nexusarchive-java/src/main/java/com/nexusarchive/modules/
cp -r _template/ {your-module}/
```

将 `{your-module}` 替换为模块名（如 `voucher`、`archive` 等）。

## 步骤 2：重命名占位符

在所有模板文件中搜索并替换：

| 占位符 | 替换为 |
|--------|--------|
| `{ModuleName}` | PascalCase 模块名（如 `Voucher`） |
| `{module}` | 小写模块名（如 `voucher`） |

**查找替换示例**：
- `{ModuleName}Controller` → `VoucherController`
- `{ModuleName}Facade` → `VoucherFacade`
- `{ModuleName}Repository` → `VoucherRepository`

## 步骤 3：创建文件结构

确保以下目录和文件存在：

```
{your-module}/
├── README.md
├── api/
│   ├── README.md
│   ├── {ModuleName}Controller.java
│   └── dto/
│       ├── {ModuleName}Request.java
│       ├── {ModuleName}Response.java
│       └── {ModuleName}Dto.java
├── app/
│   ├── README.md
│   ├── {ModuleName}Facade.java
│   └── {ModuleName}ApplicationService.java (可选)
├── domain/
│   ├── README.md
│   ├── {ModuleName}.java (聚合根)
│   ├── {ModuleName}Status.java (枚举)
│   └── {ModuleName}Repository.java (接口)
└── infra/
    ├── README.md
    ├── mapper/
    │   └── {ModuleName}Mapper.java
    └── {ModuleName}RepositoryImpl.java
```

## 步骤 4：更新模块清单

编辑 `modules/README.md`，添加新模块：

```markdown
| 模块 | 说明 | 状态 |
| --- | --- | --- |
| {your-module}/ | {模块描述} | 开发/生产 |
```

## 步骤 5：添加必要的注释

每个类文件必须包含三行头注释：

```java
// Input: 依赖的模块/框架
// Output: 类名
// Pos: 模块/层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
```

## 步骤 6：运行验证

### 6.1 编译检查

```bash
cd nexusarchive-java
mvn clean compile
```

### 6.2 架构测试

```bash
mvn test -Dtest=ModuleBoundaryTest
```

**预期结果**：所有 7 条规则通过。

### 6.3 单元测试（可选）

为模块创建测试类：

```bash
# 创建测试目录
mkdir -p src/test/java/com/nexusarchive/modules/{your-module}/

# 创建测试类
touch src/test/java/com/nexusarchive/modules/{your-module}/{ModuleName}FacadeTest.java
```

## 步骤 7：提交代码

```bash
git add modules/{your-module}/ modules/README.md
git commit -m "feat(module): add {ModuleName} DDD module

- Add api layer with Controller and DTOs
- Add app layer with Facade
- Add domain layer with entities and repository interface
- Add infra layer with Mapper and repository implementation

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

## 常见问题

### Q: 模块间如何交互？

A: 模块 A 依赖模块 B 时：
- 模块 A 可以依赖模块 B 的 `app` 层（Facade）
- 模块 A 可以依赖模块 B 的 `api.dto` 层（DTO）
- 模块 A **禁止**依赖模块 B 的 `domain` 或 `infra` 层

### Q: 如何处理共享逻辑？

A: 有以下选择：
1. 放入 `common/` 包（纯工具类）
2. 创建独立的共享模块
3. 使用依赖倒置（在 domain 层定义接口）

### Q: 是否所有模块都需要四层？

A: 是的。即使是简单模块，也建议保持四层结构以确保一致性。

## 相关文档

- [模块边界规范](module-boundaries.md)
- [模块清单](../nexusarchive-java/src/main/java/com/nexusarchive/modules/README.md)
- [ArchUnit 测试](../nexusarchive-java/src/test/java/com/nexusarchive/architecture/ModuleBoundaryTest.java)
