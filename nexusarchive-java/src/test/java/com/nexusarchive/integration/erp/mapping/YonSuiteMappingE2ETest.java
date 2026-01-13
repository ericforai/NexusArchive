// Input: JUnit 5, AssertJ, Spring, YonSuite DTO, SIP DTO
// Output: YonSuiteMappingE2ETest 类
// Pos: 集成模块 - ERP 映射端到端测试

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.common.enums.DirectionType;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * YonSuite 映射端到端测试
 * 验证完整的 ERP 数据到 SIP DTO 的映射流程
 */
@DisplayName("YonSuite 映射端到端测试")
@Tag("unit")
class YonSuiteMappingE2ETest {

    private DefaultErpMapper erpMapper;
    private MappingConfigLoader configLoader;
    private GroovyMappingEngine scriptEngine;
    private ErpConfig mockConfig;

    @BeforeEach
    void setUp() {
        // 手动创建依赖，避免 Spring 上下文
        configLoader = new MappingConfigLoader();
        scriptEngine = new GroovyMappingEngine();
        erpMapper = new DefaultErpMapper(configLoader, scriptEngine);

        mockConfig = new ErpConfig();
        mockConfig.setId("1");
        mockConfig.setName("Test YonSuite");
        mockConfig.setAppKey("test-key");
        mockConfig.setAppSecret("test-secret");
    }

    @Test
    @DisplayName("应该成功映射 YonSuite 凭证到 SIP DTO")
    void shouldMapYonSuiteResponseToSipDto() {
        // 验证配置可加载
        assertThatCode(() -> configLoader.loadMapping("yonsuite"))
            .doesNotThrowAnyException();

        // 创建模拟 YonSuite 响应
        YonVoucherListResponse.VoucherRecord record = createMockVoucherRecord();

        // 执行映射
        AccountingSipDto result = erpMapper.mapToSipDto(record, "yonsuite", mockConfig);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getSourceSystem()).isEqualTo("yonsuite");
        assertThat(result.getRequestId()).isNotNull();

        // 验证 header
        VoucherHeadDto header = result.getHeader();
        assertThat(header).isNotNull();
        assertThat(header.getVoucherNumber()).isEqualTo("记-1");
        assertThat(header.getVoucherDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(header.getAccountPeriod()).isEqualTo("2024-01");
        assertThat(header.getRemark()).isEqualTo("测试凭证");

        // 验证分录
        List<VoucherEntryDto> entries = result.getEntries();
        assertThat(entries).isNotEmpty();
        assertThat(entries).hasSize(2);

        VoucherEntryDto firstEntry = entries.get(0);
        assertThat(firstEntry.getLineNo()).isEqualTo(1);
        assertThat(firstEntry.getSummary()).isEqualTo("测试摘要");
        assertThat(firstEntry.getSubjectCode()).isEqualTo("1001");
        assertThat(firstEntry.getSubjectName()).isEqualTo("库存现金");
        assertThat(firstEntry.getDirection()).isEqualTo(DirectionType.DEBIT);
        assertThat(firstEntry.getAmount()).isEqualTo(new BigDecimal("1000.00"));

        // 验证第二笔分录（贷方）
        VoucherEntryDto secondEntry = entries.get(1);
        assertThat(secondEntry.getDirection()).isEqualTo(DirectionType.CREDIT);
        assertThat(secondEntry.getAmount()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("direction 脚本应该正确转换借贷方向")
    void shouldMapDirectionScriptCorrectly() {
        YonVoucherListResponse.VoucherRecord record = createMockVoucherRecord();

        AccountingSipDto result = erpMapper.mapToSipDto(record, "yonsuite", mockConfig);
        List<VoucherEntryDto> entries = result.getEntries();

        // 验证 direction 脚本正确转换
        for (VoucherEntryDto entry : entries) {
            assertThat(entry.getDirection()).isNotNull();
            assertThat(entry.getDirection()).isIn(DirectionType.DEBIT, DirectionType.CREDIT);
        }

        // 验证第一笔是借方
        assertThat(entries.get(0).getDirection()).isEqualTo(DirectionType.DEBIT);

        // 验证第二笔是贷方
        assertThat(entries.get(1).getDirection()).isEqualTo(DirectionType.CREDIT);
    }

    @Test
    @DisplayName("amount 脚本应该正确提取非零金额")
    void shouldMapAmountScriptCorrectly() {
        YonVoucherListResponse.VoucherRecord record = createMockVoucherRecord();

        AccountingSipDto result = erpMapper.mapToSipDto(record, "yonsuite", mockConfig);
        List<VoucherEntryDto> entries = result.getEntries();

        // 验证 amount 脚本正确取非零值
        for (VoucherEntryDto entry : entries) {
            assertThat(entry.getAmount()).isNotNull();
            assertThat(entry.getAmount()).isGreaterThan(BigDecimal.ZERO);
        }

        // 验证金额相等（借贷平衡）
        assertThat(entries.get(0).getAmount()).isEqualByComparingTo("1000.00");
        assertThat(entries.get(1).getAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("应该处理空的附件列表")
    void shouldHandleEmptyAttachments() {
        YonVoucherListResponse.VoucherRecord record = createMockVoucherRecord();

        AccountingSipDto result = erpMapper.mapToSipDto(record, "yonsuite", mockConfig);

        assertThat(result.getAttachments()).isNotNull();
        // 附件可能为空或包含默认值，取决于实现
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
