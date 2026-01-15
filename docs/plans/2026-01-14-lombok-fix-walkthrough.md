# Walkthrough: Lombok 编译修复与后端启动验证

## 变更说明
解决了由于 Lombok 注解处理器在 Java 23 环境下失效以及与手动添加的代码冲突导致的大规模编译错误。

### 核心修复点
1.  **环境对齐**：强制 Maven 使用 Java 21 (Temurin) 而非 Homebrew 默认的 Java 23。
2.  **Lombok 升级**：将 `pom.xml` 中的 Lombok 依赖升级至 `1.18.40`。
3.  **代码清理**：恢复了之前手动添加的 Getters/Setters，消除了与 Lombok 的冲突。
4.  **补全缺失方法**：修复了 `YonSuiteClient.java` 中缺失的 `queryPaymentApplyFileUrls` 方法。

## 验证结果

### 1. 编译验证
运行 `mvn clean compile` 成功，无错误输出。

### 2. 后端启动验证
后端服务成功启动，监听端口 **19090**。

### 3. 健康检查验证
通过 `curl` 验证接口响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "UP",
    ...
  }
}
```

## 下一步计划
- 回归原始任务：修复附件预览 404 错误。
