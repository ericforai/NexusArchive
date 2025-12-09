# OFD 版式文件功能

## 概述

NexusArchive 支持将 PDF 格式的电子会计档案转换为 OFD (Open Fixed-layout Document) 格式，符合国家电子文件长期保存要求。

> OFD 是中国自主可控的版式文档格式标准 (GB/T 33190-2016)，广泛应用于电子发票、电子证照等领域。

---

## 功能特性

| 功能 | 说明 |
|:---|:---|
| PDF 转 OFD | 单文件或批量转换 |
| OFD 预览 | 前端内置 OFD 阅读器 |
| 转换日志 | 记录转换状态和结果 |

---

## API 接口

### 单文件转换

```http
POST /api/archive/{id}/convert-to-ofd
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "ofdFileId": "ofd-xxx-xxx",
    "status": "SUCCESS"
  }
}
```

### 批量转换

```http
POST /api/archive/batch-convert-to-ofd
Content-Type: application/json
Authorization: Bearer {token}

{
  "archiveIds": ["id1", "id2", "id3"]
}
```

### 获取文件内容

```http
GET /api/archive/{id}/content
Authorization: Bearer {token}
```

自动识别文件类型 (PDF/OFD) 并返回对应 Content-Type。

---

## 前端预览

档案详情页自动检测 OFD 文件并使用内置阅读器渲染：

```tsx
// 自动识别文件类型
{fileName.endsWith('.ofd') ? (
  <OfdViewer fileUrl={`/api/archive/${id}/content`} />
) : (
  <iframe src={previewUrl} />
)}
```

---

## 技术实现

| 组件 | 技术 |
|:---|:---|
| 后端转换 | OFDRW (ofdrw-converter 2.2.6) |
| 前端渲染 | ofd.js |
| 字体支持 | 思源宋体 (可配置) |

---

## 注意事项

1. **字体兼容性** - 确保服务器安装中文字体
2. **大文件处理** - 超过 50MB 建议异步转换
3. **权限要求** - 需要 `archive:edit` 权限
