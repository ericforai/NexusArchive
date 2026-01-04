/**
 * Shared Pages Components
 *
 * Exports reusable page components from various page directories
 * to avoid cross-page directory imports.
 */

// Admin page components (reusable in settings)
export { UserLifecyclePage } from '../../pages/admin/UserLifecyclePage';
export { AccessReviewPage } from '../../pages/admin/AccessReviewPage';
export { LegacyImportPage } from '../../pages/admin/LegacyImportPage';

// Audit page components (reusable in settings)
export { AuditVerificationPage } from '../../pages/audit/AuditVerificationPage';
export { AuditEvidencePackagePage } from '../../pages/audit/AuditEvidencePackagePage';

// Panorama components (reusable in archives)
export { VoucherPreviewDrawer } from '../../pages/panorama/VoucherPreviewDrawer';
