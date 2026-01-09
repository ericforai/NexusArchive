# SAP S/4 HANA 集成实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 SAP S/4 HANA 系统的凭证数据集成，支持 OData 服务接口，前端展示四种接口类型（OData、RFC/BAPI、IDoc、SAP Gateway）作为产品能力预留。

**Architecture:**
- 复用现有 ErpAdapter 接口和 ErpMapper 映射框架
- SAP OData 客户端负责 HTTP 调用和 JSON 解析
- YAML 配置声明 SAP 字段到 AccountingSipDto 的映射规则
- Groovy 脚本处理 SAP 特有的德语缩写和复杂嵌套

**Tech Stack:** Spring Boot 3.1.6, Apache HttpClient 5, OData V4 Client, Jackson JSON, SnakeYAML, Groovy 4.0

---

## Phase 1: 后端 - SAP OData 适配器基础

### Task 1: 创建 SAP 响应 DTO 模型

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/sap/SapJournalEntryDto.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/sap/SapJournalEntryItemDto.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/sap/SapAttachmentDto.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/sap/SapErrorResponse.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/dto/sap/SapJournalEntryDtoTest.java`

**Step 1: Write the failing test**

```java
// SapJournalEntryDtoTest.java
@Test
void testJsonDeserialization() {
    String json = """
        {
          "JournalEntry": "10000001",
          "CompanyCode": "BR01",
          "FiscalYear": "2024",
          "PostingDate": "2024-01-15",
          "DocumentHeaderText": "测试凭证",
          "Item": [
            {
              "JournalEntryItem": "1",
              "GLAccount": "100100",
              "DebitCreditCode": "S",
              "AmountInTransactionCurrency": "1000.00",
              "TransactionCurrency": "CNY"
            }
          ]
        }
        """;

    SapJournalEntryDto dto = objectMapper.readValue(json, SapJournalEntryDto.class);

    assertThat(dto.getJournalEntry()).isEqualTo("10000001");
    assertThat(dto.getCompanyCode()).isEqualTo("BR01");
    assertThat(dto.getItem()).hasSize(1);
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SapJournalEntryDtoTest -pl nexusarchive-java`
Expected: FAIL with "class SapJournalEntryDto not found"

**Step 3: Write minimal implementation**

```java
// SapJournalEntryDto.java
package com.nexusarchive.integration.erp.dto.sap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * SAP Journal Entry OData 响应 DTO
 * 参考 SAP S/4HANA API Journal Entry – Post
 */
@Data
public class SapJournalEntryDto {

    /** 凭证号 (JournalEntry) */
    @JsonProperty("JournalEntry")
    private String journalEntry;

    /** 公司代码 (Bukrs) */
    @JsonProperty("CompanyCode")
    private String companyCode;

    /** 会计年度 (Gjahr) */
    @JsonProperty("FiscalYear")
    private String fiscalYear;

    /** 过账日期 (Budat) */
    @JsonProperty("PostingDate")
    private String postingDate;

    /** 凭证抬头文本 */
    @JsonProperty("DocumentHeaderText")
    private String documentHeaderText;

    /** 创建日期 */
    @JsonProperty("CreationDate")
    private String creationDate;

    /** 创建时间 */
    @JsonProperty("CreationTime")
    private String creationTime;

    /** 创建人 */
    @JsonProperty("CreatedByUser")
    private String createdByUser;

    /** 分录项列表 */
    @JsonProperty("to_JournalEntryItem")
    private List<SapJournalEntryItemDto> items;

    /** 附件关联 (通过导航属性) */
    @JsonProperty("to_Attachment")
    private List<SapAttachmentDto> attachments;
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SapJournalEntryDtoTest -pl nexusarchive-java`
Expected: PASS

**Step 5: Commit**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
git add src/main/java/com/nexusarchive/integration/erp/dto/sap/
git add src/test/java/com/nexusarchive/integration/erp/dto/sap/
git commit -m "feat(sap): add SAP OData response DTO models

- Add SapJournalEntryDto for journal entry header
- Add SapJournalEntryItemDto for entry items
- Add SapAttachmentDto for attachments
- Add SapErrorResponse for error handling

参考 SAP S/4HANA API Journal Entry 结构"
```

---

### Task 2: 创建 SAP HTTP 客户端基础类

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/SapHttpClient.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/adapter/client/SapHttpClientTest.java`

**Step 1: Write the failing test**

```java
// SapHttpClientTest.java
@Test
void testBuildODataRequest() {
    ErpConfig config = ErpConfig.builder()
        .baseUrl("https://sap.example.com")
        .tenantId("BR01")
        .appKey("user")
        .appSecret("password")
        .build();

    SapHttpClient client = new SapHttpClient(objectMapper);

    String requestUrl = client.buildQueryUrl(
        config,
        "2024-01-01",
        "2024-01-31"
    );

    assertThat(requestUrl)
        .contains("https://sap.example.com")
        .contains("$filter=PostingDate%20ge%202024-01-01")
        .contains("CompanyCode%20eq%20'BR01'");
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SapHttpClientTest -pl nexusarchive-java`
Expected: FAIL with "class SapHttpClient not found"

**Step 3: Write minimal implementation**

```java
// SapHttpClient.java
package com.nexusarchive.integration.erp.adapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.sap.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * SAP S/4HANA OData HTTP 客户端
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SapHttpClient {

    private final ObjectMapper objectMapper;

    private static final String JOURNAL_ENTRY_PATH =
        "/sap/opu/odata4/sap/api_journal_entry/srvd_a2x/sap/journal_entry/0001";

    public String buildQueryUrl(ErpConfig config, String startDate, String endDate) {
        StringBuilder url = new StringBuilder(config.getBaseUrl());
        url.append(JOURNAL_ENTRY_PATH);
        url.append("/JournalEntry?$filter=");

        url.append("CompanyCode%20eq%20'");
        url.append(urlEncode(config.getTenantId()));
        url.append("'");

        url.append("%20and%20PostingDate%20ge%20");
        url.append(startDate);
        url.append("%20and%20PostingDate%20le%20");
        url.append(endDate);

        return url.toString();
    }

    public SapJournalEntryListResponse queryJournalEntries(
            ErpConfig config,
            String startDate,
            String endDate) throws IOException {

        String url = buildQueryUrl(config, startDate, endDate);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");

            String auth = config.getAppKey() + ":" + config.getAppSecret();
            String encodedAuth = java.util.Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            httpGet.setHeader("Authorization", "Basic " + encodedAuth);

            try (var response = httpClient.execute(httpGet)) {
                String json = EntityUtils.toString(response.getEntity());
                int statusCode = response.getCode();

                if (statusCode >= 400) {
                    SapErrorResponse error = objectMapper.readValue(json, SapErrorResponse.class);
                    throw new SapException(error.getMessage());
                }

                return objectMapper.readValue(json, SapJournalEntryListResponse.class);
            }
        }
    }

    public SapJournalEntryDto getJournalEntryDetail(
            ErpConfig config,
            String journalEntry,
            String fiscalYear) throws IOException {

        String url = config.getBaseUrl() + JOURNAL_ENTRY_PATH +
            "/JournalEntry(JournalEntry='" + journalEntry +
            "',FiscalYear='" + fiscalYear + "')?$expand=to_JournalEntryItem,to_Attachment";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");

            String auth = config.getAppKey() + ":" + config.getAppSecret();
            String encodedAuth = java.util.Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            httpGet.setHeader("Authorization", "Basic " + encodedAuth);

            try (var response = httpClient.execute(httpGet)) {
                String json = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(json, SapJournalEntryDto.class);
            }
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @lombok.Data
    public static class SapJournalEntryListResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("d")
        private SapJournalEntryCollection collection;

        @lombok.Data
        public static class SapJournalEntryCollection {
            @com.fasterxml.jackson.annotation.JsonProperty("results")
            private java.util.List<SapJournalEntryDto> results;
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SapHttpClientTest -pl nexusarchive-java`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/nexusarchive/integration/erp/adapter/client/SapHttpClient.java
git add src/test/java/com/nexusarchive/integration/erp/adapter/client/SapHttpClientTest.java
git commit -m "feat(sap): add SAP OData HTTP client"
```

---

### Task 3: 创建 SAP OData 映射配置

**Files:**
- Create: `nexusarchive-java/src/main/resources/erp-mapping/sap-mapping.yml`

**Step 1: Create the mapping configuration**

```yaml
# SAP S/4HANA Journal Entry 映射配置
sourceSystem: sap
targetModel: AccountingSipDto
version: 1.0.0

sapConfig:
  debitCreditMapping:
    S: DEBIT   # Soll = 借方
    H: CREDIT  # Haben = 贷方

headerMappings:
  accountPeriod:
    script: |
      groovy:
        import java.time.*
        def date = LocalDate.parse(ctx.PostingDate)
        return date.format("yyyy-MM")

  voucherNumber:
    script: |
      groovy:
        return ctx.JournalEntry + "-" + ctx.FiscalYear

  voucherType:
    script: "groovy:return 'GENERIC'"

  voucherDate:
    field: PostingDate
    type: date
    format: yyyy-MM-dd

  postingDate:
    field: PostingDate
    type: date
    format: yyyy-MM-dd

  totalAmount:
    script: |
      groovy:
        if (ctx.to_JournalEntryItem == null) return 0.0
        def total = 0.0
        ctx.to_JournalEntryItem.each { item ->
          if (item.DebitCreditCode == 'S') {
            total += item.AmountInTransactionCurrency ?: 0
          }
        }
        return total

  currencyCode:
    script: |
      groovy:
        return ctx.to_JournalEntryItem?.getAt(0)?.TransactionCurrency ?: 'CNY'

  attachmentCount:
    script: "groovy:return ctx.to_Attachment?.size() ?: 0"

  issuer:
    field: CreatedByUser

  remark:
    field: DocumentHeaderText

entries:
  source: to_JournalEntryItem
  item:
    lineNo:
      field: JournalEntryItem
      type: integer

    summary:
      field: DocumentItemText

    subjectCode:
      field: GLAccount

    subjectName:
      script: "groovy:return ''"

    direction:
      script: |
        groovy:
          return ctx.DebitCreditCode == 'S' ? 'DEBIT' : 'CREDIT'

    amount:
      field: AmountInTransactionCurrency
      type: decimal

    auxiliaryInfo:
      script: |
        groovy:
          import groovy.json.JsonBuilder
          def info = [:]
          if (ctx.CostCenter) info.costCenter = ctx.CostCenter
          if (ctx.ProfitCenter) info.profitCenter = ctx.ProfitCenter
          return new JsonBuilder(info).toString()

attachments:
  source: to_Attachment
  item:
    fileName:
      field: FileName

    fileType:
      script: |
        groovy:
          def name = ctx.FileName ?: ''
          def dot = name.lastIndexOf('.')
          return dot > 0 ? name.substring(dot + 1) : 'pdf'

    fileSize:
      field: FileSize
      type: long

    downloadUrl:
      field: URL
```

**Step 2: Commit**

```bash
git add src/main/resources/erp-mapping/sap-mapping.yml
git commit -m "feat(sap): add SAP OData field mapping configuration"
```

---

### Task 4: 实现 SapErpAdapter 主适配器

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/SapErpAdapter.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/adapter/SapErpAdapterTest.java`

**Step 1-3: Implementation** (省略详细代码，结构同 YonSuiteErpAdapter)

**Commit:**

```bash
git add src/main/java/com/nexusarchive/integration/erp/adapter/SapErpAdapter.java
git commit -m "feat(sap): add SapErpAdapter with OData support"
```

---

## Phase 2: 前端 - SAP 接口类型展示

### Task 5: 前端 SAP 接口类型常量

**Files:**
- Create: `src/constants/sapInterfaces.ts`

**Step 1: Create constant**

```typescript
export const SAP_INTERFACE_TYPES = [
  {
    key: 'odata',
    name: 'OData 服务',
    description: '现代化 REST 风格集成，基于 HTTP/JSON',
    status: 'implemented' as const,
    icon: 'Cloud',
  },
  {
    key: 'rfc_bapi',
    name: 'RFC/BAPI',
    description: '传统 SAP 集成方式，需要 SAP Java Connector',
    status: 'reserved' as const,
    icon: 'Server',
  },
  {
    key: 'idoc',
    name: 'IDoc',
    description: '异步批量数据交换，类似 EDI 格式',
    status: 'reserved' as const,
    icon: 'FileText',
  },
  {
    key: 'gateway',
    name: 'SAP Gateway',
    description: '自定义 OData 服务构建',
    status: 'reserved' as const,
    icon: 'Settings',
  },
] as const;
```

---

### Task 6: 前端 SAP 接口类型展示组件

**Files:**
- Create: `src/components/settings/integration/components/SapInterfaceTypes.tsx`

---

## Phase 3: 集成测试与文档

### Task 7-8: 测试与文档更新

---

## 实施检查清单

### 后端检查
- [ ] SapJournalEntryDto 系列 DTO 序列化正确
- [ ] SapHttpClient OData 查询 URL 构建正确
- [ ] sap-mapping.yml 配置加载成功
- [ ] ErpMapper 正确转换 SAP 响应
- [ ] SapErpAdapter 实现所有 ErpAdapter 方法

### 前端检查
- [ ] SAP 适配器显示在集成列表
- [ ] 四种接口类型正确展示

### 文档检查
- [ ] ERP 框架文档已更新
- [ ] SAP 集成指南已创建

---

## 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-09 | 1.0.0 | 初始实施计划 |
