// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/mapper/BusinessSemanticMapperTest.java
// Input: -
// Output: Test results
// Pos: AI 模块 - 业务语义映射器测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.mapper;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessSemanticMapper 测试
 */
class BusinessSemanticMapperTest {

    private BusinessSemanticMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BusinessSemanticMapper();
    }

    @Test
    void shouldMapVoucherListApiToVoucherSync() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .method("get")
            .operationId("listVouchers")
            .summary("获取凭证列表")
            .tags(List.of("vouchers"))
            .build();

        // When
        var mapping = mapper.mapToScenario(definition);

        // Then
        assertEquals(StandardScenario.VOUCHER_SYNC, mapping.getScenario());
        assertEquals(ApiIntent.OperationType.QUERY, mapping.getIntent().getOperationType());
        assertEquals(ApiIntent.BusinessObject.ACCOUNTING_VOUCHER, mapping.getIntent().getBusinessObject());
    }

    @Test
    void shouldMapInvoiceApiToInvoiceSync() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/invoices")
            .method("get")
            .operationId("syncInvoices")
            .summary("同步发票数据")
            .tags(List.of("invoice"))
            .build();

        // When
        var mapping = mapper.mapToScenario(definition);

        // Then
        assertEquals(StandardScenario.INVOICE_SYNC, mapping.getScenario());
        assertEquals(ApiIntent.BusinessObject.INVOICE, mapping.getIntent().getBusinessObject());
    }

    @Test
    void shouldMapUnknownApiToUnknown() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/unknown")
            .method("get")
            .operationId("getUnknownData")
            .summary("获取未知数据")
            .tags(List.of("unknown"))
            .build();

        // When
        var mapping = mapper.mapToScenario(definition);

        // Then
        assertEquals(StandardScenario.UNKNOWN, mapping.getScenario());
        assertEquals(ApiIntent.BusinessObject.UNKNOWN, mapping.getIntent().getBusinessObject());
    }

    @Test
    void shouldDetectScheduledTimingWithDateRange() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .method("get")
            .operationId("listVouchersByDateRange")
            .summary("按日期范围获取凭证")
            .parameters(List.of(
                OpenApiDefinition.ParameterDefinition.builder()
                    .name("startDate")
                    .in("query")
                    .build(),
                OpenApiDefinition.ParameterDefinition.builder()
                    .name("endDate")
                    .in("query")
                    .build()
            ))
            .tags(List.of("vouchers"))
            .build();

        // When
        var mapping = mapper.mapToScenario(definition);

        // Then
        assertEquals(ApiIntent.TriggerTiming.SCHEDULED, mapping.getIntent().getTriggerTiming());
    }
}
