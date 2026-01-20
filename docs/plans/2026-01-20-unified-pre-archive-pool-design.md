# 统一预归档池设计方案

**日期**: 2026-01-20
**问题**: 预归档库支持多种档案门类的查看与筛选
**状态**: 待实施

---

## 一、问题背景

### 1.1 当前问题

- 预归档库同时存在"电子凭证池"和"单据池"，概念混乱
- 上传功能散落在预归档库页面，违反架构原则（数据应从资料收集模块进入）
- 不同类型档案的预览方式未区分：
  - API 同步的记账凭证 → 需要凭证格式渲染
  - 上传的 PDF 文件 → 需要 PDF 直接预览

### 1.2 档案门类（财政部79号令）

| 门类代码 | 名称 | 内容 |
|---------|------|------|
| VOUCHER | 会计凭证 | 记账凭证、原始凭证（发票、收据、银行回单等） |
| LEDGER | 会计账簿 | 总账、明细账、日记账、固定资产卡片 |
| REPORT | 财务会计报告 | 月报、季报、半年报、年报 |
| OTHER | 其他会计资料 | 银行对账单、纳税申报表等 |

---

## 二、架构设计

### 2.1 数据流向

```
┌─────────────────────────────────────────────────────────────┐
│              资料收集模块 (Collection)                        │
├─────────────────────────────────────────────────────────────┤
│  入口方式：                                                  │
│  1. API 同步（YonSuite → 记账凭证 JSON）                     │
│  2. 批量上传（PDF/OFD → 报表/账簿/原始凭证）                  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              预归档库 (Pre-Archive)                          │
├─────────────────────────────────────────────────────────────┤
│  职责：暂存、分类、匹配、整理（无上传功能）                  │
│  - 查看所有待归档资料                                        │
│  - 按档案门类筛选                                            │
│  - 根据数据来源选择预览方式                                  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              正式归档库 (Archive)                            │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 数据来源与预览方式

| 数据来源 | 档案门类 | 预览方式 | 说明 |
|---------|---------|---------|------|
| api_sync | VOUCHER | VoucherPreviewCanvas | 渲染凭证格式（分录表格） |
| upload | VOUCHER/LEDGER/REPORT/OTHER | SmartFilePreview | PDF/图片直接预览 |

---

## 三、数据模型变更

### 3.1 前端类型

```typescript
// src/types/archive.ts

/** 档案门类 */
export type ArchivalCategory =
  | 'VOUCHER'        // 会计凭证
  | 'LEDGER'         // 会计账簿
  | 'REPORT'         // 财务会计报告
  | 'OTHER';         // 其他会计资料

/** 数据来源 */
export type SourceType =
  | 'api_sync'       // ERP API 同步
  | 'upload';        // 批量上传

/** 预归档项 */
export interface PreArchiveItem {
  id: string;
  archivalCategory: ArchivalCategory;  // 档案门类
  sourceType: SourceType;              // 数据来源
  sourceData?: string;    // API 同步的 JSON
  fileId?: string;        // 文件 ID
  // ... 其他字段
}
```

### 3.2 后端实体

```java
// OriginalVoucher.java

public class OriginalVoucher {
    private String id;

    @Column(name = "archival_category")
    private String archivalCategory;  // VOUCHER/LEDGER/REPORT/OTHER

    @Column(name = "source_type")
    private String sourceType;        // API_SYNC/UPLOAD

    private String sourceData;        // JSON 数据（api_sync）
    private String fileId;            // 文件关联（upload）
    // ...
}
```

### 3.3 数据库迁移

```sql
-- V1xx__add_archival_category_fields.sql

ALTER TABLE acc_original_voucher
ADD COLUMN archival_category VARCHAR(20) DEFAULT 'VOUCHER',
ADD COLUMN source_type VARCHAR(20) DEFAULT 'UPLOAD';

-- 为现有数据设置默认值
UPDATE acc_original_voucher
SET source_type = CASE
    WHEN source_data IS NOT NULL AND source_data != '' THEN 'API_SYNC'
    ELSE 'UPLOAD'
END;
```

---

## 四、功能设计

### 4.1 批量上传：档案门类选择

**文件**: `src/pages/collection/BatchUploadView.tsx`

```tsx
interface BatchFormData {
  batchName: string;
  fondsCode: string;
  fiscalYear: string;
  fiscalPeriod?: string;
  archivalCategory: ArchivalCategory;  // 已有，保持
}

// 档案门类选择
<Select
  name="archivalCategory"
  options={[
    { label: '会计凭证', value: 'VOUCHER' },
    { label: '会计账簿', value: 'LEDGER' },
    { label: '财务报告', value: 'REPORT' },
    { label: '其他资料', value: 'OTHER' },
  ]}
/>

// 根据选择显示提示
{archivalCategory === 'VOUCHER' && (
  <Alert message="会计凭证包括：记账凭证、原始凭证（发票、收据、银行回单等）" />
)}
```

### 4.2 预归档池：档案门类筛选器

**文件**: `src/pages/pre-archive/PoolPage.tsx`

```tsx
const [categoryFilter, setCategoryFilter] = useState<ArchivalCategory | 'ALL'>('ALL');

const CATEGORY_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '会计凭证', value: 'VOUCHER' },
  { label: '会计账簿', value: 'LEDGER' },
  { label: '财务报告', value: 'REPORT' },
  { label: '其他资料', value: 'OTHER' },
];

// 筛选器 UI
<div className="flex items-center gap-2">
  <span className="text-sm text-slate-500">档案门类:</span>
  <Select value={categoryFilter} onChange={setCategoryFilter} options={CATEGORY_OPTIONS} />
</div>
```

**API**: `src/api/originalVoucher.ts`

```typescript
export interface QueryPreArchiveParams {
  status?: string;
  archivalCategory?: ArchivalCategory | 'ALL';
  page?: number;
  pageSize?: number;
}
```

### 4.3 查看按钮：条件预览逻辑

**文件**: `src/pages/archives/ArchiveDetailDrawer.tsx`

```typescript
interface PreviewDecision {
  component: 'VoucherPreview' | 'FilePreview';
}

function decidePreviewMode(item: PreArchiveItem): PreviewDecision {
  if (item.sourceType === 'api_sync') {
    return { component: 'VoucherPreview' };
  }
  return { component: 'FilePreview' };
}

// 渲染
{(() => {
  const mode = decidePreviewMode(row);
  switch (mode.component) {
    case 'VoucherPreview':
      return <VoucherPreviewCanvas sourceData={row.sourceData} />;
    case 'FilePreview':
      return <SmartFilePreview archiveId={row.id} fileId={row.fileId} />;
  }
})()}
```

### 4.4 移除预归档库的上传功能

**移除位置**:
- `src/pages/archives/ArchiveListView.tsx` 的上传按钮（多处）
- `renderEmptyState` 中的上传按钮

**替代方案**:
```tsx
// 空状态引导
<Link to="/system/collection/upload">
  <button>前往上传</button>
</Link>

// 或工具栏增加"导入"按钮
<Button onClick={() => navigate('/system/collection')}>
  <Download size={16} /> 导入资料
</Button>
```

---

## 五、实施清单

### 5.1 后端

- [ ] 数据库迁移：添加 `archival_category` 和 `source_type` 字段
- [ ] `OriginalVoucher` 实体添加新字段
- [ ] `BatchUploadService.completeBatch()` 传递门类信息
- [ ] `OriginalVoucherController` 移除直接创建接口（或限制权限）

### 5.2 前端

- [ ] `src/types/archive.ts`: 添加 `ArchivalCategory` 和 `SourceType` 类型
- [ ] `src/api/originalVoucher.ts`: 添加 `archivalCategory` 筛选参数
- [ ] `src/pages/pre-archive/PoolPage.tsx`: 添加档案门类筛选器
- [ ] `src/pages/archives/ArchiveListView.tsx`: 移除上传按钮，添加"导入"按钮
- [ ] `src/pages/archives/ArchiveDetailDrawer.tsx`: 实现条件预览逻辑
- [ ] `src/pages/collection/BatchUploadView.tsx`: 完善门类提示信息

### 5.3 测试

- [ ] 批量上传 → 选择档案门类 → 数据正确传递到预归档库
- [ ] 预归档库筛选器 → 按门类过滤正常
- [ ] API 同步凭证 → 查看显示凭证格式
- [ ] 上传 PDF 文件 → 查看显示 PDF 预览
- [ ] 预归档库无上传入口 → 只能从资料收集进入

---

## 六、UI 示意

```
┌────────────────────────────────────────────────────────────────────┐
│  电子凭证池                                           [列表] [看板] │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 📊 状态仪表板                                                 │   │
│  │   [可归档: 12] [待匹配: 5] [待校验: 3]                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  档案门类: [全部 ▼]  搜索: [________]  🔍  [导入资料]              │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ ☐ │ 凭证号  │ 来源   │ 门类   │ 日期      │ 状态    │ 操作 │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ ☐ │ 记-125  │ YonSuite │ 凭证  │ 2025-01-15 │ 可归档  │ 查看  │ ← 凭证格式
│  │ ☐ │ —       │ 上传   │ 报告  │ 2025-01-10 │ 可归档  │ 预览  │ ← PDF 预览
│  │ ☐ │ —       │ 上传   │ 账簿  │ 2025-01-08 │ 待匹配  │ 预览  │ ← PDF 预览
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

## 七、备注

- 档案门类字段 `archivalCategory` 已存在于批量上传 API，只需复用到预归档库
- 预归档库与资料收集模块的职责分离：前者管"整理"，后者管"收集"
- 查看/预览按钮文案可根据 `sourceType` 动态显示
