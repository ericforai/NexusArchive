# Specification: Fix Archive ID Type Mismatch (VARCHAR vs BIGINT)

## 1.0 Context
During the E2E Loop Test, a critical blocking bug was discovered in the Archiving phase. The backend code (Controllers/Services) expects IDs (`fondsId`, `voucherId`, `refId`) to be `Long`, but the database schema uses `VARCHAR` (e.g., UUIDs or string codes like 'BR-GROUP'). This causes `BadSqlGrammarException` in PostgreSQL because implicit casting between `varchar` and `bigint` is not supported for equality checks.

## 2.0 Goal
Refactor the Archive module to support String-based IDs, aligning the code with the current database schema and other modules (which use String IDs).

## 3.0 Scope
- **Controllers**:
    - `ArchiveSubmitBatchController`
- **Services**:
    - `ArchiveSubmitBatchService`
    - `ArchiveSubmitBatchServiceImpl`
- **Entities/DTOs**:
    - `ArchiveBatch`
    - `ArchiveBatchItem`
    - `ArchiveSubmitBatchController.CreateBatchRequest`
- **Mappers**:
    - Update XML/Annotations if necessary.

## 4.0 Detailed Changes
1.  **Change ID Types**: Update `fondsId`, `voucherId`, `docId`, `refId` from `Long` to `String` in all relevant classes.
2.  **Fix API Contracts**: Update DTOs to accept Strings.
3.  **Verify Logic**: Ensure logic that relied on numeric comparison (if any) is adapted.

## 5.0 Verification
- **Unit Tests**: Update existing tests to use String IDs.
- **Integration Test**: Re-run `tests/integration/archiving_test.sh` (modified to use String IDs) and ensure it passes without 500 errors.
