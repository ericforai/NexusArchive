一旦我所属的文件夹有所变化，请更新我。
本目录存放数据传输对象。
用于接口与服务数据传递。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `SalesOutDetailResponse.java` | ✅ DTO | 销售出库单详情响应（v2.0 新增） |
| `SalesOutListRequest.java` | ✅ DTO | 销售出库单列表请求（v2.0 新增） |
| `SalesOutListResponse.java` | ✅ DTO | 销售出库单列表响应（v2.0 新增） |
| `VoucherAttachmentRequest.java` | ✅ DTO | 凭证附件查询请求（v2.0 新增） |
| `VoucherAttachmentResponse.java` | ✅ DTO | 凭证附件查询响应（v2.0 新增） |
| `YonRefundFileRequest.java` | ✅ DTO | 退款单附件查询请求（v2.1 新增） |
| `YonRefundFileResponse.java` | ✅ DTO | 退款单附件查询响应（v2.1 新增） |
| `YonRefundListRequest.java` | ✅ DTO | 退款单列表请求（v2.1 新增） |
| `YonRefundListResponse.java` | ✅ DTO | 退款单列表响应（v2.1 新增） |
| `YonAttachmentListResponse.java` | ⚠️ 旧版 | 附件列表响应 |
| `YonCollectionBillRequest.java` | ⚠️ 旧版 | 收款单请求 |
| `YonCollectionBillResponse.java` | ⚠️ 旧版 | 收款单响应 |
| `YonCollectionDetailResponse.java` | ⚠️ 旧版 | 收款单详情响应 |
| `YonCollectionFileRequest.java` | ⚠️ 旧版 | 收款单附件请求 |
| `YonCollectionFileResponse.java` | ⚠️ 旧版 | 收款单附件响应 |
| `YonPaymentDetailResponse.java` | ⚠️ 旧版 | 付款单详情响应 |
| `YonVoucherDetailRequest.java` | ⚠️ 旧版 | 凭证详情请求 |
| `YonVoucherDetailResponse.java` | ⚠️ 旧版 | 凭证详情响应 |
| `YonVoucherListRequest.java` | ⚠️ 旧版 | 凭证列表请求 |
| `YonVoucherListResponse.java` | ⚠️ 旧版 | 凭证列表响应 |

## DTO 说明

### 1. SalesOutListRequest / SalesOutListResponse ⭐ (v2.0 新增)

销售出库单列表查询的请求和响应 DTO。

**API 端点**: `/yonbip/scm/salesout/list`

**请求字段**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageIndex | int | ✅ | 页码，从 1 开始 |
| pageSize | int | ✅ | 每页数量 |
| vouchdate | String | ❌ | 日期区间（格式: `yyyy-MM-dd HH:mm:ss\|yyyy-MM-dd HH:mm:ss`） |
| simpleVOs | List | ❌ | 查询条件（空数组表示无条件查询） |

**响应字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 响应码，200 为正常 |
| message | String | 错误提示信息 |
| data.recordCount | int | 总记录数 |
| data.recordList | List | 销售出库单记录列表 |

### 2. SalesOutDetailResponse ⭐ (v2.0 新增)

销售出库单详情响应 DTO。

**API 端点**: `/yonbip/scm/salesout/detail`

**响应字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 响应码，200 为正常 |
| message | String | 错误提示信息 |
| data | SalesOutDetail | 销售出库单详情数据 |

### 3. VoucherAttachmentRequest / VoucherAttachmentResponse ⭐ (v2.0 新增)

凭证附件查询的请求和响应 DTO。

**API 端点**: `/yonbip/EFI/rest/v1/openapi/queryBusinessFiles`

**请求字段**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| businessIds | List\<String\> | ✅ | 凭证 ID 列表 |

**响应字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 响应码，200 为正常 |
| message | String | 错误提示信息 |
| data | Map\<String, List\<VoucherAttachment\>\> | 凭证 ID -> 附件列表的映射 |

**VoucherAttachment 字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| fileId | String | 文件 ID |
| filePath | String | 文件访问路径 |
| ctime | Long | 创建时间 |
| utime | Long | 更新时间 |
| fileExtension | String | 文件扩展名 |
| fileSize | Long | 文件大小（字节） |
| fileName | String | 文件名称 |
| name | String | 文件显示名称（含扩展名） |
| yhtUserId | String | 用户 ID |
| tenantId | String | 租户 ID |
| objectId | String | 业务对象 ID（凭证 ID） |
| objectName | String | 对象名称 |

## 使用示例

### 销售出库单列表查询

```java
SalesOutListRequest request = new SalesOutListRequest();
request.setPageIndex(1);
request.setPageSize(100);
request.setVouchdate("2025-01-01 00:00:00|2025-01-31 23:59:59");
request.setSimpleVOs(new ArrayList<>());

// 调用 API
SalesOutListResponse response = callApi(request);
```

### 凭证附件查询

```java
VoucherAttachmentRequest request = new VoucherAttachmentRequest();
request.setBusinessIds(Arrays.asList("id1", "id2", "id3"));

// 调用 API
VoucherAttachmentResponse response = callApi(request);
Map<String, List<VoucherAttachment>> attachments = response.getData();
```
