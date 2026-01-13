# YonSuite 凭证同步 YAML 映射修复实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 修复 YonSuite 凭证同步中的 YAML 映射配置解析失败问题，使凭证数据能够正确从 YonSuite API 映射到 AccountingSipDto。

**架构：**
- 问题根源：SnakeYAML 无法解析 `FieldMapping.script` 属性（"No writable property 'script'"）
- 解决方案：确保 `FieldMapping` 类正确生成 setter 方法，并修复 YAML 多行脚本格式
- 测试策略：编写单元测试验证 YAML 解析和字段映射

**技术栈：**
- SnakeYAML (YAML 解析)
- Lombok (@Data/@Setter 注解)
- Groovy (脚本引擎)
- JUnit 5 (测试)

---

## 诊断摘要

**错误信息：**
```
No writable property 'script' on class: FieldMapping
Failed to parse mapping config: erp-mapping/yonsuite-mapping.yml
```

**影响：**
- 所有 YonSuite 凭证映射失败
- 同步返回 `total_count=0`
- 数据库中无新增记录

**根本原因：**
1. `FieldMapping` 类的 Lombok 注解配置可能不完整
2. YAML 多行脚本格式（`|`）与 SnakeYAML Constructor 不兼容

---

## Task 1: 验证并修复 FieldMapping 类的 Lombok 配置

**文件：**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/FieldMapping.java`
- 测试: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/FieldMappingTest.java`

**Step 1: 编写测试验证 FieldMapping 可序列化/反序列化**

```java
package com.nexusarchive.integration.erp.mapping;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FieldMappingTest {

    @Test
    public void testFieldMappingHasScriptSetter() {
        // 验证 Lombok 生成了 setter 方法
        FieldMapping mapping = new FieldMapping();
        assertDoesNotThrow(() -> mapping.setScript("test script"));
        assertEquals("test script", mapping.getScript());
    }

    @Test
    public void testFieldMappingIsScriptMethod() {
        FieldMapping mapping = new FieldMapping();
        assertFalse(mapping.isScript()); // null script

        mapping.setScript("return 1;");
        assertTrue(mapping.isScript()); // has script
    }

    @Test
    public void testFieldMappingBuilder() {
        FieldMapping mapping = FieldMapping.builder()
            .field("testField")
            .script("test script")
            .type("string")
            .build();

        assertEquals("testField", mapping.getField());
        assertEquals("test script", mapping.getScript());
        assertEquals("string", mapping.getType());
    }
}
```

**Step 2: 运行测试**

```bash
cd nexusarchive-java
mvn test -Dtest=FieldMappingTest -q
```

**预期结果：**
- 如果测试通过 → Lombok 配置正确，跳到 Task 2
- 如果测试失败 → 执行 Step 3

**Step 3: 检查并修复 FieldMapping.java**

确保 `FieldMapping.java` 包含所有必要的 Lombok 注解：

```java
package com.nexusarchive.integration.erp.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 字段映射配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)  // 支持链式调用
public class FieldMapping {
    private String field;
    private String script;
    private String type;
    private String format;

    public boolean isScript() {
        return script != null && !script.isBlank();
    }
}
```

**Step 4: 重新编译并运行测试**

```bash
mvn clean compile test -Dtest=FieldMappingTest -q
```

**预期：** PASS

**Step 5: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/FieldMapping.java
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/FieldMappingTest.java
git commit -m "fix(erp): ensure FieldMapping has proper Lombok setters for YAML deserialization"
```

---

## Task 2: 修复 yonsuite-mapping.yml 中的多行脚本格式

**文件：**
- 修改: `nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml`

**Step 1: 备份当前配置**

```bash
cp nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml \
   nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml.backup
```

**Step 2: 重写配置文件（使用单行脚本格式）**

完全替换 `yonsuite-mapping.yml` 内容：

```yaml
# YonSuite 凭证映射配置
# Source: YonSuite API (列表 API 结构)
# Target: AccountingSipDto (DA/T 94-2022 会计档案 SIP)

sourceSystem: yonsuite
targetModel: AccountingSipDto
version: 2.0.0

# 凭证头字段映射 (VoucherHeadDto)
headerMappings:
  accountPeriod:
    field: header.period

  voucherNumber:
    field: header.displayname

  voucherDate:
    field: header.maketime

  attachmentCount:
    script: ctx['header']['attachmentQuantity'] ? Integer.parseInt(ctx['header']['attachmentQuantity'].toString()) : 0

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
      script: "def debit = it['debitOrg'] ?: 0; def credit = it['creditOrg'] ?: 0; if (debit != null && debit > 0) com.nexusarchive.common.enums.DirectionType.DEBIT else if (credit != null && credit > 0) com.nexusarchive.common.enums.DirectionType.CREDIT else null"

    amount:
      script: "def debit = it['debitOrg'] ?: 0; def credit = it['creditOrg'] ?: 0; (debit != null && debit > 0) ? debit : credit"

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

**关键变更：**
- 将多行脚本改为单行
- `direction` 脚本：一行三元表达式
- `amount` 脚本：一行三元表达式
- `attachmentCount` 脚本：一行三元表达式

**Step 3: 验证 YAML 语法**

```bash
# 使用 Python 验证 YAML 语法
python3 -c "
import yaml
with open('nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml') as f:
    config = yaml.safe_load(f)
    print('YAML 语法有效')
    print(f'sourceSystem: {config.get(\"sourceSystem\")}')
    print(f'headerMappings count: {len(config.get(\"headerMappings\", {}))}')
"
```

**预期输出：**
```
YAML 语法有效
sourceSystem: yonsuite
headerMappings count: 9
```

**Step 4: 复制到 target 目录**

```bash
cp nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml \
   nexusarchive-java/target/classes/erp-mapping/yonsuite-mapping.yml
```

**Step 5: 提交**

```bash
git add nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml
git commit -m "fix(erp): convert multiline Groovy scripts to single-line for YAML parsing"
```

---

## Task 3: 编写 YAML 加载集成测试

**文件：**
- 创建: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/MappingConfigLoaderTest.java`

**Step 1: 编写测试类**

```java
package com.nexusarchive.integration.erp.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MappingConfigLoaderTest {

    @Autowired
    private MappingConfigLoader configLoader;

    @Test
    public void testLoadYonSuiteMapping() throws Exception {
        MappingConfig config = configLoader.loadMapping("yonsuite");

        assertNotNull(config);
        assertEquals("yonsuite", config.getSourceSystem());
        assertEquals("AccountingSipDto", config.getTargetModel());
        assertEquals("2.0.0", config.getVersion());

        // 验证 headerMappings
        assertNotNull(config.getHeaderMappings());
        assertFalse(config.getHeaderMappings().isEmpty());

        // 验证 entries 映射
        assertNotNull(config.getEntries());
        assertEquals("body", config.getEntries().getSource());
        assertNotNull(config.getEntries().getItem());

        // 验证 attachments 映射
        assertNotNull(config.getAttachments());
    }

    @Test
    public void testYonSuiteDirectionScriptMapping() throws Exception {
        MappingConfig config = configLoader.loadMapping("yonsuite");

        FieldMapping directionMapping = config.getEntries().getItem().get("direction");
        assertNotNull(directionMapping);
        assertTrue(directionMapping.isScript());
        assertNotNull(directionMapping.getScript());
        assertTrue(directionMapping.getScript().contains("DirectionType"));
    }

    @Test
    public void testYonSuiteAmountScriptMapping() throws Exception {
        MappingConfig config = configLoader.loadMapping("yonsuite");

        FieldMapping amountMapping = config.getEntries().getItem().get("amount");
        assertNotNull(amountMapping);
        assertTrue(amountMapping.isScript());
        assertNotNull(amountMapping.getScript());
        assertTrue(amountMapping.getScript().contains("debitOrg"));
    }

    @Test
    public void testLoadKingdeeMapping() throws Exception {
        MappingConfig config = configLoader.loadMapping("kingdee");

        assertNotNull(config);
        assertEquals("kingdee", config.getSourceSystem());
    }
}
```

**Step 2: 运行测试**

```bash
cd nexusarchive-java
mvn test -Dtest=MappingConfigLoaderTest -q
```

**预期：** PASS

**如果失败：** 检查日志中的 YAML 解析错误，返回 Task 2 修复格式

**Step 3: 提交**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/MappingConfigLoaderTest.java
git commit -m "test(erp): add integration test for YAML mapping config loading"
```

---

## Task 4: 编写端到端映射测试

**文件：**
- 创建: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/YonSuiteMappingE2ETest.java`

**Step 1: 编写 E2E 测试**

```java
package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.common.enums.DirectionType;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class YonSuiteMappingE2ETest {

    @Autowired
    private DefaultErpMapper erpMapper;

    @Autowired
    private MappingConfigLoader configLoader;

    private ErpConfig mockConfig;

    @BeforeEach
    public void setUp() {
        mockConfig = new ErpConfig();
        mockConfig.setId("1");
        mockConfig.setName("Test YonSuite");
        mockConfig.setAppKey("test-key");
        mockConfig.setAppSecret("test-secret");
    }

    @Test
    public void testMapYonSuiteResponseToSipDto() throws Exception {
        // 验证配置可加载
        MappingConfig config = configLoader.loadMapping("yonsuite");
        assertNotNull(config);

        // 创建模拟 YonSuite 响应
        YonVoucherListResponse.VoucherRecord record = createMockVoucherRecord();

        // 执行映射
        AccountingSipDto result = erpMapper.mapToSipDto(record, "yonsuite", mockConfig);

        // 验证结果
        assertNotNull(result);
        assertEquals("yonsuite", result.getSourceSystem());

        // 验证 header
        VoucherHeadDto header = result.getHeader();
        assertNotNull(header);
        assertEquals("记-1", header.getVoucherNumber());
        assertEquals(LocalDate.of(2024, 1, 1), header.getVoucherDate());
        assertEquals("2024-01", header.getAccountPeriod());
        assertEquals("测试凭证", header.getRemark());

        // 验证分录
        List<VoucherEntryDto> entries = result.getEntries();
        assertNotNull(entries);
        assertFalse(entries.isEmpty());

        VoucherEntryDto firstEntry = entries.get(0);
        assertEquals(1, firstEntry.getLineNo());
        assertEquals("测试摘要", firstEntry.getSummary());
        assertEquals("1001", firstEntry.getSubjectCode());
        assertEquals("库存现金", firstEntry.getSubjectName());
        assertEquals(DirectionType.DEBIT, firstEntry.getDirection());
        assertEquals(new BigDecimal("1000.00"), firstEntry.getAmount());

        // 验证第二笔分录（贷方）
        VoucherEntryDto secondEntry = entries.get(1);
        assertEquals(DirectionType.CREDIT, secondEntry.getDirection());
        assertEquals(new BigDecimal("1000.00"), secondEntry.getAmount());
    }

    @Test
    public void testDirectionAndAmountScripts() throws Exception {
        YonVoucherListResponse.VoucherRecord record = createMockVoucherRecord();

        AccountingSipDto result = erpMapper.mapToSipDto(record, "yonsuite", mockConfig);
        List<VoucherEntryDto> entries = result.getEntries();

        // 验证 direction 脚本正确转换
        for (VoucherEntryDto entry : entries) {
            assertNotNull(entry.getDirection());
            assertTrue(entry.getDirection() == DirectionType.DEBIT ||
                       entry.getDirection() == DirectionType.CREDIT);

            // 验证 amount 脚本正确取非零值
            assertNotNull(entry.getAmount());
            assertTrue(entry.getAmount().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    /**
     * 创建模拟的 YonSuite 凭证记录
     */
    private YonVoucherListResponse.VoucherRecord createMockVoucherRecord() {
        YonVoucherListResponse.VoucherRecord record = new YonVoucherListResponse.VoucherRecord();

        // Header
        YonVoucherListResponse.VoucherHeader header = new YonVoucherListResponse.VoucherHeader();
        header.setId("test-id-1");
        header.setPeriod("2024-01");
        header.setDisplayname("记-1");
        header.setMaketime("2024-01-01");
        header.setDescription("测试凭证");
        header.setTotalDebitOrg(new BigDecimal("1000.00"));

        YonVoucherListResponse.RefObject maker = new YonVoucherListResponse.RefObject();
        maker.setName("张三");
        header.setMaker(maker);

        record.setHeader(header);

        // Body (分录)
        YonVoucherListResponse.VoucherBody body1 = new YonVoucherListResponse.VoucherBody();
        body1.setRecordnumber(1);
        body1.setDescription("测试摘要");
        body1.setDebitOrg(new BigDecimal("1000.00"));
        body1.setCreditOrg(BigDecimal.ZERO);

        YonVoucherListResponse.AccSubject acc1 = new YonVoucherListResponse.AccSubject();
        acc1.setCode("1001");
        acc1.setName("库存现金");
        body1.setAccsubject(acc1);

        YonVoucherListResponse.VoucherBody body2 = new YonVoucherListResponse.VoucherBody();
        body2.setRecordnumber(2);
        body2.setDescription("测试摘要");
        body2.setDebitOrg(BigDecimal.ZERO);
        body2.setCreditOrg(new BigDecimal("1000.00"));

        YonVoucherListResponse.AccSubject acc2 = new YonVoucherListResponse.AccSubject();
        acc2.setCode("2001");
        acc2.setName("应付账款");
        body2.setAccsubject(acc2);

        record.setBody(List.of(body1, body2));

        return record;
    }
}
```

**Step 2: 运行测试**

```bash
cd nexusarchive-java
mvn test -Dtest=YonSuiteMappingE2ETest -q
```

**预期：** PASS

**如果失败：** 检查具体断言失败原因，调试脚本逻辑

**Step 3: 提交**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/YonSuiteMappingE2ETest.java
git commit -m "test(erp): add E2E test for YonSuite mapping with direction/amount scripts"
```

---

## Task 5: 验证完整同步流程

**文件：**
- 运行时验证，无文件修改

**Step 1: 启动后端服务**

```bash
cd nexusarchive-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

等待服务启动完成（看到 "Started NexusArchiveBackendApplication"）

**Step 2: 触发手动同步**

```bash
curl -X POST "http://localhost:19090/erp/scenario/2/sync" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "startDate": "2023-01-01",
    "endDate": "2025-12-31"
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "sync-2-XXXXXXXXX",
    "status": "SUBMITTED",
    "message": "同步任务已提交"
  }
}
```

**Step 3: 查询同步状态**

```bash
TASK_ID="sync-2-XXXXXXXXX"  # 使用上一步返回的 taskId
curl "http://localhost:19090/erp/scenario/2/sync/status/$TASK_ID" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期：** 等待约 30 秒后状态变为 `SUCCESS`，且 `totalCount > 0`

**Step 4: 检查后端日志**

```bash
tail -f /tmp/nexus-backend.log | grep -i "yonsuite\|mapping\|凭证"
```

**预期看到：**
```
Loading mapping config: erp-mapping/yonsuite-mapping.yml
Loaded mapping config for yonsuite, version: 2.0.0
凭证映射成功: yonId=XXX, voucherNo=记-1
YonSuite 凭证同步完成: 总数=X, 成功=Y
```

**不应该看到：**
```
Failed to parse mapping config
凭证映射失败: Failed to load mapping config
```

**Step 5: 验证数据库**

```bash
docker exec nexus-db psql -U postgres -d nexusarchive -c \
  "SELECT COUNT(*) FROM arc_file_content WHERE voucher_type = 'ACCOUNTING_VOUCHER';"
```

**预期：** count > 0（之前为 0）

---

## Task 6: 清理测试数据

**文件：**
- 无文件修改，数据库操作

**Step 1: 删除测试同步的凭证**

```bash
docker exec nexus-db psql -U postgres -d nexusarchive -c \
  "DELETE FROM arc_file_content WHERE source_system = 'yonsuite' AND created_time > '2026-01-13';"
```

**Step 2: 验证删除**

```bash
docker exec nexus-db psql -U postgres -d nexusarchive -c \
  "SELECT COUNT(*) FROM arc_file_content WHERE source_system = 'yonsuite';"
```

**预期：** count = 0

---

## 验收标准

完成所有任务后，以下标准应全部满足：

1. ✅ `FieldMappingTest` 全部通过
2. ✅ `MappingConfigLoaderTest` 全部通过
3. ✅ `YonSuiteMappingE2ETest` 全部通过
4. ✅ 后端日志无 "Failed to parse mapping config" 错误
5. ✅ 同步返回 `totalCount > 0`
6. ✅ 数据库中有新的 YonSuite 凭证记录
7. ✅ 凭证分录包含正确的 `direction` (DEBIT/CREDIT)
8. ✅ 凭证分录包含正确的 `amount` (非零值)

---

## 回滚计划

如果修复失败，执行以下回滚：

```bash
# 恢复备份配置
cp nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml.backup \
   nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml

# 回滚到修复前的提交
git log --oneline -5
git revert <commit-hash>

# 重启服务
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
