# 批量上传功能

## 功能概述

批量上传功能允许用户通过 Web 界面上传多个会计档案文件，系统会自动进行：
- 文件哈希校验 (幂等性控制)
- 四性检测 (真实性、完整性、可用性、安全性)
- 预归档处理
- 审计日志记录

## 功能特性

- **拖拽上传**: 支持拖拽多个文件到上传区域
- **并发上传**: 自动管理上传队列，支持暂停/继续
- **进度跟踪**: 实时显示上传进度和统计信息
- **错误处理**: 自动重试失败文件，显示详细错误信息
- **幂等性**: 通过文件哈希避免重复上传

## 用户操作流程

### 1. 创建批次

1. 导航到"资料收集" > "批量上传"
2. 填写批次信息：
   - **批次名称**: 描述性的批次名称（如：2024年1月凭证）
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

### 4. 四性检测

- 点击"执行四性检测"按钮
- 系统对所有已上传文件进行检测
- 查看检测摘要和详细报告

**四性检测内容**:
- **真实性**: 文件来源可追溯，未被篡改
- **完整性**: 文件内容完整，无缺失
- **可用性**: 文件可正常读取和渲染
- **安全性**: 无病毒或恶意代码

## API 文档

### 创建批次

**请求**:
```http
POST /api/collection/batch/create
Content-Type: application/json
Authorization: Bearer {token}

{
  "batchName": "2024年1月凭证",
  "fondsCode": "001",
  "fiscalYear": "2024",
  "fiscalPeriod": "01",
  "archivalCategory": "VOUCHER",
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
    "originalFilename": "voucher.pdf",
    "status": "UPLOADED"
  }
}
```

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

### 四性检测

**请求**:
```http
POST /api/collection/batch/{batchId}/check
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "batchId": 1,
    "totalFiles": 10,
    "checkedFiles": 10,
    "passedFiles": 9,
    "failedFiles": 1,
    "summary": "检测完成: 共 10 个文件，通过 9 个，失败 1 个"
  }
}
```

## 批次状态

| 状态 | 说明 |
|------|------|
| `UPLOADING` | 上传中，可以继续上传文件 |
| `UPLOADED` | 上传完成，等待执行四性检测 |
| `VALIDATING` | 正在执行四性检测 |
| `VALIDATED` | 四性检测完成 |
| `FAILED` | 上传或检测失败 |
| `ARCHIVED` | 已归档 |

## 文件上传状态

| 状态 | 说明 |
|------|------|
| `PENDING` | 等待上传 |
| `UPLOADING` | 上传中 |
| `UPLOADED` | 上传成功 |
| `FAILED` | 上传失败 |
| `DUPLICATE` | 重复文件 |
| `VALIDATING` | 校验中 |
| `VALIDATED` | 校验完成 |
| `CHECK_FAILED` | 四性检测失败 |

## 合规性说明

本功能符合以下国家标准:
- **GB/T 39362-2020**: 电子会计档案管理系统建设要求
- **GB/T 39784-2021**: 电子档案单套制管理规范
- **DA/T 70-2018**: 电子档案管理基本术语

## 权限要求

- **创建批次**: `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`
- **上传文件**: `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`
- **查看批次**: `archive:view` 或 `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`
- **四性检测**: `archive:manage` 或 `nav:all` 或 `SYSTEM_ADMIN`

## 技术架构

### 前端
- React 19 + TypeScript
- Ant Design Upload 组件
- TanStack Query (React Query) 状态管理
- 文件上传队列管理

### 后端
- Spring Boot 3.1.6
- MultipartFile 处理文件上传
- 事务管理确保数据一致性
- SHA-256 哈希计算

### 数据库
- `collection_batch`: 批次主表
- `collection_batch_file`: 批次文件关联表
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

## 相关文档

- [预归档库](./pre-archive.md)
- [四性检测](./four-nature-check.md)
- [全宗管理](./fonds.md)
