# Fix Config Center Button - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix P0 bug - "配置中心" button should open ConnectorForm modal instead of inline edit form

**Architecture:** Remove inline editing state from ErpConfigCard, delegate to existing ConnectorForm modal (useConnectorModal hook)

**Tech Stack:** React 19, TypeScript 5.8, Vitest, React Testing Library

---

## Problem Analysis

**Current (Broken) Behavior:**
- User clicks "配置中心" button → Shows inline edit form with only "name" field
- Missing: ERP type, URL, App Key, App Secret, Accbook codes

**Expected Behavior:**
- User clicks "配置中心" button → Opens ConnectorForm modal with all 6 fields
- Consistent UX with "添加连接器" button

**Root Cause:**
```typescript
// Current (wrong)
onClick={() => setIsEditing(!isEditing)}  // Triggers inline edit

// Should be
onClick={() => onConfig?.(config)}  // Opens ConnectorForm modal
```

**Data Flow:**
```
IntegrationSettingsPage
  └── connectorModal.actions.openModal(config)
      └── ErpConfigList
          └── onConfig prop
              └── ErpConfigCard
                  └── "配置中心" button should call onConfig(config)
```

---

### Task 1: Remove Inline Editing from ErpConfigCard

**Files:**
- Modify: `src/components/settings/integration/components/ErpConfigCard.tsx:39-41`
- Modify: `src/components/settings/integration/components/ErpConfigCard.tsx:84`
- Modify: `src/components/settings/integration/components/ErpConfigCard.tsx:134-138`
- Delete: `src/components/settings/integration/components/ErpConfigCard.tsx:159-192`

**Step 1: Remove inline editing state**

Remove lines 39-41:
```typescript
// DELETE THESE LINES:
const [isEditing, setIsEditing] = useState(false);
const [editForm, setEditForm] = useState({ name: config.name });
```

**Step 2: Fix "配置中心" button click handler**

Replace line 84:
```typescript
// OLD (WRONG):
onClick={() => setIsEditing(!isEditing)}

// NEW:
onClick={() => onConfig?.(config)}
```

**Step 3: Remove "编辑配置" from more menu**

Delete lines 134-138 (the "编辑配置" menu item):
```typescript
// DELETE THIS ENTIRE MENU ITEM:
<button
  role="menuitem"
  onClick={() => {
    setIsEditing(true);
    setShowMoreMenu(false);
  }}
  className="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
>
  <Sliders size={14} className="text-gray-500" />
  <span>编辑配置</span>
</button>
```

**Step 4: Delete inline edit form UI**

Delete lines 159-192 (entire inline edit form section):
```typescript
// DELETE THIS ENTIRE SECTION:
{/* Inline Edit Form */}
{isEditing && (
  <div className="p-5 bg-blue-50 border-t border-blue-100" data-testid="inline-config-form">
    ...all the form fields...
  </div>
)}
```

**Step 5: Remove unused imports**

Remove line 3 (Sliders icon no longer needed):
```typescript
// DELETE: Sliders from import
import { Settings, Zap, Activity, ShieldCheck, MoreHorizontal, ChevronRight } from 'lucide-react';
```

**Step 6: Run type check**

```bash
npx tsc --noEmit
```

Expected: No errors (isEditing, editForm, setIsEditing should be gone)

**Step 7: Commit**

```bash
git add src/components/settings/integration/components/ErpConfigCard.tsx
git commit -m "fix(erp-card): remove inline editing, delegate to ConnectorForm modal

- Remove isEditing state and inline edit form
- Fix '配置中心' button to call onConfig(config) which opens modal
- Remove '编辑配置' from more menu (redundant)
- User can now edit all 6 fields (name, type, URL, keys, accbooks)

Fixes P0: Config center button opens full configuration form
"
```

---

### Task 2: Update ErpConfigCard Tests

**Files:**
- Modify: `src/components/settings/integration/components/__tests__/ErpConfigCard.test.tsx`

**Step 1: Write test for config button behavior**

Add new test after existing tests:

```typescript
it('should call onConfig with config when clicking config center button', () => {
  const mockOnConfig = vi.fn();
  const { getByRole } = render(
    <ErpConfigCard
      config={mockConfig}
      status="connected"
      onConfig={mockOnConfig}
    />
  );

  const configButton = getByRole('button', { name: /配置中心/ });
  fireEvent.click(configButton);

  expect(mockOnConfig).toHaveBeenCalledTimes(1);
  expect(mockOnConfig).toHaveBeenCalledWith(mockConfig);
});
```

**Step 2: Remove inline edit form tests**

Delete these tests (no longer applicable):
```typescript
// DELETE THESE TESTS:
it('should show inline edit form when clicking config center button')
it('should update connector name in inline edit form')
it('should save edited name and close form')
it('should cancel editing and close form')
```

**Step 3: Run tests**

```bash
npm run test -- ErpConfigCard.test.tsx
```

Expected: All tests pass (new test passes, old tests removed)

**Step 4: Commit**

```bash
git add src/components/settings/integration/components/__tests__/ErpConfigCard.test.tsx
git commit -m "test(erp-card): update tests for modal-based config editing

- Add test: config button calls onConfig with full config object
- Remove inline edit form tests (functionality removed)
- Verify modal opens instead of inline form
"
```

---

### Task 3: Verify Integration Test

**Files:**
- Test: `src/components/settings/integration/components/__tests__/IntegrationSettingsPage.test.tsx`

**Step 1: Check if integration test exists**

```bash
grep -n "onConfig" src/components/settings/integration/components/__tests__/IntegrationSettingsPage.test.tsx
```

**Step 2: If exists, verify it tests modal open**

Test should verify:
1. Click "配置中心" button
2. connectorModal.actions.openModal is called
3. ConnectorForm modal appears

**Step 3: If test needs update, add/modify:**

```typescript
it('should open ConnectorForm modal when clicking config center button', () => {
  render(<IntegrationSettingsPage erpApi={mockErpApi} />);

  const configButtons = screen.getAllByRole('button', { name: /配置中心/ });
  fireEvent.click(configButtons[0]);

  expect(screen.getByText(/编辑连接器配置|创建连接器配置/)).toBeInTheDocument();
});
```

**Step 4: Run integration test**

```bash
npm run test -- IntegrationSettingsPage.test.tsx
```

Expected: Pass

**Step 5: Commit if changes made**

```bash
git add src/components/settings/integration/components/__tests__/IntegrationSettingsPage.test.tsx
git commit -m "test(integration): verify config center opens modal"
```

---

### Task 4: Update Documentation

**Files:**
- Modify: `src/components/settings/integration/README.md`
- Modify: `src/components/settings/integration/components/README.md`

**Step 1: Update main README**

Find "ErpConfigCard" section in Components table (around line 177):

```markdown
# OLD:
| ErpConfigCard.tsx | 225 | **Summary Card UI** | 连接器摘要卡片，固定高度，显示状态、健康、场景统计、操作按钮 |

# NEW:
| ErpConfigCard.tsx | ~200 | **Summary Card UI** | 连接器摘要卡片，固定高度。"配置中心"按钮打开 ConnectorForm 模态框（完整6字段） |
```

**Step 2: Update components README**

Find "ErpConfigCard" section in components/README.md:

```markdown
# OLD:
| ErpConfigCard.tsx | ~225 | **Summary Card** | 连接器摘要卡片，固定高度，显示状态、健康、场景统计、操作按钮 |

# NEW:
| ErpConfigCard.tsx | ~200 | **Summary Card** | 连接器摘要卡片，固定高度。点击"配置中心"打开 ConnectorForm 模态框编辑完整配置 |
```

**Step 3: Add note about modal delegation**

Add to components README after ErpConfigCard description:

```markdown
**Note:** "配置中心" button delegates to useConnectorModal hook → ConnectorForm modal
```

**Step 4: Commit**

```bash
git add src/components/settings/integration/README.md src/components/settings/integration/components/README.md
git commit -m "docs(integration): update ErpConfigCard documentation

- Clarify '配置中心' button opens ConnectorForm modal
- Remove references to inline editing
- Document modal delegation pattern
"
```

---

### Task 5: Final Verification

**Step 1: Run full test suite**

```bash
npm run test:run
```

Expected: All tests pass (should see ~220 tests)

**Step 2: Type check**

```bash
npx tsc --noEmit
```

Expected: No errors

**Step 3: Architecture check**

```bash
npm run check:arch
```

Expected: Pass (acceptable pre-existing warnings)

**Step 4: Visual verification**

Start dev server:
```bash
npm run dev
```

Manual test:
1. Navigate to 集成设置 page
2. Click "配置中心" button on any connector card
3. **Expected:** ConnectorForm modal opens with all 6 fields
4. **Not expected:** Inline form on card itself

**Step 5: Create milestone commit**

```bash
git add .
git commit -m "feat(erp): fix P0 - config center opens full modal

Complete fix for '配置中心' button behavior:
- Removed inline editing (only had 'name' field)
- Now opens ConnectorForm modal with all 6 fields:
  1. 配置名称
  2. ERP类型
  3. 服务地址 (URL)
  4. 应用密钥 (App Key)
  5. 应用密钥 (App Secret)
  6. 账套编码

Testing:
- Updated ErpConfigCard tests
- Verified integration test
- All 220+ tests passing

Documentation:
- Updated README.md files
- Clarified modal delegation pattern

Fixes: P0 bug - users can now edit complete connector configuration
"
```

---

## Success Criteria

After implementation:

✅ "配置中心" button opens ConnectorForm modal
✅ Modal shows all 6 configuration fields
✅ Consistent UX with "添加连接器" button
✅ All tests pass (220+)
✅ No TypeScript errors
✅ Documentation updated

---

## Rollback Plan (if needed)

```bash
git revert HEAD  # Revert the milestone commit
```

---

**Estimated Total Time:** 30 minutes
**Priority:** P0 (Critical bug - users cannot edit connector configuration properly)
