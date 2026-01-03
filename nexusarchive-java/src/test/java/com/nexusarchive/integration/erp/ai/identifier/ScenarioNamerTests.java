// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/identifier/ScenarioNamerTests.java
// Input: JUnit 5, OpenApiDefinition fixture
// Output: Test verification for ScenarioNamer
// Pos: AI 模块 - 场景命名生成器单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScenarioNamer 单元测试
 * <p>
 * 测试场景命名生成器的各种场景
 * </p>
 */
@DisplayName("ScenarioNamer 测试")
class ScenarioNamerTests {

    private final ScenarioNamer namer = new ScenarioNamer();

    @Test
    @DisplayName("测试生成销售出库单场景名称")
    void testGenerateSalesOutScenario() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
                .path("/yonbip/digitalModel/salesout/doc/query")
                .method("POST")
                .operationId("querySalesOutDoc")
                .summary("查询销售出库单列表")
                .description("分页查询销售出库单据信息")
                .build();

        // When
        ScenarioName result = namer.generateScenarioName(definition);

        // Then
        assertNotNull(result, "ScenarioName should not be null");
        assertEquals("SALESOUT_DOC_QUERY", result.scenarioKey(),
                "Scenario key should be SALESOUT_DOC_QUERY");
        assertEquals("查询销售出库单列表", result.displayName(),
                "Display name should match summary");
        assertTrue(result.description().contains("AI 自动识别"),
                "Description should contain AI auto-identification prefix");
        assertTrue(result.description().contains("/yonbip/digitalModel/salesout/doc/query"),
                "Description should contain the API path");
        assertTrue(result.description().contains("POST"),
                "Description should contain HTTP method");
    }

    @Test
    @DisplayName("测试生成收据单场景 - 降级到 operationId")
    void testGenerateReceiptScenario() {
        // Given - No summary, should fall back to operationId
        OpenApiDefinition definition = OpenApiDefinition.builder()
                .path("/yonbip/digitalModel/receipt/list")
                .method("GET")
                .operationId("getReceiptList")
                .summary(null) // No summary
                .build();

        // When
        ScenarioName result = namer.generateScenarioName(definition);

        // Then
        assertNotNull(result, "ScenarioName should not be null");
        assertEquals("RECEIPT_LIST", result.scenarioKey(),
                "Scenario key should be RECEIPT_LIST");
        assertEquals("Get Receipt List", result.displayName(),
                "Display name should be converted from operationId");
        assertTrue(result.description().contains("AI 自动识别"),
                "Description should contain AI auto-identification prefix");
    }

    @Test
    @DisplayName("测试处理路径参数 - 移除 {id}")
    void testHandlePathParameters() {
        // Given - Path contains parameters
        OpenApiDefinition definition = OpenApiDefinition.builder()
                .path("/yonbip/digitalModel/invoice/{id}/detail")
                .method("GET")
                .operationId("getInvoiceDetail")
                .summary("获取发票详情")
                .build();

        // When
        ScenarioName result = namer.generateScenarioName(definition);

        // Then
        assertNotNull(result, "ScenarioName should not be null");
        assertEquals("INVOICE_DETAIL", result.scenarioKey(),
                "Scenario key should be INVOICE_DETAIL without {id}");
        assertFalse(result.scenarioKey().contains("{"),
                "Scenario key should not contain {");
        assertFalse(result.scenarioKey().contains("}"),
                "Scenario key should not contain }");
    }

    @Test
    @DisplayName("测试复杂路径 - 提取最后2-3段")
    void testComplexPathSegments() {
        // Given - Long path
        OpenApiDefinition definition = OpenApiDefinition.builder()
                .path("/yonbip/digitalModel/srm/purchase/order/approve")
                .method("POST")
                .summary("采购订单审批")
                .build();

        // When
        ScenarioName result = namer.generateScenarioName(definition);

        // Then
        assertNotNull(result, "ScenarioName should not be null");
        assertEquals("PURCHASE_ORDER_APPROVE", result.scenarioKey(),
                "Scenario key should extract last 2-3 segments");
    }

    @Test
    @DisplayName("测试空摘要时使用路径作为降级方案")
    void testFallbackToPath() {
        // Given - No summary and no operationId
        OpenApiDefinition definition = OpenApiDefinition.builder()
                .path("/yonbip/digitalModel/inventory/check")
                .method("POST")
                .operationId(null)
                .summary(null)
                .build();

        // When
        ScenarioName result = namer.generateScenarioName(definition);

        // Then
        assertNotNull(result, "ScenarioName should not be null");
        assertEquals("INVENTORY_CHECK", result.scenarioKey(),
                "Scenario key should be INVENTORY_CHECK");
        assertNotNull(result.displayName(), "Display name should have fallback value");
    }

    @Test
    @DisplayName("测试特殊字符清理")
    void testCleanSpecialCharacters() {
        // Given - Path with special characters
        OpenApiDefinition definition = OpenApiDefinition.builder()
                .path("/yonbip/digitalModel/sales-order/list")
                .method("GET")
                .summary("查询销售订单")
                .build();

        // When
        ScenarioName result = namer.generateScenarioName(definition);

        // Then
        assertNotNull(result, "ScenarioName should not be null");
        assertEquals("SALESORDER_LIST", result.scenarioKey(),
                "Scenario key should clean hyphens and special characters");
        assertFalse(result.scenarioKey().contains("-"),
                "Scenario key should not contain hyphens");
    }

    @Test
    @DisplayName("测试描述格式")
    void testDescriptionFormat() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
                .path("/api/v1/test/endpoint")
                .method("DELETE")
                .summary("删除测试数据")
                .build();

        // When
        ScenarioName result = namer.generateScenarioName(definition);

        // Then
        String expectedDescription = "AI 自动识别: /api/v1/test/endpoint (DELETE)";
        assertEquals(expectedDescription, result.description(),
                "Description should follow format: AI 自动识别: {path} ({method})");
    }
}
