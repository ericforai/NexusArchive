# 详细开发计划：基于元数据驱动的预归档库重构

## 1. 核心目标
将“档案门类”从简单的文本字段提升为系统级的“驾驶舱”级元数据。通过“上传即分类”的设计，彻底简化后续的归档和预览逻辑，同时通过轻量化设计确保系统高效运行。

## 2. 详细技术方案

### 2.1 数据模型变更 (Data Model)
- **表结构**：`acc_original_voucher` 将原本可选的 `archival_category` 设为 `NOT NULL`。
- **历史数据处理**：通过 SQL 脚本，将所有 `source_type = 'API_SYNC'` 的记录默认刷为 `VOUCHER`。

### 2.2 全局组件设计 (Global Component)
在 `src/components/common/ArchivalCategoryBadge.tsx` 提供一个标准的徽标组件：
- **逻辑**：读取 `ARCHIVE_CATEGORIES` 常量，根据 ID 返回对应的图标和背景色调。
- **性能**：直接使用 `lucide-react`，无需引入额外的资产文件。

### 2.3 采集模块 (Collection) 强校验逻辑
- **UI 呈现**：在 `BatchUploadView` 的门类下拉框下方，增加一个 `div` 容器。
- **交互**：
  - `onChange` 事件触发时，容器显示对应门类的合规要求（如：“注意：会计账簿归档需包含封面、账页及封底”）。
  - 如果未选择，提交按钮处于 `disabled` 状态。

### 2.4 预览引擎 (Preview Engine) 极简重构
- **解耦**：移除原有的文件扩展名嗅探逻辑。
- **映射**：
| 门类 (Category) | 来源 (Source) | 渲染组件 |
| :--- | :--- | :--- |
| VOUCHER | API | `VoucherPreviewCanvas` |
| VOUCHER | UPLOAD | `SmartFilePreview` |
| LEDGER/REPORT/OTHER | ANY | `SmartFilePreview` |

---

## 3. 分阶段执行路径

### 第一步：固化规范 (1d)
- 产出：`src/constants/archivalCategories.ts` 定义。
- 目标：确保整个系统的“门类”定义只有唯一事实来源。

### 第二步：后端联动 (1.5d)
- 产出：数据库脚本、实体类更新、DTO 更新。
- 目标：使后端具备拦截“无门类数据”的能力。

### 第三步：前端入口改造 (2d)
- 产出：更新后的 `BatchUploadView`。
- 目标：实现用户可感知的“显性强校验”，移除预归档库中冗余的上传入口。

### 第四步：展示与预览优化 (1.5d)
- 产出：全新的预归档池列表页（含带图标筛选器）与详情弹窗。
- 目标：实现“所见即所得”的分类预览。

---

## 4. 补充说明 (User Review Feedback)

### 4.1 预览池筛选器实现
在 `PoolPage.tsx` 中引入 `ARCHIVE_CATEGORIES` 常量，实现如下筛选器：
```typescript
const CATEGORY_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '会计凭证', value: 'VOUCHER', icon: Receipt },
  { label: '会计账簿', value: 'LEDGER', icon: BookOpen },
  { label: '财务报告', value: 'REPORT', icon: FileText },
  { label: '其他资料', value: 'OTHER', icon: Folder },
];
```

### 4.2 清理陈旧出口
- **ArchiveListView**: 全面移除顶部及列表内的 `Upload` 相关的 `Floating Action Button` 或 `Toolbar Button`。
- **Empty State**: 将 `renderEmpty` 的操作按钮从“点击上传”改为：
  ```tsx
  <Link to="/system/collection/upload">
    <Button type="primary">前往资料收集模块导入</Button>
  </Link>
  ```

---

## 4. 关键风险与应对策略

| 风险点 | 应对措施 |
| :--- | :--- |
| **性能损失** | 严禁在分类列表页使用大图 Icon，统一使用 SVG 精灵或 Lucide 图标库。 |
| **误分类风险** | 在确认上传前增加二次确认弹窗：“您选择的门类是 [财务报告]，请确认”。 |
| **私有化环境数据库偏移** | 迁移脚本使用 `ALTER TABLE ... ALTER COLUMN ... SET NOT NULL` 前必须先清理存量 null 值。 |

---

## 5. 验收标准
1. **合规性**：所有预归档数据必须带有合规门类标签。
2. **易用性**：用户在上传时通过图标和提示能清晰知道自己该选哪一类。
3. **技术健壮性**：查看按钮的响应速度相比旧版本（嗅探模式）提升 30% 以上。
