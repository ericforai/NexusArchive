# Implementation Plan: Fix Archive ID Type Mismatch

## Phase 1: Entity & DTO Refactoring
- [ ] Task: Update `ArchiveBatch` and `ArchiveBatchItem` entities.
    - [ ] Sub-task: Change `fondsId` type from `Long` to `String`.
    - [ ] Sub-task: Change `refId` in `ArchiveBatchItem` is already String (VARCHAR), but check Entity definition (`refId` should be String).
- [ ] Task: Update Controller DTOs.
    - [ ] Sub-task: Update `ArchiveSubmitBatchController.CreateBatchRequest`.
    - [ ] Sub-task: Update `addVouchers` / `addDocs` payloads to accept `List<String>`.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Entity & DTO Refactoring' (Protocol in workflow.md)

## Phase 2: Service & Mapper Logic Update
- [ ] Task: Refactor `ArchiveSubmitBatchService` interface and implementation.
    - [ ] Sub-task: Update method signatures to use `String` for IDs.
    - [ ] Sub-task: Fix logic in `createBatch`, `addVouchersToBatch`, `executeBatchArchive`.
- [ ] Task: Update MyBatis Mappers.
    - [ ] Sub-task: Check `ArchiveSubmitBatchMapper.xml` (if exists) for parameter types.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Service & Mapper Logic Update' (Protocol in workflow.md)

## Phase 3: Verification
- [ ] Task: Update and Run Integration Test.
    - [ ] Sub-task: Modify `tests/integration/archiving_test.sh` to use the correct API contract (String IDs).
    - [ ] Sub-task: Execute the test and verify `ARCHIVED` status.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Verification' (Protocol in workflow.md)
