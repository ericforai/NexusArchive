# ConnectorForm Modal to Drawer Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Convert the ConnectorForm from center modal to right-side drawer, matching ScenarioDrawer style

**Architecture:** Replace custom modal overlay (div-based) with Ant Design Drawer component. Keep all form logic, validation, and state management intact. Only change the presentation layer.

**Tech Stack:** React, Ant Design (Drawer component), TypeScript, lucide-react icons

---

## Task 1: Add Ant Design Drawer Import

**Files:**
- Modify: `src/components/settings/integration/components/ConnectorForm.tsx:8-10`

**Step 1: Add Drawer import**

Add `Drawer` to the existing Ant Design imports (if any) or create new import.

```typescript
import { Drawer } from 'antd';
```

**Step 2: Verify import works**

Run: `npm run dev`
Expected: Dev server starts without import errors

---

## Task 2: Replace Modal Overlay with Drawer Component

**Files:**
- Modify: `src/components/settings/integration/components/ConnectorForm.tsx:32-221`

**Step 1: Replace the entire return statement**

Replace lines 32-221 with Drawer-based implementation:

```typescript
  return (
    <Drawer
      title={
        <div className="flex items-center justify-between">
          <span className="text-lg font-semibold">
            {isEdit ? '编辑连接器配置' : '创建连接器配置'}
          </span>
        </div>
      }
      placement="right"
      width={640}
      open={show}
      onClose={actions.closeModal}
      styles={{
        body: { padding: '16px' },
      }}
      extra={
        <button
          onClick={actions.closeModal}
          className="p-1 hover:bg-gray-100 rounded"
        >
          <X size={18} />
        </button>
      }
    >
      {/* Form */}
      <div className="space-y-4">
        {/* Config Name */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            配置名称 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={configForm.name}
            onChange={e => actions.updateForm('name', e.target.value)}
            placeholder="例如: 用友YonSuite生产环境"
            className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* ERP Type */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            ERP类型 <span className="text-red-500">*</span>
          </label>
          <select
            value={configForm.erpType}
            onChange={e => actions.updateForm('erpType', e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">请选择ERP类型</option>
            <option value="yonsuite">用友 YonSuite</option>
            <option value="kingdee">金蝶云星空</option>
            <option value="weaver">泛微 OA</option>
            <option value="generic">通用 REST API</option>
          </select>
        </div>

        {/* Base URL */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            服务地址 <span className="text-red-500">*</span>
          </label>
          <div className="flex gap-2">
            <input
              type="url"
              value={configForm.baseUrl}
              onChange={e => actions.updateForm('baseUrl', e.target.value)}
              placeholder="https://api.example.com"
              className="flex-1 px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              onClick={() => actions.detectErpType(configForm.baseUrl)}
              disabled={!configForm.baseUrl}
              className="px-4 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded disabled:opacity-50"
            >
              自动检测
            </button>
          </div>
          {detectedType && (
            <div className="mt-1 text-sm text-green-600 flex items-center gap-1">
              <CheckCircle size={14} />
              检测到: {detectedType}
            </div>
          )}
        </div>

        {/* App Key */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            应用密钥 (App Key) <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={configForm.appKey}
            onChange={e => actions.updateForm('appKey', e.target.value)}
            placeholder="您的应用密钥"
            className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* App Secret */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            应用密钥 (App Secret) <span className="text-red-500">*</span>
          </label>
          <input
            type="password"
            value={configForm.appSecret}
            onChange={e => actions.updateForm('appSecret', e.target.value)}
            placeholder="您的应用密钥"
            className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* Accbook Codes */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            账套编码
          </label>
          <div className="space-y-2">
            {configForm.accbookCodes.map(code => (
              <div key={code} className="flex items-center gap-2">
                <input
                  type="text"
                  value={code}
                  readOnly
                  className="flex-1 px-3 py-2 border border-gray-300 rounded bg-gray-50"
                />
                <button
                  onClick={() => actions.removeAccbookCode(code)}
                  className="p-2 text-red-600 hover:bg-red-50 rounded"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
            <div className="flex gap-2">
              <input
                type="text"
                value={newAccbookCode}
                onChange={e => actions.updateForm('newAccbookCode', e.target.value)}
                placeholder="输入账套编码"
                className="flex-1 px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <button
                onClick={() => actions.addAccbookCode(newAccbookCode)}
                disabled={!newAccbookCode}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 flex items-center gap-1"
              >
                <Plus size={16} />
                添加
              </button>
            </div>
          </div>
        </div>

        {/* Connection Test */}
        <div className="pt-4 border-t">
          <button
            onClick={actions.testConnection}
            disabled={!configForm.baseUrl || !configForm.appKey || !configForm.appSecret || isTesting}
            className="w-full px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {isTesting ? (
              <>
                <Loader2 size={16} className="animate-spin" />
                测试连接中...
              </>
            ) : (
              <>
                <CheckCircle size={16} />
                测试连接
              </>
            )}
          </button>
        </div>
      </div>

      {/* Footer Actions */}
      <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
        <button
          onClick={actions.closeModal}
          className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-100"
        >
          取消
        </button>
        <button
          onClick={actions.saveConfig}
          disabled={!configForm.name || !configForm.erpType || !configForm.baseUrl || !configForm.appKey || !configForm.appSecret}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isEdit ? '保存' : '创建'}
        </button>
      </div>
    </Drawer>
  );
```

**Step 2: Verify the file compiles**

Run: `npx tsc --noEmit`
Expected: No type errors

---

## Task 3: Remove Unused X Icon Import (If Not Needed)

**Files:**
- Modify: `src/components/settings/integration/components/ConnectorForm.tsx:9`

**Step 1: Check if X icon is still used elsewhere**

Search for `X` usage in the file after Drawer conversion.

Run: `grep -n "<X" src/components/settings/integration/components/ConnectorForm.tsx`

**Step 2: Remove X import if no longer needed**

If X is only used in the close button (now handled by Drawer's built-in close), remove from import:

```typescript
import { Plus, Trash2, Loader2, CheckCircle, XCircle } from 'lucide-react';
```

(Note: X is kept since we use it in the `extra` prop for custom close button)

---

## Task 4: Test the Drawer

**Files:**
- Test: Manual browser test

**Step 1: Start dev server**

Run: `npm run dev`
Expected: Server starts on http://localhost:5173

**Step 2: Navigate to integration settings**

1. Open http://localhost:5173/system/settings/integration
2. Click "配置中心" button
3. Verify drawer opens from right side

**Step 3: Verify functionality**

- [ ] Drawer slides in from right
- [ ] Header shows correct title (创建/编辑连接器配置)
- [ ] Close button (X) works
- [ ] Click outside drawer closes it
- [ ] All form fields are visible and functional
- [ ] Form validation still works
- [ ] Test connection button works
- [ ] Save/Create button works
- [ ] Cancel button works

---

## Task 5: Update File Header Comment

**Files:**
- Modify: `src/components/settings/integration/components/ConnectorForm.tsx:1-4`

**Step 1: Update comment to reflect Drawer usage**

```typescript
// Input: ConnectorModal State/Actions interfaces
// Output: ConnectorForm drawer component (right-side slide-in)
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
```

---

## Task 6: Commit Changes

**Step 1: Stage modified file**

```bash
git add src/components/settings/integration/components/ConnectorForm.tsx
```

**Step 2: Commit with descriptive message**

```bash
git commit -m "refactor(integration): convert ConnectorForm from modal to right-side drawer

- Replace custom modal overlay with Ant Design Drawer
- Change placement to 'right' with width 640px
- Match ScenarioDrawer style for consistency
- Keep all form logic and validation intact
```

---

## Summary of Changes

1. **Import changes**: Add `Drawer` from 'antd'
2. **Component structure**: Replace div-based modal with Drawer component
3. **Layout**: Center overlay → Right-side slide-in (width: 640px)
4. **Header**: Custom header → Drawer title + extra close button
5. **Content**: Form fields remain unchanged
6. **Footer**: Action buttons moved to bottom of Drawer body

## Testing Checklist

- [ ] Drawer opens from right side smoothly
- [ ] Close button (X) works
- [ ] Click outside (backdrop) closes drawer
- [ ] Form validation still prevents invalid submissions
- [ ] Test connection functionality works
- [ ] Save/Create persists data correctly
- [ ] Edit mode loads existing data
- [ ] Mobile responsiveness (if applicable)
