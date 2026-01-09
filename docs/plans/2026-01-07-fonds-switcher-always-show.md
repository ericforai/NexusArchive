# FondsSwitcher Adaptive Display Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Modify FondsSwitcher component to always display (never hide), with adaptive styling based on number of fonds available to user.

**Architecture:** Conditional rendering based on `fondsList.length` - plain text for single fonds, dropdown button for multiple fonds.

**Tech Stack:** React 19.2, TypeScript, lucide-react icons, Tailwind CSS classes

---

## Task 1: Modify FondsSwitcher Component for Adaptive Display

**Files:**
- Modify: `src/components/common/FondsSwitcher.tsx:63-132`

**Step 1: Remove the hide logic for single fonds**

Remove lines 63-66 that hide the component when `fondsList.length <= 1`:

```typescript
// DELETE THESE LINES:
// 如果只有一个全宗或没有全宗，隐藏切换器
if (fondsList.length <= 1) {
    return null;
}
```

**Step 2: Create adaptive render logic**

Replace the entire return statement (lines 68-132) with conditional rendering based on `fondsList.length`:

```typescript
// 单个全宗：显示为纯文本（无图标，无下拉）
if (fondsList.length === 1 && currentFonds) {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm">
            <span className="text-slate-600 font-medium">
                {currentFonds.fondsName}
            </span>
            <span className="ml-2 text-xs text-slate-400 font-mono">
                {currentFonds.fondsCode}
            </span>
        </div>
    );
}

// 多个全宗：显示下拉按钮（原有逻辑）
return (
    <div className="relative">
        {/* [existing dropdown button code remains unchanged] */}
    </div>
);
```

**Step 3: Handle empty state**

Add handling for when no fonds are available (but `hasHydrated` is true):

```typescript
// 无全宗时显示提示
if (fondsList.length === 0 && hasHydrated) {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm text-slate-400">
            暂无全宗权限
        </div>
    );
}
```

**Step 4: Handle loading state for single fonds**

When `isLoading === true` and we expect a single fonds, show loader:

```typescript
if (fondsList.length === 0 && isLoading) {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm">
            <Loader2 size={14} className="animate-spin text-slate-400" />
        </div>
    );
}
```

**Step 5: Complete modified component**

Here's the full modified return logic (lines 63-132 replacement):

```typescript
const [isOpen, setIsOpen] = React.useState(false);

// 加载状态
if (isLoading && fondsList.length === 0) {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm">
            <Loader2 size={14} className="animate-spin text-slate-400" />
        </div>
    );
}

// 无全宗权限
if (fondsList.length === 0 && hasHydrated) {
    return (
        <div className="flex items-center px-3 py-1.5 text-sm text-slate-400">
            暂无全宗权限
        </div>
    );
}

// 单个全宗：纯文本显示
if (fondsList.length === 1) {
    const fonds = fondsList[0];
    return (
        <div className="flex items-center px-3 py-1.5 text-sm">
            <span className="text-slate-700 font-medium">
                {fonds.fondsName}
            </span>
            <span className="ml-2 text-xs text-slate-400 font-mono">
                {fonds.fondsCode}
            </span>
        </div>
    );
}

// 多个全宗：下拉按钮（原有逻辑）
return (
    <div className="relative">
        <button
            onClick={() => setIsOpen(!isOpen)}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 transition-colors text-sm"
        >
            <Building2 size={16} className="text-slate-500" />
            <span className="font-medium text-slate-700 max-w-[120px] truncate">
                {currentFonds?.fondsName || '选择全宗'}
            </span>
            <ChevronDown
                size={14}
                className={`text-slate-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
            />
        </button>

        {/* 下拉菜单 [existing dropdown code unchanged] */}
    </div>
);
```

**Step 6: Update component header comment**

Update line 2-3 header comment:

```typescript
// Output: 全宗切换下拉组件 FondsSwitcher（自适应：单个全宗显示纯文本，多个全宗显示下拉）
```

**Step 7: Type check**

Run: `npm run typecheck`

Expected: No TypeScript errors

**Step 8: Build check**

Run: `npm run build`

Expected: Build succeeds without errors

---

## Task 2: Visual Testing

**Step 1: Test with single fonds (user1 - BR-GROUP only)**

1. Login as user1 (password: `User@123`)
2. Verify FondsSwitcher shows: "BR-GROUP 全宗测试" (plain text, no icon, no dropdown)
3. Check browser console for errors

Expected behavior:
- Pure text display, no Building2 icon
- No ChevronDown icon
- Shows both fonds name and code
- No dropdown functionality

**Step 2: Test with multiple fonds (admin user)**

1. Login as admin user
2. Verify FondsSwitcher shows dropdown button with Building2 icon
3. Click dropdown, verify all fonds are listed
4. Switch between fonds

Expected behavior:
- Building2 icon visible
- ChevronDown icon visible
- Dropdown menu shows all fonds
- Checkmark on currently selected fonds

**Step 3: Test with no fonds permission**

1. Create a test user with no fonds assigned
2. Login and verify "暂无全宗权限" message

Expected behavior:
- Shows "暂无全宗权限" in gray text
- No dropdown functionality

---

## Task 3: Update Documentation

**Files:**
- Modify: `src/components/common/components.md` (if exists)

**Step 1: Document the adaptive behavior**

Add or update component documentation:

```markdown
## FondsSwitcher

全宗切换组件，根据用户权限显示全宗信息。

### 行为模式

- **单个全宗**: 显示为纯文本（全宗名称 + 全宗代码），无图标
- **多个全宗**: 显示为下拉按钮（Building2 图标 + ChevronDown），可点击切换
- **无全宗**: 显示 "暂无全宗权限" 提示
- **加载中**: 显示旋转的 Loader2 图标
```

**Step 4: Commit changes**

```bash
git add src/components/common/FondsSwitcher.tsx
git commit -m "feat(fonds): adaptive FondsSwitcher display

- Always show FondsSwitcher (never hide)
- Single fonds: plain text display (name + code)
- Multiple fonds: dropdown button with icon
- No fonds: show '暂无全宗权限' message
- Loading state: spinner indicator"
```

---

## Success Criteria

1. Component always renders (never returns `null`)
2. Single fonds displays as plain text with name and code
3. Multiple fonds displays as clickable dropdown
4. No TypeScript errors
5. No console errors during testing
6. Visual appearance matches design specification

---

## Notes

- The component maintains the same props interface - no breaking changes
- All existing dropdown functionality is preserved for multiple fonds case
- The adaptive behavior is purely visual/interaction based
