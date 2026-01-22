# Fix PDF Preview in FinancialReportDetailDrawer

The `FinancialReportDetailDrawer` is used for "Ledger", "Financial Report", and "Other Accounting Materials". When these items are in the "Pre-archive" (Pool) state, the current implementation fails to show the PDF preview because it incorrectly calls the archive preview API instead of the pool preview API.

## Proposed Changes

### Frontend

#### [MODIFY] [ArchiveListView.tsx](file:///Users/user/nexusarchive/src/pages/archives/ArchiveListView.tsx)
- Pass `isPool={mode.isPoolView}` to `FinancialReportDetailDrawer`.

#### [MODIFY] [FinancialReportDetailDrawer.tsx](file:///Users/user/nexusarchive/src/pages/archives/FinancialReportDetailDrawer.tsx)
- Update `FinancialReportDetailDrawerProps` to include `isPool?: boolean`.
- Pass `isPool` to `SmartFilePreview`.
- When `isPool` is true, ensure we pass the item's primary ID as `fileId` to `SmartFilePreview`, as `useFilePreview` uses `fileId` for pool items.

## Verification Plan

### Manual Verification
1. Navigate to "Electronic Voucher Pool" -> "Ledgers", "Reports", or "Other".
2. Click the "View" button for an item that has a file.
3. Verify that the PDF preview loads correctly in the drawer.
4. Verify that "Voucher Pool" (Vouchers) still works as before.
