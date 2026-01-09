一旦我所属的文件夹有所变化，请更新我。
本目录存放 ERP 系统字段映射配置。
使用 YAML + Groovy 脚本定义 ERP 数据到 AccountingSipDto 的转换规则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `kingdee-mapping.yml` | 配置 | 金蝶云星空凭证字段映射 |
| `sap-mapping.yml` | 配置 | SAP S/4HANA 凭证字段映射 |
| `yonsuite-mapping.yml` | 配置 | 用友 YonSuite 凭证字段映射 |

## 映射配置说明

### 配置结构

```yaml
sourceSystem: erp类型
targetModel: AccountingSipDto
version: 1.0.0

# ERP 特定配置
erpConfig:
  ...

# 凭证头字段映射
headerMappings:
  accountPeriod: ...
  voucherNumber: ...
  ...

# 分录映射
entries:
  source: 分录数据源字段
  item:
    lineNo: ...
    summary: ...
    ...

# 附件映射
attachments:
  source: 附件数据源字段
  item:
    fileName: ...
    fileType: ...
    ...
```

### 映射方式

| 方式 | 语法 | 示例 |
|------|------|------|
| 字段映射 | `field: fieldName` | 直接取字段值 |
| 类型转换 | `type: date\|decimal\|integer\|long` | 日期、数字类型转换 |
| 格式化 | `format: yyyy-MM-dd` | 日期格式化 |
| Groovy 脚本 | `script: "groovy:..."` | 复杂转换逻辑 |

### sap-mapping.yml

**用途**: SAP S/4HANA Journal Entry 到 AccountingSipDto 映射

**特性**:
- 支持德语借贷标识映射 (S=借, H=贷)
- 会计期间从 PostingDate 提取
- 凭证号格式: "凭证号-年度"
- 辅助信息 JSON (成本中心、利润中心、税码)

### yonsuite-mapping.yml

**用途**: 用友 YonSuite 凭证到 AccountingSipDto 映射

**特性**:
- 直接字段映射为主
- 支持嵌套对象 (maker.name, auditor.name)
- 附件数量直接取值

### kingdee-mapping.yml

**用途**: 金蝶云星空凭证到 AccountingSipDto 映射

**特性**:
- 类似 YonSuite 结构
- 支持金蝶特有字段 (FVoucherID, FDate 等)
