# 凭证预览组件设计文档

**日期**: 2026-01-01
**设计师**: Claude + User
**状态**: 已批准，待实施

---

## 概述

设计一个标准化的**凭证预览组件**，用于在全景视图、会计档案、预归档库-电子凭证池、凭证关联等多个场景复用。

**核心需求**：
- 左侧展示 ERP 同步的原始元数据（字段列表）
- 右侧展示格式化的会计凭证样式（JSON → CSS 表格渲染）
- 支持会计凭证和原始凭证（PDF附件）两种预览模式
- 性能优先，支持单凭证预览和列表批量展示

---

## 组件架构

### 组件层次结构

```
VoucherPreview (组合容器，可选)
├── VoucherMetadata (左侧：元数据展示)
└── VoucherPreviewTabs (右侧：标签容器)
    ├── 会计凭证 → VoucherPreviewCanvas (格式化渲染)
    └── 关联附件 → OriginalDocumentPreview (PDF预览)
```

### 核心组件

| 组件 | 功能 | 可独立使用 |
|------|------|------------|
| `VoucherMetadata` | 左侧元数据字段列表 | ✓ |
| `VoucherPreviewCanvas` | 会计凭证CSS表格渲染 | ✓ |
| `OriginalDocumentPreview` | 原始凭证PDF预览 | ✓ |
| `VoucherPreviewTabs` | 右侧标签切换容器 | ✓ |
| `VoucherPreview` | 左右分栏完整预览 | - |

### 架构选择理由

采用**组合式组件**（方案B）而非单一组件：

1. **关注点分离**：左侧专注数据展示，右侧专注格式化渲染
2. **性能优化**：右侧可用 `React.memo` 优化，左侧变化不影响右侧
3. **复用性强**：可单独使用元数据列表，或单独使用渲染引擎
4. **可独立演进**：右侧渲染引擎未来可用于后端PDF生成、批量导出

---

## 数据结构

### 输入数据：VoucherDTO

```typescript
interface VoucherDTO {
  // 基本信息
  voucherId: string;
  voucherNo: string;
  voucherWord: string;
  voucherDate: string;
  accountPeriod: string;
  accbookCode: string;

  // 金额与状态
  summary: string;
  debitTotal: number;
  creditTotal: number;
  attachmentCount: number;
  status: string;

  // 人员
  creator: string;
  auditor: string;
  poster: string;

  // 分录列表
  entries: VoucherEntryDTO[];

  // 附件列表
  attachments?: AttachmentDTO[];
}

interface VoucherEntryDTO {
  lineNo: number;
  summary: string;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
}
```

---

## 组件详细设计

### 1. VoucherPreviewCanvas（会计凭证渲染）

**功能**：将 JSON 数据渲染成标准凭证样式

**布局结构**：
```
┌─────────────────────────────────────────────────┐
│ 表头区                                           │
│   核算单位: [xxx]    日期: [xxx]    凭证号: [xxx] │
├─────────────────────────────────────────────────┤
│ 分录表格区                                       │
│   摘要    | 科目           | 借方    | 贷方    │
│   ------- | -------------- | ------- | ------- │
│   xxx     | xxx / xxx      | xxx     | xxx     │
│   xxx     | xxx / xxx      | xxx     | xxx     │
├─────────────────────────────────────────────────┤
│ 合计区                                           │
│   合计                      | xxx     | xxx     │
│   大写: xxxxxxxxxxx                         │
├─────────────────────────────────────────────────┤
│ 签章区                                           │
│   制单人: xxx  审核人: xxx  记账人: xxx          │
└─────────────────────────────────────────────────┘
```

**样式原则**：
- 简化设计，不追求与截图完全一致
- 简单边框 `1px solid #e5e7eb`
- 等宽字体用于金额
- 黑白灰为主色调，蓝色强调选中状态
- 紧凑间距适配全景视图

**数据映射**：
| VoucherDTO 字段 | 表格位置 |
|-----------------|----------|
| `voucherWord` + `voucherNo` | 表头-凭证号 |
| `voucherDate` | 表头-日期 |
| `orgName` | 表头-核算单位 |
| `entries[]` | 分录表格区 |
| `debitTotal`, `creditTotal` | 合计区 |
| `creator`, `auditor`, `poster` | 签章区 |

### 2. VoucherMetadata（元数据展示）

**功能**：以字段列表形式展示 ERP 原始数据

**布局**：
```
┌─────────────────────────────────┐
│ 业务元数据                       │
├─────────────────────────────────┤
│ 记账凭证号  YS-20251231-xxx     │
│ 金额        ¥ 150.00            │
│ 业务日期    2025-08-01           │
│ 关联发票数  3                   │
│ 关联合同    CT-2025-001         │
│ 匹配度      95%                 │
│ 关联方式    智能算法             │
│ 入池时间    2025-08-01           │
│ 存储ID      abc123def           │
└─────────────────────────────────┘
```

**字段映射**：
| 显示标签 | VoucherDTO 字段 | 格式化 |
|----------|-----------------|--------|
| 记账凭证号 | `voucherWord` + `voucherNo` | `{word}-{no}` |
| 金额 | `debitTotal` / `creditTotal` | `¥ {amount.toLocaleString()}` |
| 业务日期 | `voucherDate` | `YYYY-MM-DD` |
| 关联发票数 | `attachments` 过滤 | 数字 |
| 关联合同 | 关联查询 | 链接或 `-` |
| 匹配度 | 外部注入 | 百分比 |
| 关联方式 | 外部注入 | 文本 |
| 入池时间 | `createdTime` | `YYYY-MM-DD` |
| 存储ID | `id` | 短格式 |

### 3. VoucherPreviewTabs（标签容器）

**功能**：标签页切换，支持会计凭证和原始凭证预览

**Tabs**：
- `会计凭证`（默认选中） → `VoucherPreviewCanvas`
- `关联附件` → `OriginalDocumentPreview`

### 4. OriginalDocumentPreview（PDF预览）

**功能**：原始凭证 PDF 文件预览

**特性**：
- 支持多文件切换
- 懒加载（切换到标签时才加载）
- 使用 `react-pdf` 或 iframe

---

## 组件接口

```typescript
// 左侧元数据
interface VoucherMetadataProps {
  data: VoucherDTO;
  compact?: boolean;        // 紧凑模式
  fields?: string[];        // 自定义显示字段
}

// 右侧标签容器
interface VoucherPreviewTabsProps {
  voucherData: VoucherDTO;
  attachments?: AttachmentDTO[];
  defaultTab?: 'voucher' | 'attachments';
}

// 会计凭证渲染
interface VoucherPreviewCanvasProps {
  data: VoucherDTO;
  compact?: boolean;        // 紧凑间距
  showSignature?: boolean;  // 显示签章区
}

// 原始凭证预览
interface OriginalDocumentPreviewProps {
  files: AttachmentDTO[];
  defaultFileIndex?: number;
}

// 组合容器
interface VoucherPreviewProps {
  data: VoucherDTO;
  attachments?: AttachmentDTO[];
  layout?: 'horizontal' | 'vertical';
  size?: 'compact' | 'normal' | 'large';
}
```

---

## 性能优化策略

### 全景视图"往下翻"场景

1. **组件级 memo**（必需）
   ```typescript
   const VoucherPreviewCanvas = React.memo(({ data }) => {
     // 只在 data.voucherId 变化时重新渲染
   }, (prev, next) => prev.data.voucherId === next.data.voucherId);
   ```

2. **PDF 懒加载**（必需）
   - 只在切换到"关联附件"标签时加载
   - 使用 Intersection Observer

3. **数据缓存**（必需）
   - 已加载凭证数据缓存到状态或 Context
   - 避免重复请求

### 列表批量展示（可选）

4. **虚拟滚动**
   - 使用 `react-window`
   - 只渲染可见区域
   - 适用场景：50+ 张凭证列表

5. **缩略图模式**
   - 初始只显示元数据 + 迷你凭证
   - 点击展开完整预览

6. **按需渲染**
   - 初始渲染前 10 张
   - 滚动到底部加载更多

---

## 文件组织

```
src/
├── components/
│   └── voucher/
│       ├── VoucherMetadata.tsx
│       ├── VoucherPreviewTabs.tsx
│       ├── VoucherPreviewCanvas.tsx
│       ├── OriginalDocumentPreview.tsx
│       ├── VoucherPreview.tsx
│       ├── index.ts
│       └── styles.ts               # 共享样式
```

---

## 实施计划

### Phase 1: 核心渲染
- [ ] `VoucherPreviewCanvas` 基础实现
- [ ] 表头区渲染
- [ ] 分录表格渲染
- [ ] 合计区渲染
- [ ] 签章区渲染

### Phase 2: 元数据展示
- [ ] `VoucherMetadata` 基础实现
- [ ] 字段列表渲染
- [ ] 格式化函数

### Phase 3: 标签切换
- [ ] `VoucherPreviewTabs` 实现
- [ ] `OriginalDocumentPreview` 实现
- [ ] 标签切换逻辑

### Phase 4: 组合容器
- [ ] `VoucherPreview` 容器实现
- [ ] 左右分栏布局
- [ ] 响应式适配

### Phase 5: 性能优化
- [x] React.memo 优化
- [ ] PDF 懒加载
- [ ] 数据缓存机制
- [ ] （可选）虚拟滚动

---

## 使用场景

| 场景 | 使用组件 | 模式 |
|------|----------|------|
| 全景视图 | `VoucherPreview` | horizontal, normal |
| 会计档案详情 | `VoucherPreview` | horizontal, large |
| 电子凭证池列表 | `VoucherMetadata` | compact |
| 凭证关联 | `VoucherPreviewTabs` | 默认 |

---

## 技术栈

- **React**：UI 框架
- **TypeScript**：类型安全
- **Tailwind CSS**：样式
- **react-pdf**：PDF 预览（可选）
- **React.memo**：性能优化

---

## 设计决策记录

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 组件架构 | 组合式组件 | 关注点分离、性能优化、复用性强 |
| 渲染方式 | 纯CSS表格 | 性能优先、开发成本低 |
| 布局方向 | 水平分栏 | 符合用户习惯、左右对照清晰 |
| 样式复杂度 | 简化设计 | 性能优先、不需要完全复刻截图 |

---

## 实施状态

- [x] Phase 1: VoucherPreviewCanvas 核心渲染
- [x] Phase 2: VoucherMetadata 元数据展示
- [x] Phase 3: VoucherPreviewTabs 标签切换
- [x] Phase 4: VoucherPreview 组合容器
- [x] Phase 5: 性能优化

**实施日期**: 2026-01-01
**状态**: ✅ 已完成

---

## 待确认问题

1. PDF 预览库选择：`react-pdf` vs `pdf.js` vs iframe？
2. 虚拟滚动是否必需（取决于全景视图实际数据量）？
3. 缓存策略：组件状态 vs React Query vs Context？

---

**下一步**：用户确认后进入实施阶段
