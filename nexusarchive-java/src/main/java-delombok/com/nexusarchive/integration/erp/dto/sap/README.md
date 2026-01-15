一旦我所属的文件夹有所变化，请更新我。
本目录存放 SAP S/4HANA OData 响应 DTO。
用于 SAP S/4HANA API Journal Entry 数据反序列化。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `SapJournalEntryDto.java` | DTO | SAP 凭证抬头 OData 响应 |
| `SapJournalEntryItemDto.java` | DTO | SAP 凭证分录项 OData 响应 |
| `SapAttachmentDto.java` | DTO | SAP 附件 OData 响应 |
| `SapErrorResponse.java` | DTO | SAP OData 错误响应 |

## DTO 说明

### SapJournalEntryDto

**用途**: SAP S/4HANA 凭证抬头数据结构

**关键字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| journalEntry | String | 凭证号 |
| companyCode | String | 公司代码 |
| fiscalYear | String | 会计年度 |
| postingDate | String | 过账日期 |
| items | List&lt;SapJournalEntryItemDto&gt; | 分录项列表 (导航属性 to_JournalEntryItem) |
| attachments | List&lt;SapAttachmentDto&gt; | 附件列表 (导航属性 to_Attachment) |

### SapJournalEntryItemDto

**用途**: SAP 凭证分录项数据结构

**关键字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| journalEntryItem | String | 行项目号 |
| glAccount | String | 总账科目 |
| debitCreditCode | String | 借贷标识 (S=借, H=贷) |
| amountInTransactionCurrency | String | 交易货币金额 |
| costCenter | String | 成本中心 (可选) |
| profitCenter | String | 利润中心 (可选) |

### SapErrorResponse

**用途**: SAP OData API 错误响应

**关键字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| error.code | String | 错误代码 |
| error.message.value | String | 错误消息 |
| error.innerError | InnerError | 内部错误详情 (可选) |
