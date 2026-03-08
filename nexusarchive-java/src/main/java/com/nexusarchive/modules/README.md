一旦我所属的文件夹有所变化，请更新我。
本目录存放后端业务模块（模块化领域划分）。
统一采用 api/app/domain/infra 四层结构。

## 模块清单

| 模块 | 说明 | 状态 |
| --- | --- | --- |
| `_template/` | DDD 模块模板（新建模块起点） | 模板 |
| `borrowing/` | 借阅模块（试点） | 生产 |
| `signature/` | 档案签名验签基础模块 | 生产 |

## 如何创建新模块

1. 复制 `_template/` 目录
2. 重命名为新模块名
3. 替换 `{ModuleName}` 和 `{module}` 占位符
4. 更新本文件的模块清单
5. 运行验证命令

详见：[模块创建 SOP](../../../../docs/architecture/backend-module-creation-sop.md)

## 架构规则

模块必须遵守以下 ArchUnit 规则（共 7 条）：

1. **分层架构约束**：API → Application → Domain/Infrastructure
2. **领域实体封箱**：API 层不得暴露数据库实体
3. **Borrowing 模块公开契约**：外部只能访问 app 层和 api.dto 层
4. **Borrowing 内部约束**：不得依赖传统分层
5. **DDD 模块独立性**：不得依赖 service.impl/controller
6. **传统分层访问约束**：只能通过 Facade 访问模块
7. **禁止模块间循环依赖**

验证命令：
```bash
mvn test -Dtest=ModuleBoundaryTest
```

## 目录结构规范

```
{module}/
├── api/           # 对外接口（Controller + DTO）
├── app/           # 应用层（Facade + 用例编排）
├── domain/        # 领域层（Entity + Repository 接口）
└── infra/         # 基础设施（Mapper + Repository 实现）
```

## 依赖方向

```
外部 → api.dto
外部 → app (Facade)

api → app
app → domain
app → infra
infra → domain (实现接口)
```
