# Fix Missing Panorama Entries

## Problem
Users reported that accounting entries ("会计分录") were missing in the Archival Panorama View ("全景视图"), displaying "暂无分录数据".

## Root Cause Analysis
1.  **Data Gap**: The initial seed data (`V69`) for `acc_archive` records did not populate the `custom_metadata` column, which stores the JSON structure for journal entries.
2.  **Backend DTO Filtering**: The backend `ArchiveResponse` DTO explicitly excluded the `customMetadata` field, considering it "sensitive" or "large". This caused the API to strip this field from the response, even when the database was populated.

## Resolution
1.  **Database Updates**:
    *   Created `V105__seed_voucher_entries_data.sql` to populate entries for `arc-2024-*`.
    *   Created `V106__seed_demo_voucher_entries.sql` to populate entries for `demo-2025-*`.
    *   Executed these migrations to ensure data exists in the database.

2.  **Backend Code Changes**:
    *   Modified `src/main/java/com/nexusarchive/dto/response/ArchiveResponse.java`.
    *   Added `customMetadata` field to the DTO and updated the `fromEntity` method to map it.
    *   Updated the class documentation.

## Verification
1.  **Restart Backend**: The backend service must be restarted for the Java code changes to take effect.
2.  **Refresh Page**: Navigate to the Panorama View.
3.  **Inspect Entries**: Select a voucher (e.g., "2024年1月会计凭证01" or "记账凭证-差旅费报销"). The "会计分录" table should now populate with data (Summary, Account, Debit, Credit).

## Technical Details
-   **Field**: `customMetadata` (JSON String)
-   **API**: `GET /archives/{id}`
-   **Frontend Component**: `ArchivalPanoramaView.tsx` -> `VoucherDetailCard.tsx`
