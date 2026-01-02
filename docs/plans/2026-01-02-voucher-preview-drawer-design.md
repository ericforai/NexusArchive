# Voucher Preview Drawer - UX Optimization Design

**Date:** 2026-01-02
**Status:** ✅ Completed
**Implementation:** 2026-01-02
**Goal:** Replace modal-based voucher preview with drawer-based UX for better user experience

---

## Problem Statement

Current modal implementation has critical UX issues:
1. Modal too small, content doesn't display properly (`h-[85vh]`)
2. Modal positioned too low (`mt-10` pushes it down)
3. Modal doesn't close when navigating to other menus
4. User feedback: "弹窗是最不好的体验" (modals are the worst experience)

---

## Design Solution: Side Drawer with Expand-to-Page Feature

### Section 1: Architecture

**Component Choice:** Ant Design Drawer

**Responsive Width Strategy:**
| Screen Size | Width | Behavior |
|-------------|-------|----------|
| ≥1280px | 50vw (max 900px) | Standard side drawer |
| 768-1279px | 70vw | Wide drawer |
| <768px | 100vw | Full-screen drawer |

**Structure:**
- Fixed header (title + action buttons)
- Scrollable content area with tabs
- Optional footer (expand to new page button)

---

### Section 2: Interaction Design

**Open Methods:**
- Click list item (table row)
- Keyboard shortcuts (Enter/Space on focused row)

**Close Methods:**
- Click mask (overlay)
- Press ESC key
- Click close button (X icon)
- Route change listener (auto-close on navigation)

**Expand to New Page Feature:**
- Button in drawer header: "展开到新页"
- Navigates to `/system/archives/:id` route
- New page shows same content in full-width layout
- Back button returns to previous list

**Mobile Gestures:**
- Swipe from right edge: Open drawer
- Pull down from top: Close drawer

---

### Section 3: Responsive Design Details

**Large Screens (≥1280px) - Standard Drawer:**
- Width: 50vw (max 900px)
- Tab layout: Horizontal, icon + text
- Content: Two-column layout (metadata grid, full-width table)
- Font: Base 14px, headings 16px
- Spacing: padding-4 (16px)

**Medium Screens (768-1279px) - Wide Drawer:**
- Width: 70vw
- Tab layout: Horizontal, text only
- Content: Single-column layout
- Font: Base 13px, headings 15px
- Spacing: padding-3 (12px)

**Small Screens (<768px) - Full-Screen Drawer:**
- Width: 100vw
- Tab layout: Bottom navigation bar style (icons only)
- Content: Single-column, compact layout
- Font: Base 12px, headings 14px
- Spacing: padding-2 (8px)
- Touch targets: Minimum 44px × 44px

**Mobile-Specific:**
- Table horizontal scroll
- Monospace font for amounts (prevent line break)
- Long text truncation (ellipsis + click to expand)
- Close button fixed in top-right (doesn't disappear on scroll)

**Animation:**
- Desktop: cubic-bezier(0.4, 0, 0.2, 1) 300ms
- Mobile: cubic-bezier(0.25, 0.46, 0.45, 0.94) 250ms (more elastic)

---

### Section 4: Performance Optimization

**Lazy Loading:**
- Drawer content not rendered until opened
- Tab content loaded on-demand:
  - Business metadata: Default load (first tab)
  - Accounting voucher: Load on tab switch (defer Canvas render)
  - Attachments: Load on tab switch (image lazy loading)
- Use React.lazy() for non-critical components

**Data Caching:**
- React Query cache with 5-minute TTL
- Same voucher number: Read from cache
- Preload adjacent records (current ±1)

**Render Optimization:**
- VoucherMetadata: React.memo wrapped
- VoucherPreviewCanvas: Virtual scroll when > 50 entries
- Debounce inputs: Search/filter 300ms debounce
- Throttle scroll: Scroll events 100ms throttle

**Resource Optimization:**
- Attachment thumbnails: Compressed version (max 200px), load original on click
- PDF preview: Paginated load, not all-at-once
- Abort mechanism: Cancel incomplete API requests when drawer closes

**Memory Management:**
- Clear internal state on drawer close (useEffect cleanup)
- Large images: Revoke URL.createObjectURL after unmount
- Limit concurrent attachment previews (max 3)

**Performance Targets:**
- First open: < 500ms (click to content visible)
- Tab switch: < 100ms (cached data)
- Mobile Lighthouse score: > 90

---

### Section 5: Implementation Details

**Component Structure:**
```
ArchiveDetailDrawer (new component)
├── DrawerHeader (fixed header, title + actions)
├── DrawerContent (scrollable content area)
│   ├── VoucherMetadataTab (business metadata)
│   ├── VoucherCanvasTab (accounting voucher)
│   └── AttachmentsTab (related attachments)
└── DrawerFooter (optional, expand-to-page button)
```

**State Management:**
- Create `useDrawerStore` with Zustand:
  - `isOpen`: Drawer open/close state
  - `activeTab`: Current active tab
  - `archiveId`: Current viewing archive ID
  - `expandedMode`: Whether expanded to full page
- Listen to React Router location changes, auto-close drawer

**Ant Design Drawer Config:**
```typescript
<Drawer
  open={isOpen}
  onClose={handleClose}
  width={drawerWidth} // responsive
  placement="right"
  maskClosable={true}
  keyboard={true} // ESC close
  destroyOnClose={true} // destroy content on close
  styles={{
    body: { padding: 0 },
    header: { padding: '16px 24px' }
  }}
  closeIcon={null} // custom close button
>
```

**Route Listener:**
```typescript
useEffect(() => {
  const unlisten = window.history.listen(({ location }) => {
    drawerStore.close();
  });
  return unlisten;
}, []);
```

**Expand to New Page:**
- Route: `/system/archives/:id`
- Navigate: `navigate(`/system/archives/${archiveId}`)`
- Reuse same tab components in new page
- Breadcrumb: `档案管理 > 凭证详情 > {凭证号}`

**Migration Steps:**
1. Create `ArchiveDetailDrawer.tsx` (keep `ArchiveDetailModal.tsx` as backup)
2. Update all Modal usages to Drawer
3. Update Playwright test selectors
4. Verify responsive layout (Chrome DevTools device simulation)
5. Performance testing (Lighthouse)
6. Remove old Modal component

**Testing Checklist:**
- E2E: Drawer open/close, tab switching, route change auto-close
- Responsive: Test 375px / 768px / 1280px / 1920px breakpoints
- Performance: First render < 500ms
- Accessibility: Keyboard navigation, focus management, ARIA labels

---

## User Feedback Integration

**Addressed Issues:**
- ✅ Content display: Wider drawer (50-100vw) vs modal (max-w-6xl)
- ✅ Position: Drawer from right edge vs modal with mt-10
- ✅ Auto-close: Route listener implementation
- ✅ UX improvement: Drawer pattern vs modal pattern

**User Requests:**
- "考虑性能、体验、用户习惯、用户友好度" → Performance & interaction optimization sections
- "弹窗是最不好的体验" → Drawer with expand-to-page alternative

---

## Next Steps

After this design approval:
1. Create implementation plan using `superpowers:writing-plans`
2. Set up isolated worktree using `superpowers:using-git-worktrees`
3. Execute implementation with `superpowers:subagent-driven-development`
