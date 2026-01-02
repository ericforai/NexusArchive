# Code Review TODO Backlog

Generated from comprehensive code review on 2025-01-02

## Backend TODOs

### Controllers
- [ ] `BankReceiptController.java:31` - Implement actual sync logic
- [ ] `TicketSyncController.java:31` - Implement actual sync logic

### Services
- [ ] `AuthTicketServiceImpl.java:150` - Add admin permission validation
- [ ] `DestructionLogServiceImpl.java:92` - Implement Excel/PDF export
- [ ] `ArchiveAppraisalServiceImpl.java:171` - Implement Excel/PDF export
- [ ] `StreamingPreviewServiceImpl.java:83,92,114,301,439` - Complete streaming preview
- [ ] `UserLifecycleServiceImpl.java` - Implement employee-user mapping
- [ ] `FileStoragePolicyServiceImpl.java:140` - Implement retention calculation
- [ ] `RoleService.java:146` - Add role usage check before deletion
- [ ] `AdvancedArchiveSearchServiceImpl.java:208` - Extract metadata fields
- [ ] `FondsHistoryServiceImpl.java:218` - Implement archive distribution logic

### ERP Adapters
- [ ] `KingdeeAdapter.java` - Implement voucher parsing and attachments
- [ ] `WeaverAdapter.java:54` - Implement Ecology API calls
- [ ] `GenericErpAdapter.java` - Implement parsing and attachments

### Database
- [ ] `ArchiveMapper.java:24-32` - Extract hardcoded table name and interval to constants

## Frontend TODOs

### Features
- [ ] `ArchiveListPage.tsx:41` - Refactor View component
- [ ] `useSettings.ts:82` - Add settings validation logic
- [ ] `compliance/index.ts:13` - Extract compliance business logic to hooks
- [ ] `borrowing/index.ts:13` - Extract borrowing business logic to hooks

## Prioritization Matrix

| TODO | Impact | Effort | Priority |
|------|--------|--------|----------|
| MFA implementation | High | High | P0 |
| BankReceipt sync | High | Medium | P1 |
| Streaming preview | Medium | High | P1 |
| Export functions | Medium | Medium | P2 |
| ERP adapters | High | High | P2 |
| View refactoring | Low | Medium | P3 |
| Hooks extraction | Low | Low | P3 |
