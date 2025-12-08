# 用友凭证同步与组卷管理 API 接口文档

## 版本信息
- **版本**: v2.0
- **更新日期**: 2025-12-04
- **基础路径**: `http://localhost:8080/api`

---

## 一、用友云集成接口

### 1.1 同步凭证

按会计期间从用友云 YonSuite 同步凭证数据。

**接口地址**: `POST /integration/yonsuite/vouchers/sync`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| accbookCode | String | ✅ | 账簿代码，如 `BR01` |
| periodStart | String | ✅ | 起始期间，格式 `YYYY-MM` |
| periodEnd | String | ✅ | 结束期间，格式 `YYYY-MM` |

**请求示例**:
```json
{
  "accbookCode": "BR01",
  "periodStart": "2025-08",
  "periodEnd": "2025-09"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "synced_count": 6,
    "synced_ids": ["archive-001", "archive-002", ...],
    "status": "SUCCESS"
  }
}
```

---

## 二、案卷管理接口

### 2.1 按月组卷

将指定会计期间内未组卷的凭证自动组成案卷。

**接口地址**: `POST /volumes/assemble`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fiscalPeriod | String | ✅ | 会计期间，格式 `YYYY-MM` |

**请求示例**:
```json
{
  "fiscalPeriod": "2025-08"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "组卷成功",
  "data": {
    "id": "9ea8f602a9e340db984932cd3c0365db",
    "volumeCode": "BR01-AC01-202508",
    "title": "泊冉演示集团2025年08月会计凭证",
    "fondsNo": "BR01",
    "fiscalYear": "2025",
    "fiscalPeriod": "2025-08",
    "categoryCode": "AC01",
    "fileCount": 4,
    "retentionPeriod": "10Y",
    "status": "draft"
  }
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "该期间没有待组卷的凭证"
}
```

---

### 2.2 获取案卷列表

分页查询所有案卷。

**接口地址**: `GET /volumes`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | ❌ | 页码，默认 1 |
| limit | Integer | ❌ | 每页条数，默认 20 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "page": 1,
    "limit": 20,
    "total": 2,
    "records": [
      {
        "id": "8c33bb96f29244d5ae6e0a934db111a3",
        "volumeCode": "BR01-AC01-202509",
        "title": "泊冉演示集团2025年09月会计凭证",
        "fileCount": 2,
        "retentionPeriod": "10Y",
        "status": "draft"
      }
    ]
  }
}
```

---

### 2.3 获取案卷详情

**接口地址**: `GET /volumes/{volumeId}`

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": "9ea8f602a9e340db984932cd3c0365db",
    "volumeCode": "BR01-AC01-202508",
    "title": "泊冉演示集团2025年08月会计凭证",
    "status": "archived",
    "reviewedBy": "admin",
    "archivedAt": "2025-12-04T17:09:15"
  }
}
```

---

### 2.4 获取卷内文件列表

**接口地址**: `GET /volumes/{volumeId}/files`

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": "archive-001",
      "archiveCode": "YS-2025-08-记-1",
      "title": "会计凭证-记-1",
      "creator": "王心尹",
      "amount": 150.00,
      "docDate": "2025-08-01",
      "status": "archived"
    }
  ]
}
```

---

### 2.5 提交审核

将草稿状态的案卷提交审核。

**接口地址**: `POST /volumes/{volumeId}/submit-review`

**前置条件**: 案卷状态为 `draft`

**响应示例**:
```json
{
  "code": 200,
  "message": "已提交审核"
}
```

---

### 2.6 审核归档

审批通过，完成正式归档。

**接口地址**: `POST /volumes/{volumeId}/approve`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reviewerId | String | ✅ | 审核人 ID |

**前置条件**: 案卷状态为 `pending`

**响应示例**:
```json
{
  "code": 200,
  "message": "归档成功"
}
```

**归档后影响**:
- 案卷状态变为 `archived`
- 卷内所有凭证状态同步变为 `archived`
- 记录 `reviewedBy`、`archivedAt`

---

### 2.7 驳回

驳回审核，退回草稿状态。

**接口地址**: `POST /volumes/{volumeId}/reject`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reviewerId | String | ✅ | 审核人 ID |
| reason | String | ❌ | 驳回原因 |

**响应示例**:
```json
{
  "code": 200,
  "message": "已驳回"
}
```

---

### 2.8 获取归档登记表

生成符合 GB/T 18894 附录 A 格式的归档登记表。

**接口地址**: `GET /volumes/{volumeId}/registration-form`

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "registrationNo": "GD-2025-08-2882",
    "volumeCode": "BR01-AC01-202508",
    "volumeTitle": "泊冉演示集团2025年08月会计凭证",
    "fondsNo": "BR01",
    "fiscalYear": "2025",
    "fiscalPeriod": "2025-08",
    "categoryCode": "AC01",
    "categoryName": "会计凭证",
    "fileCount": 4,
    "retentionPeriod": "10Y",
    "status": "archived",
    "registrationTime": "2025-12-04T17:09:22",
    "fileList": [
      {
        "序号": 1,
        "档号": "YS-2025-08-记-1",
        "题名": "会计凭证-记-1",
        "日期": "2025-08-01",
        "金额": 150.00,
        "制单人": "王心尹",
        "保管期限": "10Y"
      }
    ]
  }
}
```

---

## 三、状态码说明

| HTTP 状态码 | 业务码 | 说明 |
|-------------|--------|------|
| 200 | 200 | 成功 |
| 400 | 400 | 业务校验失败（如无待组卷凭证） |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |

---

## 四、案卷状态流转

| 状态 | 英文 | 说明 |
|------|------|------|
| 草稿 | draft | 组卷完成，待提交审核 |
| 待审核 | pending | 已提交，等待审批 |
| 已归档 | archived | 审核通过，正式归档 |

---

## 五、配置说明

### 用友云配置 (application.yml)

```yaml
yonsuite:
  enabled: true
  base-url: https://dbox.yonyoucloud.com/iuap-api-gateway
  app-key: your_app_key
  app-secret: your_app_secret
```
