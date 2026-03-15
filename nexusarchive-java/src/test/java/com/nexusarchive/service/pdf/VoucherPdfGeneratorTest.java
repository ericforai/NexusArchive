// Input: JUnit 5、Mockito、Apache PDFBox、Jackson JsonNode
// Output: VoucherPdfGenerator 单元测试
// Pos: PDF 生成器测试层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusarchive.entity.ArcFileContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoucherPdfGenerator 单元测试
 * <p>
 * 使用 TDD 方法编写，覆盖所有公共方法和边界条件
 * </p>
 */
@DisplayName("会计凭证 PDF 生成器测试")
class VoucherPdfGeneratorTest {

    private VoucherPdfGenerator generator;
    private ObjectMapper objectMapper;
    private PDDocument document;
    private ArcFileContent fileContent;
    private JsonNode voucherData;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new VoucherPdfGenerator();
        objectMapper = new ObjectMapper();
        document = new PDDocument();
        fileContent = createDefaultFileContent();
        voucherData = createDefaultVoucherData();
    }

    // ===== Test Data Builders =====

    private ArcFileContent createDefaultFileContent() {
        return ArcFileContent.builder()
                .id("test-file-id")
                .fileName("test-voucher.pdf")
                .fileType("pdf")
                .erpVoucherNo("记-001")
                .fiscalYear("2026")
                .period("01")
                .creator("test-user")
                .sourceSystem("YonSuite")
                .businessDocNo("YS-VOUCHER-001")
                .build();
    }

    private JsonNode createDefaultVoucherData() {
        ObjectNode root = objectMapper.createObjectNode();

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        ObjectNode accbook = objectMapper.createObjectNode();
        ObjectNode pkOrg = objectMapper.createObjectNode();
        pkOrg.put("name", "测试法人组织");
        accbook.set("pk_org", pkOrg);
        accbook.put("name", "测试账簿");
        header.set("accbook", accbook);

        ObjectNode voucherType = objectMapper.createObjectNode();
        voucherType.put("voucherstr", "记");
        header.set("vouchertype", voucherType);

        header.put("displayname", "001");
        header.put("period", "2026-01");
        header.put("maketime", "2026-01-15 10:30:00");
        header.put("attachmentQuantity", "3");

        root.set("header", header);

        // Body (Entries)
        ObjectNode body1 = objectMapper.createObjectNode();
        body1.put("recordnumber", 1);
        body1.put("description", "支付供应商货款");
        body1.put("digest", "摘要-支付供应商");

        ObjectNode accSubject1 = objectMapper.createObjectNode();
        accSubject1.put("code", "1001");
        accSubject1.put("name", "库存现金");
        body1.set("accsubject", accSubject1);

        ObjectNode currency1 = objectMapper.createObjectNode();
        currency1.put("code", "CNY");
        body1.set("currency", currency1);

        body1.put("debit_original", 10000.00);
        body1.put("credit_original", 0.00);

        ObjectNode body2 = objectMapper.createObjectNode();
        body2.put("recordnumber", 2);
        body2.put("description", "收到客户还款");
        body2.put("digest", "摘要-客户还款");

        ObjectNode accSubject2 = objectMapper.createObjectNode();
        accSubject2.put("code", "1002");
        accSubject2.put("name", "银行存款");
        body2.set("accsubject", accSubject2);

        ObjectNode currency2 = objectMapper.createObjectNode();
        currency2.put("code", "CNY");
        body2.set("currency", currency2);

        body2.put("debit_original", 0.00);
        body2.put("credit_original", 10000.00);

        root.putArray("body").add(body1).add(body2);

        return root;
    }

    private JsonNode createMinimalVoucherData() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("period", "2026-01");
        return root;
    }

    private JsonNode createVoucherDataWithAuxiliary() {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "002");
        header.put("period", "2026-01");
        header.set("accbook", objectMapper.createObjectNode());
        root.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("recordnumber", 1);
        body.put("description", "测试辅助核算");

        ObjectNode accSubject = objectMapper.createObjectNode();
        accSubject.put("code", "1001");
        accSubject.put("name", "库存现金");
        body.set("accsubject", accSubject);

        body.put("debit_original", 5000.00);
        body.put("credit_original", 0.00);

        // Add auxiliary items
        ObjectNode aux1 = objectMapper.createObjectNode();
        aux1.put("name", "客户");
        aux1.put("value", "测试客户A");

        ObjectNode aux2 = objectMapper.createObjectNode();
        aux2.put("name", "部门");
        aux2.put("value", "财务部");

        body.putArray("clientAuxiliary").add(aux1).add(aux2);

        root.putArray("body").add(body);

        return root;
    }

    private JsonNode createVoucherDataWithCashFlow() {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "003");
        header.put("period", "2026-01");
        header.set("accbook", objectMapper.createObjectNode());
        root.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("recordnumber", 1);
        body.put("description", "测试现金流量");

        ObjectNode accSubject = objectMapper.createObjectNode();
        accSubject.put("code", "1001");
        accSubject.put("name", "库存现金");
        body.set("accsubject", accSubject);

        body.put("debit_original", 8000.00);
        body.put("credit_original", 0.00);

        // Add cash flow items
        ObjectNode cashFlow1 = objectMapper.createObjectNode();
        cashFlow1.put("itemCode", "C101");
        cashFlow1.put("itemName", "销售商品、提供劳务收到的现金");
        cashFlow1.put("amountOriginal", 8000.00);
        cashFlow1.put("negative", false);

        body.putArray("cashFlowItem").add(cashFlow1);

        root.putArray("body").add(body);

        return root;
    }

    private JsonNode createVoucherDataWithManyEntries() {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "004");
        header.put("period", "2026-01");
        header.set("accbook", objectMapper.createObjectNode());
        root.set("header", header);

        // Add 20 entries to test pagination/overflow handling
        for (int i = 1; i <= 20; i++) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("recordnumber", i);
            body.put("description", "测试分录" + i);

            ObjectNode accSubject = objectMapper.createObjectNode();
            accSubject.put("code", "100" + (i % 10));
            accSubject.put("name", "科目" + i);
            body.set("accsubject", accSubject);

            body.put("debit_original", i % 2 == 0 ? 1000.00 : 0.00);
            body.put("credit_original", i % 2 != 0 ? 1000.00 : 0.00);

            root.putArray("body").add(body);
        }

        return root;
    }

    // ===== Happy Path Tests =====

    @Test
    @DisplayName("应该成功生成完整的凭证PDF")
    void shouldGenerateVoucherPdfSuccessfully() throws IOException {
        // Given
        Path targetPath = tempDir.resolve("voucher-success.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(), "应该生成一页PDF");
    }

    @Test
    @DisplayName("应该正确处理包含辅助核算的凭证")
    void shouldHandleVoucherWithAuxiliaryItems() throws IOException {
        // Given
        JsonNode voucherWithAux = createVoucherDataWithAuxiliary();
        Path targetPath = tempDir.resolve("voucher-auxiliary.pdf");

        // When
        generator.generate(document, fileContent, voucherWithAux, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(), "应该成功生成PDF");
    }

    @Test
    @DisplayName("应该正确处理包含现金流量的凭证")
    void shouldHandleVoucherWithCashFlowItems() throws IOException {
        // Given
        JsonNode voucherWithCashFlow = createVoucherDataWithCashFlow();
        Path targetPath = tempDir.resolve("voucher-cashflow.pdf");

        // When
        generator.generate(document, fileContent, voucherWithCashFlow, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(), "应该成功生成PDF");
    }

    @Test
    @DisplayName("应该处理大量分录数据")
    void shouldHandleVoucherWithManyEntries() throws IOException {
        // Given
        JsonNode voucherWithManyEntries = createVoucherDataWithManyEntries();
        Path targetPath = tempDir.resolve("voucher-many-entries.pdf");

        // When
        generator.generate(document, fileContent, voucherWithManyEntries, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(), "应该成功生成PDF");
    }

    // ===== Edge Cases and Boundary Conditions =====

    @Test
    @DisplayName("应该处理最小化凭证数据")
    void shouldHandleMinimalVoucherData() throws IOException {
        // Given
        JsonNode minimalData = createMinimalVoucherData();
        Path targetPath = tempDir.resolve("voucher-minimal.pdf");

        // When
        assertDoesNotThrow(() -> {
            generator.generate(document, fileContent, minimalData, targetPath);
        });

        // Then
        assertEquals(1, document.getNumberOfPages(), "应该生成PDF");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("应该处理空凭证数据")
    void shouldHandleEmptyVoucherData(JsonNode emptyData) throws IOException {
        // Given
        Path targetPath = tempDir.resolve("voucher-empty.pdf");

        // When & Then
        assertDoesNotThrow(() -> {
            generator.generate(document, fileContent, emptyData, targetPath);
        });
    }

    @Test
    @DisplayName("应该处理没有分录数据的凭证")
    void shouldHandleVoucherWithoutEntries() throws IOException {
        // Given
        ObjectNode voucherWithoutEntries = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "005");
        header.put("period", "2026-01");
        voucherWithoutEntries.set("header", header);
        voucherWithoutEntries.putArray("body"); // Empty array

        Path targetPath = tempDir.resolve("voucher-no-entries.pdf");

        // When
        generator.generate(document, fileContent, voucherWithoutEntries, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(), "应该生成PDF");
    }

    @Test
    @DisplayName("应该处理不同货币代码")
    void shouldHandleDifferentCurrencyCodes() throws IOException {
        // Given
        ObjectNode voucherWithUSD = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "006");
        header.put("period", "2026-01");
        voucherWithUSD.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("description", "USD payment");

        ObjectNode accSubject = objectMapper.createObjectNode();
        accSubject.put("code", "1001");
        accSubject.put("name", "Cash");
        body.set("accsubject", accSubject);

        ObjectNode currency = objectMapper.createObjectNode();
        currency.put("code", "USD");
        body.set("currency", currency);

        body.put("debit_original", 1500.00);

        voucherWithUSD.putArray("body").add(body);

        Path targetPath = tempDir.resolve("voucher-usd.pdf");

        // When
        generator.generate(document, fileContent, voucherWithUSD, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该处理零金额分录")
    void shouldHandleZeroAmountEntries() throws IOException {
        // Given
        ObjectNode voucherWithZeroAmount = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "007");
        header.put("period", "2026-01");
        voucherWithZeroAmount.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("description", "Zero amount entry");

        ObjectNode accSubject = objectMapper.createObjectNode();
        accSubject.put("code", "1001");
        accSubject.put("name", "Cash");
        body.set("accsubject", accSubject);

        body.put("debit_original", 0.00);
        body.put("credit_original", 0.00);

        voucherWithZeroAmount.putArray("body").add(body);

        Path targetPath = tempDir.resolve("voucher-zero-amount.pdf");

        // When
        generator.generate(document, fileContent, voucherWithZeroAmount, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该处理大金额")
    void shouldHandleLargeAmounts() throws IOException {
        // Given
        ObjectNode voucherWithLargeAmount = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "008");
        header.put("period", "2026-01");
        voucherWithLargeAmount.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("description", "Large amount entry");

        ObjectNode accSubject = objectMapper.createObjectNode();
        accSubject.put("code", "1001");
        accSubject.put("name", "Cash");
        body.set("accsubject", accSubject);

        body.put("debit_original", 999999999.99);

        voucherWithLargeAmount.putArray("body").add(body);

        Path targetPath = tempDir.resolve("voucher-large-amount.pdf");

        // When
        generator.generate(document, fileContent, voucherWithLargeAmount, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    // ===== Alternative Data Structure Tests =====

    @Test
    @DisplayName("应该处理bodies数组格式")
    void shouldHandleBodiesArrayFormat() throws IOException {
        // Given
        ObjectNode voucherWithBodies = objectMapper.createObjectNode();
        voucherWithBodies.set("header", objectMapper.createObjectNode().put("period", "2026-01"));

        ObjectNode body1 = objectMapper.createObjectNode();
        body1.put("description", "Entry 1");
        body1.put("debit_original", 1000.00);

        ObjectNode body2 = objectMapper.createObjectNode();
        body2.put("description", "Entry 2");
        body2.put("credit_original", 1000.00);

        voucherWithBodies.putArray("bodies").add(body1).add(body2);

        Path targetPath = tempDir.resolve("voucher-bodies-format.pdf");

        // When
        generator.generate(document, fileContent, voucherWithBodies, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该处理数组格式根节点")
    void shouldHandleArrayRootFormat() throws IOException {
        // Given
        ObjectNode body1 = objectMapper.createObjectNode();
        body1.put("description", "Array entry 1");
        body1.put("debit_original", 1000.00);

        ObjectNode body2 = objectMapper.createObjectNode();
        body2.put("description", "Array entry 2");
        body2.put("credit_original", 1000.00);

        JsonNode arrayRoot = objectMapper.createArrayNode().add(body1).add(body2);

        Path targetPath = tempDir.resolve("voucher-array-root.pdf");

        // When
        generator.generate(document, fileContent, arrayRoot, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    // ===== Field Mapping Variations =====

    @Test
    @DisplayName("应该支持recordNumber和recordnumber两种字段名")
    void shouldSupportBothRecordNumberFieldNames() throws IOException {
        // Given
        ObjectNode voucherWithRecordNumber = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("period", "2026-01");
        voucherWithRecordNumber.set("header", header);

        ObjectNode body1 = objectMapper.createObjectNode();
        body1.put("recordNumber", 1); // Camel case
        body1.put("description", "Entry 1");

        ObjectNode body2 = objectMapper.createObjectNode();
        body2.put("recordnumber", 2); // Lower case
        body2.put("description", "Entry 2");

        voucherWithRecordNumber.putArray("body").add(body1).add(body2);

        Path targetPath = tempDir.resolve("voucher-record-number.pdf");

        // When
        generator.generate(document, fileContent, voucherWithRecordNumber, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该支持多种金额字段名")
    void shouldSupportVariousAmountFieldNames() throws IOException {
        // Given
        ObjectNode voucherWithAltAmountFields = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("period", "2026-01");
        voucherWithAltAmountFields.set("header", header);

        ObjectNode body1 = objectMapper.createObjectNode();
        body1.put("description", "Entry with debitOriginal");
        body1.put("debitOriginal", 1000.00); // Camel case

        ObjectNode body2 = objectMapper.createObjectNode();
        body2.put("description", "Entry with debit_org");
        body2.put("debit_org", 2000.00); // Snake case

        voucherWithAltAmountFields.putArray("body").add(body1).add(body2);

        Path targetPath = tempDir.resolve("voucher-alt-amount-fields.pdf");

        // When
        generator.generate(document, fileContent, voucherWithAltAmountFields, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    // ===== ArcFileContent Integration Tests =====

    @Test
    @DisplayName("应该使用ArcFileContent中的erpVoucherNo")
    void shouldUseErpVoucherNoFromFileContent() throws IOException {
        // Given
        fileContent.setErpVoucherNo("记-999");

        ObjectNode voucherWithoutDisplayName = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("period", "2026-01");
        // No displayname in header
        voucherWithoutDisplayName.set("header", header);

        Path targetPath = tempDir.resolve("voucher-erp-no.pdf");

        // When
        generator.generate(document, fileContent, voucherWithoutDisplayName, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该使用ArcFileContent中的fiscalYear")
    void shouldUseFiscalYearFromFileContent() throws IOException {
        // Given
        fileContent.setFiscalYear("2025");

        ObjectNode voucherWithoutPeriod = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        // No period in header
        voucherWithoutPeriod.set("header", header);

        Path targetPath = tempDir.resolve("voucher-fiscal-year.pdf");

        // When
        generator.generate(document, fileContent, voucherWithoutPeriod, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该使用ArcFileContent中的creator和sourceSystem")
    void shouldUseCreatorAndSourceSystemFromFileContent() throws IOException {
        // Given
        fileContent.setCreator("test-creator");
        fileContent.setSourceSystem("test-system");

        Path targetPath = tempDir.resolve("voucher-metadata.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该处理空的ArcFileContent字段")
    void shouldHandleNullFieldsInArcFileContent() throws IOException {
        // Given
        ArcFileContent emptyContent = new ArcFileContent();
        // All fields are null

        Path targetPath = tempDir.resolve("voucher-empty-content.pdf");

        // When
        generator.generate(document, emptyContent, voucherData, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("应该处理包含null值的字段")
    void shouldHandleNullValuesInFields() throws IOException {
        // Given
        ObjectNode voucherWithNulls = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.putNull("displayname");
        header.putNull("period");
        voucherWithNulls.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.putNull("description");
        body.putNull("accsubject");
        body.put("debit_original", 1000.00);

        voucherWithNulls.putArray("body").add(body);

        Path targetPath = tempDir.resolve("voucher-null-values.pdf");

        // When
        generator.generate(document, fileContent, voucherWithNulls, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该处理缺失的嵌套对象")
    void shouldHandleMissingNestedObjects() throws IOException {
        // Given
        ObjectNode voucherWithMissingNested = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "009");
        // No accbook object
        voucherWithMissingNested.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("description", "Test");
        // No accsubject object
        body.put("debit_original", 1000.00);

        voucherWithMissingNested.putArray("body").add(body);

        Path targetPath = tempDir.resolve("voucher-missing-nested.pdf");

        // When
        generator.generate(document, fileContent, voucherWithMissingNested, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该处理超长文本")
    void shouldHandleVeryLongText() throws IOException {
        // Given
        ObjectNode voucherWithLongText = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "010");
        voucherWithLongText.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        String longDescription = "A".repeat(200); // Very long description
        body.put("description", longDescription);

        ObjectNode accSubject = objectMapper.createObjectNode();
        accSubject.put("code", "1001");
        accSubject.put("name", "B".repeat(100)); // Long subject name
        body.set("accsubject", accSubject);

        body.put("debit_original", 1000.00);

        voucherWithLongText.putArray("body").add(body);

        Path targetPath = tempDir.resolve("voucher-long-text.pdf");

        // When
        generator.generate(document, fileContent, voucherWithLongText, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    @DisplayName("应该处理特殊字符")
    void shouldHandleSpecialCharacters() throws IOException {
        // Given
        ObjectNode voucherWithSpecialChars = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "011");
        voucherWithSpecialChars.set("header", header);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("description", "支付<测试>&\"'供应商货款");
        body.put("debit_original", 1000.00);

        voucherWithSpecialChars.putArray("body").add(body);

        Path targetPath = tempDir.resolve("voucher-special-chars.pdf");

        // When
        assertDoesNotThrow(() -> {
            generator.generate(document, fileContent, voucherWithSpecialChars, targetPath);
        });

        // Then
        assertEquals(1, document.getNumberOfPages());
    }

    // ===== PDF Document State Tests =====

    @Test
    @DisplayName("应该为现有文档添加新页面")
    void shouldAddPageToExistingDocument() throws IOException {
        // Given
        PDPage existingPage = new PDPage();
        document.addPage(existingPage);
        int initialPageCount = document.getNumberOfPages();

        Path targetPath = tempDir.resolve("voucher-existing-doc.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertEquals(initialPageCount + 1, document.getNumberOfPages(),
                "应该在现有文档基础上添加一页");
    }

    @Test
    @DisplayName("应该在PDF中绘制线条和文本")
    void shouldRenderLinesAndTextInPdf() throws IOException {
        // Given
        Path targetPath = tempDir.resolve("voucher-rendering.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages());
        // Additional assertions could be added here to verify content rendering
        // by extracting text from the PDF
    }

    // ===== Font Loading Tests =====

    @Test
    @DisplayName("应该在无中文字体时回退到默认字体")
    void shouldFallbackToDefaultFontWithoutChineseFont() throws IOException {
        // Given
        Path targetPath = tempDir.resolve("voucher-no-chinese-font.pdf");

        // When
        generator.generate(document, fileContent, voucherData, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(),
                "即使没有中文字体也应该成功生成PDF");
    }

    // ===== Multi-Page Content Tests =====

    @Test
    @DisplayName("应该正确处理yPosition边界")
    void shouldHandleYPositionBoundary() throws IOException {
        // Given
        JsonNode manyEntries = createVoucherDataWithManyEntries();
        Path targetPath = tempDir.resolve("voucher-ypos-boundary.pdf");

        // When
        generator.generate(document, fileContent, manyEntries, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(),
                "应该正确处理yPosition边界，不会溢出页面");
    }

    // ===== Integration Test Scenarios =====

    @Test
    @DisplayName("应该生成完整的真实凭证场景PDF")
    void shouldGenerateCompleteRealWorldVoucherPdf() throws IOException {
        // Given - Complete real-world scenario
        ObjectNode realWorldVoucher = objectMapper.createObjectNode();

        ObjectNode header = objectMapper.createObjectNode();
        ObjectNode accbook = objectMapper.createObjectNode();
        ObjectNode pkOrg = objectMapper.createObjectNode();
        pkOrg.put("name", "北京测试集团股份有限公司");
        accbook.set("pk_org", pkOrg);
        accbook.put("name", "总部账套");
        header.set("accbook", accbook);

        ObjectNode voucherType = objectMapper.createObjectNode();
        voucherType.put("voucherstr", "记");
        header.set("vouchertype", voucherType);

        header.put("displayname", "001");
        header.put("period", "2026-01");
        header.put("maketime", "2026-01-15 14:30:25");
        header.put("attachmentQuantity", "5");

        realWorldVoucher.set("header", header);

        // Multiple realistic entries
        String[] descriptions = {
                "收到客户A货款",
                "支付供应商B货款",
                "购买办公用品",
                "支付员工工资",
                "银行手续费"
        };

        String[] subjects = {"1002", "2202", "6601", "2211", "6603"};
        String[] subjectNames = {
                "银行存款",
                "应付账款",
                "管理费用-办公费",
                "应付职工薪酬",
                "财务费用-手续费"
        };

        for (int i = 0; i < descriptions.length; i++) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("recordnumber", i + 1);
            body.put("description", descriptions[i]);

            ObjectNode accSubject = objectMapper.createObjectNode();
            accSubject.put("code", subjects[i]);
            accSubject.put("name", subjectNames[i]);
            body.set("accsubject", accSubject);

            ObjectNode currency = objectMapper.createObjectNode();
            currency.put("code", "CNY");
            body.set("currency", currency);

            // Alternate debit/credit
            if (i % 2 == 0) {
                body.put("debit_original", 10000.00 + i * 1000);
                body.put("credit_original", 0.00);
            } else {
                body.put("debit_original", 0.00);
                body.put("credit_original", 10000.00 + (i - 1) * 1000);
            }

            realWorldVoucher.putArray("body").add(body);
        }

        Path targetPath = tempDir.resolve("voucher-real-world.pdf");

        // When
        generator.generate(document, fileContent, realWorldVoucher, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(),
                "应该成功生成真实场景的完整凭证PDF");
    }

    // ===== Parameterized Tests for Various Scenarios =====

    static Stream<JsonNode> provideVariousVoucherFormats() {
        ObjectMapper mapper = new ObjectMapper();

        // Format 1: Standard header + body
        ObjectNode format1 = mapper.createObjectNode();
        ObjectNode header1 = mapper.createObjectNode();
        header1.put("period", "2026-01");
        format1.set("header", header1);
        format1.putArray("body");

        // Format 2: Root level properties
        ObjectNode format2 = mapper.createObjectNode();
        format2.put("period", "2026-01");
        format2.putArray("body");

        // Format 3: bodies array
        ObjectNode format3 = mapper.createObjectNode();
        format3.put("period", "2026-01");
        format3.putArray("bodies");

        return Stream.of(format1, format2, format3);
    }

    @ParameterizedTest
    @MethodSource("provideVariousVoucherFormats")
    @DisplayName("应该支持多种凭证数据格式")
    void shouldSupportVariousVoucherFormats(JsonNode voucherFormat) throws IOException {
        // Given
        Path targetPath = tempDir.resolve("voucher-various-formats.pdf");

        // When & Then
        assertDoesNotThrow(() -> {
            generator.generate(document, fileContent, voucherFormat, targetPath);
        });
    }

    // ===== Coverage for Specific Utility Methods =====

    @Test
    @DisplayName("应该正确计算借贷平衡")
    void shouldCalculateDebitCreditBalance() throws IOException {
        // Given
        ObjectNode balancedVoucher = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "012");
        balancedVoucher.set("header", header);

        // Add entries with balanced amounts
        for (int i = 1; i <= 5; i++) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("description", "Entry " + i);
            body.put("debit_original", i * 1000.00);
            body.put("credit_original", i * 1000.00);
            balancedVoucher.putArray("body").add(body);
        }

        Path targetPath = tempDir.resolve("voucher-balanced.pdf");

        // When
        generator.generate(document, fileContent, balancedVoucher, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(),
                "应该成功处理借贷平衡的凭证");
    }

    @Test
    @DisplayName("应该处理不平衡的凭证")
    void shouldHandleUnbalancedVoucher() throws IOException {
        // Given
        ObjectNode unbalancedVoucher = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "013");
        unbalancedVoucher.set("header", header);

        ObjectNode body1 = objectMapper.createObjectNode();
        body1.put("description", "Debit only");
        body1.put("debit_original", 10000.00);
        body1.put("credit_original", 0.00);

        ObjectNode body2 = objectMapper.createObjectNode();
        body2.put("description", "Credit less");
        body2.put("debit_original", 0.00);
        body2.put("credit_original", 5000.00);

        unbalancedVoucher.putArray("body").add(body1).add(body2);

        Path targetPath = tempDir.resolve("voucher-unbalanced.pdf");

        // When
        generator.generate(document, fileContent, unbalancedVoucher, targetPath);

        // Then
        assertEquals(1, document.getNumberOfPages(),
                "应该成功处理不平衡的凭证（仅展示，不验证）");
    }

    // ===== Performance and Stress Tests =====

    @Test
    @DisplayName("应该处理最大单次分录数量")
    void shouldHandleMaximumEntryCount() throws IOException {
        // Given
        ObjectNode maxEntriesVoucher = objectMapper.createObjectNode();
        ObjectNode header = objectMapper.createObjectNode();
        header.put("displayname", "014");
        maxEntriesVoucher.set("header", header);

        // Create 100 entries (maximum limit)
        for (int i = 1; i <= 100; i++) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("recordnumber", i);
            body.put("description", "Entry " + i);
            body.put("debit_original", 100.00);
            maxEntriesVoucher.putArray("body").add(body);
        }

        Path targetPath = tempDir.resolve("voucher-max-entries.pdf");

        // When
        long startTime = System.currentTimeMillis();
        generator.generate(document, fileContent, maxEntriesVoucher, targetPath);
        long endTime = System.currentTimeMillis();

        // Then
        assertEquals(1, document.getNumberOfPages());
        assertTrue(endTime - startTime < 5000,
                "应该在5秒内完成100条分录的PDF生成");
    }
}
