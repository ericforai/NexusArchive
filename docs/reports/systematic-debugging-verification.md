# Systematic Debugging 验证报告

**日期**: 2026-01-07
**问题**: ErpConfig.getTargetAccbookCode() 映射方向 Bug
**方法**: Systematic Debugging 四阶段流程

---

## 执行概要

| 阶段 | 状态 | 关键发现 |
|------|------|---------|
| **Phase 1: 根因调查** | ✅ 完成 | Bug 已稳定复现，确认原始代码 `map.get(currentFonds)` 返回 null |
| **Phase 2: 模式分析** | ✅ 完成 | 在 `ErpConfigServiceImpl.java:230` 找到相同的反向查找模式 |
| **Phase 3: 假设与测试** | ✅ 完成 | 18 个测试全部通过，假设验证成功 |
| **Phase 4: 实施与验证** | ✅ 完成 | 单一修复，架构测试 24/24 通过 |

---

## Phase 1: Root Cause Investigation (根因调查)

### 1.1 Bug 复现

**测试程序** (`/tmp/SystematicDebugReproduction.java`):

```
【原始 Bug 代码】
  代码: accbookMapping.get(currentFonds)
  结果: null
  ❌ BUG: 返回 null，会抛出 IllegalStateException
  原因: "FONDS_A" 是 VALUE，不是 KEY

【修复后的代码】
  代码: accbookMapping.entrySet().stream()
        .filter(e -> currentFonds.equals(e.getValue()))
        .map(Map.Entry::getKey)
        .findFirst()
  结果: BR01
  ✅ 正确: 返回了对应的 accbookCode (BR01)
```

### 1.2 数据结构分析

```
accbookMapping = {BR01: FONDS_A, BR02: FONDS_B, BR03: FONDS_C}
                  ^^^^   ^^^^^^^
                  KEY     VALUE
                 (账套)   (全宗)

currentFonds = "FONDS_A"  ← 这是 VALUE，不是 KEY！

原始代码: map.get("FONDS_A")  → 查找 KEY="FONDS_A" → 不存在 → null
修复代码: 按 VALUE="FONDS_A" 查找 → 返回 KEY="BR01" → 正确 ✅
```

---

## Phase 2: Pattern Analysis (模式分析)

### 2.1 代码库中的参考模式

在 `ErpConfigServiceImpl.java:230` 找到**相同的反向查找模式**：

```java
// 用于验证全宗唯一性的代码
String duplicates = fondsCount.entrySet().stream()
    .filter(e -> e.getValue() > 1)      // ← 按值过滤
    .map(e -> e.getKey() + ...)         // ← 取出键
    .collect(Collectors.joining(", "));
```

### 2.2 模式对比

| 方面 | 错误代码 | 正确代码 | 代码库参考 |
|------|---------|---------|-----------|
| 方法 | `map.get(key)` | `entrySet().stream().filter(e -> value).map(e -> key)` | ErpConfigServiceImpl:230 |
| 查找方向 | KEY → VALUE | VALUE → KEY | 相同 |
| 数据结构理解 | 错误 | 正确 | 一致 |

---

## Phase 3: Hypothesis and Testing (假设与测试)

### 3.1 假设陈述

> **假设**: 使用 `entrySet().stream().filter(e -> currentFonds.equals(e.getValue())).map(Map.Entry::getKey)` 能够正确地从 `{accbookCode: fondsCode}` 映射中，根据全宗编码(VALUE)找到对应的账套编码(KEY)。

### 3.2 测试结果

```
[INFO] Running com.nexusarchive.integration.erp.dto.ErpConfigTests
[INFO] Tests run: 3, Failures: 0, Errors: 0  ← BuilderTests
[INFO] Tests run: 3, Failures: 0, Errors: 0  ← ResolveAllAccbookCodesTests
[INFO] Tests run: 4, Failures: 0, Errors: 0  ← GetTargetAccbookCodeLegacyTests
[INFO] Tests run: 8, Failures: 0, Errors: 0  ← GetTargetAccbookCodeWithMappingTests
[INFO]
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 3.3 测试覆盖

| 测试类 | 测试数 | 覆盖场景 |
|--------|-------|---------|
| GetTargetAccbookCodeWithMappingTests | 8 | 正常查找、缺失映射、null/blank 检查、空映射 |
| GetTargetAccbookCodeLegacyTests | 4 | 向后兼容（accbookCode/accbookCodes） |
| ResolveAllAccbookCodesTests | 3 | 废弃方法行为 |
| BuilderTests | 3 | Lombok 注解验证 |
| **总计** | **18** | **100% 通过** |

---

## Phase 4: Implementation and Verification (实施与验证)

### 4.1 单一修复确认

**修改文件**: `ErpConfig.java:129-141`

```java
// 修复代码（仅修改核心查找逻辑）
String targetAccbook = accbookMapping.entrySet().stream()
        .filter(e -> currentFonds.equals(e.getValue()))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
```

**确认**:
- ✅ 只修改了核心查找逻辑
- ✅ 没有额外改动
- ✅ 保留了向后兼容性

### 4.2 回归测试结果

| 测试类别 | 结果 | 说明 |
|---------|------|------|
| 单元测试 | 18/18 PASS | ErpConfigTests |
| 架构测试 | 24/24 PASS | ArchitectureTest |
| 向后兼容 | ✅ PASS | Legacy fallback 正常工作 |

### 4.3 边界条件覆盖

| 场景 | 输入 | 预期 | 实际 |
|------|------|------|------|
| 正常查找 | FONDS_A → BR01 | BR01 | ✅ |
| 缺失映射 | FONDS_X | Exception | ✅ |
| 空 currentFonds | "" | Exception | ✅ |
| Null currentFonds | null | Exception | ✅ |
| 空映射 | {} | Exception | ✅ |

---

## 最终结论

### Iron Law 遵守情况

```
NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

| 规则 | 状态 | 证据 |
|------|------|------|
| 先读错误消息 | ✅ | 分析了 Map.get() 返回 null 的原因 |
| 稳定复现 | ✅ | SystematicDebugReproduction.java |
| 检查最近更改 | ✅ | 确认是新增功能的逻辑错误 |
| 找到工作示例 | ✅ | ErpConfigServiceImpl.java:230 |
| 单一假设 | ✅ | 明确陈述并验证 |
| 创建失败用例 | ✅ | 18 个测试用例 |
| 单一修复 | ✅ | 只修改了查找逻辑 |

### Verdict: **[PASS]** ✅

**修复已通过 Systematic Debugging 四阶段验证**:
1. ✅ 根因已明确理解并复现
2. ✅ 修复基于代码库中的既有模式
3. ✅ 测试覆盖了所有关键场景
4. ✅ 回归测试通过
5. ✅ 架构测试通过

---

*生成时间: 2026-01-07*
*调试方法: Systematic Debugging*
*测试工具: JUnit 5, Maven Surefire*
