# YonSuite 凭证预览问题修复记录

**日期**: 2026-01-13
**相关功能**: YonSuite ERP 凭证同步、凭证预览组件
**严重程度**: P1 - 核心功能显示异常

---

## 问题清单

本次修复涵盖 5 个独立但相关的问题：

| # | 问题描述 | 影响范围 | 严重程度 |
|---|----------|----------|----------|
| 1 | 科目名称不显示 | 分录表格 | P1 |
| 2 | 凭证号重复显示 (记-8-记-8) | 凭证头 | P1 |
| 3 | 币种/原币列缺失 | 分录表格 | P2 |
| 4 | 同步返回 0 条记录 | 数据同步 | P0 |
| 5 | SM4 解密导致签名失败 | 认证 | P0 |

---

## 问题 1: 科目名称不显示

### 问题描述
- 位置: 凭证预览 → 会计凭证表格
- 现象: 分录行的"科目"列显示 `-` 或为空
- 期望: 显示科目名称（如"生产成本_直接材料费"）

### 根本原因

**数据格式不匹配**:

1. YonSuite API 返回的原始数据格式：
```json
{
  "accsubject": {
    "code": "500101",
    "name": "生产成本_直接材料费"
  }
}
```

2. 数据库 `source_data` 存储格式（VoucherDTO）：
```json
{
  "accountCode": "500101",
  "accountName": "生产成本_直接材料费"
}
```

3. 前端解析器 `voucherDataParser.ts` 只支持 `accountName` 格式，不支持 `accsubject.name`

### 修复方案

**文件**: `src/pages/archives/utils/voucherDataParser.ts`

```typescript
// 获取科目信息 - 支持 YonSuite 原始格式 (accsubject) 和 VoucherDTO 格式 (accountName)
let accountCode = body.accountCode || body.account_code || '';
let accountName = body.accountName || body.account_name || '';

// 如果还没有科目信息，尝试从 accsubject 获取 (YonSuite 原始格式)
if (!accountCode && !accountName && body.accsubject) {
  if (typeof body.accsubject === 'object') {
    accountCode = body.accsubject.code || '';
    accountName = body.accsubject.name || '';
  }
}
```

---

## 问题 2: 凭证号重复显示

### 问题描述
- 位置: 凭证预览 → 会计凭证（凭证号）和 业务元数据（记账凭证号）
- 现象: 凭证号显示为 `记-8-记-8`（重复）
- 期望: 只显示 `记-8`

### 根本原因

**数据库字段值错误**:

```sql
SELECT voucher_word, erp_voucher_no FROM arc_file_content;
-- voucher_word = "记-8"  ← 错误！应该是 "记"
-- erp_voucher_no = "记-8"
```

前端组合逻辑：
```typescript
const voucherNumber = `${voucherWord}-${voucherNo}`;
// "记-8" + "-" + "记-8" = "记-8-记-8"
```

### 代码缺陷位置

**文件 1**: `nexusarchive-java/src/main/java/com/nexusarchive/service/erp/VoucherMapper.java`
```java
// 错误代码（第 51 行）
content.setVoucherWord(dto.getVoucherNo());  // ← 直接用完整凭证号作为凭证字
```

**文件 2**: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/mapper/YonVoucherMapper.java`
```java
// 错误代码（toPreArchiveFile 方法）
String voucherWord = extractVoucherWord(displayName);  // ← 已有提取方法但未正确使用
```

### 修复方案

**VoucherMapper.java**: 添加凭证字提取方法
```java
// 从凭证号中解析凭证字 (如 "记-8" -> "记")
String voucherWord = extractVoucherWord(dto.getVoucherNo());
content.setVoucherWord(voucherWord);

private static String extractVoucherWord(String voucherNo) {
    if (voucherNo == null || voucherNo.isEmpty()) {
        return "记"; // 默认凭证字
    }
    // 按横线分割: "记-8" -> ["记", "8"]
    String[] parts = voucherNo.split("-");
    if (parts.length > 1) {
        String word = parts[0].trim();
        // 验证是有效的凭证字
        if (isValidVoucherWord(word)) {
            return word;
        }
    }
    // 默认返回 "记"
    return "记";
}

private static boolean isValidVoucherWord(String word) {
    // 常见凭证字: 记、收、付、转、资、银、现
    return word.matches("^[记收付转资产银现]$");
}
```

**YonVoucherMapper.java**: 确保调用提取方法
```java
// 第 339 行已正确实现
String voucherWord = extractVoucherWord(displayName);
```

---

## 问题 3: 币种/原币列缺失

### 问题描述
- 位置: 凭证预览 → 会计凭证表格
- 现象: 只有"摘要"、"科目"、"借方"、"贷方"四列，缺少"币种"和"原币"列
- 期望: 增加外币凭证的币种和原币金额显示

### YonSuite API 数据结构

**YonSuite 原始响应包含币种信息**:
```json
{
  "body": [
    {
      "currency": {
        "id": "...",
        "code": "USD",
        "name": "美元"
      },
      "debitOrg": 1000.00,
      "creditOrg": 0,
      "debitOriginal": 142.86,
      "creditOriginal": 0
    }
  ]
}
```

### 修复方案

**1. 后端 DTO 扩展**

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/VoucherDTO.java`
```java
public static class VoucherEntryDTO {
    private Integer lineNo;
    private String summary;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    /** 币种代码 (如: CNY, USD, EUR) */
    private String currencyCode;
    /** 币种名称 (如: 人民币, 美元, 欧元) */
    private String currencyName;
    /** 原币借方金额 */
    private BigDecimal debitOriginal;
    /** 原币贷方金额 */
    private BigDecimal creditOriginal;
    /** 汇率 */
    private BigDecimal exchangeRate;
}
```

**2. 前端类型定义**

**文件**: `src/components/voucher/types.ts`
```typescript
export interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
  // 币种字段
  currencyCode?: string;
  currencyName?: string;
  debitOriginal?: number | string;
  creditOriginal?: number | string;
  exchangeRate?: number | string;
}
```

**3. 前端数据解析**

**文件**: `src/pages/archives/utils/voucherDataParser.ts`
```typescript
// 获取币种信息
let currencyCode = body.currencyCode || body.currency_code || '';
let currencyName = body.currencyName || body.currency_name || '';

// 如果还没有币种信息，尝试从 currency 获取 (YonSuite 原始格式)
if (!currencyCode && !currencyName && body.currency) {
  if (typeof body.currency === 'object') {
    currencyCode = body.currency.code || '';
    currencyName = body.currency.name || '';
  }
}

return {
  lineNo: body.recordNumber || body.recordnumber || body.lineNo || index + 1,
  summary: body.description || body.digest || body.summary || '',
  accountCode,
  accountName,
  debit: Number(debit) || 0,
  credit: Number(credit) || 0,
  currencyCode,
  currencyName,
  debitOriginal: Number(debitOriginal) || undefined,
  creditOriginal: Number(creditOriginal) || undefined,
  exchangeRate: exchangeRate ? Number(exchangeRate) : undefined,
};
```

**4. 前端显示组件**

**文件**: `src/components/voucher/VoucherPreviewCanvas.tsx`
```typescript
// 表头增加两列
<th style={{ ...voucherTableStyles.tableHeadCell, width: '12%' }}>币种</th>
<th style={{ ...voucherTableStyles.tableHeadCellRight, width: '10%' }}>原币</th>

// 表体数据
<td style={{ ...voucherTableStyles.tableCell, fontSize: '12px' }}>
  {entry.currencyName || entry.currencyCode || '-'}
</td>
<td style={{ ...voucherTableStyles.tableCellRight, fontSize: '12px' }}>
  {hasForeignCurrency ? (
    <span>{formatCurrency(originalDebit || originalCredit)}</span>
  ) : '-'}
</td>

// 判断是否有外币
const hasForeignCurrency = entry.currencyCode && entry.currencyCode !== 'CNY';
const originalDebit = entry.debitOriginal ? Number(entry.debitOriginal) : 0;
const originalCredit = entry.creditOriginal ? Number(entry.creditOriginal) : 0;
```

---

## 问题 4: 同步返回 0 条记录

### 问题描述
- 点击"同步凭证数据"后显示：`同步成功: 获取 0 条，其中新增 0 条`
- 实际 YonSuite 中有凭证数据

### 根本原因

**SM4 解密失败导致认证失败**:

```
SM4 解密失败 (BadPaddingException)
  → 使用错误的 appSecret (hex 字符串)
  → YonSuite API 签名验证失败
  → token 获取失败
  → fallback 到 @Value 配置 (空)
  → 抛出 "appKey is null or empty"
```

### 数据库配置问题

```json
{
  "appSecret_encrypted": "true",  // ← 标记为已加密
  "appSecret": "b1dfb694..."      // ← 但实际是原始值，无法用当前 SM4 密钥解密
}
```

### 修复方案

**1. 代码修复**

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/erp/ErpConfigDtoBuilder.java`
```java
// 检查 appSecret_encrypted 标志，只有为 true 时才解密
boolean secretEncrypted = json.getBool("appSecret_encrypted", true);
if (secretEncrypted && appSecret != null && !appSecret.isEmpty()) {
    try {
        appSecret = SM4Utils.decrypt(appSecret);
    } catch (Exception e) {
        log.error("ErpConfigDtoBuilder: appSecret解密失败", e);
        throw new RuntimeException("appSecret 解密失败: " + e.getMessage(), e);
    }
}
dtoConfig.setAppSecret(appSecret);
```

**2. 数据库修复**
```sql
UPDATE sys_erp_config
SET config_json = '{
  "appKey": "96a95c00982446cba484ccc4936b221b",
  "baseUrl": "https://dbox.yonyoucloud.com/iuap-api-gateway",
  "appSecret": "b1dfb6943c1f9f6f790a1159713a1a28d8e8ff00aad18a3feb727720db69f2c41a1d10b55a27730d62107a46d6b239fd",
  "appSecret_encrypted": false,
  "requireClosedPeriod": false
}'
WHERE id = 1;
```

---

## 问题 5: SM4 解密失败 (问题 4 的子问题)

### 错误日志
```
WARN  com.nexusarchive.util.SM4Utils - SM4 解密失败,返回原始内容 (兼容模式): hex=b1dfb694..., error=BadPaddingException: pad block corrupted
```

### 原因分析

| 场景 | 结果 |
|------|------|
| 用密钥 A 加密，用密钥 B 解密 | BadPaddingException |
| 尝试解密未加密的值 | BadPaddingException |
| 密钥长度不是 16 字节 | 无效密钥 |

### 经验教训

1. **加密标志必须正确**: `appSecret_encrypted` 标志必须与实际状态一致
2. **密钥管理**: 生产环境 SM4_KEY 必须稳定，更换密钥需重新加密所有数据
3. **兼容模式陷阱**: SM4Utils.decrypt() 失败时返回原始值是"兼容模式"，但这会导致后续签名失败

---

## 数据流图

```
┌─────────────────┐
│  YonSuite API   │
│  accsubject     │ ← 科目对象嵌套
│  currency       │ ← 币种对象嵌套
│  debitOrg       │ ← 本位币借方
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│   YonVoucherMapper.java         │
│   → extractVoucherWord()        │ ← 提取凭证字
│   → toPreArchiveFile()          │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   ErpConfigDtoBuilder.java      │
│   → check appSecret_encrypted   │ ← 检查加密标志
│   → SM4Utils.decrypt()          │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   arc_file_content.source_data  │ ← JSON 存储
│   voucher_word                  │ ← 提取的凭证字
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   voucherDataParser.ts          │
│   → 支持 accsubject.name        │ ← 兼容原始格式
│   → 支持 currency.code/name     │ ← 兼容原始格式
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   VoucherPreviewCanvas.tsx      │
│   → 科目列                      │
│   → 币种/原币列                  │
│   → 凭证号格式化                 │
└─────────────────────────────────┘
```

---

## 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `VoucherMapper.java` | 新增方法 | `extractVoucherWord()`, `isValidVoucherWord()` |
| `YonVoucherMapper.java` | 确认正确 | 已有提取方法，确保调用 |
| `VoucherDTO.java` | 新增字段 | VoucherEntryDTO 增加 6 个币种字段 |
| `ErpConfigDtoBuilder.java` | 修复 bug | 增加 `appSecret_encrypted` 检查 |
| `voucher/types.ts` | 新增字段 | VoucherEntryDTO 接口增加 6 个币种字段 |
| `voucherDataParser.ts` | 修复 bug | 支持 `accsubject` 和 `currency` 嵌套对象 |
| `VoucherPreviewCanvas.tsx` | 新增列 | 币种、原币两列 |

---

## 测试检查清单

- [ ] 凭证预览显示科目名称
- [ ] 凭证号格式正确（不重复）
- [ ] 外币凭证显示币种和原币
- [ ] 人民币凭证不显示原币
- [ ] 同步能成功获取数据
- [ ] 多组织同步都能工作
- [ ] 日志无 SM4 解密错误

---

## 相关文档

- [DA/T 94-2022 会计档案管理规范](../guides/用友集成.md)
- [SM4 加密使用指南](../guides/安全指南.md)
- [凭证预览组件设计](../components/voucher/README.md)
