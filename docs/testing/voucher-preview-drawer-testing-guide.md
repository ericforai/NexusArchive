# Voucher Preview Drawer - Manual Testing Guide

**Created:** 2026-01-02
**Purpose:** Comprehensive manual testing checklist for voucher preview drawer components

---

## Test Environment Setup

### Browser DevTools Configuration

**Chrome/Edge DevTools:**
1. Open DevTools (F12 or Cmd+Option+I)
2. Click device toolbar icon (Cmd+Shift+M)
3. Test at standard breakpoints:
   - iPhone SE (375px)
   - iPad (768px)
   - Laptop (1280px)
   - Desktop (1920px)

**Firefox Responsive Design Mode:**
1. Open DevTools (F12 or Cmd+Option+I)
2. Click responsive design mode icon (Cmd+Shift+M)
3. Use preset devices or custom widths

---

## Task 7: Responsive Design Testing

### Test Case 1: Mobile View (375px)

**Viewport:** 375px × 667px (iPhone SE)

**Expected Behavior:**
- [ ] Drawer opens at full screen width (100vw)
- [ ] No visible overlay mask behind drawer
- [ ] Content area uses full available width
- [ ] Close button visible in top-right corner
- [ ] Touch targets are at least 44px × 44px
- [ ] Horizontal scroll appears for wide tables
- [ ] Tab labels remain readable (may use icons only)

**Steps:**
1. Open Chrome DevTools device toolbar
2. Select "iPhone SE" or set width to 375px
3. Navigate to archive list
4. Click any archive item
5. Verify drawer opens at full screen

**Pass Criteria:**
- Drawer covers entire screen
- No white space on edges
- Close button easily tappable
- Content scrolls smoothly

---

### Test Case 2: Tablet View (768px)

**Viewport:** 768px × 1024px (iPad)

**Expected Behavior:**
- [ ] Drawer opens at 70% viewport width
- [ ] Overlay mask visible on left side
- [ ] Content area uses available space efficiently
- [ ] Close button and title visible
- [ ] Tab labels use text format (may drop icons)
- [ ] Metadata grid adapts to single column

**Steps:**
1. Open Chrome DevTools device toolbar
2. Select "iPad" or set width to 768px
3. Navigate to archive list
4. Click any archive item
5. Verify drawer width and layout

**Pass Criteria:**
- Drawer covers 70% of screen
- 30% overlay mask visible
- Layout adapts without horizontal scroll
- All controls accessible

---

### Test Case 3: Desktop View (1280px)

**Viewport:** 1280px × 720px (Laptop)

**Expected Behavior:**
- [ ] Drawer opens at 50% viewport width (~640px)
- [ ] Overlay mask visible on left side
- [ ] Content area uses two-column layout (metadata grid)
- [ ] All tabs visible with icons and text
- [ ] Voucher canvas renders properly
- [ ] Attachments display in grid

**Steps:**
1. Resize browser window to 1280px width
2. Navigate to archive list
3. Click any archive item
4. Verify drawer width and layout

**Pass Criteria:**
- Drawer width approximately 640px (50vw)
- List remains visible on left
- Two-column metadata layout
- Canvas renders without scrollbars

---

### Test Case 4: Large Desktop View (1920px)

**Viewport:** 1920px × 1080px (Full HD)

**Expected Behavior:**
- [ ] Drawer opens at 50% viewport width (~960px, but max 900px)
- [ ] Content area comfortably displays all information
- [ ] No horizontal scrolling within drawer
- [ ] All interactive elements easily clickable
- [ ] Performance remains smooth

**Steps:**
1. Resize browser window to 1920px width
2. Navigate to archive list
3. Click any archive item
4. Verify drawer width and content

**Pass Criteria:**
- Drawer width not exceeding 900px
- Content fits without horizontal scroll
- Smooth animations
- Fast rendering

---

## Interaction Testing

### Test Case 5: Open Drawer

**Methods to Test:**
1. [ ] **Click list item**
   - Click any row in archive table
   - Drawer opens smoothly

2. [ ] **Keyboard navigation**
   - Focus on table row (Tab to navigate)
   - Press Enter or Space
   - Drawer opens

**Expected:**
- Drawer slides in from right
- Animation duration ~300ms
- First tab (metadata) is active
- Close button visible

---

### Test Case 6: Close Drawer

**Methods to Test:**
1. [ ] **Click overlay mask**
   - Click dark overlay on left side
   - Drawer closes immediately

2. [ ] **Press ESC key**
   - With drawer open, press ESC
   - Drawer closes immediately

3. [ ] **Click close button (X)**
   - Click X button in top-right
   - Drawer closes immediately

4. [ ] **Route change**
   - With drawer open, click different menu item
   - Drawer closes before navigation

**Expected:**
- Drawer slides out to right
- Animation matches open direction
- List view regains focus
- No residual content rendered

---

### Test Case 7: Tab Switching

**Tabs to Test:**
1. [ ] **Business Metadata (业务元数据)**
   - Displays voucher metadata
   - Grid layout on desktop
   - Single column on mobile

2. [ ] **Accounting Voucher (会计凭证)**
   - Canvas renders properly
   - No horizontal scroll
   - Amounts visible

3. [ ] **Attachments (关联附件)**
   - File list displays
   - Thumbnails load
   - Click to preview

**Expected:**
- Tab content switches instantly (< 100ms)
- Active tab is highlighted
- Content area scrolls independently
- No visual glitches during switch

---

### Test Case 8: Expand to New Page

**Steps:**
1. [ ] Open drawer for any archive
2. [ ] Click "展开到新页" button (ExternalLink icon)
3. [ ] Verify navigation to `/system/archives/:id`
4. [ ] Click browser back button
5. [ ] Verify return to list view

**Expected:**
- New page opens with same content
- Full-width layout (no drawer)
- Breadcrumb shows navigation path
- Back button works correctly
- Drawer state is reset

---

## Performance Testing

### Test Case 9: First Render Performance

**Tool:** Chrome DevTools Performance tab

**Steps:**
1. Open DevTools Performance tab
2. Start recording
3. Click archive item to open drawer
4. Stop recording when drawer appears

**Metrics to Check:**
- [ ] **First Contentful Paint:** < 500ms
- [ ] **Time to Interactive:** < 1s
- [ ] **Total rendering time:** < 100ms (after data loaded)

**Pass Criteria:**
- Drawer appears within 500ms of click
- No noticeable jank or stutter
- Smooth slide-in animation

---

### Test Case 10: Tab Switch Performance

**Steps:**
1. Open drawer
2. Switch to "Accounting Voucher" tab
3. Switch to "Attachments" tab
4. Switch back to "Business Metadata"

**Metrics:**
- [ ] Tab switch: < 100ms (cached data)
- [ ] Canvas render: < 300ms
- [ ] Attachment thumbnails: Progressive load

**Pass Criteria:**
- Instant tab switch for metadata
- Canvas renders without blocking UI
- Images load progressively

---

### Test Case 11: Memory Management

**Tool:** Chrome DevTools Memory tab

**Steps:**
1. Open Memory tab
2. Take heap snapshot
3. Open and close drawer 10 times
4. Take another heap snapshot
5. Compare snapshots

**Expected:**
- [ ] No memory leaks detected
- [ ] Heap returns to baseline after closes
- [ ] Event listeners properly cleaned up
- [ ] DOM nodes properly destroyed

---

## Accessibility Testing

### Test Case 12: Keyboard Navigation

**Steps:**
1. [ ] Open archive list
2. [ ] Use Tab to navigate to table row
3. [ ] Press Enter to open drawer
4. [ ] Use Tab to navigate within drawer
5. [ ] Press ESC to close drawer

**Expected:**
- Focus visible on all interactive elements
- Logical tab order (close button → tabs → content)
- ESC closes drawer
- No keyboard traps

---

### Test Case 13: Screen Reader Compatibility

**Tools:** NVDA (Windows), VoiceOver (Mac)

**Steps:**
1. Enable screen reader
2. Navigate to archive list
3. Open drawer
4. Navigate through tabs

**Expected:**
- [ ] Drawer announces "凭证预览"
- [ ] Tab labels announced correctly
- [ ] Button labels descriptive
- [ ] Close button announced as "关闭"

---

### Test Case 14: Touch Target Sizes

**Device:** Mobile device or device simulator

**Steps:**
1. Set viewport to 375px
2. Open drawer
3. Attempt to tap all interactive elements

**Expected:**
- [ ] Close button: ≥ 44px × 44px
- [ ] Tab buttons: ≥ 44px height
- [ ] Action buttons: ≥ 44px × 44px
- [ ] No targets smaller than 44px

---

## Cross-Browser Testing

### Test Case 15: Browser Compatibility

**Browsers to Test:**
1. [ ] **Chrome/Edge (Chromium)**
   - Version: Latest stable
   - All features work

2. [ ] **Firefox**
   - Version: Latest stable
   - Animations smooth
   - Layout correct

3. [ ] **Safari**
   - Version: Latest stable
   - Swipe gestures (if implemented)
   - Scroll behavior

**Expected:**
- Consistent behavior across browsers
- No visual differences > 5px
- Animations smooth in all browsers

---

## Regression Testing

### Test Case 16: Route Change Auto-Close

**Steps:**
1. Open drawer
2. Click different menu item (e.g., "凭证池")
3. Verify drawer closes before navigation

**Expected:**
- Drawer closes smoothly
- No console errors
- New page loads correctly
- No drawer overlay remains

---

### Test Case 17: Data Refresh

**Steps:**
1. Open drawer for Archive A
2. Close drawer
3. Open drawer for Archive B
4. Verify data is correct for Archive B

**Expected:**
- No data from Archive A visible
- All data updates to Archive B
- No stale data displayed
- No flickering or loading errors

---

## Bug Reporting Template

If any test fails, document with this template:

```markdown
### Bug Report: [Brief Description]

**Test Case:** [Test case number and name]

**Steps to Reproduce:**
1.
2.
3.

**Expected Behavior:**
[What should happen]

**Actual Behavior:**
[What actually happened]

**Environment:**
- Browser: [Name and version]
- Viewport: [Width × Height]
- OS: [Operating system]

**Screenshots:**
[Attach screenshots if applicable]

**Console Errors:**
[Paste any console errors]

**Severity:** [Critical / High / Medium / Low]
```

---

## Test Execution Checklist

**Tester Name:** _______________
**Test Date:** _______________
**Browser Version:** _______________

**Overall Results:**
- Total Test Cases: 17
- Passed: _____
- Failed: _____
- Skipped: _____

**Sign-off:**
- [ ] All critical tests passed
- [ ] All high-priority tests passed
- [ ] Documentation updated
- [ ] Bugs reported (if any)

**Tester Comments:**
_______________________________________________
_______________________________________________
_______________________________________________
