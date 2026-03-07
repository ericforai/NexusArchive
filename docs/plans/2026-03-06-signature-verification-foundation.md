# Signature Verification Foundation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为档案签名验签能力建立独立的领域接口、结果模型、持久化记录与服务边界，并保持业务层对具体 PDF/OFD 验签库零感知。

**Architecture:** 采用独立 `modules/signature` 模块，按 `api/app/domain/infra` 四层组织。`domain` 负责定义验签端口、结果模型和仓储接口；`app` 只负责记录持久化边界与查询边界；`infra` 通过 MyBatis 实现仓储并映射到新的 `arc_signature_verification` 表。旧 `service.signature`、`SignatureController` 与 `arc_signature_log` 暂不改造，以避免本任务把新旧链路耦合在一起。

**Tech Stack:** Java 17 + Spring Boot 3 + MyBatis-Plus + Flyway + Jackson + JUnit 5 + ArchUnit。

---

### Task 1: 设计纯领域验签模型

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/SignatureDocumentType.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/SignatureVerificationStatus.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/SignatureValidationStatus.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/SignatureVerificationSignature.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/SignatureVerificationResult.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/ArchiveSignatureVerification.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/ArchiveSignatureVerificationRepository.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/SignatureVerificationPort.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/modules/signature/domain/SignatureVerificationResultTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_derive_signature_counters_from_details() {
    SignatureVerificationResult result = SignatureVerificationResult.builder()
            .status(SignatureVerificationStatus.FAILED)
            .signatureDetails(List.of(
                    SignatureVerificationSignature.valid("Alice"),
                    SignatureVerificationSignature.invalid("Bob", "digest mismatch")))
            .build();

    assertEquals(2, result.getSignatureCount());
    assertEquals(1, result.getValidSignatureCount());
    assertEquals(1, result.getInvalidSignatureCount());
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -Dtest=SignatureVerificationResultTest test`
Expected: FAIL（领域模型尚不存在）

**Step 3: Write minimal implementation**

实现纯 Java 领域模型，不引入 Spring、MyBatis、PDF/OFD SDK 类型。结果模型至少包含：
- 文档类型 `PDF/OFD/UNKNOWN`
- 总体状态 `PASSED/FAILED/NO_SIGNATURE/UNSUPPORTED/ERROR`
- 明细级状态 `VALID/INVALID/UNKNOWN`
- 提供方代码/版本、验证时间、错误码/错误消息
- 多签章明细列表与从明细推导出的计数字段

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -Dtest=SignatureVerificationResultTest test`
Expected: PASS

### Task 2: 建立应用服务与仓储边界

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/app/SignatureVerificationRecordFacade.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/app/SignatureVerificationRecordService.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/modules/signature/app/SignatureVerificationRecordServiceTest.java`

**Step 1: Write the failing test**

```java
@Test
void save_should_require_archive_id() {
    assertThrows(IllegalArgumentException.class, () -> service.save(
            ArchiveSignatureVerification.builder().build()));
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -Dtest=SignatureVerificationRecordServiceTest test`
Expected: FAIL（Facade/Service 尚不存在）

**Step 3: Write minimal implementation**

应用层只提供：
- `save(ArchiveSignatureVerification verification)`
- `findByArchiveId(String archiveId)`
- `findByFileId(String fileId)`

保存时至少校验：`archiveId`、`documentType`、`result` 非空。

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -Dtest=SignatureVerificationRecordServiceTest test`
Expected: PASS

### Task 3: 添加基础设施映射与 Flyway 迁移

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V109__create_signature_verification_records.sql`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/infra/SignatureVerificationRecordEntity.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/infra/mapper/SignatureVerificationRecordMapper.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/infra/MybatisArchiveSignatureVerificationRepository.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/modules/README.md`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/README.md`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/api/README.md`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/api/dto/README.md`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/app/README.md`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/domain/README.md`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/modules/signature/infra/README.md`

**Step 1: Write the failing test**

```java
@Test
void aggregate_should_create_persistence_ready_summary() {
    ArchiveSignatureVerification verification = ArchiveSignatureVerification.create(
            "archive-1",
            "file-1",
            "invoice.pdf",
            SignatureDocumentType.PDF,
            "MANUAL",
            result);

    assertEquals(1, verification.getValidSignatureCount());
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -Dtest=SignatureVerificationResultTest test`
Expected: FAIL（聚合/汇总逻辑未实现）

**Step 3: Write minimal implementation**

迁移表至少包含：
- `id`
- `archive_id`
- `file_id`
- `file_name`
- `document_type`
- `trigger_source`
- `provider_code`
- `provider_version`
- `verification_status`
- `signature_count`
- `valid_signature_count`
- `invalid_signature_count`
- `error_code`
- `error_message`
- `verified_at`
- `result_payload`（JSONB）
- `created_time`

并建立索引：
- `(archive_id, verified_at DESC)`
- `(file_id, verified_at DESC)`
- `verification_status`

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -Dtest=SignatureVerificationResultTest,SignatureVerificationRecordServiceTest test`
Expected: PASS

### Task 4: 跑架构与目标测试并同步工单

**Files:**
- Update: Linear workpad comment for `ERI-10`

**Step 1: Run focused verification**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -Dtest=ModuleBoundaryTest,SignatureVerificationResultTest,SignatureVerificationRecordServiceTest test`
Expected: PASS

**Step 2: If environment permits, run migration-oriented verification**

Run: `cd /Users/user/code/symphony-workspaces-v3/ERI-10/nexusarchive-java && mvn -q -DskipTests compile`
Expected: PASS（至少验证新增实体/Mapper/迁移资源在构建中无装配错误）

**Step 3: Update workpad**

记录：
- 已选择独立 `modules/signature` 模块方案
- 已新增 `arc_signature_verification` 持久化表
- 已完成哪些测试
- 若本机无数据库验证条件，明确说明缺口
