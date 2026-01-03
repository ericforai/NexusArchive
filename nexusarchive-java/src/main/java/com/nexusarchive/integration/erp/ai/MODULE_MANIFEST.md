# ERP AI 模块清单

> **模块 ID**: `integration.erp.ai`
> **所有者**: `team-integration`
> **创建日期**: 2026-01-02
> **版本**: 1.0.0-MVP

---

## 模块边界

### 职责
AI 驱动的 ERP 接口自动适配系统，实现从 OpenAPI 文档到适配器代码的全流程自动化。

### 包结构
```
com.nexusarchive.integration.erp.ai/
├── agent/          # Agent 编排层（协调各组件）
├── parser/         # 文档解析层（OpenAPI → 定义）
├── mapper/         # 业务映射层（API → 场景）
├── generator/      # 代码生成层（场景 → Java 代码）
├── controller/     # REST API 层（用户交互）
└── config/         # AI 配置和提示词模板
```

---

## Public API（公开接口）

### 入口点
```java
// Agent 编排器（主要入口）
com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator

// REST API 控制器
com.nexusarchive.integration.erp.ai.controller.ErpAdaptationController
```

### 使用方式
```java
// 依赖注入使用
@Autowired
private ErpAdaptationOrchestrator orchestrator;

AdaptationRequest request = AdaptationRequest.builder()
    .erpType("kingdee")
    .erpName("金蝶云星空")
    .apiFiles(files)
    .build();

AdaptationResult result = orchestrator.adapt(request);
```

---

## 依赖白名单（允许导入的模块）

```java
// 可以依赖的模块
canImportFrom = [
    "com.nexusarchive.integration..",      // 集成层接口
    "com.nexusarchive.service..",           // 业务服务
    "com.nexusarchive.dto..",               // 数据传输对象
    "com.nexusarchive.common..",            // 通用工具
    "io.swagger.v3..",                      // Swagger Parser
    "lombok..",                             // Lombok
    "org.springframework..",                  // Spring
    "org.springframework.web.multipart.."    // Spring Web
];
```

### 禁止导入
```java
// 禁止直接依赖
disallowedImports = [
    "com.nexusarchive.mapper..",     // 不能直接依赖 MyBatis Mapper
    "com.nexusarchive.entity..",      // 不能直接依赖实体层
    "com.nexusarchive.controller..*"  // 不能依赖其他 Controller
];
```

---

## 架构约束规则

### 规则 1: 单向依赖
```
Controller → Agent → (Parser | Mapper | Generator) → 共享工具
```
禁止反向依赖。

### 规则 2: 层次分离
- **Parser 层**: 只负责解析，不包含业务逻辑
- **Mapper 层**: 只负责映射，不调用外部 API
- **Generator 层**: 只负责生成代码，不执行代码
- **Agent 层**: 协调各层，不包含具体实现

### 规则 3: 接口隔离
```java
// 公开接口
public interface ApiParser {
    ParseResult parse(MultipartFile file) throws IOException;
}

// 内部实现不暴露
class OpenApiDocumentParser implements ApiParser { ... }
```

---

## 依赖关系矩阵

| 模块 | Parser | Mapper | Generator | Agent | Controller |
|------|--------|--------|-----------|-------|------------|
| Parser | - | ❌ | ❌ | ✅ | ❌ |
| Mapper | ✅ | - | ❌ | ✅ | ❌ |
| Generator | ✅ | ✅ | - | ✅ | ❌ |
| Agent | ✅ | ✅ | ✅ | - | ✅ |
| Controller | ❌ | ❌ | ❌ | ✅ | - |

**说明**: ✅ 表示可以依赖，❌ 表示禁止依赖

---

## 依赖计数（Fan-out）

| 模块 | Fan-out | 状态 |
|------|---------|------|
| OpenApiDocumentParser | 1 (swagger-parser) | ✅ ≤ 5 |
| BusinessSemanticMapper | 0 | ✅ ≤ 5 |
| ErpAdapterCodeGenerator | 0 | ✅ ≤ 5 |
| ErpAdaptationOrchestrator | 3 (Parser + Mapper + Generator) | ✅ ≤ 5 |
| ErpAdaptationController | 1 (Orchestrator) | ✅ ≤ 5 |

---

## 测试策略

### 单元测试覆盖
```
parser/     → OpenApiDocumentParserTest       (2 tests)
mapper/     → BusinessSemanticMapperTest       (4 tests)
generator/  → ErpAdapterCodeGeneratorTest      (4 tests)
agent/      → ErpAdaptationOrchestratorTest    (2 tests)
```

### 集成测试
```java
// 端到端测试：文件上传 → 代码生成
@Test
void endToEndAdaptation() {
    // 1. 上传 OpenAPI 文件
    // 2. 调用 REST API
    // 3. 验证生成的代码
    // 4. 清理临时文件
}
```

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0-MVP | 2026-01-02 | 初始版本，支持 OpenAPI JSON 解析和代码生成 |

---

## 待办事项（Phase 2）

- [ ] 添加 PDF 解析支持
- [ ] 添加 Markdown 文档支持
- [ ] 集成 Claude API 进行智能语义理解
- [ ] 实现代码自动编译和部署
- [ ] 添加运行时学习优化机制
