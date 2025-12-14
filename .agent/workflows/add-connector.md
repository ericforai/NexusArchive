---
description: Add a new ERP Source Connector (Detailed SOP)
---

# Add New ERP Connector Workflow

This workflow provides a comprehensive guide to adding a new ERP or System connector to NexusArchive using the `ErpAdapter` architecture. It incorporates critical lessons learned regarding data persistence and document generation.

## 1. Prerequisites
- **Source Access**: API keys, Base URL, and documentation.
- **Data Model**: Understand the source entity (e.g., Voucher, Bill) and how it maps to `VoucherDTO`.
- **Artifact Strategy**: Decide if you will sync files (PDF/OFD) from source or generate them from data.

## 2. Define the Adapter Class
Create a new class in `com.nexusarchive.integration.erp.adapter` implementing `ErpAdapter`.

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class NewSystemErpAdapter implements ErpAdapter {

    private final NewSystemClient client; // Your Feign/Http Client

    @Override
    public String getIdentifier() { return "new_system"; }

    @Override
    public String getName() { return "New System ERP"; }

    // ... other metadata methods
}
```

## 3. Data Transfer Object (DTO) Strategy
**CRITICAL**: The standard `VoucherDTO` may not cover all fields needed for document generation (e.g., "Customer Name" or specific line item details).
If your connector relies on **Data-to-PDF Generation**:
1.  **Extend VoucherDTO**: Create `NewSystemVoucherDTO extends VoucherDTO` adding a `private Map<String, Object> rawData;` field.
2.  **Populate Raw Data**: During sync, stick the original JSON or full detail response into this field.
3.  **Why?**: The `ErpScenarioService` serializes the DTO into the `source_data` database column. If you lose data during DTO mapping, you cannot regenerate the PDF later.

## 4. Implement Sync Logic
Implement `syncVouchers` (or specialized method for Collection Bills).

```java
@Override
public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDateTime startDate, LocalDateTime endDate) {
    // 1. Convert Dates
    // External APIs often need strings (yyyy-MM-dd HH:mm:ss)
    
    // 2. Fetch Data (Handle Pagination!)
    List<SourceEntity> sourceEntities = client.fetch(startDate, endDate);
    
    // 3. Map to DTO
    return sourceEntities.stream().map(this::convertToDto).toList();
}

private VoucherDTO convertToDto(SourceEntity source) {
    VoucherDTO dto = VoucherDTO.builder()
        .voucherNo(source.getCode())
        // ... map standard fields ...
        .build();
    
    // 4. CRITICAL: Persist Raw Data for PDF Generation
    if (source.hasExtraDetails()) {
        // Option A: If DTO extended
        // ((NewSystemVoucherDTO)dto).setRawData(source.getRaw());
        
        // Option B: Serialize to a reserved field if available, or rely on DTO holding enough info.
        // WARNING: Standard VoucherDTO is strict. Ideally ensure source logic is reproducible.
    }
    return dto;
}
```

## 5. Document Persistence & Path Handling
The `ErpScenarioService` handles saving the record, but you must ensure paths are correct if you download files manually.

### 5.1 If Downloading Files
If the ERP provides a PDF URL:
1.  Download the stream.
2.  Calculate Hash (SM3).
3.  Save to disk using **Relative Paths**.
    ```java
    // BAD
    Path p = Paths.get("/data/storage", filename); 
    
    // GOOD
    @Value("${archive.root.path}") String rootPath;
    Path p = Paths.get(rootPath, "pre-archive", fonds, filename);
    ```

### 5.2 If Generating Files (On-Demand)
If you rely on the `VoucherPdfGeneratorService`:
1.  Ensure `source_data` column is populated (handled by `ErpScenarioService` automatically if DTO is correct).
2.  Ensure `VoucherPdfGeneratorService` supports your logic (might need to add a method for your specific DTO structure).

## 6. Registration
Register your adapter in `ErpAdapterFactory` (usually automatic if using Spring Component scanning, otherwise update the factory switch case).

## 7. Verification Checklist
1.  **Connection Test**: Verify `testConnection()` works.
2.  **Sync Test**: Run a sync. Check `arc_file_content` table.
    - `source_data` column must NOT be NULL/Empty.
    - `storage_path` must be relative (start with `./` or configured root).
3.  **Preview Test**: Click "Preview" in frontend.
    - If file missing, backend should auto-generate using `source_data`.
    - Verify PDF content matches source.
