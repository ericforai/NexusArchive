# ERP Integration & Sync Persistence Lessons Learned

## 1. Entity vs DTO Mismatch Pattern
**Issue**: The most critical bug encountered was passing a database Entity (`com.nexusarchive.entity.ErpConfig`) directly to an Adapter interface that expected a DTO (`com.nexusarchive.integration.erp.dto.ErpConfig`).
- **Context**: `ErpConfig` entity stores connection details (host, appKey, secret) packed inside a JSON string `configJson` to keep the table schema generic. The DTO has explicit fields for these properties.
- **Symptom**: Compilation errors or runtime NullPointerExceptions because the Adapter tried to call getters (e.g., `getAppKey()`) that didn't exist or were null on the Entity.
- **Solution**: Always implement an explicit **Conversion Step** in the Service layer before calling the Adapter.
  ```java
  // Correct Pattern in Service
  ErpConfig entity = repository.selectById(id);
  ErpConfigDTO dto = convertToDto(entity); // Parse JSON -> DTO fields
  adapter.sync(dto);
  ```

## 2. Sync Persistence Flow
**Issue**: The initial implementation of `triggerSync` was a "Mock" that only updated the status text but didn't save data.
- **Requirement**: Real sync requires the full pipeline: `External API -> DTO -> Entity (ArcFileContent) -> DB`.
- **Key Learnings**:
    1.  **Idempotency is Logic, not Magic**: We must explicitly check if a voucher already exists (`isVoucherExist`) before inserting to prevent duplicates.
    2.  **DTO Field Mapping**: Ensure DTO fields (e.g., `creator`) match or are mapped to Entity fields (`creator`). Avoid assuming fields like `title` exist if they don't.
    3.  **Missing Metadata**: Real data often lacks system-specific metadata needed for the Archive (e.g., `fondsCode`). Provide sensible defaults (e.g., `DEFAULT`) to satisfy database constraints.

## 3. Frontend-Backend Contract & Debugging
**Issue**: The Frontend UI showed "0 items" or empty logs even after "Success".
- **Root Cause 1 (Stale Code)**: The backend code defining the DTO mapping (`lastSyncMsg` -> `lastSyncMsg`) was updated but not effectively compiled.
    - **Lesson**: `mvn compile` might say "Nothing to compile" if it thinks classes are up to date. Always use **`mvn clean compile`** when debugging mysterious logic disappearances.
- **Root Cause 2 (Hardcoded Placeholders)**: The `receivedCount` was hardcoded to `0` in the service ("TODO").
    - **Lesson**: If the data exists in a structured string (e.g., "Synced 5 items"), **parse it** immediately rather than waiting for a perfect database solution. A partial fix (parsing text) is better than a broken UI.

## 4. Development Environment & Testing
**Issue**: `dev-start.sh` failed to start the backend because of compilation errors in *Test* classes (`ArchiveControllerTest`), even though the main application code was fine.
- **Context**: `mvn spring-boot:run` triggers a build that includes test compilation by default.
- **Lesson**:
    - **Blocking Tests**: Broken tests should not block local development startup.
    - **Workaround**: Use `-Dmaven.test.skip=true` (skips compilation AND execution) instead of `-DskipTests` (skips execution but still compiles).
    - **Long-term**: Fix the tests (mismatched arguments in Mockito are a common cause when Service signatures change).

## 5. Document Generation & Storage Strategy (Collection Bill Case Study)
**Case**: The "Collection Bill" (收款单) sync feature required generating PDFs from ERP JSON data and providing a preview interface.
**Issue**: Synced records had valid metadata but physical PDF files were creating errors or missing because they were generated once and discarded/mis-pathed.

- **Problem 1 (Data Persistence vs Regeneration)**: 
  - **Scenario**: The initial sync logic (Scenario 1) consumed the ERP JSON, generated a PDF, extracted metadata, and then **discarded the JSON**.
  - **Consequence**: When the PDF generation failed (due to path bugs), or when we wanted to update the PDF template later, we were stuck. We couldn't regenerate the file because the source data was gone.
  - **Lesson**: **Store the Raw Source Data**.
    - **Action**: Added a `source_data` (TEXT/JSON) column to the `arc_file_content` table.
    - **Benefit**: Enables on-demand regeneration (Self-Healing), auditing, and template updates without re-syncing from ERP.

- **Problem 2 (Path Handling in Hybrid Env)**:
  - **Scenario**: The code used absolute paths like `/data/nexusarchive/storage/...`. This is fine for production Docker containers but fails on local Mac development environments where `/data` is read-only or non-existent at root.
  - **Lesson**: **Always use Relative Paths & Config Injection**.
    - **Bad**: `fileContent.setStoragePath("/data/..." + fileName)`
    - **Good**: Inject `@Value("${archive.root.path}")` and use `Paths.get(rootPath, subDir, fileName)`. Ensure the database stores the structure relative to the root or normalized.

- **Problem 3 (Resilient Preview Interfaces)**:
  - **Scenario**: The Preview API returned 404 if the physical file was missing, even if the database record existed.
  - **Lesson**: **Implement "On-Demand" Logic**.
    - **Pattern**: 
      1. Check if file exists. 
      2. If No, check if `source_data` exists. 
      3. If Yes, **Regenerate the file** immediately, save it, and serve it.
    - **Result**: The system heals itself from filesystem data loss or sync gltiches.

- **Problem 4 (Refactoring Safety)**:
  - **Scenario**: A refactoring of the PDF service changed a method signature from `(ArcFileContent, JsonNode, Path)` to `(Path, ArcFileContent, JsonNode)`. The calling code in another service wasn't updated correctly, passing a `String` instead of `JsonNode`.
  - **Lesson**: When modifying shared service methods, **Search Usage** globally. Java's type safety usually catches this, but with Hot-Swap or partial recompilation, errors can be masked until runtime or specific execution paths.
