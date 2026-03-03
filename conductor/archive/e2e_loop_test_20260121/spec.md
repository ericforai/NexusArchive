# Specification: E2E Loop Test (Ingestion to Archiving)

## 1.0 Goal
Verify the complete lifecycle of an electronic accounting file from **Data Ingestion** to **Archival Information Package (AIP) Generation**. Ensure that the automated pipeline works correctly under "real-world" conditions with test data, and that all "Four-Nature" (Authenticity, Integrity, Availability, Safety) checks are correctly executed.

## 2.0 Scope
### 2.1 In Scope
- **Data Collection API**: `/api/collection/upload` (Batch upload of vouchers and attachments).
- **Async Processing**: Validation of asynchronous parsing and storage of uploaded data.
- **Original Voucher Management**: Creation and versioning of `OriginalVoucher` entities.
- **Compliance Testing**: Execution of the 4-nature detection engine.
- **Archiving**: Grouping vouchers into `CollectionBatch` and generating `PoolItem`s.
- **AIP Export**: Generation of the final archival package conforming to GB/T 39674.

### 2.2 Out of Scope
- ERP Integration (SAP/YonSuite) - *Mock data will be used*.
- UI Polishing - *Focus is on functional correctness*.
- Mobile compatibility.

## 3.0 Success Criteria
1.  **Zero Critical Bugs**: The full flow from upload to download AIP must succeed without 500 errors.
2.  **Automated E2E Test**: A Playwright test case must exist that covers the "Happy Path" and passes reliably.
3.  **Data Integrity**: The hash of the exported AIP must match the recorded hash in the audit log.
4.  **Compliance Report**: The system must generate a correct "Four-Nature Detection Report" for the test batch.

## 4.0 Data Strategy
- Use `tests/seed_test_data.sql` to generate specific test scenarios (e.g., "Standard Voucher", "Missing Attachment Voucher").
- Use `scripts/root-legacy/create_abnormal_data.sh` to test error handling for the compliance engine.
