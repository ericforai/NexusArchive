# YonSuite 关账状态检查功能设计

> **目标**: 在凭证同步前检查用友 YonSuite 的关账状态，确保归档数据的准确性

**背景**: 用友 YonSuite 提供了 `/yonbip/EFI/closeInfo` 接口查询账簿关账状态。本功能在凭证同步前调用此接口检查，避免同步未关账期间的凭证。

---

## 目录

1. [需求概述](#需求概述)
2. [API 定义](#api-定义)
3. [后端设计](#后端设计)
4. [前端设计](#前端设计)
5. [数据流程](#数据流程)
6. [错误处理](#错误处理)
7. [文件清单](#文件清单)

---

## 需求概述

### 核心需求

- 在凭证同步前，检查目标期间是否已在用友系统关账
- 未关账时根据配置采取不同行为（强制阻止 / 软提醒）
- 支持在前端切换检查模式

### 用户决策

| 决策项 | 选择 |
|--------|------|
| 检查位置 | YonSuiteErpAdapter 层 |
| 缓存策略 | 不缓存，实时调用用友 API |
| 多账簿支持 | 一个全宗对应一个账簿 |
| 失败处理 | API 失败时阻止同步 |
| 前端交互 | 后端同步检查 |
| 应用场景 | 仅 VOUCHER_SYNC（凭证同步） |
| 配置方式 | 前端可切换开关 |

---

## API 定义

### 用友 YonSuite 关账状态查询接口

```
端点: /yonbip/EFI/closeInfo
方法: POST
ContentType: application/json
```

#### 请求参数

| 参数 | 类型 | 位置 | 必填 | 说明 |
|------|------|------|------|------|
| access_token | string | query | 是 | 调用方 token |
| books | string[] | body | 是 | 账簿 ID 数组 |
| period | string | body | 是 | 期间，格式 "2024-12" |

#### 请求示例

```json
POST /yonbip/EFI/closeInfo?access_token=xxx
{
  "books": [1566777788889999001],
  "period": "2024-12"
}
```

#### 响应参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | string | 响应码，"200" 表示成功 |
| message | string | 响应消息 |
| data.closeStatus | boolean | 关账状态，true=已关账 |
| data.closeTime | string | 关账时间，格式 "yyyy-MM-dd HH:mm:ss" |

#### 响应示例

```json
{
  "code": "200",
  "message": "",
  "data": {
    "closeStatus": true,
    "closeTime": "2024-12-25 15:24:03"
  }
}
```

---

## 后端设计

### 1. DTO 定义

#### YonCloseInfoRequest.java

```java
package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;
import java.util.List;

@Data
public class YonCloseInfoRequest {
    private List<String> books;
    private String period;
}
```

#### YonCloseInfoResponse.java

```java
package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class YonCloseInfoResponse {
    private String code;
    private String message;
    private CloseInfoData data;

    @Data
    public static class CloseInfoData {
        @JsonProperty("closeStatus")
        private boolean closed;

        @JsonProperty("closeTime")
        private String closeTime;
    }
}
```

#### CloseInfoResult.java

```java
package com.nexusarchive.integration.erp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloseInfoResult {
    private boolean closed;
    private String closeTime;
    private String period;
    private String accbookCode;
    private boolean checkSuccess;
    private String errorMessage;
}
```

### 2. 异常定义

#### PeriodNotClosedException.java

```java
package com.nexusarchive.integration.erp.exception;

import lombok.Getter;

@Getter
public class PeriodNotClosedException extends RuntimeException {
    private final String period;
    private final String accbookCode;

    public PeriodNotClosedException(String period, String accbookCode) {
        super(String.format("期间 %s 未关账，请先在用友系统完成关账", period));
        this.period = period;
        this.accbookCode = accbookCode;
    }
}
```

### 3. YonSuiteClient 新增方法

```java
/**
 * 查询账簿关账状态
 * POST /yonbip/EFI/closeInfo
 *
 * @param accessToken 访问令牌（可选）
 * @param bookId 账簿 ID
 * @param period 期间，格式 "yyyy-MM"
 * @return 关账状态信息
 */
public YonCloseInfoResponse queryCloseInfo(String accessToken,
                                           String bookId,
                                           String period) {
    String token = getToken(accessToken);
    String url = baseUrl + "/yonbip/EFI/closeInfo"
            + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

    try {
        YonCloseInfoRequest request = new YonCloseInfoRequest();
        request.setBooks(List.of(bookId));
        request.setPeriod(period);

        String body = objectMapper.writeValueAsString(request);
        log.info("查询关账状态: bookId={}, period={}", bookId, period);

        String respStr = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(30_000)
                .execute()
                .body();

        log.info("关账状态查询响应: {}", respStr);

        YonCloseInfoResponse response = objectMapper.readValue(respStr, YonCloseInfoResponse.class);

        if (!"200".equals(response.getCode())) {
            throw new RuntimeException("查询关账状态失败: " + response.getMessage());
        }

        return response;
    } catch (Exception e) {
        log.error("查询关账状态异常: bookId={}, period={}", bookId, period, e);
        throw new RuntimeException("查询关账状态失败: " + e.getMessage(), e);
    }
}
```

### 4. YonSuiteErpAdapter 修改

```java
@Override
public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
    String targetAccbookCode = config.getTargetAccbookCode();
    String period = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

    // 1. 检查关账状态
    CloseInfoResult closeInfo = checkPeriodCloseStatus(config, targetAccbookCode, period);

    // 2. 未关账时根据配置处理
    if (!closeInfo.isClosed()) {
        if (isRequireClosedPeriod()) {
            throw new PeriodNotClosedException(period, targetAccbookCode);
        }
        log.warn("期间 {} 未关账，用户选择继续同步", period);
    }

    // 3. 继续原有同步逻辑
    return syncVouchersInternal(config, startDate, endDate);
}

/**
 * 检查期间关账状态
 */
public CloseInfoResult checkPeriodCloseStatus(ErpConfig config,
                                               String accbookCode,
                                               String period) {
    try {
        // 需要先获取账簿 ID（从 accbookCode 转换）
        String bookId = getBookIdByCode(config, accbookCode);

        YonCloseInfoResponse response = yonSuiteClient.queryCloseInfo(null, bookId, period);

        boolean closed = response.getData() != null && response.getData().isClosed();
        String closeTime = closed ? response.getData().getCloseTime() : null;

        return CloseInfoResult.builder()
                .closed(closed)
                .closeTime(closeTime)
                .period(period)
                .accbookCode(accbookCode)
                .checkSuccess(true)
                .build();

    } catch (Exception e) {
        log.error("检查关账状态失败: accbookCode={}, period={}", accbookCode, period, e);
        return CloseInfoResult.builder()
                .closed(false)
                .period(period)
                .accbookCode(accbookCode)
                .checkSuccess(false)
                .errorMessage(e.getMessage())
                .build();
    }
}

/**
 * 从配置获取是否强制要求关账
 */
private boolean isRequireClosedPeriod() {
    // TODO: 从系统配置读取，默认 false
    return false;
}

/**
 * 根据账簿代码获取账簿 ID
 */
private String getBookIdByCode(ErpConfig config, String accbookCode) {
    // TODO: 实现账簿代码到 ID 的转换
    // 可能需要调用另一个 API 或从缓存获取
    return config.getTargetAccbookId(); // 假设配置中已有
}
```

### 5. 配置项

#### application.yml

```yaml
voucher:
  sync:
    require-closed-period: false  # true=强制阻止, false=软提醒
```

#### SystemConfigService

```java
// 提供配置读取接口
public boolean isRequireClosedPeriod();
public void setRequireClosedPeriod(boolean require);
```

---

## 前端设计

### 1. 配置开关位置

在「集成中心」-「用友YonSuite」配置页面添加开关：

```
┌─────────────────────────────────────────────────────┐
│  用友 YonSuite 配置                                  │
├─────────────────────────────────────────────────────┤
│  账簿代码: [____________]                            │
│  App Key:   [____________]                            │
│  App Secret:[____________]                            │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │ 关账检查模式                                   │   │
│  │                                              │   │
│  │ ○ 强制模式 - 未关账时阻止同步               │   │
│  │ ● 提醒模式 - 未关账时警告但允许继续         │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  [保存配置]  [取消]                                  │
└─────────────────────────────────────────────────────┘
```

### 2. 同步时提示

#### 软提醒模式（requireClosedPeriod = false）

```
┌─────────────────────────────────────────────────────┐
│  ⚠️  期间未关账提醒                                  │
├─────────────────────────────────────────────────────┤
│                                                      │
│  期间 2024-12 在用友系统中尚未关账。                 │
│                                                      │
│  建议：完成关账后再同步，确保数据准确性。             │
│                                                      │
│  ┌───────────┐  ┌───────────┐                       │
│  │  取消同步  │  │  仍要同步  │                       │
│  └───────────┘  └───────────┘                       │
│                                                      │
└─────────────────────────────────────────────────────┘
```

#### 强制模式（requireClosedPeriod = true）

```
┌─────────────────────────────────────────────────────┐
│  ❌ 无法同步                                         │
├─────────────────────────────────────────────────────┤
│                                                      │
│  期间 2024-12 未关账，无法同步凭证。                 │
│                                                      │
│  请先在用友系统中完成该期间的关账操作。               │
│                                                      │
│         ┌───────────┐                                │
│         │   知道了   │                                │
│         └───────────┘                                │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### 3. API 调用失败提示

```
┌─────────────────────────────────────────────────────┐
│  ❌ 无法获取关账状态                                 │
├─────────────────────────────────────────────────────┤
│                                                      │
│  无法连接到用友系统获取关账状态，请稍后重试。         │
│                                                      │
│  错误信息: [网络超时]                                │
│                                                      │
│         ┌───────────┐                                │
│         │   确定     │                                │
│         └───────────┘                                │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## 数据流程

### 正常流程（已关账）

```
用户选择期间 2024-12 → 点击同步
        │
        ▼
   Controller 接收请求
        │
        ▼
   ErpAdapter.checkPeriodCloseStatus()
        │
        ▼
   YonSuiteClient.queryCloseInfo()
        │
        ▼
   用友 API 返回: { closeStatus: true, closeTime: "..." }
        │
        ▼
   检查通过，继续同步凭证
```

### 软提醒流程（未关账，提醒模式）

```
用户选择期间 2024-12 → 点击同步
        │
        ▼
   用友 API 返回: { closeStatus: false }
        │
        ▼
   前端弹窗警告: "期间未关账，是否继续？"
        │
    ┌───┴───┐
    │       │
  取消    继续
    │       │
    │       ▼
    │   继续同步凭证
    │   （记录警告日志）
```

### 强制流程（未关账，强制模式）

```
用户选择期间 2024-12 → 点击同步
        │
        ▼
   用友 API 返回: { closeStatus: false }
        │
        ▼
   抛出 PeriodNotClosedException
        │
        ▼
   前端显示错误: "期间未关账，无法同步"
        │
        ▼
   同步终止
```

---

## 错误处理

### 异常类型

| 异常 | 触发条件 | 处理方式 |
|------|---------|---------|
| `PeriodNotClosedException` | 期间未关账且配置为强制模式 | 前端提示错误，阻止同步 |
| `RuntimeException` | API 调用失败 | 前端提示"无法获取关账状态"，阻止同步 |
| `IllegalArgumentException` | 参数错误（期间格式、账簿ID） | 前端提示参数错误 |

### 降级策略

本设计**不采用降级策略**，关账状态查询失败时同步操作也会失败，确保数据准确性。

---

## 文件清单

### 新建文件

| 文件 | 说明 |
|------|------|
| `YonCloseInfoRequest.java` | 关账查询请求 DTO |
| `YonCloseInfoResponse.java` | 关账查询响应 DTO |
| `CloseInfoResult.java` | 关账信息结果 DTO |
| `PeriodNotClosedException.java` | 期间未关账异常 |

### 修改文件

| 文件 | 修改内容 |
|------|---------|
| `YonSuiteClient.java` | 新增 `queryCloseInfo()` 方法 |
| `YonSuiteErpAdapter.java` | 新增 `checkPeriodCloseStatus()` 方法，修改 `syncVouchers()` |
| `ErpConfigService.java` | 新增配置读写方法 |
| `application.yml` | 新增配置项 |
| 前端配置页面 | 新增关账检查模式开关 |
| 前端同步组件 | 新增关账状态处理逻辑 |

---

## 附录：前端可配置开关详细设计

### 1. 数据库扩展

#### erp_config 表新增字段

```sql
ALTER TABLE erp_config ADD COLUMN require_closed_period BOOLEAN DEFAULT FALSE;
COMMENT ON COLUMN erp_config.require_closed_period IS '是否强制要求关账后才能同步凭证';
```

### 2. 类型定义扩展

#### ErpConfig 类型 (src/types.ts)

```typescript
export interface ErpConfig {
  id: number;
  name: string;
  erpType: string;
  baseUrl: string;
  appKey: string;
  // ... 其他字段

  // 新增：关账检查模式
  requireClosedPeriod?: boolean;
}
```

#### ConnectorModal Form 扩展

```typescript
export interface ConnectorModalState {
  // ... 现有字段

  configForm: {
    name: string;
    erpType: string;
    baseUrl: string;
    appKey: string;
    appSecret: string;
    accbookMapping: Record<string, string>;

    // 新增：关账检查模式（仅 YonSuite 显示）
    requireClosedPeriod?: boolean;
  };
}
```

### 3. 前端 UI 实现

#### ConnectorModal 开关组件

```tsx
// 仅当 erpType === 'YONSUITE' 时显示
{erpType === 'YONSUITE' && (
  <div className="space-y-3 mt-4 pt-4 border-t">
    <div>
      <div className="flex items-center justify-between mb-2">
        <label className="text-sm font-medium text-gray-700">
          关账检查模式
        </label>
        <Switch
          checked={configForm.requireClosedPeriod ?? false}
          onChange={(checked) => updateForm('requireClosedPeriod', checked)}
        />
      </div>
      <p className="text-xs text-gray-500">
        {configForm.requireClosedPeriod
          ? "强制模式：未关账期间将无法同步凭证"
          : "提醒模式：未关账时警告但允许继续同步"
        }
      </p>
    </div>
  </div>
)}
```

#### Ant Design Switch 组件

```tsx
import { Switch } from 'antd';

<Switch
  checkedChildren="强制"
  unCheckedChildren="提醒"
  checked={configForm.requireClosedPeriod ?? false}
  onChange={(checked) => updateForm('requireClosedPeriod', checked)}
/>
```

### 4. 后端 API 扩展

#### ErpConfigController 新增接口

```java
/**
 * 更新关账检查模式
 */
@PutMapping("/{configId}/close-check-mode")
public ResponseEntity<Void> updateCloseCheckMode(
    @PathVariable Long configId,
    @RequestBody Map<String, Boolean> request
) {
    boolean require = request.get("requireClosedPeriod");
    erpConfigService.updateRequireClosedPeriod(configId, require);
    return ResponseEntity.ok().build();
}

/**
 * 获取关账检查模式
 */
@GetMapping("/{configId}/close-check-mode")
public ResponseEntity<Boolean> getCloseCheckMode(@PathVariable Long configId) {
    Boolean require = erpConfigService.getRequireClosedPeriod(configId);
    return ResponseEntity.ok(require);
}
```

#### ErpConfigService 扩展

```java
public class ErpConfigServiceImpl implements ErpConfigService {

    public void updateRequireClosedPeriod(Long configId, boolean require) {
        ErpConfig config = getById(configId);
        config.setRequireClosedPeriod(require);
        save(config);
    }

    public boolean getRequireClosedPeriod(Long configId) {
        ErpConfig config = getById(configId);
        // 默认 false（提醒模式）
        return config.getRequireClosedPeriod() != null
            ? config.getRequireClosedPeriod()
            : false;
    }
}
```

### 5. 前端 API 调用

#### integrationSettingsApi.ts

```typescript
// 更新关账检查模式
export async updateCloseCheckMode(
  configId: number,
  requireClosedPeriod: boolean
): Promise<void> {
  await client.put(`/erp/config/${configId}/close-check-mode`, {
    requireClosedPeriod,
  });
}

// 获取关账检查模式
export async getCloseCheckMode(
  configId: number
): Promise<boolean> {
  const { data } = await client.get<boolean>(
    `/erp/config/${configId}/close-check-mode`
  );
  return data;
}
```

### 6. 使用流程

```
┌─────────────────────────────────────────────────────┐
│  1. 用户进入"集成中心" → "用友YonSuite"配置         │
└─────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│  2. 在配置页面看到"关账检查模式"开关               │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │ 关账检查模式    [提醒模式] ← Switch 开关      │   │
│  │                                          │   │
│  │ 提醒模式：未关账时警告但允许继续           │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│  3. 用户切换开关 → 点击"保存"                       │
│                                                     │
│  PUT /erp/config/{id}/close-check-mode              │
│  { requireClosedPeriod: true }                      │
└─────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│  4. 下次同步时生效                                   │
│                                                     │
│  - 强制模式：未关账直接阻止                           │
│  - 提醒模式：弹窗警告，用户可选择继续                │
└─────────────────────────────────────────────────────┘
```

---

## 附录：API 文档

保存用友关账状态查询接口文档到 `docs/api/关账状态查询.md`。

```markdown
# 关账状态查询 API

## 接口信息
- 服务名称：批量查询关账状态
- 发布时间：2025-06-26
- 接口类别：EFI（企业金融）

## 请求说明
- 请求地址：/yonbip/EFI/closeInfo
- 请求方式：POST
- ContentType：application/json

## 请求参数
...
```
