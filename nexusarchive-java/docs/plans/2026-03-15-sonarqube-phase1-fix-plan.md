# SonarQube Phase 1 修复计划

## 问题概述

| 级别 | 数量 | 说明 |
|------|------|------|
| **VULNERABILITY** | 2 | 安全漏洞，最高优先级 |
| **BLOCKER** | 35 | 阻塞级问题，可能导致系统故障 |
| **CRITICAL** | 317 | 严重问题，影响代码质量 |

---

## 第一阶段：安全漏洞修复 (P0 - 最高优先级)

### VULNERABILITY-1: S2647 - Basic Authentication 不安全
**文件**: `TimestampService.java:197`

**问题**:
```java
// 第 194-197 行
String auth = tsaUsername + ":" + tsaPassword;
String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
```

**风险**: Basic Authentication 将凭证以 Base64 编码传输，可轻易解码，存在凭证泄露风险。

**修复方案**:
1. 使用 OAuth 2.0 Client Credentials Flow (推荐)
2. 或使用 API Key + HMAC 签名认证
3. 如必须用 Basic Auth，强制使用 HTTPS 并在配置文档中明确风险

**工作量**: 2-3 小时
**依赖**: TSA 服务端支持 OAuth 2.0

---

### VULNERABILITY-2: S3329 - IV 硬编码问题
**文件**: `YonSuiteEventCrypto.java:57`

**问题**:
```java
// 第 55 行 - IV 是 AESKey 的前 16 位，虽非硬编码但协议固定
byte[] iv = Arrays.copyOfRange(aesKey, 0, 16);
```

**风险**: IV 应该随机生成，固定 IV 会降低相同明文加密的安全性。

**分析**: 此为 YonSuite 官方协议要求，需与用友确认是否可修改。

**修复方案**:
1. **短期**: 添加注释说明这是外部协议要求，非自主设计缺陷
2. **长期**: 与用友协商改用随机 IV + 预留 IV 传输方式

**工作量**: 0.5 小时 (添加注释)
**依赖**: 外部协议，可能无法修改

---

## 第二阶段：BLOCKER 问题修复 (P1)

### BLOCKER-1: S2095 - 资源未关闭
**文件**: `ArchiveExportServiceImpl.java`

**问题分析**:
- `HttpURLConnection` 在 `TimestampService.java` 中未正确关闭
- 临时文件/目录未正确清理

**修复方案**:
```java
// 使用 try-with-resources
try (InputStream is = conn.getInputStream()) {
    byte[] response = is.readAllBytes();
    // ...
}
```

**工作量**: 1 小时
**风险**: 低

---

### BLOCKER-2: S2229 - 路径遍历风险 (4处)

| 文件 | 行号 | 问题 |
|------|------|------|
| `AuditLogService.java` | - | 需验证路径输入 |
| `ArchiveStateTransitionService.java` | - | 需验证路径输入 |
| `BorrowExpirationServiceImpl.java` | - | 需验证路径输入 |

**修复方案**:
1. 使用 `PathSecurityUtils.validatePath()` 进行路径验证
2. 禁止 `../` 等跳转字符
3. 限制在允许的根目录内

```java
// 示例修复
Path resolved = allowedRootPath.resolve(userInput).normalize();
if (!resolved.startsWith(allowedRootPath)) {
    throw new SecurityException("路径遍历攻击检测");
}
```

**工作量**: 2-3 小时
**风险**: 中 - 可能影响现有功能

---

### BLOCKER-3: S2699 - 测试未覆盖 (4处)

| 文件 | 问题 |
|------|------|
| `ArchiveStateTransitionService` | 需添加单元测试 |
| 其他相关服务 | 需添加测试覆盖 |

**修复方案**:
1. 为关键服务添加单元测试
2. 目标覆盖率达到 80%

**工作量**: 4-6 小时
**风险**: 低

---

## 第三阶段：CRITICAL 问题修复 (P2)

### CRITICAL-1: S1192 - 字符串字面量重复 (58处)

**问题**: 相同字符串在多处重复定义，应提取为常量

**示例**:
```java
// 错误: 重复字面量
conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
conn.setRequestProperty("Content-Type", "application/timestamp-query");

// 正确: 提取常量
private static final String HEADER_AUTHORIZATION = "Authorization";
private static final String HEADER_CONTENT_TYPE = "Content-Type";
private static final String CONTENT_TYPE_TIMESTAMP_QUERY = "application/timestamp-query";
```

**批量修复策略**:
1. **按模块分组**: 为每个包创建 `Constants` 类
2. **优先级**: HTTP 头 > 错误消息 > 状态值
3. **自动化**: 使用 IDE "Extract Constant" 重构功能

**工作量**: 8-12 小时
**风险**: 低 (纯重构，不改变逻辑)

---

### CRITICAL-2: S3776 - 认知复杂度过高 (26处)

**问题**: 函数复杂度超过 15，难以理解和维护

**常见模式**:
```java
// 高复杂度示例
public void complexMethod() {
    if (condition1) {
        if (condition2) {
            for (item : items) {
                if (item.isValid()) {
                    // 深层嵌套
                }
            }
        }
    }
}
```

**重构策略**:
1. **提取方法**: 将复杂逻辑拆分为小函数
2. **卫语句**: 提前返回，减少嵌套
3. **策略模式**: 用多态替代复杂条件

**工作量**: 16-24 小时 (按 26 处计算)
**风险**: 中 - 需充分测试

---

## 修复顺序建议

### Week 1: 安全漏洞 (P0)
```
Day 1-2: VULNERABILITY-1 (Basic Auth) - 修复并测试
Day 3:   VULNERABILITY-2 (IV) - 添加注释和文档
```

### Week 2-3: BLOCKER 问题 (P1)
```
Week 2:
  Day 1-2:  BLOCKER-1 (资源关闭)
  Day 3-4:  BLOCKER-2 (路径遍历)
  Day 5:    BLOCKER-3 开始 (测试覆盖)

Week 3:
  Day 1-3:  完成测试覆盖
  Day 4-5:  验证和回归测试
```

### Week 4-6: CRITICAL 问题 (P2)
```
Week 4: S1192 字符串常量提取 (批量处理)
Week 5-6: S3776 复杂度重构 (按模块逐步进行)
```

---

## 并行策略

### 可并行处理的任务

| 任务组 | 可并行原因 |
|--------|-----------|
| S1192 字符串常量提取 | 不同模块互不影响 |
| S3776 复杂度重构 | 不同类互不影响 |
| 测试覆盖补充 | 独立测试类 |

### 必须串行的任务
1. 安全漏洞修复 (必须先修复)
2. 路径遍历修复 (需统一工具类)
3. 资源关闭修复 (需统一模式)

---

## 风险评估

| 风险类型 | 描述 | 缓解措施 |
|----------|------|----------|
| **功能回归** | 修改可能影响现有功能 | 1. 完整的回归测试<br>2. 分批上线<br>3. 保留回滚方案 |
| **性能影响** | 路径验证可能增加开销 | 1. 性能测试<br>2. 优化热点路径 |
| **外部依赖** | Basic Auth 修改需 TSA 支持 | 1. 提前与 TSA 供应商沟通<br>2. 准备降级方案 |
| **工作量超预期** | 复杂度重构可能耗时更长 | 1. 优先处理简单问题<br>2. 分阶段交付 |

---

## 验收标准

### 安全漏洞
- [ ] S2647: Basic Auth 替换为 OAuth 2.0 或文档化风险
- [ ] S3329: 添加注释说明外部协议要求

### BLOCKER
- [ ] S2095: 所有资源使用 try-with-resources
- [ ] S2229: 所有路径输入经过验证
- [ ] S2699: 关键服务测试覆盖 >= 80%

### CRITICAL
- [ ] S1192: 重复字符串减少 80%
- [ ] S3776: 最高复杂度 <= 15

---

## 成功指标

| 指标 | 目标 | 当前 |
|------|------|------|
| Vulnerability | 0 | 2 |
| Blocker | 0 | 35 |
| Critical | < 50 | 317 |
| 测试覆盖率 | >= 80% | 待测 |
| 最大复杂度 | <= 15 | 待测 |

---

## 附录：修复检查清单

### 修复前检查
- [ ] 确认问题真实存在 (非误报)
- [ ] 理解问题上下文和影响范围
- [ ] 准备测试用例

### 修复中检查
- [ ] 遵循项目代码规范
- [ ] 添加/更新单元测试
- [ ] 更新相关文档

### 修复后检查
- [ ] 本地测试通过
- [ ] SonarQube 扫描通过
- [ ] 代码审查通过
- [ ] 集成测试通过

---

*计划制定日期: 2026-03-15*
*预计完成日期: 2026-04-15*
*负责人: 待分配*
