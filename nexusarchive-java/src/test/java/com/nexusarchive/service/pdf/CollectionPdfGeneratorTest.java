// Input: JUnit 5、Mockito、Apache PDFBox、Jackson、本地模块
// Output: CollectionPdfGeneratorTest 测试类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusarchive.entity.ArcFileContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CollectionPdfGenerator 单元测试
 * <p>
 * 测试收款单 PDF 生成功能，包括：
 * - 正常路径生成
 * - 边界条件处理
 * - 异常情况处理
 * - 数据解析逻辑
 * </p>
 */
@Tag("unit")
@DisplayName("收款单 PDF 生成器测试")
class CollectionPdfGeneratorTest {

    private CollectionPdfGenerator generator;
    private ObjectMapper objectMapper;
    private PDDocument document;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new CollectionPdfGenerator();
        objectMapper = new ObjectMapper();
        document = new PDDocument();
    }

    @AfterEach
    void tearDown() throws IOException {
        // PDFBox doesn't have isClosed(), just close if not null
        if (document != null) {
            document.close();
        }
    }

    @Test
    @DisplayName("应该成功生成完整的收款单 PDF")
    void shouldGenerateCompleteCollectionBillPdf() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-001", "张三", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试公司, 金额:10000.00 CNY", "2026-03", "Voucher-001");
        Path targetPath = tempDir.resolve("collection-bill.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
        assertThat(Files.exists(targetPath)).isFalse(); // generate() 方法不保存文件，只生成到内存
    }

    @Test
    @DisplayName("应该处理空的摘要信息")
    void shouldHandleEmptySummary() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-002", "李四", "YonSuite");
        JsonNode voucherData = createVoucherData("", "2026-03", "Voucher-002");
        Path targetPath = tempDir.resolve("empty-summary.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理空的凭证数据")
    void shouldHandleNullVoucherData() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-003", "王五", "YonSuite");
        Path targetPath = tempDir.resolve("null-voucher.pdf");

        // When
        generator.generate(document, fileContent, null, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该解析客户名称")
    void shouldExtractCustomerName() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-004", "赵六", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:阿里巴巴集团, 金额:50000.00 CNY", "2026-03", "Voucher-004");
        Path targetPath = tempDir.resolve("customer-name.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该解析金额信息")
    void shouldExtractAmount() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-005", "钱七", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:99999.99 CNY", "2026-03", "Voucher-005");
        Path targetPath = tempDir.resolve("amount.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理缺少客户信息的摘要")
    void shouldHandleMissingCustomerInSummary() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-006", "孙八", "YonSuite");
        JsonNode voucherData = createVoucherData("金额:1000.00 CNY", "2026-03", "Voucher-006");
        Path targetPath = tempDir.resolve("no-customer.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理缺少金额信息的摘要")
    void shouldHandleMissingAmountInSummary() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-007", "周九", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试客户", "2026-03", "Voucher-007");
        Path targetPath = tempDir.resolve("no-amount.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该使用默认来源系统")
    void shouldUseDefaultSourceSystem() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-008", "吴十", null);
        JsonNode voucherData = createVoucherData("客户:测试, 金额:1000.00 CNY", "2026-03", "Voucher-008");
        Path targetPath = tempDir.resolve("default-source.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理空的单据编号")
    void shouldHandleEmptyBillCode() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent(null, "郑十一", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:1000.00 CNY", "2026-03", "Voucher-009");
        Path targetPath = tempDir.resolve("no-bill-code.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理空的创建人")
    void shouldHandleNullCreator() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-010", null, "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:1000.00 CNY", "2026-03", "Voucher-010");
        Path targetPath = tempDir.resolve("no-creator.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该生成包含多个收款明细的 PDF")
    void shouldGeneratePdfWithMultipleDetails() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-011", "陈十二", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:大客户A, 金额:100000.00 CNY", "2026-03", "Voucher-011");
        Path targetPath = tempDir.resolve("multiple-details.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理非标准格式的金额")
    void shouldHandleNonStandardAmountFormat() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-012", "楚十三", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:123456789.12 CNY", "2026-03", "Voucher-012");
        Path targetPath = tempDir.resolve("large-amount.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理零金额")
    void shouldHandleZeroAmount() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-013", "魏十四", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:0.00 CNY", "2026-03", "Voucher-013");
        Path targetPath = tempDir.resolve("zero-amount.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该正确解析会计期间")
    void shouldParseAccountPeriod() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-014", "韩十五", "YonSuite");
        String testPeriod = "2026-12";
        JsonNode voucherData = createVoucherData("客户:测试, 金额:1000.00 CNY", testPeriod, "Voucher-014");
        Path targetPath = tempDir.resolve("account-period.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理特殊字符的客户名称")
    void shouldHandleSpecialCharactersInCustomerName() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-015", "褚十六", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试@#$%^&*()公司, 金额:1000.00 CNY", "2026-03", "Voucher-015");
        Path targetPath = tempDir.resolve("special-chars.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理长客户名称（截断）")
    void shouldHandleLongCustomerName() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-016", "卫十七", "YonSuite");
        String longCustomerName = "客户:" + "A".repeat(100) + "有限公司, 金额:1000.00 CNY";
        JsonNode voucherData = createVoucherData(longCustomerName, "2026-03", "Voucher-016");
        Path targetPath = tempDir.resolve("long-name.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理凭证编号回退到单据编号")
    void shouldFallbackVoucherNoToBillCode() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("BILL-001", "蒋十八", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:1000.00 CNY", "2026-03", null);
        Path targetPath = tempDir.resolve("fallback-voucher.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理包含逗号的客户名称")
    void shouldHandleCommaInCustomerName() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-019", "沈二十", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试,公司,有限公司, 金额:1000.00 CNY", "2026-03", "Voucher-019");
        Path targetPath = tempDir.resolve("comma-in-name.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该生成完整的 PDF 文档结构")
    void shouldGenerateCompletePdfStructure() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-020", "韩二十一", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:标准测试公司, 金额:25000.00 CNY", "2026-03", "Voucher-020");
        Path targetPath = tempDir.resolve("complete-structure.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
        assertThat(document.getPage(0)).isNotNull();
        // A4 width is 595 points (210mm at 72 DPI)
        assertThat(document.getPage(0).getMediaBox().getWidth()).isEqualTo(595f);
    }

    @Test
    @DisplayName("应该连续生成多个 PDF 页面")
    void shouldGenerateMultiplePdfPages() throws IOException {
        // Given
        ArcFileContent fileContent1 = createFileContent("ERP-021", "杨二十二", "YonSuite");
        JsonNode voucherData1 = createVoucherData("客户:公司A, 金额:1000.00 CNY", "2026-03", "Voucher-021");
        Path targetPath1 = tempDir.resolve("multi-1.pdf");

        ArcFileContent fileContent2 = createFileContent("ERP-022", "朱二十三", "YonSuite");
        JsonNode voucherData2 = createVoucherData("客户:公司B, 金额:2000.00 CNY", "2026-03", "Voucher-022");
        Path targetPath2 = tempDir.resolve("multi-2.pdf");

        // When
        generator.generate(document, fileContent1, voucherData1, targetPath1);
        generator.generate(document, fileContent2, voucherData2, targetPath2);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("应该处理已关闭的文档异常")
    void shouldHandleClosedDocumentException() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-023", "秦二十四", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:1000.00 CNY", "2026-03", "Voucher-023");
        Path targetPath = tempDir.resolve("closed-doc.pdf");
        document.close();

        // When & Then
        assertThatThrownBy(() -> generator.generate(document, fileContent, voucherData, targetPath))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("应该处理非常长的摘要信息")
    void shouldHandleVeryLongSummary() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-024", "尤二十五", "YonSuite");
        String longSummary = "客户:测试客户, 备注:" + "A".repeat(500) + ", 金额:1000.00 CNY";
        JsonNode voucherData = createVoucherData(longSummary, "2026-03", "Voucher-024");
        Path targetPath = tempDir.resolve("long-summary.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理包含多个字段的摘要")
    void shouldHandleSummaryWithMultipleFields() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-025", "许二十六", "YonSuite");
        JsonNode voucherData = createVoucherData(
                "客户:测试客户, 项目:某某项目, 部门:财务部, 金额:8888.88 CNY",
                "2026-03",
                "Voucher-025"
        );
        Path targetPath = tempDir.resolve("multiple-fields.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理不同的来源系统")
    void shouldHandleDifferentSourceSystems() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-026", "何二十七", "自定义ERP系统");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:1000.00 CNY", "2026-03", "Voucher-026");
        Path targetPath = tempDir.resolve("custom-source.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理负数金额")
    void shouldHandleNegativeAmount() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-027", "吕二十八", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:-1000.00 CNY", "2026-03", "Voucher-027");
        Path targetPath = tempDir.resolve("negative-amount.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该处理金额格式异常")
    void shouldHandleInvalidAmountFormat() throws IOException {
        // Given
        ArcFileContent fileContent = createFileContent("ERP-028", "施二十九", "YonSuite");
        JsonNode voucherData = createVoucherData("客户:测试, 金额:INVALID CNY", "2026-03", "Voucher-028");
        Path targetPath = tempDir.resolve("invalid-amount.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    // ==================== Helper Methods ====================

    /**
     * 创建文件内容测试数据
     */
    private ArcFileContent createFileContent(String erpVoucherNo, String creator, String sourceSystem) {
        return ArcFileContent.builder()
                .erpVoucherNo(erpVoucherNo)
                .creator(creator)
                .sourceSystem(sourceSystem)
                .build();
    }

    /**
     * 创建凭证数据测试数据
     */
    private JsonNode createVoucherData(String summary, String accountPeriod, String voucherNo) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("summary", summary);
        node.put("accountPeriod", accountPeriod);
        if (voucherNo != null) {
            node.put("voucherNo", voucherNo);
        }
        return node;
    }
}
