# Implementation Plan: Fix Archive ID Type Mismatch

## Phase 1: Entity & DTO Refactoring
- [x] Task: Update `ArchiveBatch` and `ArchiveBatchItem` entities. 0b81f4e
    - [x] Sub-task: Change `fondsId` type from `Long` to `String`.
    - [x] Sub-task: Change `refId` in `ArchiveBatchItem` is already String (VARCHAR), but check Entity definition (`refId` should be String).
- [x] Task: Update Controller DTOs. 0b81f4e
    - [x] Sub-task: Update `ArchiveSubmitBatchController.CreateBatchRequest`.
    - [x] Sub-task: Update `addVouchers` / `addDocs` payloads to accept `List<String>`.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Entity & DTO Refactoring' (Protocol in workflow.md)

## Phase 2: Service & Mapper Logic Update
- [x] Task: Refactor `ArchiveSubmitBatchService` interface and implementation. 7dc21e1
    - [x] Sub-task: Update method signatures to use `String` for IDs.
    - [x] Sub-task: Fix logic in `createBatch`, `addVouchersToBatch`, `executeBatchArchive`.
- [x] Task: Update MyBatis Mappers. 7dc21e1
    - [x] Sub-task: Check `ArchiveSubmitBatchMapper.xml` (if exists) for parameter types.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Service & Mapper Logic Update' (Protocol in workflow.md)

## Phase 3: Verification
- [ ] Task: Update and Run Integration Test.
    - [ ] Sub-task: Modify `tests/integration/archiving_test.sh` to use the correct API contract (String IDs).
    - [ ] Sub-task: Execute the test and verify `ARCHIVED` status.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Verification' (Protocol in workflow.md)
