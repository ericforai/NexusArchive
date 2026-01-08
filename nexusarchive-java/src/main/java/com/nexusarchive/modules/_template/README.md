一旦我所属的文件夹有所变化，请更新我。
本目录是 DDD 模块的标准模板，创建新模块时请复制此目录。

## 如何使用

1. 复制整个 `_template/` 目录
2. 重命名为新模块名（如 `voucher/`, `archive/` 等）
3. 替换所有文件中的 `{ModuleName}` 占位符
4. 更新 `../README.md` 模块清单
5. 运行验证: `mvn test -Dgroups=architecture`

## 目录结构

- `api/`: 对外接口层（Controller + DTO）
  - 对外暴露 REST API
  - 定义请求/响应 DTO
  - 仅依赖 app 层

- `app/`: 应用层（Facade / 用例编排）
  - 编排业务流程
  - 实现 Facade 接口
  - 协调 domain 和 infra

- `domain/`: 领域层（Entity、Value Object、Status）
  - 核心业务逻辑
  - 领域模型
  - 无基础设施依赖

- `infra/`: 基础设施层（Mapper、Repository 实现）
  - 数据访问
  - 外部服务调用
  - 实现 domain 定义的接口

## 依赖规则

```
┌─────────────────────────────────────────┐
│  api/        → 只依赖 app/              │
├─────────────────────────────────────────┤
│  app/        → 依赖 domain/ + infra/    │
├─────────────────────────────────────────┤
│  domain/     → 纯业务逻辑，无依赖       │
├─────────────────────────────────────────┤
│  infra/      → 实现 domain 接口         │
└─────────────────────────────────────────┘
```

## 对外契约

- 外部只能依赖 `com.nexusarchive.modules.{module}.app..`
- 外部只能依赖 `com.nexusarchive.modules.{module}.api.dto..`
- 禁止外部访问 `domain/` 和 `infra/`

## 验证命令

```bash
# 编译检查
mvn clean compile

# 架构测试
mvn test -Dgroups=architecture
```
