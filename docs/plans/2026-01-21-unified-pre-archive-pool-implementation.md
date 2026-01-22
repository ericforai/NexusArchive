# 实施计划：轻量化分类与强校验预归档库重构

## 方案目标
根据头脑风暴结论，将“档案门类”提升为全局元模型，实现“上传即分类、分类即固化、固化即简易预览”的闭环，同时确保系统轻量化与操作的显性强校验。

## Proposed Changes

---

### 1. 全局定义层

#### [NEW] [archiveCategories.ts](file:///Users/user/nexusarchive/src/constants/archiveCategories.ts)
定义统一的门类枚举、标签、校验提示及轻量化图标（使用现有的 `lucide-react`）。

```typescript
import { Ticket, Book, BarChart3, Files } from 'lucide-react';

export const ARCHIVE_CATEGORIES = {
  VOUCHER: {
    label: '会计凭证',
    icon: Ticket,
    description: '记账凭证、原始凭证（发票、收据、银行回单等）',
    validationRule: '建议上传 PDF/OFD 格式，API 同步数据自动关联。'
  },
  LEDGER: {
    label: '会计账簿',
    icon: Book,
    description: '总账、明细账、日记账、固定资产卡片等',
    validationRule: '必须上传 PDF 格式，需包含年度/期间信息。'
  },
  // ... REPORT, OTHER
};
```

---

### 2. 资料收集模块 (Collection)

#### [MODIFY] [BatchUploadView.tsx](file:///Users/user/nexusarchive/src/pages/collection/BatchUploadView.tsx)
- **强校验 UI**：在门类选择器下方增加显眼的 `Alert` 组件，根据选择实时切换描述和校验规则。
- **视觉强化**：门类选择器增加 `status="error"` 标记（若未选），并在选项中展示对应图标。

---

### 3. 预归档池模块 (Pre-Archive)

#### [MODIFY] [PoolPage.tsx](file:///Users/user/nexusarchive/src/pages/pre-archive/PoolPage.tsx)
- **分类视图**：列表项开头展示门类图标（极简 CSS 着色，不增加切图体积）。
- **极简预览按钮**：直接读取 `item.archivalCategory`。逻辑改为：
  ```javascript
  const isVoucherApi = category === 'VOUCHER' && source === 'api_sync';
  return isVoucherApi ? <VoucherCanvas /> : <FilePreview />;
  ```

---

### 4. 数据持久化 (Backend)

#### [MODIFY] [OriginalVoucher.java](file:///Users/user/nexusarchive/backend/src/main/java/com/nexus/archive/entity/OriginalVoucher.java)
增加 `@NotNull` 校验，确保 `archival_category` 为必填项。

#### [NEW] [V105__force_archive_category.sql](file:///Users/user/nexusarchive/backend/src/main/resources/db/migration/V105__force_archive_category.sql)
数据库字段非空约束及历史数据刷库。

---

## Verification Plan

### Automated Tests
1. **接口幂等性测试**：运行 `curl` 模拟上传缺失 `archivalCategory` 的请求，确认返回 400 错误。
2. **预览逻辑单元测试**：编写测试用例验证 `DecidePreviewMode` 对于不同输入产生正确组件映射。

### Manual Verification
1. **强校验体验**：在上传页面尝试不选门类直接提交，确认系统拦截并高显提示。
2. **视觉验证**：在预归档池查看列表，确认 4 类档案图标显示正确且无布局偏移。
3. **预览验证**：分别点击一条 API 同步凭证和一条上传的 PDF 账簿，确认渲染方式符合预期。
