// Input: 目录结构
// Output: 目录说明文档
// Pos: src/pages/admin/LegacyImportPage/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# LegacyImportPage 组件目录

本目录包含历史数据导入页面的模块化组件。

## 目录结构

```
LegacyImportPage/
├── index.tsx              # 主组件入口（标签页切换、状态管理、API调用）
├── types.ts               # 类型定义
├── README.md              # 本目录说明文档
├── components/            # 子组件
│   ├── index.ts          # 组件统一导出
│   ├── FileUploader.tsx  # 文件上传组件（拖拽、选择、验证）
│   ├── ImportTab.tsx     # 导入标签页（合规警告、指南、模板、预览/导入结果）
│   └── HistoryTab.tsx    # 历史标签页（筛选、列表、分页）
└── hooks/                 # 自定义 Hooks
    ├── index.ts          # Hooks 统一导出
    ├── useFileUpload.ts  # 文件上传 Hook（验证逻辑）
    ├── useImportPreview.ts # 导入预览 Hook
    ├── useImportHistory.ts  # 导入历史 Hook
    └── useFieldMapping.ts   # 字段映射 Hook
```

## 组件说明

### index.tsx (主组件)
**职责**：
- 标签页切换（导入/历史）
- 全局状态管理（文件、预览结果、导入结果、历史列表）
- API 调用（预览、导入、下载模板、下载错误报告）

### components/FileUploader.tsx
**职责**：
- 文件拖拽上传
- 文件选择（input[type=file]）
- 文件大小验证（最大100MB）
- 文件格式验证（CSV、Excel）
- 显示已选文件信息
- 预览/导入按钮

### components/ImportTab.tsx
**职责**：
- 合规警告提示（P0级别）
- 导入指南说明（可展开/收起）
- 模板下载区域
- 集成 FileUploader 组件
- 预览结果展示
- 导入结果展示

**子组件**：
- `ComplianceWarning`: 合规警告
- `ImportGuide`: 导入指南
- `DetailedGuideContent`: 详细指南内容
- `TemplateDownloadSection`: 模板下载区域
- `PreviewResultSection`: 预览结果
- `ImportResultSection`: 导入结果
- `FourPropertyWarning`: 四性检测警告

### components/HistoryTab.tsx
**职责**：
- 导入历史列表展示
- 状态筛选
- 分页导航
- 下载错误报告

**子组件**：
- `HistoryFilter`: 历史筛选器
- `TaskList`: 任务列表
- `TaskRow`: 任务行
- `Pagination`: 分页组件

### hooks/useFileUpload.ts
**职责**：
- 文件验证逻辑（大小、格式）
- 拖拽事件处理
- 文件状态管理

**导出**：
- `FileUploadState`: 文件上传状态类型
- `FileValidationResult`: 文件验证结果类型
- `MAX_FILE_SIZE`: 最大文件大小常量（100MB）
- `ACCEPTED_FILE_TYPES`: 支持的文件类型数组

### hooks/useImportPreview.ts
**职责**：
- 导入预览逻辑
- 执行导入逻辑
- 预览/导入状态管理
- 统计信息获取

**导出**：
- `PreviewState`: 预览状态类型
- `handlePreview`: 预览函数
- `handleImport`: 导入函数
- `resetPreview`: 重置状态函数
- `canImport`: 是否可导入判断
- `canPreview`: 是否可预览判断
- `getPreviewStats`: 获取预览统计
- `getImportStats`: 获取导入统计

### hooks/useImportHistory.ts
**职责**：
- 导入历史查询
- 分页逻辑
- 状态筛选
- 错误报告下载
- 日期格式化

**导出**：
- `ImportHistoryState`: 历史记录状态类型
- `PaginationParams`: 分页参数类型
- `STATUS_OPTIONS`: 状态选项数组
- `STATUS_BADGE_STYLES`: 状态样式映射
- `loadHistory`: 加载历史函数
- `refreshHistory`: 刷新历史函数
- `handleDownloadErrorReport`: 下载错误报告函数
- 分页方法: `goToNextPage`, `goToPreviousPage`, `goToPage`
- 辅助方法: `getStatusBadge`, `getStatusLabel`, `formatDate`

### hooks/useFieldMapping.ts
**职责**：
- 模板下载（CSV、Excel）
- 字段说明管理
- 必需/可选字段定义

**导出**：
- `DownloadState`: 下载状态类型
- `RequiredField`: 必需字段类型
- `OptionalField`: 可选字段类型
- `TemplateType`: 模板类型
- `REQUIRED_FIELDS`: 必需字段数组
- `OPTIONAL_FIELDS`: 可选字段数组
- `downloadTemplate`: 下载模板函数
- `handleDownloadCsvTemplate`: 下载CSV模板
- `handleDownloadExcelTemplate`: 下载Excel模板

## 使用示例

```tsx
import { LegacyImportPage } from '@/pages/admin/LegacyImportPage';

// 在路由中使用
<Route path="/admin/legacy-import" element={<LegacyImportPage />} />
```

## API 类型

所有 API 类型从 `/api/legacyImport` 导入：
- `ImportPreviewResult`: 预览结果
- `ImportResult`: 导入结果
- `LegacyImportTask`: 导入任务
- `ImportError`: 导入错误

## 状态管理

主组件管理以下状态：
- `activeTab`: 当前标签页
- `file`: 选择的文件
- `previewResult`: 预览结果
- `importResult`: 导入结果
- `loading`: 加载状态
- `importing`: 导入状态
- `tasks`: 历史任务列表
- `currentPage`: 当前页码
- `total`: 总记录数
- `statusFilter`: 状态筛选
