# Phase 1 剩余问题修复计划

**制定日期**: 2026-03-15
**基于**: 并行代理分析结果
**目标**: CRITICAL 从 296 降至 <100

---

## 执行摘要

| 指标 | 当前值 | 目标值 | 差距 |
|------|--------|--------|------|
| **Vulnerability** | 2 | 0 | -2 |
| **CRITICAL** | 296 | <100 | -196 |
| **Bug** | 55 | <30 | -25 |

---

## 问题分类

### P0 - 紧急修复 (安全)

| 任务 | 文件 | 问题 | 预计时间 |
|------|------|------|----------|
| P0-1 | `MfaServiceImpl.java` | TOTP 硬编码 "000000" | 30 分钟 |
| P0-2 | `PoolController.java:219` | HTTP 响应头注入 | 15 分钟 |
| P0-3 | `ModuleDiscoveryService.java:124` | 命令注入风险 | 20 分钟 |

### P1 - 高优先级 (安全)

| 任务 | 文件 | 问题 | 预计时间 |
|------|------|------|----------|
| P1-1 | `XmlInvoiceParser.java:94,108` | 空 catch 块 | 15 分钟 |
| P1-2 | `FourNatureCheckServiceImpl.java:203,253,279` | 空 catch 块 | 15 分钟 |
| P1-3 | `CompilationService.java` | 命令注入 | 20 分钟 |
| P1-4 | `TestExecutionService.java` | 命令注入 | 20 分钟 |
| P1-5 | `SmartParserServiceImpl.java` | 路径遍历 | 15 分钟 |
| P1-6 | `PoolHelper.java` | 路径遍历 | 15 分钟 |

### P2 - 中优先级 (代码质量)

| 任务 | 文件 | 问题 | 预计时间 |
|------|------|------|----------|
| P2-1 | `PasswordHashGenerator.java` | System.out.println | 10 分钟 |
| P2-2 | `LocalAuditBuffer.java` | System.err.println | 10 分钟 |
| P2-3 | `NexusArchiveApplication.java` | System.out.println | 5 分钟 |
| P2-4 | `HighlightMetaBatchProcessor.java` | 路径验证 | 15 分钟 |
| P2-5 | `OfdSignatureHelper.java` | 路径验证 | 15 分钟 |

---

## 详细修复方案

### P0-1: MFA 服务安全缺陷

**问题**: `MfaServiceImpl.java` TOTP 验证返回硬编码值

```java
// ❌ 当前代码 (第 36-48 行)
public String generateTotpSecret(String userId) {
    return "JBSWY3DPEHPK3PXP"; // 硬编码
}

public boolean verifyTotp(String userId, String code) {
    return true; // 总是返回 true
}

public List<String> generateBackupCodes(int count) {
    return List.of("000000", "111111"); // 硬编码
}
```

```java
// ✅ 修复后
public String generateTotpSecret(String userId) {
    throw new UnsupportedOperationException(
        "MFA TOTP 功能尚未实现。请配置外部 MFA 提供商（如 Yundun OIDC）"
    );
}

public boolean verifyTotp(String userId, String code) {
    log.warn("MFA TOTP 验证功能未实现，拒绝所有验证请求");
    return false;
}
```

### P0-2: HTTP 响应头注入

**问题**: `PoolController.java:219` 直接拼接用户输入

```java
// ❌ 当前代码
String headerValue = "attachment; filename=\"" + name + "\"";
response.setHeader("Content-Disposition", headerValue);
```

```java
// ✅ 修复后
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

String safeName = name.replaceAll("[^a-zA-Z0-9._-]", "_");
String headerValue = "attachment; filename=\"" + safeName + "\"; filename*=UTF-8''" +
                     URLEncoder.encode(name, StandardCharsets.UTF_8);
response.setHeader("Content-Disposition", headerValue);
```

### P0-3: 命令注入

**问题**: `ModuleDiscoveryService.java:124` 字符串分割

```java
// ❌ 当前代码
String[] cmd = FRONTEND_DISCOVERY_SCRIPT.split(" ");
ProcessBuilder pb = new ProcessBuilder(cmd);
```

```java
// ✅ 修复后
ProcessBuilder pb = new ProcessBuilder(
    "/bin/bash",
    "-c",
    FRONTEND_DISCOVERY_SCRIPT
);
pb.directory(new File(frontendPath));
```

### P1-1 & P1-2: 空 catch 块

```java
// ❌ 当前代码
try {
    parseXml();
} catch (Exception ignored) {}

// ✅ 修复后
try {
    parseXml();
} catch (Exception e) {
    log.error("XML 解析失败: {}", e.getMessage(), e);
}
```

### P2-1 ~ P2-3: 调试代码替换

```java
// ❌ 当前代码
System.out.println("Password: " + password);

// ✅ 修复后
log.debug("Password hash generated for user: {}", username);
// 生产环境自动过滤敏感日志
```

---

## 执行顺序

```
Step 1: P0 修复（安全紧急）
├── P0-1: MFA 服务
├── P0-2: HTTP 响应头注入
└── P0-3: 命令注入
    │
    ▼ 运行测试验证
Step 2: P1 修复（高优先级）
├── P1-1 ~ P1-2: 空 catch 块
├── P1-3 ~ P1-4: 命令注入
└── P1-5 ~ P1-6: 路径遍历
    │
    ▼ 运行测试验证
Step 3: P2 修复（代码质量）
├── P2-1 ~ P2-3: 调试代码
└── P2-4 ~ P2-5: 路径验证
    │
    ▼ 运行测试 + 代码审查
```

---

## 验证清单

每个步骤完成后执行：

- [ ] `mvn clean compile` - 编译通过
- [ ] `mvn test` - 单元测试通过
- [ ] `mvn test -Dgroups=architecture` - 架构测试通过
- [ ] 手动验证修复的功能

---

## 文件清单

### P0 文件
- `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/MfaServiceImpl.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/controller/PoolController.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/governance/discovery/ModuleDiscoveryService.java`

### P1 文件
- `nexusarchive-java/src/main/java/com/nexusarchive/service/parser/impl/XmlInvoiceParser.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FourNatureCheckServiceImpl.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/CompilationService.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/TestExecutionService.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/SmartParserServiceImpl.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/helper/PoolHelper.java`

### P2 文件
- `nexusarchive-java/src/main/java/com/nexusarchive/util/PasswordHashGenerator.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/LocalAuditBuffer.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/NexusArchiveApplication.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/HighlightMetaBatchProcessor.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/OfdSignatureHelper.java`

---

**文档版本**: v1.0
**更新日期**: 2026-03-15
**状态**: 待用户确认
