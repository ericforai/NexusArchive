# Phase 2 修复计划 - Bug 修复和 Code Smell 降低

**制定日期**: 2026-03-15
**基于**: 并行代理分析结果

---

## 执行摘要

| 指标 | 当前值 | 目标值 | 差距 |
|------|--------|--------|------|
| **Bug** | ~55 | <30 | -25 |
| **Code Smell** | ~1398 | <2000 | 已达标 ✅ |
| **测试覆盖率** | ~23% | 80% | +57% |

---

## 问题统计

### Bug 问题分类

| 类型 | 数量 | 严重程度 | 示例 |
|------|------|----------|------|
| 空指针风险 | 10 | HIGH | `request.getAttribute("userId")` 直接强制转换 |
| 资源泄漏 | 3 | MEDIUM | FileWriter 未使用 try-with-resources |
| NumberFormatException | 8 | MEDIUM | parseInt/parseLong 未捕获 |
| 数组越界 | 12 | MEDIUM | split() 后直接访问元素 |
| 并发问题 | 2 | LOW | RateLimitFilter 非原子操作 |
| **总计** | **~35** | - | |

### Code Smell 分类

| 类型 | 数量 | 严重程度 | 示例 |
|------|------|----------|------|
| 过大类 (>400行) | 12 | MEDIUM | ComplianceController (518行) |
| 重复代码 | 5 | HIGH | ArchiveFileController 文件名编码 |
| 过长参数列表 | 8 | HIGH | AuditLogService 13个参数 |
| 复杂方法 | 15 | MEDIUM | PoolServiceImpl.searchCandidates |
| 魔法数字 | 5 | LOW | 提取为常量 |
| **总计** | **~45** | - | |

### 测试覆盖缺口

| 层 | 当前覆盖率 | 缺失关键模块 |
|----|-----------|-------------|
| Controller | 31% | 58 个无测试 |
| Service | 40% | 40+ 个无测试 |
| Security | 10% | JwtUtil, SM3Utils 无测试 |
| Mapper | 0% | 全部无测试 |

---

## 修复计划

### P0 - 高优先级 Bug 修复 (预计 2 小时)

| # | 文件 | 问题 | 修复方案 |
|---|------|------|----------|
| 1 | `PoolController.java` | 空指针风险 | 添加 null 检查或使用工具方法 |
| 2 | `ArchiveController.java` | 空指针风险 | 同上 |
| 3 | `OriginalVoucherController.java` | 空指针风险 | 同上 |
| 4 | `StandardReportGenerator.java` | 资源泄漏 | try-with-resources |
| 5 | `PreviewHelper.java` | NumberFormatException | 添加 try-catch |
| 6 | `LoginAttemptService.java` | NumberFormatException | 改进日志 |

### P1 - Code Smell 修复 (预计 3 小时)

| # | 文件 | 问题 | 修复方案 |
|---|------|------|----------|
| 1 | `ArchiveFileController.java` | 重复代码 | 提取 `HttpHeaderUtils.encodeFileName()` |
| 2 | `AuditLogService.java` | 过长参数 | 使用 `LogRequest` DTO |
| 3 | `VoucherEntryDto.java` | 过长参数 | 添加 `@Builder` |
| 4 | `VoucherHeadDto.java` | 过长参数 | 添加 `@Builder` |
| 5 | `PoolServiceImpl.java` | 复杂方法 | 提取子方法 |

### P2 - 测试覆盖提升 (预计 5 小时)

| # | 模块 | 优先级 | 测试类型 |
|---|------|--------|----------|
| 1 | `JwtUtil`, `SM3Utils`, `PasswordUtil` | P0 | 单元测试 |
| 2 | `FourNatureCheckServiceImpl` | P0 | 单元测试 |
| 3 | `DestructionApprovalServiceImpl` | P0 | 单元测试 |
| 4 | `AuthTicketServiceImpl` | P1 | 单元测试 |
| 5 | `MfaServiceImpl` | P1 | 单元测试 |

---

## 执行顺序

```
Step 1: P0 Bug 修复（2小时）
├── 空指针风险防护
├── 资源泄漏修复
└── 异常处理改进
    │
    ▼ 编译验证
Step 2: P1 Code Smell 修复（3小时）
├── 提取重复代码
├── DTO Builder 模式
└── 方法拆分
    │
    ▼ 编译验证
Step 3: P2 测试补充（5小时）
├── 安全模块测试
├── 核心业务测试
└── 集成测试
    │
    ▼ 覆盖率验证
```

---

## 验证清单

- [ ] `mvn clean compile` - 编译通过
- [ ] `mvn test` - 测试通过
- [ ] 覆盖率 > 50% (中期目标)
- [ ] 无 CRITICAL安全问题

---

**文档版本**: v1.0
**状态**: 待用户确认
