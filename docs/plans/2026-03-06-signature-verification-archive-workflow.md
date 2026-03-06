# Signature Verification Archive Workflow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Integrate signature verification persistence into the async archive four-nature workflow and add regression coverage for normal and degraded cases.

**Architecture:** Reuse the existing async four-nature archive workflow as the smallest real business entrypoint. Persist per-file verification outcomes to `arc_signature_log`, keep workflow response behavior unchanged, and treat signature logging/persistence failures as non-blocking so the archive workflow still completes.

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, Mockito, MockMvc

---

### Task 1: Lock the workflow choice and persistence shape

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/compliance/impl/AsyncFourNatureCheckServiceImpl.java`
- Reference: `nexusarchive-java/src/main/java/com/nexusarchive/controller/SignatureController.java`
- Reference: `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArcSignatureLog.java`
- Reference: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArcSignatureLogMapper.java`

**Step 1: Write the failing test**

```java
@Test
void performParallelCheck_shouldPersistSignatureVerificationLogForPdfFile() {
    // Arrange archive + one PDF file + authenticity result carrying signature details.
    // Act performParallelCheck(...).join()
    // Assert signatureLogMapper.insert(...) called with archiveId/fileId/verifyResult/message.
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=AsyncFourNatureCheckServiceImplTest#performParallelCheck_shouldPersistSignatureVerificationLogForPdfFile test`
Expected: FAIL because the workflow does not persist any signature verification log yet.

**Step 3: Write minimal implementation**

```java
private void persistSignatureVerificationResult(...) {
    // Only persist for PDF/OFD files.
    // Convert authenticity outcome into ArcSignatureLog.
    // Catch mapper exceptions and log warnings instead of failing the workflow.
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=AsyncFourNatureCheckServiceImplTest#performParallelCheck_shouldPersistSignatureVerificationLogForPdfFile test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/compliance/impl/AsyncFourNatureCheckServiceImpl.java nexusarchive-java/src/test/java/com/nexusarchive/service/compliance/AsyncFourNatureCheckServiceImplTest.java
git commit -m "feat: persist signature verification in archive workflow"
```

### Task 2: Protect degraded behavior

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/compliance/impl/AsyncFourNatureCheckServiceImpl.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/service/compliance/AsyncFourNatureCheckServiceImplTest.java`

**Step 1: Write the failing test**

```java
@Test
void performParallelCheck_shouldKeepWorkflowRunningWhenSignatureLogPersistenceFails() {
    // Arrange signatureLogMapper.insert(...) throws RuntimeException.
    // Act performParallelCheck(...).join()
    // Assert report still returned and task marked completed.
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=AsyncFourNatureCheckServiceImplTest#performParallelCheck_shouldKeepWorkflowRunningWhenSignatureLogPersistenceFails test`
Expected: FAIL because persistence exceptions still escape the workflow.

**Step 3: Write minimal implementation**

```java
try {
    signatureLogMapper.insert(log);
} catch (Exception ex) {
    log.warn("签名验证结果持久化失败", ex);
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=AsyncFourNatureCheckServiceImplTest#performParallelCheck_shouldKeepWorkflowRunningWhenSignatureLogPersistenceFails test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/compliance/impl/AsyncFourNatureCheckServiceImpl.java nexusarchive-java/src/test/java/com/nexusarchive/service/compliance/AsyncFourNatureCheckServiceImplTest.java
git commit -m "test: cover signature persistence fallback"
```

### Task 3: Prove downstream exposure stays available

**Files:**
- Modify: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ComplianceControllerIntegrationTest.java`
- Reference: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ComplianceController.java`
- Reference: `nexusarchive-java/src/main/java/com/nexusarchive/controller/SignatureController.java`

**Step 1: Write the failing test**

```java
@Test
void getAsyncCheckResult_shouldReturnCompletedReportWithAuthenticityDetails() {
    // Mock task status = COMPLETED and async result containing authenticity message from signature verification.
    // Assert controller returns 200 and data.authenticity.message is present.
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=ComplianceControllerIntegrationTest#getAsyncCheckResult_shouldReturnCompletedReportWithAuthenticityDetails test`
Expected: FAIL because the controller test coverage does not include the async result path yet.

**Step 3: Write minimal implementation**

```java
// Extend the existing WebMvc test with AsyncFourNatureCheckService mocks
// and assertions for the completed-result endpoint.
```

**Step 4: Run test to verify it passes**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=ComplianceControllerIntegrationTest#getAsyncCheckResult_shouldReturnCompletedReportWithAuthenticityDetails test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/controller/ComplianceControllerIntegrationTest.java
git commit -m "test: cover async archive signature verification result exposure"
```

### Task 4: Full verification

**Files:**
- Verify only

**Step 1: Run focused regression tests**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=AsyncFourNatureCheckServiceImplTest,ComplianceControllerIntegrationTest test`
Expected: PASS

**Step 2: Run broader service regression if needed**

Run: `mvn -f nexusarchive-java/pom.xml -Dtest=FourNatureCheckServiceTest,ComplianceListenerTest test`
Expected: PASS

**Step 3: Re-check ticket acceptance criteria**

```text
- Real workflow: async archive four-nature check
- Stored + linked: arc_signature_log rows include archive_id/file_id
- Failure handling: persistence errors do not fail workflow
- Regression coverage: service + controller tests
```

**Step 4: Commit**

```bash
git add docs/plans/2026-03-06-signature-verification-archive-workflow.md
git commit -m "docs: add signature verification workflow plan"
```
