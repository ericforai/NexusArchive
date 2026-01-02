# 凭证预览元数据优化 - 添加分录列表

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 在凭证预览标签页的左侧"业务元数据"区域中，保持当前基础字段的同时，在下方添加完整的分录列表（摘要、科目、借方、贷方）。

**架构:** 修改 VoucherMetadata 组件，添加分录列表展示区域，使用 VoucherDTO.entries 数据源。

**Tech Stack:** React 19, TypeScript 5.8, Tailwind CSS

---

## 背景说明

### 当前状态
- VoucherMetadata 组件显示基础元数据字段（凭证号、金额、日期等）
- 数据存储在 VoucherDTO.entries 数组中
- 每个分录包含：lineNo, summary, accountCode, accountName, debit, credit

### 目标状态
- 保持基础字段显示
- 在元数据列表下方添加分录列表
- 分录列表显示：摘要、科目（代码+名称）、借方、贷方

---

## Task 1: 修改 VoucherMetadata 组件添加分录列表区域

**Files:**
- Modify: `src/components/voucher/VoucherMetadata.tsx`

**Step 1: 在 VoucherMetadata 组件中添加分录列表渲染逻辑**

在组件 return 语句中，元数据字段列表后添加分录列表区域：

```tsx
// 在 </div> 标签后（metadataFields 列表结束后），添加分录列表
{data.entries && data.entries.length > 0 && (
  <div className="border-t border-slate-200">
    <div className={`px-4 py-3 border-b border-slate-100 bg-slate-50 ${compact ? 'py-2' : ''}`}>
      <h3 className="font-semibold text-slate-700 text-sm">凭证分录</h3>
    </div>
    <div className={`divide-y divide-slate-100 ${compact ? 'text-sm' : ''}`}>
      {data.entries.map((entry, index) => (
        <div key={entry.lineNo || index} className="px-4 py-3">
          {/* 分录头部 - 摘要 */}
          <div className="mb-2">
            <span className="text-xs text-slate-500 uppercase tracking-wide mr-2">
              分录 {entry.lineNo || index + 1}
            </span>
            <span className="font-medium text-slate-700">
              {entry.summary || '-'}
            </span>
          </div>

          {/* 分录详情 - 科目和金额 */}
          <div className="flex items-center justify-between text-sm">
            <div className="flex-1">
              <div className="text-slate-700">
                {entry.accountName || '-'}
              </div>
              {entry.accountCode && (
                <div className="text-xs text-slate-500 font-mono">
                  {entry.accountCode}
                </div>
              )}
            </div>

            <div className="flex gap-4 font-mono">
              <div className="text-right">
                <div className="text-xs text-slate-500">借方</div>
                <div className={`font-medium ${entry.debit ? 'text-slate-800' : 'text-slate-400'}`}>
                  {entry.debit ? formatCurrency(entry.debit) : '-'}
                </div>
              </div>
              <div className="text-right">
                <div className="text-xs text-slate-500">贷方</div>
                <div className={`font-medium ${entry.credit ? 'text-slate-800' : 'text-slate-400'}`}>
                  {entry.credit ? formatCurrency(entry.credit) : '-'}
                </div>
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  </div>
)}
```

**Step 2: 确保 formatCurrency 函数已导入**

检查文件顶部的 import 语句：

```tsx
import { formatCurrency, formatDate } from './styles';
```

如果没有 formatCurrency，添加它。

**Step 3: 在浏览器中验证**

Run: 刷新浏览器页面
Expected: 凭证预览标签页左侧元数据区域下方显示分录列表
- 每条分录显示：分录号、摘要
- 下方显示：科目名称、科目代码、借方金额、贷方金额

---

## Task 2: 调整样式优化

**Files:**
- Modify: `src/components/voucher/VoucherMetadata.tsx`

**Step 1: 优化分录列表样式**

调整分录列表的边框和间距，使其与元数据字段区分开：

```tsx
{data.entries && data.entries.length > 0 && (
  <div className="border-t-2 border-slate-200 mt-2">
    <div className={`px-4 py-3 bg-slate-100 border-b border-slate-200 ${compact ? 'py-2' : ''}`}>
      <h3 className="font-semibold text-slate-800 text-sm">
        凭证分录 ({data.entries.length}条)
      </h3>
    </div>
    <div className={`bg-white ${compact ? 'text-sm' : ''}`}>
      {data.entries.map((entry, index) => (
        <div
          key={entry.lineNo || index}
          className={`px-4 ${compact ? 'py-2' : 'py-3'} ${
            index < data.entries.length - 1 ? 'border-b border-slate-100' : ''
          }`}
        >
          {/* 分录头部 - 摘要 */}
          <div className="mb-2">
            <span className="inline-block px-2 py-0.5 bg-blue-100 text-blue-700 text-xs font-medium rounded mr-2">
              {entry.lineNo || index + 1}
            </span>
            <span className="font-medium text-slate-800">
              {entry.summary || '-'}
            </span>
          </div>

          {/* 分录详情 - 科目和金额 */}
          <div className="flex items-center justify-between text-sm">
            <div className="flex-1 min-w-0">
              <div className="text-slate-800 font-medium truncate">
                {entry.accountName || '-'}
              </div>
              {entry.accountCode && (
                <div className="text-xs text-slate-500 font-mono truncate">
                  {entry.accountCode}
                </div>
              )}
            </div>

            <div className="flex gap-6 font-mono text-sm">
              <div className="text-right">
                <div className="text-xs text-slate-500 mb-0.5">借方</div>
                <div className={`font-semibold ${Number(entry.debit || 0) > 0 ? 'text-green-700' : 'text-slate-400'}`}>
                  {entry.debit ? formatCurrency(entry.debit) : '-'}
                </div>
              </div>
              <div className="text-right">
                <div className="text-xs text-slate-500 mb-0.5">贷方</div>
                <div className={`font-semibold ${Number(entry.credit || 0) > 0 ? 'text-red-700' : 'text-slate-400'}`}>
                  {entry.credit ? formatCurrency(entry.credit) : '-'}
                </div>
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  </div>
)}
```

**Step 2: 在浏览器中验证样式**

Run: 刷新浏览器页面
Expected:
- 分录列表有明显的视觉分隔（顶部加粗边框）
- 分录号有蓝色标签
- 借方金额显示为绿色，贷方金额显示为红色
- 分录之间有分隔线

---

## Task 3: 验证数据源和类型

**Files:**
- Read: `src/components/voucher/VoucherPreviewTabs.tsx`

**Step 1: 确认 VoucherDTO.entries 类型定义**

查看 VoucherDTO 接口定义，确保 entries 字段类型正确：

```typescript
export interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
}

export interface VoucherDTO {
  // ... 其他字段
  entries?: VoucherEntryDTO[];
}
```

**Step 2: 确认数据传递链路**

检查 ArchiveDetailModal.tsx 中解析分录数据的逻辑：

Read: `src/pages/archives/ArchiveDetailModal.tsx` lines 167-182

确认解析逻辑正确处理了 entries 数组。

---

## Task 4: 测试完整流程

**Files:**
- Test: `src/components/voucher/__tests__/VoucherMetadata.test.tsx` (可选)

**Step 1: 准备测试数据**

在电子凭证池中选择一个有完整分录数据的记录进行测试：
- JZ-202311-0052
- BR-GROUP-2024-30Y-FIN-AC01-3001
- V-202511-TEST

**Step 2: 手动测试流程**

Run:
1. 刷新浏览器
2. 访问电子凭证池页面 (http://localhost:15175/system/pre-archive/pool)
3. 点击上述任一记录
4. 在弹窗中点击"凭证预览"标签
5. 检查左侧"业务元数据"区域

**Expected Results:**
- ✅ 上方显示基础元数据字段（凭证号、金额、日期等）
- ✅ 下方显示"凭证分录 (N条)"标题
- ✅ 每条分录正确显示：分录号、摘要、科目代码、科目名称、借方、贷方
- ✅ 借方金额为绿色，贷方金额为红色
- ✅ 分录之间有分隔线

**Step 3: 验证空数据情况**

点击一个没有分录数据的记录（如 YonSuite 格式的记录）：

**Expected Results:**
- ✅ 只显示基础元数据字段
- ✅ 不显示分录列表区域
- ✅ 不报错

---

## Task 5: 代码提交

**Files:**
- Commit: `src/components/voucher/VoucherMetadata.tsx`

**Step 1: 提交代码**

```bash
git add src/components/voucher/VoucherMetadata.tsx
git commit -m "feat(voucher): 添加分录列表到元数据区域

- 在业务元数据下方添加凭证分录列表展示
- 显示分录号、摘要、科目代码/名称、借贷方金额
- 借方绿色显示，贷方红色显示
- 支持多条分录，有数据时才显示分录区域

Related: 凭证预览功能优化"
```

**Step 2: 验证提交**

Run: `git log -1 --stat`
Expected: 显示刚才的提交信息和修改的文件

---

## 验收标准

### 功能验收
- [ ] 凭证预览标签页左侧"业务元数据"区域显示完整
- [ ] 基础字段正常显示（凭证号、金额、日期等）
- [ ] 有分录数据时，下方显示分录列表
- [ ] 分录列表包含：分录号、摘要、科目代码、科目名称、借方、贷方
- [ ] 无分录数据时，不显示分录列表区域
- [ ] 借方金额显示为绿色，贷方金额显示为红色

### 样式验收
- [ ] 分录列表与基础字段有明显的视觉分隔
- [ ] 分录号有蓝色标签标识
- [ ] 科目代码以小号灰色字体显示
- [ ] 金额使用等宽字体右对齐
- [ ] 分录之间有分隔线

### 数据验收
- [ ] 能够正确显示 YonSuite 格式数据（有 bodies 的记录）
- [ ] 能够正确显示 GENERATED 格式数据
- [ ] 无分录数据时不报错
- [ ] 金额正确格式化（¥ 符号，千分位分隔，两位小数）

---

## 后续优化（可选）

### 性能优化
- 如果分录数量很多（>10条），考虑添加虚拟滚动
- 对 VoucherMetadata 组件添加 React.memo

### 交互优化
- 点击分录项可高亮右侧表格对应行
- 分录列表支持展开/折叠

### 样式优化
- 添加 hover 效果
- 支持紧凑/正常/大尺寸模式

---

**实施日期:** 2026-01-02
**预计时间:** 30-45 分钟
**风险等级:** 低（仅修改一个组件，不影响其他功能）
