# 批量上传功能

## 功能定位

> **重要变更**：根据 DA/T 94-2022《电子会计档案管理规范》要求，批量上传功能已重新定位为**原始凭证附件上传器**。

- ✅ **用于**：上传原始凭证附件（发票扫描件、合同、银行回单等）
- ❌ **不用于**：直接上传记账凭证（应通过 ERP 同步）
- ⚠️ **注意**：上传后需在凭证关联页面匹配到记账凭证后方可正式归档

## 功能概述

批量上传功能允许用户通过 Web 界面上传多个原始凭证附件文件，系统会自动进行：
- 文件哈希校验 (幂等性控制)
- **立即创建档案记录** (符合 DA/T 94-2022 元数据同步捕获要求)
- 四性检测 (真实性、完整性、可用性、安全性)
- 预归档处理
- 审计日志记录

## 合规性设计

### DA/T 94-2022 第14条要求

> 在电子会计资料归档和电子会计档案管理过程中应同时捕获、归档和管理元数据。

### 系统实现

1. **上传后立即创建档案记录**
   - 文件上传成功后，系统立即在 `acc_archive` 表中创建档案记录
   - 状态设置为 `PENDING_METADATA` (待补录)
   - 分类代码固定为 `AC04` (其他会计资料/原始凭证附件)

2. **智能解析元数据**
   - 系统自动尝试 OCR 提取发票信息
   - 用户需确认或补录元数据

3. **凭证关联**
   - 上传完成后引导用户到凭证关联页面
   - 将原始凭证附件匹配到对应的记账凭证

4. **兜底机制**
   - 未关联的文件保持 `PENDING_METADATA` 状态
   - 可通过手动归档弹窗补录元数据后归档

## 功能特性

- **拖拽上传**: 支持拖拽多个文件到上传区域
- **并发上传**: 自动管理上传队列，支持暂停/继续
- **进度跟踪**: 实时显示上传进度和统计信息
- **错误处理**: 自动重试失败文件，显示详细错误信息
- **幂等性**: 通过文件哈希避免重复上传
- **合规提示**: 页面显示 DA/T 94-2022 合规要求提示

## 用户操作流程

### 1. 创建批次

1. 导航到"资料收集" > "批量上传"
2. 填写批次信息：
   - **批次名称**: 描述性的批次名称（如：2024年1月发票）
   - **全宗代码**: 选择所属全宗
   - **会计年度**: 选择档案所属年度
   - **会计期间**: (可选) 选择月份或期间
   - **档案门类**: 选择凭证/账簿/报告/其他
3. 拖拽或选择文件
4. 点击"开始上传"

### 2. 上传文件

**支持格式**:
- PDF (.pdf)
- OFD (.ofd)
- XML (.xml)
- JPG (.jpg, .jpeg)
- PNG (.png)
- TIFF (.tif, .tiff)

**限制**:
- 单文件大小: 最大 100MB
- 批次文件数: 无限制

**上传控制**:
- 暂停/继续上传
- 查看每个文件的上传进度
- 移除或重试失败的文件

### 3. 完成上传

- 点击"完成上传"按钮
- 系统统计上传结果
  - 总文件数
  - 成功上传数
  - 失败数
  - 重复文件数
- **已自动创建档案记录** (状态：待补录)
- 点击"前往凭证关联"按钮

### 4. 凭证关联

1. 在凭证关联页面选择已上传的文件
2. 匹配到对应的记账凭证
3. 确认关联关系
4. 提交归档

### 5. 手动归档 (兜底)

对于无法自动关联的文件：
1. 在档案管理页面找到待补录的文件
2. 点击"手动归档"
3. 补录必要的元数据（题名、凭证号、日期等）
4. 提交归档

## API 文档

### 创建批次

**请求**:
```http
POST /api/collection/batch/create
Content-Type: application/json
Authorization: Bearer {token}

{
  "batchName": "2024年1月发票",
  "fondsCode": "001",
  "fiscalYear": "2024",
  "fiscalPeriod": "01",
  "archivalCategory": "OTHER",
  "totalFiles": 10,
  "autoCheck": true
}
```

**响应**:
```json
{
  "code": 200,
  "message": "批次创建成功",
  "data": {
    "batchId": 1,
    "batchNo": "COL-20240105-001",
    "status": "UPLOADING",
    "uploadToken": "abc123...",
    "totalFiles": 10,
    "uploadedFiles": 0,
    "failedFiles": 0,
    "progress": 0
  }
}
```

### 上传文件

**请求**:
```http
POST /api/collection/batch/{batchId}/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: <binary>
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "fileId": "uuid-123",
    "originalFilename": "invoice.pdf",
    "status": "UPLOADED",
    "archiveId": "archive-uuid-456"
  }
}
```

**说明**: 文件上传成功后会立即创建 `acc_archive` 档案记录，`archiveId` 为新创建的档案ID。

### 完成批次

**请求**:
```http
POST /api/collection/batch/{batchId}/complete
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "message": "批次已完成",
  "data": {
    "batchId": 1,
    "batchNo": "COL-20240105-001",
    "status": "UPLOADED",
    "totalFiles": 10,
    "uploadedFiles": 10,
    "failedFiles": 0
  }
}
```

## 批次状态

| 状态 | 说明 |
|------|------|
| `UPLOADING` | 上传中，可以继续上传文件 |
| `UPLOADED` | 上传完成，等待执行四性检测或凭证关联 |
| `VALIDATING` | 正在执行四性检测 |
| `VALIDATED` | 四性检测完成 |
| `FAILED` | 上传或检测失败 |
| `ARCHIVED` | 已归档 |

## 档案状态

| 状态 | 说明 |
|------|------|
| `PENDING_METADATA` | 待补录 - 文件已上传，需补录或关联凭证 |
| `MATCH_PENDING` | 待匹配 - 等待智能匹配原始凭证 |
| `MATCHED` | 匹配成功 - 已关联到记账凭证 |
| `PENDING_ARCHIVE` | 准备归档 - 等待提交归档 |
| `ARCHIVED` | 已归档 - 正式归档完成 |

## 合规性说明

本功能符合以下国家标准:
- **DA/T 94-2022**: 电子会计档案管理规范 (第14条 - 元数据同步捕获)
- **GB/T 39362-2020**: 电子会计档案管理系统建设要求
- **GB/T 39784-2021**: 电子档案单套制管理规范
- **DA/T 70-2018**: 电子档案管理基本术语

## 权限要求

- **创建批次**: `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`
- **上传文件**: `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`
- **查看批次**: `archive:view` 或 `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`
- **四性检测**: `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`
- **手动归档**: `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`

## 技术架构

### 前端
- React 19 + TypeScript
- Ant Design Upload 组件
- TanStack Query (React Query) 状态管理
- 文件上传队列管理
- ComplianceAlert 合规提示组件

### 后端
- Spring Boot 3.1.6
- MultipartFile 处理文件上传
- BatchToArchiveService 档案记录创建服务
- 事务管理确保数据一致性
- SHA-256 哈希计算

### 数据库
- `collection_batch`: 批次主表
- `collection_batch_file`: 批次文件关联表 (含 `archive_id` 字段)
- `acc_archive`: 档案主表 (上传后立即创建记录)
- `arc_file_content`: 文件内容表
- 级联删除保证数据完整性

## 故障处理

### 上传失败
- 检查文件大小 (不超过 100MB)
- 检查文件格式 (仅支持指定格式)
- 检查网络连接
- 查看错误消息了解详情

### 检测失败
- 确认文件格式正确
- 检查文件是否损坏
- 查看四性检测详细报告

### 批次状态异常
- 刷新页面重新加载状态
- 检查后台日志
- 联系管理员

### 档案记录未创建
- 检查 `acc_archive` 表是否有记录
- 检查 `collection_batch_file.archive_id` 是否关联
- 查看后端日志中的 BatchToArchiveService 输出

## 相关文档

- [预归档库](./pre-archive.md)
- [四性检测](./four-nature-check.md)
- [全宗管理](./fonds.md)
- [凭证关联](./voucher-linking.md)
- [合规性实施计划](../plans/2025-01-05-batch-upload-compliance-fix.md)
