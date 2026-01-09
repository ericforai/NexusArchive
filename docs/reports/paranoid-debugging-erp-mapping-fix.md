# Paranoid Debugging Report: ErpConfig Mapping Bug Fix

**Date**: 2026-01-07
**Issue**: ERP Account Set-Fonds Mapping Direction Bug
**Severity**: CRITICAL (Production Blocking)
**Status**: [PASS] ✅

---

## A. Snapshot

| Field | Value |
|-------|-------|
| **Expected Behavior** | Given `accbookMapping = {"BR01": "FONDS_A"}` and `currentFonds = "FONDS_A"`, `getTargetAccbookCode()` returns `"BR01"` |
| **Observed Behavior** | Original code threw `IllegalStateException: "当前全宗未配置对应账套"` because `accbookMapping.get(currentFonds)` returned `null` |
| **Blast Radius** | **Data** - ERP synchronization would fail for all fonds-isolated requests |
| **Reproducibility** | 100% - Deterministic bug in logic |
| **Root Cause Category** | Incorrect data structure understanding (Map.get() searches by KEY, not VALUE) |

### Evidence Links

**Bug Verification (before fix)**:
```bash
# Test program output
Mapping: {BR01=FONDS_A, BR02=FONDS_B}
currentFonds: FONDS_A
accbookMapping.get(currentFonds) = null     # BUG: searches for KEY="FONDS_A"
正确查找结果: BR01                          # CORRECT: searches for VALUE="FONDS_A"
```

**Fix Verification (after fix)**:
```bash
# Test results
mvn test -Dtest=ErpConfigTests
# Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
```

---

## B. First-Principles Decomposition

### Invariants

1. **Map Structure Invariant**: `Map<String, String>` with key=accbookCode, value=fondsCode
2. **Lookup Direction**: Given a fonds (VALUE), find the corresponding accbook (KEY)
3. **1:1 Correspondence**: One fonds maps to exactly one accbook, and vice versa
4. **Fonds Context**: `currentFonds` is injected from `FondsContext` (trusted source)

### Constraints

| Constraint | Value | Rationale |
|------------|-------|-----------|
| Null safety | currentFonds must not be null/blank | System invariant - every request has a fonds context |
| Fallback behavior | Legacy accbookCode/accbookCodes for backward compatibility | Existing deployments may not have migrated |
| Exception type | `IllegalStateException` | Indicates invalid system state |
| Validation | JSON values must be unique (enforced at save time) | 1:1 mapping requirement |

### Minimal Checkable Claims

| Claim | Test | Status |
|-------|------|--------|
| Normal mapping lookup: `{"BR01": "FONDS_A"}` → `currentFonds="FONDS_A"` → returns `"BR01"` | `shouldReturnCorrectAccbookWhenMappingExists()` | ✅ PASS |
| Missing mapping throws `IllegalStateException` | `shouldThrowExceptionWhenFondsNotMapped()` | ✅ PASS |
| Null/blank currentFonds throws exception | `shouldThrowExceptionWhenCurrentFondsIsNull()` | ✅ PASS |
| Legacy accbookCodes fallback works | `shouldUseLegacyAccbookCodesWhenFondsMatches()` | ✅ PASS |
| Mapping is 1:1 (values unique) | Validated by `ErpConfigServiceImpl.validateAccbookMapping()` | ✅ PASS |

---

## C. Q→A Loop

### Round 1: Understanding the Data Structure

**Q**: What is the structure of `accbookMapping`?

**A**: `Map<String, String>` where:
- **Key (KEY)**: Account book code (账套编码), e.g., "BR01", "BR02"
- **Value (VALUE)**: Fonds code (全宗编码), e.g., "FONDS_A", "FONDS_B"

**Test**: Verify JSON structure
```json
{
  "BR01": "FONDS_A",
  "BR02": "FONDS_B"
}
```

**Result**: Confirmed - database stores `{"BR01": "FONDS_A"}` meaning BR01 (账套) maps to FONDS_A (全宗)

**Decision**: Original bug used `accbookMapping.get(currentFonds)` which searches for a KEY equal to the fonds code - but the keys are accbook codes, not fonds codes.

---

### Round 2: Identifying the Bug

**Q**: Why did `accbookMapping.get(currentFonds)` return null?

**A**: Because `Map.get(key)` searches by **KEY**, not by VALUE.
- `currentFonds = "FONDS_A"` (this is a VALUE in the map)
- `accbookMapping.get("FONDS_A")` searches for a KEY="FONDS_A"
- But the actual keys are "BR01", "BR02" (accbook codes)
- Result: `null`

**Test**: Java verification program
```java
Map<String, String> accbookMapping = new HashMap<>();
accbookMapping.put("BR01", "FONDS_A");
String currentFonds = "FONDS_A";
System.out.println(accbookMapping.get(currentFonds)); // prints: null
```

**Result**: Confirmed bug - always returns null when mapping is used

**Decision**: Fix by searching entries where VALUE equals currentFonds, then return the KEY.

---

### Round 3: Implementing the Fix

**Q**: How to correctly search by VALUE?

**A**: Use `entrySet().stream()` to find entry with matching value, then return the key.

**Test**: Fixed code
```java
String targetAccbook = accbookMapping.entrySet().stream()
    .filter(e -> currentFonds.equals(e.getValue()))
    .map(Map.Entry::getKey)
    .findFirst()
    .orElse(null);
```

**Result**: 18 unit tests pass, covering:
- Normal lookup
- Missing mapping (throws exception)
- Null/blank currentFonds (throws exception)
- Empty/null mapping (throws exception)
- Legacy fallback (accbookCode/accbookCodes)
- Builder behavior

**Decision**: Fix is correct and complete. Proceed to validation.

---

## D. Fix Plan

### Root Cause

**Causal Chain**:
1. `accbookMapping` structure: `{accbookCode (KEY): fondsCode (VALUE)}`
2. `currentFonds` contains a **fonds code** (which is a VALUE in the map)
3. Original code: `accbookMapping.get(currentFonds)` searches for KEY = currentFonds
4. Since currentFonds is a VALUE, not a KEY, the search always fails → returns `null`
5. Exception thrown: `"当前全宗未配置对应账套"`

**Summary**: Direction confusion - searched by KEY when should have searched by VALUE.

### Fix

**File**: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpConfig.java:129-141`

```java
// BEFORE (BUGGY):
String targetAccbook = accbookMapping.get(currentFonds);

// AFTER (CORRECT):
String targetAccbook = accbookMapping.entrySet().stream()
    .filter(e -> currentFonds.equals(e.getValue()))
    .map(Map.Entry::getKey)
    .findFirst()
    .orElse(null);

if (targetAccbook == null || targetAccbook.isBlank()) {
    throw new IllegalStateException("当前全宗 [" + currentFonds + "] 未配置对应账套，请检查 ERP 配置的账套-全宗映射");
}
```

### Risk Assessment

| Risk | Mitigation |
|------|------------|
| Stream performance overhead | Mapping size is small (typically < 10 entries), O(n) is acceptable |
| Null handling | Explicit null/blank checks before returning |
| Legacy compatibility | Fallback to accbookCode/accbookCodes preserved |

### New/Updated Tests

**File**: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/dto/ErpConfigTests.java`

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `GetTargetAccbookCodeWithMappingTests` | 8 | Normal lookup, missing mapping, null/blank checks |
| `GetTargetAccbookCodeLegacyTests` | 4 | Backward compatibility with accbookCode/accbookCodes |
| `ResolveAllAccbookCodesTests` | 3 | Deprecated method behavior |
| `BuilderTests` | 3 | Lombok annotation verification |
| **Total** | **18** | **All passing** |

---

## E. Ship Gate

### Verdict: **[PASS]** ✅

### Binary Verdict Checklist

| Condition | Verification | Status |
|-----------|--------------|--------|
| Bug is reproducible | Verified with Java test program | ✅ |
| Bug is fixed after changes | 18 tests pass, all assertions green | ✅ |
| New tests cover root cause | 8 tests for new mapping logic | ✅ |
| Regression tests pass | Legacy fallback tested (4 tests) | ✅ |
| Architecture tests pass | 24 ArchUnit tests pass | ✅ |
| Edge cases covered | Null, blank, empty, missing mapping | ✅ |
| 1:1 uniqueness validated | JSON value uniqueness enforced | ✅ |

### Evidence Summary

```
=== Bug Reproduction ===
accbookMapping.get(currentFonds) = null
正确查找结果: BR01

=== Test Results ===
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

=== Architecture Tests ===
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

### Deliverables Checklist

- [x] Bug fix commit: `ErpConfig.getTargetAccbookCode()` at line 129-141
- [x] Test file: `ErpConfigTests.java` (18 tests, 100% pass rate)
- [x] Reproduction script: `/tmp/VerifyMappingBug.java`
- [x] Documentation updated: integration/erp/dto/README.md

### Final Notes

**Impact**: This was a CRITICAL bug that would have prevented all ERP synchronization from working in multi-fonds environments. The fix ensures proper routing from fonds context to the correct ERP account book.

**Confidence**: HIGH - Comprehensive test coverage with 18 tests including edge cases, legacy compatibility, and proper exception handling.

**Recommendation**: Safe to deploy. The fix is minimal, well-tested, and preserves backward compatibility.

---

*Generated using Paranoid Debugging methodology - Evidence-driven, binary verdict, no assumptions.*
