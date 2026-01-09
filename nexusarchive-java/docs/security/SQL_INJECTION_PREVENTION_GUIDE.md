# SQL Injection Prevention Guide

## Overview

This document provides guidelines for preventing SQL injection vulnerabilities in the NexusArchive project. It outlines secure coding practices, common risks, and mitigation strategies.

## Severity Classification

| Level | Description | Example |
|-------|-------------|---------|
| **CRITICAL** | Direct user input concatenated into SQL | `"SELECT * FROM table WHERE id = " + userInput` |
| **HIGH** | Dynamic SQL without proper validation | `.last("LIMIT " + limit)` where limit is user input |
| **MEDIUM** | Complex query building with user input | `.apply("column = '" + value + "'")` |
| **LOW** | Indirect risk, mitigated by existing controls | White-listed values with validation |

## Secure Coding Rules

### Rule 1: Always Use Parameterized Queries

**PROHIBITED**:
```java
// DON'T: Direct string concatenation
@Select("SELECT * FROM acc_archive WHERE fonds_no = '" + fondsNo + "'")
List<Archive> findByFondsNo(String fondsNo);
```

**REQUIRED**:
```java
// DO: Use #{} placeholder (MyBatis parameter binding)
@Select("SELECT * FROM acc_archive WHERE fonds_no = #{fondsNo}")
List<Archive> findByFondsNo(@Param("fondsNo") String fondsNo);
```

### Rule 2: Validate Numeric Parameters Used in SQL Fragments

When using `.last()`, `.apply()`, or similar methods that append SQL fragments:

**PROHIBITED**:
```java
// DON'T: Unvalidated user input
public List<Archive> getRecentArchives(int limit) {
    wrapper.last("LIMIT " + limit);  // RISK: user could pass "1000; DROP TABLE archives;--"
}
```

**REQUIRED**:
```java
// DO: Validate and sanitize
public List<Archive> getRecentArchives(int limit) {
    // Security boundary check
    int safeLimit = Math.max(1, Math.min(100, limit));
    wrapper.last("LIMIT " + safeLimit);
}
```

### Rule 3: Add Controller-Layer Validation

Use Jakarta validation annotations on Controller parameters:

```java
@GetMapping("/recent")
public Result<List<Archive>> recent(
        @RequestParam(defaultValue = "5")
        @Min(value = 1, message = "limit must be greater than 0")
        @Max(value = 100, message = "limit cannot exceed 100") int limit) {
    return Result.success(archiveService.getRecentArchives(limit));
}
```

### Rule 4: Use MyBatis-Plus LambdaQueryWrapper

LambdaQueryWrapper provides type-safe query building:

```java
// PREFERRED: Type-safe, injection-proof
LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Archive::getFondsNo, fondsNo)
       .like(Archive::getTitle, keyword)
       .gt(Archive::getAmount, minAmount);
```

### Rule 5: Whitelist Validation for Dynamic Values

When dynamic values are unavoidable, use whitelist validation:

```java
public static boolean isValidSubType(String subType, String categoryCode) {
    if ("AC02".equals(categoryCode)) {
        return Set.of("GENERAL_LEDGER", "SUBSIDIARY_LEDGER", "JOURNAL",
                      "CASH_BOOK", "BANK_BOOK", "CASH_JOURNAL", "BANK_JOURNAL",
                      "FIXED_ASSETS_CARD", "OTHER_BOOKS").contains(subType);
    }
    // ... other categories
    return false;
}
```

### Rule 6: Escape Special Characters in JSON Queries

When querying JSONB columns:

```java
private String escapeJson(String input) {
    if (input == null) return "";
    return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
}

// Combined with whitelist validation
if (isValidSubType(subType, categoryCode)) {
    wrapper.apply("custom_metadata::jsonb @> {0}::jsonb",
            String.format("{\"bookType\":\"%s\"}", escapeJson(subType)));
}
```

## Current Project Status

### Files Reviewed

| Category | Location | Status |
|----------|----------|--------|
| **Controllers** | `/controller/` | Reviewed |
| **Services** | `/service/` | Reviewed |
| **Mappers** | `/mapper/` | Reviewed |
| **XML Resources** | `/resources/` | None found |

### Findings Summary

| Risk Level | Count | Status |
|------------|-------|--------|
| **CRITICAL** | 0 | N/A |
| **HIGH** | 1 | FIXED |
| **MEDIUM** | 0 | N/A |
| **LOW** | 15+ | Acceptable (hardcoded values) |

### Fixed Issues

#### 1. ArchiveController.recent() - HIGH Risk (FIXED)

**Issue**: User-provided `limit` parameter used directly in SQL concatenation.

**Fix Applied**:
- Controller layer: Added `@Min(1)` and `@Max(100)` validation
- Service layer: Added `Math.max(1, Math.min(100, limit))` boundary check

**Files Modified**:
- `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveController.java`
- `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveService.java`

### Acceptable Uses of `.last("LIMIT ...")`

The following uses are **safe** because they use hardcoded constant values:

| File | Line | Value | Context |
|------|------|-------|---------|
| `ArchiveSecurityServiceImpl.java` | 150 | `pageSize = 100` | Internal variable |
| `PerformanceMetricsServiceImpl.java` | 240 | `"LIMIT 1"` | Hardcoded |
| `PerformanceMetricsServiceImpl.java` | 287 | `"LIMIT 1"` | Hardcoded |
| `ArchiveBatchService.java` | 156 | `"LIMIT 1"` | Hardcoded |
| `PreviewFilePathResolver.java` | 86 | `"LIMIT 1"` | Hardcoded |
| `ArchiveFileController.java` | 217 | `"LIMIT 1"` | Hardcoded |
| `AuditLogVerificationServiceImpl.java` | 201 | `"LIMIT 1"` | Hardcoded |
| `PoolServiceImpl.java` | 103 | `"LIMIT 50"` | Hardcoded |
| `PoolServiceImpl.java` | 115 | `"LIMIT 1"` | Hardcoded |
| `PoolServiceImpl.java` | 253 | `"LIMIT 1"` | Hardcoded |
| `NotificationServiceImpl.java` | 40 | `"LIMIT 5"` | Hardcoded |
| `NotificationServiceImpl.java` | 59 | `"LIMIT 3"` | Hardcoded |
| `GlobalSearchServiceImpl.java` | 66 | `"LIMIT 20"` | Hardcoded |
| `GlobalSearchServiceImpl.java` | 87 | `"LIMIT 20"` | Hardcoded |
| `EnterpriseArchitectureServiceImpl.java` | 130 | `"LIMIT 1000"` | Hardcoded |
| `ErpScenarioService.java` | 243 | `"LIMIT 10"` | Hardcoded |
| `ArchiveFileContentService.java` | 41 | `"LIMIT 1"` | Hardcoded |

### MyBatis @Select Annotations

All `@Select`, `@Update`, `@Insert`, `@Delete` annotations in the project correctly use `#{}` parameter binding. No `${}` string substitution was found in Mapper annotations.

## Code Review Checklist

When reviewing code for SQL injection risks:

- [ ] Are all user input parameters validated?
- [ ] Does the code use `#{}` placeholders instead of string concatenation?
- [ ] Are numeric parameters bounded (min/max checks)?
- [ ] Is `LambdaQueryWrapper` preferred over `QueryWrapper` with string column names?
- [ ] Are `.last()`, `.apply()`, or similar SQL fragment methods using only constants?
- [ ] Is whitelist validation applied to enumerated values?
- [ ] Are JSON queries properly escaped and validated?

## Testing Recommendations

1. **Unit Tests**: Add tests for boundary conditions (negative numbers, extremely large values)
2. **Security Tests**: Include SQL injection payloads in integration tests
3. **Static Analysis**: Run tools like SpotBugs or SonarQube regularly

## References

- [OWASP SQL Injection](https://owasp.org/www-community/attacks/SQL_Injection)
- [MyBatis Parameter Binding](https://mybatis.org/mybatis-3/sqlmap-xml.html#Parameters)
- [Jakarta Bean Validation](https://beanvalidation.org/2.0/spec/)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-09
**Reviewed By**: Security Audit
