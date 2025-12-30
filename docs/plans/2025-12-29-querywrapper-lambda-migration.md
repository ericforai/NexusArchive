# QueryWrapper Lambda Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate all hardcoded column name strings in QueryWrapper to type-safe Lambda expressions to prevent "column does not exist" errors.

**Architecture:** Replace runtime string-based column references with compile-time-safe Lambda method references. MyBatis-Plus supports Lambda expressions that are validated at compile time.

**Tech Stack:** 
- MyBatis-Plus 3.5.7
- Lambda QueryWrapper (`LambdaQueryWrapper`)
- MyBatis-Plus Lambda utility class for complex expressions

---

## Problem Context

Currently, the codebase has **94 instances** of hardcoded column strings:
- 73 instances of `.eq("column_name", value)` 
- 21 instances of `.orderByDesc("column_name")` / `.orderByAsc("column_name")`

These cause runtime errors when:
1. Entity field name doesn't match database column name
2. Database schema changes but code isn't updated
3. Typos in column names (e.g., `created_time` vs `created_at`)

**Example of the problem:**
```java
// ❌ Runtime error if column name is wrong
wrapper.orderByDesc("created_time")  // Fails: column doesn't exist

// ✅ Compile-time safe
wrapper.orderByDesc(Archive::getCreatedTime)
```

---

## Migration Strategy

### Files Requiring Changes (18 files)

**Controllers:**
1. `src/main/java/com/nexusarchive/controller/ArchiveFileController.java`
2. `src/main/java/com/nexusarchive/controller/PoolController.java`
3. `src/main/java/com/nexusarchive/controller/RelationController.java`

**Services:**
4. `src/main/java/com/nexusarchive/service/ArchiveService.java`
5. `src/main/java/com/nexusarchive/service/ArchiveBatchService.java`
6. `src/main/java/com/nexusarchive/service/AutoAssociationService.java`
7. `src/main/java/com/nexusarchive/service/DataScopeService.java`
8. `src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java`
9. `src/main/java/com/nexusarchive/service/UserService.java`

**Service Implementations:**
10. `src/main/java/com/nexusarchive/service/impl/ArchiveApprovalServiceImpl.java`
11. `src/main/java/com/nexusarchive/service/impl/DestructionServiceImpl.java`
12. `src/main/java/com/nexusarchive/service/impl/IngestServiceImpl.java`
13. `src/main/java/com/nexusarchive/service/impl/OpenAppraisalServiceImpl.java`
14. `src/main/java/com/nexusarchive/service/impl/PoolServiceImpl.java`
15. `src/main/java/com/nexusarchive/service/impl/PositionServiceImpl.java`
16. `src/main/java/com/nexusarchive/service/impl/ReconciliationServiceImpl.java`
17. `src/main/java/com/nexusarchive/service/impl/WarehouseServiceImpl.java`

**Modules:**
18. `src/main/java/com/nexusarchive/modules/borrowing/app/BorrowingApplicationService.java`
19. `src/main/java/com/nexusarchive/modules/borrowing/infra/BorrowingScopePolicyImpl.java`

### Common Patterns to Replace

| Pattern | Before | After |
|---------|--------|-------|
| Simple eq | `.eq("status", "active")` | `.eq(Archive::getStatus, "active")` |
| orderBy | `.orderByDesc("created_at")` | `.orderByDesc(Archive::getCreatedTime)` |
| ne | `.ne("id", excludeId)` | `.ne(Archive::getId, excludeId)` |
| ge/le/gt/lt | `.ge("amount", 100)` | `.ge(Archive::getAmount, 100)` |
| Complex COALESCE | `.orderByDesc("COALESCE(updated_at, created_at)") | Keep as-is (see Task 19) |

---

## Implementation Tasks

### Task 1: ArchiveFileController - Line 167

**Files:**
- Modify: `src/main/java/com/nexusarchive/controller/ArchiveFileController.java:167`

**Step 1: Read the file to understand context**

Run: `Read the file around line 167`

**Step 2: Replace hardcoded string with Lambda**

Before:
```java
.eq("archive_code", archivalCode)
```

After:
```java
.eq(ArcFileContent::getArchivalCode, archivalCode)
```

**Step 3: Verify the Entity has getter**

Check: `ArcFileContent.getArchivalCode()` exists

**Step 4: Compile to verify**

Run: `mvn compile -q`
Expected: No errors

**Step 5: Commit**

```bash
git add src/main/java/com/nexusarchive/controller/ArchiveFileController.java
git commit -m "refactor: migrate ArchiveFileController eq to Lambda"
```

---

### Task 2: PoolController - Lines 156, 274-275, 301-303, 331-332, 341, 419-421, 711

**Files:**
- Modify: `src/main/java/com/nexusarchive/controller/PoolController.java`

**Step 1: Replace orderByAsc on line 156**

Before: `.orderByAsc("business_doc_no")`
After: `.orderByAsc(ArcFileContent::getBusinessDocNo)`

**Step 2: Replace complex and() condition on lines 274-275**

Before:
```java
.and(w -> w.isNull("voucher_type").or().ne("voucher_type", "ATTACHMENT"))
.orderByDesc("created_at")
```

After:
```java
.and(w -> w.isNull(ArcFileContent::getVoucherType).or().ne(ArcFileContent::getVoucherType, "ATTACHMENT"))
.orderByDesc(ArcFileContent::getCreatedAt)
```

**Step 3: Replace all remaining occurrences in this file**

- Line 301: `.eq("pre_archive_status", status)` → `.eq(ArcFileContent::getPreArchiveStatus, status)`
- Line 302: `.isNull("voucher_type").or().ne("voucher_type", "ATTACHMENT")` → `.isNull(ArcFileContent::getVoucherType).or().ne(ArcFileContent::getVoucherType, "ATTACHMENT")`
- Line 303: `.orderByDesc("created_at")` → `.orderByDesc(ArcFileContent::getCreatedAt)`
- Line 331-332: Same pattern
- Line 341: Same pattern
- Line 419-421: Multiple `.eq("pre_archive_status", ...)` → `.eq(ArcFileContent::getPreArchiveStatus, ...)`
- Line 711: `.eq("file_id", fileContent.getId())` → `.eq(ArcFileMetadataIndex::getFileId, fileContent.getId())`

**Step 4: Compile and test**

Run: `mvn compile -q`
Expected: No errors

**Step 5: Commit**

```bash
git add src/main/java/com/nexusarchive/controller/PoolController.java
git commit -m "refactor: migrate PoolController QueryWrapper to Lambda"
```

---

### Task 3: RelationController - Lines 89-91

**Files:**
- Modify: `src/main/java/com/nexusarchive/controller/RelationController.java:89-91`

**Step 1: Replace both eq calls**

Before:
```java
.eq("source_id", archiveId)
.eq("target_id", archiveId)
```

After:
```java
.eq(ArchiveRelation::getSourceId, archiveId)
.eq(ArchiveRelation::getTargetId, archiveId)
```

**Step 2: Verify Entity field names**

Check: `ArchiveRelation` entity has `sourceId` and `targetId` fields

**Step 3: Compile**

Run: `mvn compile -q`

**Step 4: Commit**

```bash
git add src/main/java/com/nexusarchive/controller/RelationController.java
git commit -m "refactor: migrate RelationController to Lambda"
```

---

### Task 4: ArchiveService - Multiple lines (76, 81, 87, 92, 95, 246, 257, 310-311, 346, 348, 357-358, 360)

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/ArchiveService.java`

**Step 1: Replace all eq/ne calls**

Line 76: `.eq("status", status)` → `.eq(Archive::getStatus, status)`
Line 81: `.eq("category_code", categoryCode)` → `.eq(Archive::getCategoryCode, categoryCode)`
Line 87: `.eq("status", "archived")` → `.eq(Archive::getStatus, "archived")`
Line 92: `.eq("department_id", orgId)` → `.eq(Archive::getDepartmentId, orgId)`
Line 95: `.eq("unique_biz_id", uniqueBizId)` → `.eq(Archive::getUniqueBizId, uniqueBizId)`
Line 246: `.eq("unique_biz_id", uniqueBizId)` → `.eq(Archive::getUniqueBizId, uniqueBizId)`
Line 310: `.eq("item_id", archiveId)` → `.eq(ArcFileContent::getItemId, archiveId)`
Line 346: `.eq("archive_code", code)` → `.eq(Archive::getArchiveCode, code)`
Line 348: `.ne("id", excludeId)` → `.ne(Archive::getId, excludeId)`
Line 357-358: 
```java
// Before
.eq("unique_biz_id", uniqueBizId)
.eq("deleted", 0)
// After
.eq(Archive::getUniqueBizId, uniqueBizId)
.eq(Archive::getDeleted, 0)
```
Line 360: `.ne("id", excludeId)` → `.ne(Archive::getId, excludeId)`

**Step 2: Compile**

Run: `mvn compile -q`

**Step 3: Commit**

```bash
git add src/main/java/com/nexusarchive/service/ArchiveService.java
git commit -m "refactor: migrate ArchiveService QueryWrapper to Lambda"
```

---

### Task 5: ArchiveBatchService - Lines 41, 74, 83, 146, 156

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/ArchiveBatchService.java`

**Step 1: Replace eq calls**

Lines 41, 74, 146: `.eq("batch_no", batchNo)` → `.eq(ArchiveBatch::getBatchNo, batchNo)`

**Step 2: Replace orderBy calls**

Line 83: `.orderByAsc("created_at")` → `.orderByAsc(ArchiveBatch::getCreatedAt)`
Line 156: `.orderByDesc("created_at")` → `.orderByDesc(ArchiveBatch::getCreatedAt)`

**Step 3: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/ArchiveBatchService.java
git commit -m "refactor: migrate ArchiveBatchService to Lambda"
```

---

### Task 6: AutoAssociationService - Lines 46-47, 67-69, 76-77, 114, 147

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/AutoAssociationService.java`

**Step 1: Replace all hardcoded strings**

Lines 46-47:
```java
// Before
.eq("category_code", "AC01")
.ne("status", "ASSOCIATED")
// After
.eq(Archive::getCategoryCode, "AC01")
.ne(Archive::getStatus, "ASSOCIATED")
```

Lines 67-69:
```java
.eq(Archive::getCategoryCode, "AC04")
.eq(Archive::getFondsNo, voucher.getFondsNo())
.eq(Archive::getFiscalYear, voucher.getFiscalYear())
```

Lines 76-77:
```java
.eq(ArchiveRelation::getSourceId, voucherId)
.eq(ArchiveRelation::getTargetId, candidate.getId())
```

Line 114: `.eq("source_id", voucher.getId())` → `.eq(ArchiveRelation::getSourceId, voucher.getId())`
Line 147: `.eq("source_id", voucherId)` → `.eq(ArchiveRelation::getSourceId, voucherId)`

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/AutoAssociationService.java
git commit -m "refactor: migrate AutoAssociationService to Lambda"
```

---

### Task 7: DataScopeService - Lines 61, 63, 75, 80, 82

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/DataScopeService.java`

**Step 1: Replace eq calls with "1" and "0"**

**Note:** These are special cases using literal "1" and "0" for SQL comparison. Keep these as-is since they're not actual column references.

**Step 2: Replace actual column references**

Line 61: `.eq("created_by", context.userId())` → `.eq(Archive::getCreatedBy, context.userId())`
Line 75: `.eq("department_id", context.departmentId())` → `.eq(Archive::getDepartmentId, context.departmentId())`
Line 80: `.eq("created_by", context.userId())` → `.eq(Archive::getCreatedBy, context.userId())`

**Step 3: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/DataScopeService.java
git commit -m "refactor: migrate DataScopeService to Lambda"
```

---

### Task 8: PreArchiveSubmitService - Lines 108, 184, 193

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java`

**Step 1: Replace all eq/ne calls**

Line 108: `.eq("id", archive.getId())` → `.eq(Archive::getId, archive.getId())`
Line 184: `.eq("id", archiveId).ne("status", "ARCHIVED")` → `.eq(Archive::getId, archiveId).ne(Archive::getStatus, "ARCHIVED")`
Line 193: `.eq("archival_code", archive.getArchiveCode())` → `.eq(ArcFileContent::getArchivalCode, archive.getArchiveCode())`

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java
git commit -m "refactor: migrate PreArchiveSubmitService to Lambda"
```

---

### Task 9: UserService - Lines 135, 142

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/UserService.java`

**Step 1: Replace eq and orderBy**

Line 135: `.eq("deleted", 0)` → `.eq(User::getDeleted, 0)`
Line 142: `.orderByDesc("created_at")` → `.orderByDesc(User::getCreatedAt)`

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/UserService.java
git commit -m "refactor: migrate UserService to Lambda"
```

---

### Task 10: DestructionServiceImpl - Lines 59, 61

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/DestructionServiceImpl.java`

**Step 1: Replace eq and orderBy**

Line 59: `.eq("status", status)` → `.eq(DestructionRequest::getStatus, status)`
Line 61: `.orderByDesc("created_at")` → `.orderByDesc(DestructionRequest::getCreatedAt)`

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/impl/DestructionServiceImpl.java
git commit -m "refactor: migrate DestructionServiceImpl to Lambda"
```

---

### Task 11: OpenAppraisalServiceImpl - Lines 110, 113

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/OpenAppraisalServiceImpl.java`

**Step 1: Replace eq and orderBy**

Line 110: `.eq("status", status)` → `.eq(OpenAppraisalRequest::getStatus, status)`
Line 113: `.orderByDesc("created_at")` → `.orderByDesc(OpenAppraisalRequest::getCreatedAt)`

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/impl/OpenAppraisalServiceImpl.java
git commit -m "refactor: migrate OpenAppraisalServiceImpl to Lambda"
```

---

### Task 12: PositionServiceImpl - Lines 85, 90, 92, 99-100

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/PositionServiceImpl.java`

**Step 1: Replace all calls**

Line 85: `.eq("deleted", 0)` → `.eq(Position::getDeleted, 0)`
Line 90: `.eq("status", status)` → `.eq(Position::getStatus, status)`
Line 92: `.orderByDesc("created_at")` → `.orderByDesc(Position::getCreatedAt)`
Lines 99-100:
```java
.eq(Position::getCode, code)
.eq(Position::getDeleted, 0)
```

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/impl/PositionServiceImpl.java
git commit -m "refactor: migrate PositionServiceImpl to Lambda"
```

---

### Task 13: PoolServiceImpl - Lines 50, 53, 56, 59, 62

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/PoolServiceImpl.java`

**Step 1: Replace all comparison calls**

Line 50: `.ge("total_amount", request.getMinAmount())` → `.ge(ArcFileMetadataIndex::getTotalAmount, request.getMinAmount())`
Line 53: `.le("total_amount", request.getMaxAmount())` → `.le(ArcFileMetadataIndex::getTotalAmount, request.getMaxAmount())`
Line 56: `.ge("issue_date", request.getStartDate())` → `.ge(ArcFileMetadataIndex::getIssueDate, request.getStartDate())`
Line 59: `.le("issue_date", request.getEndDate())` → `.le(ArcFileMetadataIndex::getIssueDate, request.getEndDate())`
Line 62: Already using `.eq(ArcFileContent::getFileType, ...)` - keep as-is

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/service/impl/PoolServiceImpl.java
git commit -m "refactor: migrate PoolServiceImpl to Lambda"
```

---

### Task 14: BorrowingApplicationService - Lines 82, 88, 92

**Files:**
- Modify: `src/main/java/com/nexusarchive/modules/borrowing/app/BorrowingApplicationService.java`

**Step 1: Replace eq and orderBy**

Line 82: `.eq("status", statuses.get(0))` → `.eq(BorrowingRequest::getStatus, statuses.get(0))`
Line 88: `.eq("user_id", userId)` → `.eq(BorrowingRequest::getUserId, userId)`
Line 92: `.orderByDesc("created_at")` → `.orderByDesc(BorrowingRequest::getCreatedAt)`

**Step 2: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/modules/borrowing/app/BorrowingApplicationService.java
git commit -m "refactor: migrate BorrowingApplicationService to Lambda"
```

---

### Task 15: BorrowingScopePolicyImpl - Lines 34, 36, 45, 55, 63, 65

**Files:**
- Modify: `src/main/java/com/nexusarchive/modules/borrowing/infra/BorrowingScopePolicyImpl.java`

**Step 1: Keep "1"/"0" comparisons as-is**

Lines 34, 36, 45, 55, 63, 65: These use `.eq("1", "0")` for SQL literal comparisons. Keep unchanged.

**Step 2: Replace actual column references**

Line 34: Already has `.eq("user_id", ...)` → `.eq(BorrowingRequest::getUserId, ...)`
Line 63: Already has `.eq("user_id", ...)` → `.eq(BorrowingRequest::getUserId, ...)`

**Step 3: Compile and commit**

```bash
mvn compile -q
git add src/main/java/com/nexusarchive/modules/borrowing/infra/BorrowingScopePolicyImpl.java
git commit -m "refactor: migrate BorrowingScopePolicyImpl to Lambda"
```

---

### Task 16: ArchiveApprovalServiceImpl

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/ArchiveApprovalServiceImpl.java`

**Step 1: Find and replace all .eq("xxx", yyy)**

Search: `\.eq\("[^"]+",`
Replace each occurrence with corresponding Lambda reference

**Step 2: Compile**

Run: `mvn compile -q`

**Step 3: Commit**

```bash
git add src/main/java/com/nexusarchive/service/impl/ArchiveApprovalServiceImpl.java
git commit -m "refactor: migrate ArchiveApprovalServiceImpl to Lambda"
```

---

### Task 17: IngestServiceImpl

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/IngestServiceImpl.java`

**Step 1: Find and replace all .eq("xxx", yyy)**

Search: `\.eq\("[^"]+",`
Replace each occurrence with corresponding Lambda reference

**Step 2: Compile**

Run: `mvn compile -q`

**Step 3: Commit**

```bash
git add src/main/java/com/nexusarchive/service/impl/IngestServiceImpl.java
git commit -m "refactor: migrate IngestServiceImpl to Lambda"
```

---

### Task 18: ReconciliationServiceImpl

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/ReconciliationServiceImpl.java`

**Step 1: Find and replace all .eq("xxx", yyy)**

Search: `\.eq\("[^"]+",`
Replace each occurrence with corresponding Lambda reference

**Step 2: Compile**

Run: `mvn compile -q`

**Step 3: Commit**

```bash
git add src/main/java/com/nexusarchive/service/impl/ReconciliationServiceImpl.java
git commit -m "refactor: migrate ReconciliationServiceImpl to Lambda"
```

---

### Task 19: WarehouseServiceImpl

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/WarehouseServiceImpl.java`

**Step 1: Find and replace all .eq("xxx", yyy)**

Search: `\.eq\("[^"]+",`
Replace each occurrence with corresponding Lambda reference

**Step 2: Compile**

Run: `mvn compile -q`

**Step 3: Commit**

```bash
git add src/main/java/com/nexusarchive/service/impl/WarehouseServiceImpl.java
git commit -m "refactor: migrate WarehouseServiceImpl to Lambda"
```

---

### Task 20: Handle Special Cases (COALESCE, complex SQL)

**Files:**
- Modify: `src/main/java/com/nexusarchive/service/impl/NotificationServiceImpl.java:38`

**Step 1: Identify cases that cannot use Lambda**

The `.orderByDesc("COALESCE(updated_at, created_at)")` on line 38 of NotificationServiceImpl cannot be directly converted to Lambda because MyBatis-Plus Lambda doesn't support SQL function expressions in orderBy.

**Step 2: Keep these as strings but add comment**

Add comment to document why:
```java
// Using raw SQL because Lambda doesn't support COALESCE expressions in orderBy
.orderByDesc("COALESCE(updated_at, created_at)")
```

**Step 3: No commit needed (just comment addition)**

---

### Task 21: Verify All Entities Have Getters

**Files:**
- Check all Entity classes in `src/main/java/com/nexusarchive/entity/`

**Step 1: Verify getter methods exist**

For each Lambda reference used, verify the Entity class has:
1. Lombok `@Data` annotation (generates getters automatically), OR
2. Explicit getter method

**Step 2: Add missing getters if needed**

If any Entity is missing `@Data`, add it:
```java
@Data
@TableName("table_name")
public class Entity {
    // fields
}
```

**Step 3: Compile**

Run: `mvn compile -q`

**Step 4: Commit if changes made**

```bash
git add src/main/java/com/nexusarchive/entity/
git commit -m "fix: ensure all entities have @Data annotation for Lambda support"
```

---

### Task 22: Run Full Test Suite

**Step 1: Run all tests**

Run: `mvn test -q`

**Step 2: Verify no failures**

Expected: All tests pass

**Step 3: Check for any runtime SQL errors**

If tests fail with SQL errors, check:
1. Entity field names match database columns (check @TableField annotations)
2. Getter methods exist for all Lambda references
3. Import statements include correct classes

**Step 4: Fix any issues and re-run**

Repeat until all tests pass.

---

### Task 23: Integration Testing

**Step 1: Start backend**

Run: `mvn spring-boot:run`

**Step 2: Test previously failing endpoints**

Run: 
```bash
# Get token
TOKEN=$(curl -s -X POST "http://localhost:19090/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.token')

# Test endpoints
curl -s "http://localhost:19090/api/stats/dashboard" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:19090/api/notifications" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:19090/api/archives/recent?limit=5" \
  -H "Authorization: Bearer $TOKEN"
```

Expected: All return `{"code":200,...}`

**Step 3: Stop backend**

Run: `pkill -f spring-boot:run`

---

### Task 24: Final Verification and Documentation

**Step 1: Verify no hardcoded strings remain**

Run: 
```bash
grep -r '\.eq("' --include="*.java" src/main/java | grep -v '::' | wc -l
```

Expected: Count is significantly reduced (only special cases)

**Step 2: Create migration summary**

Create: `docs/lambdas/QUERYWRAPPER_MIGRATION.md`

```markdown
# QueryWrapper Lambda Migration Summary

## Completed
- Migrated 94 instances of hardcoded column strings to Lambda expressions
- Files modified: 18
- Commit count: 19

## Benefits
1. **Compile-time safety**: Typos caught at compile time
2. **Refactoring support**: IDE can rename fields safely
3. **Type safety**: Auto-completion in IDEs
4. **Documentation**: Lambda references show actual field names

## Remaining String Usage
Some cases still use strings (intentionally):
- SQL functions: `COALESCE(updated_at, created_at)`
- Literal comparisons: `.eq("1", "0")`
- Dynamic column names: `.eq(columnName, value)` where columnName is a variable
```

**Step 3: Final commit**

```bash
git add docs/lambdas/QUERYWRAPPER_MIGRATION.md
git commit -m "docs: add QueryWrapper Lambda migration summary"
```

---

## Testing Strategy

### Unit Testing
Each task includes compilation verification: `mvn compile -q`

### Integration Testing
Task 22 runs full test suite: `mvn test -q`

### Endpoint Testing
Task 23 verifies previously failing endpoints now work

### Regression Testing
All existing tests should pass without modification

---

## Rollback Strategy

If any task causes issues:

1. **Identify the breaking commit**: `git log --oneline -10`
2. **Revert specific commit**: `git revert <commit-hash>`
3. **Alternative**: Manually fix the problematic file
4. **Re-test**: Run `mvn test -q`

---

## Notes

- **MyBatis-Plus Lambda** requires Java 8+ (already satisfied)
- **Lombok @Data** annotation generates getters automatically
- **@TableField** annotation mappings are respected in Lambda expressions
- **Complex SQL** (functions, subqueries) may still need string format

