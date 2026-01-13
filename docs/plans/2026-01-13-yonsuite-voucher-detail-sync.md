# YonSuite Voucher Detail Sync & YAML Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复 `yonsuite-mapping.yml` 解析失败并确保 VOUCHER_SYNC 能在“列表→详情→回退”流程中拿到分录金额。

**Architecture:** 保持现有 ErpAdapter + ErpMapper + YAML 映射不变，新增“详情转列表结构”的适配器；同步时使用列表记录的 `header.id` 拉详情，详情失败回退列表；YAML 脚本改为单行且统一用 `ctx` 变量以保证 SnakeYAML 与 Groovy 执行稳定。

**Tech Stack:** Java 17, Spring Boot, SnakeYAML, Groovy, JUnit 5, AssertJ

---

### Task 0: Workspace Setup (Prep)

**Files:**
- None

**Step 1: Create a dedicated worktree**

Run: `git worktree add ../nexusarchive-yonsuite-voucher-detail -b yonsuite-voucher-detail`
Expected: New worktree created without errors.

**Step 2: Verify clean status**

Run: `git -C ../nexusarchive-yonsuite-voucher-detail status -sb`
Expected: Clean worktree on branch `yonsuite-voucher-detail`.

---

### Task 1: Fix YAML Parse Regression + Script Mapping

**Files:**
- Modify: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/MappingConfigLoaderTest.java`
- Modify: `nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml`

**Step 1: Write the failing test**

Add this test method to `MappingConfigLoaderTest.java`:

```java
@Test
@DisplayName("YonSuite 映射应能加载含脚本的字段")
void shouldLoadYonSuiteMappingWithScripts() throws IOException {
    MappingConfig config = loader.loadMapping("yonsuite");

    FieldMapping direction = config.getEntries().getItem().get("direction");
    FieldMapping amount = config.getEntries().getItem().get("amount");

    assertThat(direction).isNotNull();
    assertThat(direction.getScript()).isNotBlank();
    assertThat(amount).isNotNull();
    assertThat(amount.getScript()).isNotBlank();
}
```

**Step 2: Run test to verify it fails**

Run: `cd nexusarchive-java && mvn -q -Dtest=MappingConfigLoaderTest test`
Expected: FAIL with “Failed to parse mapping config… No writable property 'script'”.

**Step 3: Rewrite `yonsuite-mapping.yml` with single-line scripts (no `|`)**

Replace file content with:

```yaml
# YonSuite 凭证映射配置
# Source: YonSuite API (列表 API 结构)
# Target: AccountingSipDto (DA/T 94-2022 会计档案 SIP)

sourceSystem: yonsuite
targetModel: AccountingSipDto
version: 2.0.1

# 凭证头字段映射 (VoucherHeadDto)
headerMappings:
  accountPeriod:
    field: header.period

  voucherNumber:
    field: header.displayname

  voucherDate:
    field: header.maketime

  attachmentCount:
    script: "def qty = ctx.header?.attachmentQuantity; qty ? Integer.parseInt(qty.toString()) : 0"

  issuer:
    field: header.maker.name

  reviewer:
    field: header.auditor.name

  poster:
    field: header.tallyman.name

  remark:
    field: header.description

  totalAmount:
    field: header.totalDebitOrg

# 分录映射 (VoucherEntryDto)
entries:
  source: body
  item:
    lineNo:
      field: recordnumber

    summary:
      field: description

    subjectCode:
      field: accsubject.code

    subjectName:
      field: accsubject.name

    direction:
      script: "def debit = ctx.debitOrg ?: 0; def credit = ctx.creditOrg ?: 0; debit > 0 ? com.nexusarchive.common.enums.DirectionType.DEBIT : (credit > 0 ? com.nexusarchive.common.enums.DirectionType.CREDIT : null)"

    amount:
      script: "def debit = ctx.debitOrg ?: 0; def credit = ctx.creditOrg ?: 0; debit > 0 ? debit : credit"

# 附件映射 (AttachmentDto)
attachments:
  source: header.attachments
  item:
    fileName:
      field: fileName
    fileType:
      field: fileExtension
    fileSize:
      field: fileSize
    downloadUrl:
      field: url
    attachmentId:
      field: id
```

**Step 4: Run test to verify it passes**

Run: `cd nexusarchive-java && mvn -q -Dtest=MappingConfigLoaderTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml \
        nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/MappingConfigLoaderTest.java
git commit -m "fix(erp): make yonsuite mapping scripts parseable"
```

---

### Task 2: Add Detail Adapter + Use Voucher ID for Detail Sync

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherDetailAdapter.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherClient.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherDetailAdapterTest.java`

**Step 1: Write the failing test**

```java
package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.yonsuite.dto.YonVoucherDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YonSuiteVoucherDetailAdapterTest {

    @Test
    void shouldWrapDetailAsVoucherRecord() {
        YonVoucherDetailResponse.VoucherDetail detail = new YonVoucherDetailResponse.VoucherDetail();
        detail.setId("V001");
        detail.setDisplayName("记-001");
        detail.setPeriodUnion("2025-12");
        detail.setMakeTime("2025-12-31 10:00:00");

        YonVoucherDetailResponse.RefObject makerObj = new YonVoucherDetailResponse.RefObject();
        makerObj.setName("张三");
        detail.setMakerObj(makerObj);

        YonVoucherDetailResponse.VoucherBodyDetail body = new YonVoucherDetailResponse.VoucherBodyDetail();
        body.setRecordNumber(1);
        body.setDescription("测试摘要");
        body.setDebitOrg(new BigDecimal("100.00"));
        body.setCreditOrg(BigDecimal.ZERO);
        body.setAccSubject("1001");
        body.setAccSubjectVid("1001");
        detail.setBodies(List.of(body));

        YonVoucherListResponse.VoucherRecord record =
            YonSuiteVoucherDetailAdapter.toVoucherRecord(detail);

        assertThat(record.getHeader().getId()).isEqualTo("V001");
        assertThat(record.getHeader().getDisplayname()).isEqualTo("记-001");
        assertThat(record.getBody()).hasSize(1);
        assertThat(record.getBody().get(0).getRecordnumber()).isEqualTo(1);
        assertThat(record.getBody().get(0).getDebitOrg()).isEqualByComparingTo("100.00");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd nexusarchive-java && mvn -q -Dtest=YonSuiteVoucherDetailAdapterTest test`
Expected: FAIL (class not found).

**Step 3: Implement adapter class**

```java
package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.yonsuite.dto.YonVoucherDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;

import java.util.ArrayList;
import java.util.List;

public final class YonSuiteVoucherDetailAdapter {

    private YonSuiteVoucherDetailAdapter() {}

    public static YonVoucherListResponse.VoucherRecord toVoucherRecord(
            YonVoucherDetailResponse.VoucherDetail detail) {
        YonVoucherListResponse.VoucherRecord record = new YonVoucherListResponse.VoucherRecord();
        record.setHeader(toHeader(detail));
        record.setBody(toBody(detail));
        return record;
    }

    private static YonVoucherListResponse.VoucherHeader toHeader(
            YonVoucherDetailResponse.VoucherDetail detail) {
        YonVoucherListResponse.VoucherHeader header = new YonVoucherListResponse.VoucherHeader();
        header.setId(detail.getId());
        header.setDisplayname(detail.getDisplayName());
        header.setDescription(detail.getDescription());
        header.setPeriod(detail.getPeriodUnion());
        header.setMaketime(detail.getMakeTime());
        header.setVoucherstatus(detail.getVoucherStatus());
        header.setTotalDebitOrg(detail.getTotalDebitOrg());
        header.setTotalCreditOrg(detail.getTotalCreditOrg());

        if (detail.getMakerObj() != null) {
            header.setMaker(toRef(detail.getMakerObj()));
        }
        if (detail.getAuditorObj() != null) {
            header.setAuditor(toRef(detail.getAuditorObj()));
        }
        if (detail.getTallyManObj() != null) {
            header.setTallyman(toRef(detail.getTallyManObj()));
        }

        if (detail.getAccBookObj() != null) {
            YonVoucherListResponse.AccBook accBook = new YonVoucherListResponse.AccBook();
            accBook.setId(detail.getAccBookObj().getId());
            accBook.setCode(detail.getAccBookObj().getCode());
            accBook.setName(detail.getAccBookObj().getName());
            header.setAccbook(accBook);
        }

        if (detail.getVoucherTypeObj() != null) {
            YonVoucherListResponse.VoucherType voucherType = new YonVoucherListResponse.VoucherType();
            voucherType.setId(detail.getVoucherTypeObj().getId());
            voucherType.setCode(detail.getVoucherTypeObj().getCode());
            voucherType.setName(detail.getVoucherTypeObj().getName());
            header.setVouchertype(voucherType);
        }

        return header;
    }

    private static List<YonVoucherListResponse.VoucherBody> toBody(
            YonVoucherDetailResponse.VoucherDetail detail) {
        List<YonVoucherListResponse.VoucherBody> bodies = new ArrayList<>();
        if (detail.getBodies() == null) {
            return bodies;
        }

        for (YonVoucherDetailResponse.VoucherBodyDetail bodyDetail : detail.getBodies()) {
            YonVoucherListResponse.VoucherBody body = new YonVoucherListResponse.VoucherBody();
            body.setId(bodyDetail.getId());
            body.setVoucherid(detail.getId());
            body.setRecordnumber(bodyDetail.getRecordNumber());
            body.setDescription(bodyDetail.getDescription());
            body.setDebitOrg(bodyDetail.getDebitOrg());
            body.setCreditOrg(bodyDetail.getCreditOrg());

            if (bodyDetail.getAccSubject() != null || bodyDetail.getAccSubjectVid() != null) {
                YonVoucherListResponse.AccSubject acc = new YonVoucherListResponse.AccSubject();
                acc.setCode(bodyDetail.getAccSubjectVid() != null ? bodyDetail.getAccSubjectVid() : bodyDetail.getAccSubject());
                acc.setName(bodyDetail.getAccSubject() != null ? bodyDetail.getAccSubject() : bodyDetail.getAccSubjectVid());
                body.setAccsubject(acc);
            }

            if (bodyDetail.getCurrency() != null) {
                YonVoucherListResponse.Currency currency = new YonVoucherListResponse.Currency();
                currency.setCode(bodyDetail.getCurrency());
                currency.setName(bodyDetail.getCurrency());
                body.setCurrency(currency);
            }

            bodies.add(body);
        }

        return bodies;
    }

    private static YonVoucherListResponse.RefObject toRef(
            YonVoucherDetailResponse.RefObject ref) {
        YonVoucherListResponse.RefObject obj = new YonVoucherListResponse.RefObject();
        obj.setId(ref.getId());
        obj.setCode(ref.getCode());
        obj.setName(ref.getName());
        return obj;
    }
}
```

**Step 4: Update `YonSuiteVoucherClient` to use voucherId + adapter**

Replace `syncVouchers` and `getVoucherDetail` with:

```java
public List<AccountingSipDto> syncVouchers(String accessToken, String accbookCode,
                                      LocalDate startDate, LocalDate endDate, ErpConfig config) {
    try {
        YonVoucherListRequest request = buildVoucherListRequest(accbookCode, startDate, endDate);
        YonVoucherListResponse response = yonSuiteClient.queryVouchers(accessToken, request);

        if (!"200".equals(response.getCode()) || response.getData() == null) {
            log.warn("YonSuite 同步凭证失败 (组织: {}): {}", accbookCode, response.getMessage());
            return Collections.emptyList();
        }

        List<AccountingSipDto> enrichedVouchers = new ArrayList<>();
        if (response.getData().getRecordList() == null) {
            return enrichedVouchers;
        }

        for (var record : response.getData().getRecordList()) {
            AccountingSipDto listDto = mapRecordToSipDto(record, config);
            String voucherId = record.getHeader() != null ? record.getHeader().getId() : null;

            if (voucherId == null || voucherId.isBlank()) {
                enrichedVouchers.add(listDto);
                continue;
            }

            AccountingSipDto detailDto = getVoucherDetail(accessToken, voucherId, config);
            if (detailDto != null && detailDto.getEntries() != null && !detailDto.getEntries().isEmpty()) {
                enrichedVouchers.add(detailDto);
            } else {
                enrichedVouchers.add(listDto);
            }
        }

        return enrichedVouchers;
    } catch (Exception e) {
        log.error("YonSuite 同步凭证异常 (组织: {})", accbookCode, e);
        return Collections.emptyList();
    }
}

public AccountingSipDto getVoucherDetail(String accessToken, String voucherId, ErpConfig config) {
    try {
        YonVoucherDetailResponse response = yonSuiteClient.queryVoucherById(accessToken, voucherId);
        if (response == null || response.getData() == null) {
            return null;
        }
        YonVoucherListResponse.VoucherRecord wrapped =
            YonSuiteVoucherDetailAdapter.toVoucherRecord(response.getData());
        return erpMapper.mapToSipDto(wrapped, "yonsuite", config);
    } catch (Exception e) {
        log.error("YonSuite getVoucherDetail error", e);
        return null;
    }
}

private AccountingSipDto mapRecordToSipDto(YonVoucherListResponse.VoucherRecord record,
                                           ErpConfig config) {
    return erpMapper.mapToSipDto(record, "yonsuite", config);
}
```

**Step 5: Run tests to verify they pass**

Run: `cd nexusarchive-java && mvn -q -Dtest=MappingConfigLoaderTest,YonSuiteVoucherDetailAdapterTest test`
Expected: PASS.

**Step 6: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherDetailAdapter.java \
        nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherClient.java \
        nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherDetailAdapterTest.java
git commit -m "feat(erp): sync YonSuite voucher details using voucherId"
```

---

## Execution Handoff

Plan complete and saved to `docs/plans/2026-01-13-yonsuite-voucher-detail-sync.md`. Two execution options:

1. Subagent-Driven (this session) - I dispatch fresh subagent per task, review between tasks, fast iteration
2. Parallel Session (separate) - Open new session with executing-plans, batch execution with checkpoints

Which approach?
