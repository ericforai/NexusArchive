# Audit Log Verification Closed Loop Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close the audit-log verification loop so write-time hashing, range-scoped verification, and structured integrity results are consistent across single-log, date-range, and ID-list verification flows.

**Architecture:** Move hash-chain canonical logic into the backend service layer and let both write-time persistence and verification-time checks share the same payload algorithm. Centralize audit-log range querying in the mapper/service boundary so `verifyChain`, `verifyChainByLogIds`, and sampling-based verification all evaluate the same ordered log stream and report the same structured anomaly categories.

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, Mockito, React TypeScript types.

---

### Task 1: Lock current behavior with failing backend tests

**Files:**
- Modify: `nexusarchive-java/src/test/java/com/nexusarchive/service/AuditLogServiceTest.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/service/AuditLogVerificationServiceImplTest.java`

**Step 1: Write failing tests for write-time hash chaining**

Add tests that assert `AuditLogService.saveAuditLogWithHash(...)`:
- loads the latest persisted hash,
- sets `prevLogHash`,
- computes `logHash` with the same canonical payload used by verification,
- still inserts the record when the previous hash is absent.

**Step 2: Write failing tests for verification result categorization**

Add tests that assert `AuditLogVerificationServiceImpl` can distinguish:
- missing log,
- broken chain,
- hash mismatch,
- intact chain,
- missing requested IDs in `verifyChainByLogIds`,
- `fondsNo`-filtered date-range verification.

**Step 3: Run the focused tests to verify they fail**

Run: `mvn -f /Users/user/code/symphony-workspaces-v3/ERI-89/nexusarchive-java/pom.xml -Dtest=AuditLogServiceTest,AuditLogVerificationServiceImplTest test`

Expected: FAIL because production code does not currently compute chain hashes on write, does not support structured anomaly categories, and does not apply `fondsNo` range filtering consistently.

### Task 2: Unify write-time hash-chain generation

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/AuditLogService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/SysAuditLogMapper.java`

**Step 1: Implement canonical hash-chain generation in `AuditLogService`**

Update `saveAuditLogWithHash(...)` so it:
- ensures `createdTime` exists before hashing,
- loads the latest log hash from persistence,
- sets `prevLogHash`,
- computes `logHash` with the canonical payload and HMAC fallback behavior,
- inserts the record only after the chain fields are populated.

**Step 2: Add mapper query support for ordered verification scopes**

Add mapper methods for:
- ordered date-range lookup with optional `fondsNo`,
- ordered batch lookup by ID list,
- optional `fondsNo` filtering aligned with available audit-log fields.

**Step 3: Re-run the write-path tests**

Run: `mvn -f /Users/user/code/symphony-workspaces-v3/ERI-89/nexusarchive-java/pom.xml -Dtest=AuditLogServiceTest test`

Expected: PASS.

### Task 3: Refactor verification to use one ordered chain evaluator

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/AuditLogVerificationServiceImpl.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/AuditLogVerificationService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/dto/VerificationResult.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/dto/ChainVerificationResult.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/entity/SysAuditLog.java`

**Step 1: Introduce structured anomaly semantics**

Extend DTOs so results can encode:
- result category/status,
- human-readable reason,
- expected vs actual hash,
- whether the issue is missing log, chain break, or hash tampering.

**Step 2: Evaluate single-log and chain verification through shared logic**

Implement a shared evaluator that:
- recomputes the canonical hash,
- checks predecessor linkage against the immediately previous record in the evaluated scope,
- flags missing records for `verifyChainByLogIds`,
- produces consistent `VerificationResult` items for all failure categories,
- aggregates counts into `ChainVerificationResult`.

**Step 3: Support `fondsNo` filtering without TODOs**

Align entity and query logic with the actual schema/available columns so `fondsNo` filtering is executed by code rather than ignored.

**Step 4: Re-run verification tests**

Run: `mvn -f /Users/user/code/symphony-workspaces-v3/ERI-89/nexusarchive-java/pom.xml -Dtest=AuditLogVerificationServiceImplTest test`

Expected: PASS.

### Task 4: Align sampling and API-facing contracts

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/AuditLogSamplingServiceImpl.java`
- Modify: `src/api/auditVerification.ts`
- Optionally Modify: `src/pages/audit/AuditVerificationPage.tsx`

**Step 1: Remove sampling-side `fondsNo` TODO**

Make sampling criteria apply the same `fondsNo` filtering semantics as the verification service.

**Step 2: Keep frontend contract compatible with structured results**

Update TypeScript types or rendering only if backend response fields change in a way that needs explicit typing/display support.

**Step 3: Run impacted backend/frontend tests**

Run:
- `./mvnw -pl nexusarchive-java -Dtest=AuditLogServiceTest,AuditLogVerificationServiceImplTest test`
- `mvn -f /Users/user/code/symphony-workspaces-v3/ERI-89/nexusarchive-java/pom.xml -Dtest=AuditLogServiceTest,AuditLogVerificationServiceImplTest test`
- `npx vitest run src/__tests__/self-verify/page_audit_enhanced.test.ts`

Expected: PASS, or no frontend changes required beyond type compatibility.

### Task 5: Update docs/workpad and verify end-to-end readiness

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/README.md`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/README.md`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/README.md`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/dto/README.md`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/entity/README.md`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/README.md`

**Step 1: Update directory README inventories**

Reflect any touched files or changed responsibilities.

**Step 2: Run final targeted verification**

Run: `mvn -f /Users/user/code/symphony-workspaces-v3/ERI-89/nexusarchive-java/pom.xml -Dtest=AuditLogServiceTest,AuditLogVerificationServiceImplTest test`

Expected: PASS.

**Step 3: Update Linear workpad**

Record:
- what changed,
- exact validation commands,
- remaining risks if any.
