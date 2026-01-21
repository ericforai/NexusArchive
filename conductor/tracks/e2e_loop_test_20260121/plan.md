# Implementation Plan: E2E Loop Test

## Phase 1: Environment & Data Preparation
- [x] Task: Clean up test database and verify clean slate state. a5522ce
    - [ ] Sub-task: Create cleanup script `tests/cleanup_e2e.sql`.
    - [ ] Sub-task: Execute cleanup and verify tables are empty.
- [ ] Task: Create robust seed data for E2E scenarios.
    - [ ] Sub-task: Enhance `tests/seed_test_data.sql` to include a full "Happy Path" set (Voucher + XML + PDF + OFD).
    - [ ] Sub-task: Verify seed data loads correctly via `npm run seed`.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Environment & Data Preparation' (Protocol in workflow.md)

## Phase 2: Ingestion Pipeline Verification
- [ ] Task: Verify Batch Upload API (`/api/collection/upload`).
    - [ ] Sub-task: Write integration test `tests/integration/upload_api_test.java` (or .ts).
    - [ ] Sub-task: Fix any 500 errors or timeout issues during large file uploads.
- [ ] Task: Verify Async Parsing Logic.
    - [ ] Sub-task: Check Redis queue processing for uploaded files.
    - [ ] Sub-task: Verify `OriginalVoucher` entities are created with correct status.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Ingestion Pipeline Verification' (Protocol in workflow.md)

## Phase 3: Four-Nature Testing Verification
- [ ] Task: Execute Compliance Engine on Seed Data.
    - [ ] Sub-task: Trigger compliance check via API.
    - [ ] Sub-task: Verify `ComplianceResult` records are generated.
- [ ] Task: Verify Detection Logic Correctness.
    - [ ] Sub-task: Ensure "Tampered" data is correctly flagged as "Failed".
    - [ ] Sub-task: Ensure "Standard" data passes all checks.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Four-Nature Testing Verification' (Protocol in workflow.md)

## Phase 4: Archiving & AIP Generation
- [ ] Task: Verify Archiving Action.
    - [ ] Sub-task: Trigger "Archive" action for the passed batch.
    - [ ] Sub-task: Verify status transition to `ARCHIVED`.
- [ ] Task: Verify AIP Export.
    - [ ] Sub-task: Download AIP package via API.
    - [ ] Sub-task: Validate AIP structure (XML metadata, folder structure) against GB/T 39674.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Archiving & AIP Generation' (Protocol in workflow.md)

## Phase 5: End-to-End Integration Test
- [ ] Task: Create Comprehensive E2E Test Suite.
    - [ ] Sub-task: Write `tests/playwright/e2e/full_lifecycle.spec.ts`.
    - [ ] Sub-task: Implement test steps: Login -> Upload -> Check -> Archive -> Download.
- [ ] Task: Fix & Stabilize.
    - [ ] Sub-task: Run test suite and fix any flakiness.
    - [ ] Sub-task: Ensure < 5s execution time for the critical path (excluding wait times).
- [ ] Task: Conductor - User Manual Verification 'Phase 5: End-to-End Integration Test' (Protocol in workflow.md)
