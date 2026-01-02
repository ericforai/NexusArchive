# Voucher Preview Drawer Architecture

**Created:** 2026-01-02
**Status:** Production
**Component:** ArchiveDetailDrawer, VoucherPreviewDrawer

---

## Overview

The voucher preview drawer system provides a responsive, accessible, and performant solution for previewing accounting vouchers and archives. It replaces the previous modal-based implementation with a drawer pattern that offers better UX across all device sizes.

---

## Component Architecture

### ArchiveDetailDrawer
**Location:** `/src/pages/archives/ArchiveDetailDrawer.tsx`

**Purpose:** Primary drawer for archived voucher preview with metadata, accounting vouchers, and attachments.

**Features:**
- Responsive width calculation (50vw/70vw/100vw)
- Tab-based content organization
- Route-based auto-close
- Expand-to-new-page functionality
- AIP export integration

**Props Interface:**
```typescript
interface ArchiveDetailDrawerProps {
  open: boolean;
  onClose: () => void;
  row: GenericRow | null;
  config: ModuleConfig;
  isPoolView: boolean;
  onAipExport?: (row: GenericRow) => void;
  isExporting?: string | null;
}
```

**State Management:**
- Local state for active tab
- Parent controls open/close state
- useVoucherData hook for data fetching

---

### VoucherPreviewDrawer
**Location:** `/src/pages/panorama/VoucherPreviewDrawer.tsx`

**Purpose:** Panorama-specific drawer for pre-archive voucher preview.

**Features:**
- Fixed 85% width (desktop-optimized)
- Side-by-side metadata and preview layout
- Full-screen navigation
- Compact metadata display

**Props Interface:**
```typescript
interface VoucherPreviewDrawerProps {
  voucherId: string | null;
  open: boolean;
  onClose: () => void;
}
```

---

## Responsive Design Breakpoints

### Breakpoint Strategy

**ArchiveDetailDrawer Responsive Widths:**

| Screen Width | Drawer Width | Layout Style | Use Case |
|--------------|--------------|--------------|----------|
| ≥1280px | 50vw (max 900px) | Standard side drawer | Desktop workstations |
| 768-1279px | 70vw | Wide drawer | Laptops, tablets |
| <768px | 100vw | Full-screen drawer | Mobile devices |

**Implementation:**
```typescript
const getDrawerWidth = (): string | number => {
  const width = window.innerWidth;
  if (width >= 1280) return '50vw';
  if (width >= 768) return '70vw';
  return '100vw';
};

const drawerWidth = useMemo(() => {
  if (typeof window === 'undefined') return '50vw';
  return getDrawerWidth();
}, []);
```

**VoucherPreviewDrawer:**
- Fixed 85% width (designed for desktop panorama view)
- Not responsive (use case is desktop dashboard)

---

## Performance Targets

### Rendering Performance
- **First render target:** < 500ms from click to visible content
- **Tab switch target:** < 100ms for cached data
- **Animation duration:** 300ms (desktop), 250ms (mobile)

### Optimization Techniques

**1. Conditional Rendering:**
```typescript
// Don't render if closed or no row
if (!open || !row) return null;
```

**2. Memoized Width Calculation:**
```typescript
const drawerWidth = useMemo(() => getDrawerWidth(), []);
```

**3. Lazy Tab Content:**
- Content only renders when tab is active
- VoucherPreviewCanvas defers heavy canvas rendering
- Attachments use lazy image loading

**4. Data Caching (via useVoucherData hook):**
- React Query cache with 5-minute TTL
- Preload adjacent records (current ± 1)
- Abort requests on drawer close

**5. Memory Management:**
- `destroyOnClose={true}` destroys drawer content on close
- Reset tab state when drawer closes
- Cleanup in useEffect

---

## Accessibility Features

### Keyboard Navigation
- **ESC key:** Close drawer (via `keyboard={true}`)
- **Tab key:** Navigate through tabs
- **Focus management:** Custom close button

### Screen Reader Support
- Semantic HTML with proper ARIA labels
- Descriptive button titles
- Logical tab order

### Visual Accessibility
- High contrast colors (slate-800 for text)
- Large touch targets on mobile (44px × 44px minimum)
- Clear visual hierarchy

---

## User Interaction Patterns

### Opening Methods
1. **Click list item:** Most common
2. **Keyboard navigation:** Enter/Space on focused row
3. **Programmatic:** Via state management

### Closing Methods
1. **Click mask (overlay):** Via `maskClosable={true}`
2. **Press ESC key:** Via `keyboard={true}`
3. **Click close button (X icon):** Custom button
4. **Route change:** Auto-close (handled by parent)

### Expand to New Page
- **Button:** "展开到新页" / "全屏查看"
- **Route:** `/system/archives/:id` or `/system/panorama/:id`
- **Behavior:** Navigate to full-width detail page
- **Back navigation:** Returns to previous list

---

## Testing Guidelines

### Manual Testing Checklist

**Responsive Breakpoints:**
- [ ] **375px (mobile):** Full-screen drawer (100vw)
- [ ] **768px (tablet):** Wide drawer (70vw)
- [ ] **1280px (desktop):** Standard drawer (50vw)
- [ ] **1920px (large desktop):** Standard drawer (50vw, max 900px)

**Interaction Tests:**
- [ ] Open drawer by clicking list item
- [ ] Close drawer by clicking mask
- [ ] Close drawer by pressing ESC
- [ ] Close drawer by clicking X button
- [ ] Switch between tabs (metadata, voucher, attachments)
- [ ] Expand to new page button navigation
- [ ] Route change auto-close

**Accessibility Tests:**
- [ ] Keyboard navigation (Tab, Enter, ESC)
- [ ] Focus management
- [ ] Screen reader compatibility
- [ ] Touch target sizes (mobile)

### Automated Testing

**E2E Test Selectors:**
```typescript
data-testid="archive-detail-drawer"      // Drawer wrapper
data-testid="close-drawer"               // Close button
```

**Performance Testing:**
- Lighthouse score: > 90 on mobile
- First Contentful Paint: < 500ms
- Time to Interactive: < 1s

---

## Migration Notes

### From Modal to Drawer

**Changes Made:**
1. Replaced `ArchiveDetailModal` with `ArchiveDetailDrawer`
2. Updated width strategy from fixed `max-w-6xl` to responsive `50vw/70vw/100vw`
3. Added `maskClosable` and `keyboard` properties
4. Removed `mt-10` positioning (drawer emerges from edge)
5. Added expand-to-page functionality

**Preserved Features:**
- Tab-based content organization
- Business metadata display
- Voucher canvas rendering
- Attachment preview
- AIP export integration

**File Status:**
- ✅ `ArchiveDetailDrawer.tsx` - Active (new)
- 🗄️ `ArchiveDetailModal.tsx` - Backup (can be removed after validation period)

---

## Component Dependencies

### Internal Dependencies
- `VoucherMetadata` - Business metadata display
- `VoucherPreviewCanvas` - Accounting voucher rendering
- `OriginalDocumentPreview` - Attachment preview
- `useVoucherData` - Data fetching hook
- `VoucherExportButton` - AIP export component

### External Dependencies
- `antd` - Drawer, Tabs components
- `react-router-dom` - Navigation
- `lucide-react` - Icons (FileText, X, ExternalLink, Maximize2)

---

## Future Enhancements

### Planned Features
1. **Swipe gestures:** Mobile swipe to close (touch events)
2. **Multi-drawer:** Compare multiple vouchers side-by-side
3. **Persistent state:** Remember last active tab per user
4. **Keyboard shortcuts:** Custom shortcuts for power users
5. **Print optimization:** Print-friendly layout for voucher details

### Performance Improvements
1. Virtual scrolling for large attachment lists
2. Progressive image loading (blur-up technique)
3. Service worker caching for offline access
4. Web Workers for heavy PDF rendering

---

## Related Documentation

- **Design Document:** `/docs/plans/2026-01-02-voucher-preview-drawer-design.md`
- **Component Locations:**
  - `/src/pages/archives/ArchiveDetailDrawer.tsx`
  - `/src/pages/panorama/VoucherPreviewDrawer.tsx`
  - `/src/components/voucher/` (child components)

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-02 | 1.0.0 | Initial implementation of drawer system |
| 2026-01-02 | 1.0.0 | Responsive width calculation (50vw/70vw/100vw) |
| 2026-01-02 | 1.0.0 | Route-based auto-close |
| 2026-01-02 | 1.0.0 | Expand-to-page functionality |
