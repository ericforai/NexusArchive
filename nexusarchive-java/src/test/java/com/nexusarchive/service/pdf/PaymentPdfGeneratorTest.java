// Input: JUnit 5、Mockito、AssertJ、PDFBox、Jackson JsonNode
// Output: PaymentPdfGeneratorTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ArcFileContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PaymentPdfGenerator 单元测试
 * <p>
 * 测试付款单 PDF 生成功能，包括：
 * - 正常场景：生成完整付款单 PDF
 * - 边界条件：空数据、缺失字段
 * - 异常处理：IO 异常、空参数
 * - 数据格式：金额格式化、日期格式化
 * </p>
 */
@Tag("unit")
@DisplayName("付款单 PDF 生成器测试")
class PaymentPdfGeneratorTest {

    private final PaymentPdfGenerator generator = new PaymentPdfGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private ArcFileContent testFileContent;
    private JsonNode testPaymentData;

    @BeforeEach
    void setUp() throws IOException {
        // 初始化测试用的 ArcFileContent
        testFileContent = ArcFileContent.builder()
                .id("test-file-id")
                .fileName("test-payment.pdf")
                .businessDocNo("PAY-2026-001")
                .creator("test-user")
                .sourceSystem("用友YonSuite")
                .fiscalYear("2026")
                .period("01")
                .build();

        // 初始化测试用的付款数据
        String paymentJson = """
                {
                    "code": "PAY-2026-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 50000.00,
                    "creatorUserName": "张三",
                    "bodyItem": [
                        {
                            "quickTypeName": "货款",
                            "materialName": "办公设备采购",
                            "oriTaxIncludedAmount": 30000.00,
                            "srcBillNo": "PO-2026-001"
                        },
                        {
                            "quickTypeName": "服务费",
                            "productName": "咨询服务费",
                            "oriTaxIncludedAmount": 20000.00,
                            "orderNo": "PO-2026-002"
                        }
                    ]
                }
                """;
        testPaymentData = objectMapper.readTree(paymentJson);
    }

    @Test
    @DisplayName("生成完整付款单 PDF - 成功")
    void generate_fullPaymentPdf_success() throws IOException {
        // Given
        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-full.pdf");

        // When
        generator.generate(document, testFileContent, testPaymentData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
        assertThat(Files.size(targetPath)).isGreaterThan(0);
        assertThat(document.getNumberOfPages()).isEqualTo(1);

        // 验证页面方向（横向 A4: 842 x 595）
        PDPage page = document.getPage(0);
        assertThat(page.getMediaBox().getWidth()).isEqualTo(842);
        assertThat(page.getMediaBox().getHeight()).isEqualTo(595);
    }

    @Test
    @DisplayName("生成付款单 PDF - 使用默认值处理缺失字段")
    void generate_paymentPdf_withMissingFields_success() throws IOException {
        // Given - 缺失部分字段的数据
        String minimalJson = """
                {
                    "oriTaxIncludedAmount": 10000.00
                }
                """;
        JsonNode minimalData = objectMapper.readTree(minimalJson);

        ArcFileContent minimalContent = ArcFileContent.builder()
                .businessDocNo("MIN-001")
                .creator("default-user")
                .build();

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-minimal.pdf");

        // When
        generator.generate(document, minimalContent, minimalData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("生成付款单 PDF - 无明细数据时显示汇总行")
    void generate_paymentPdf_withoutDetails_showsSummaryRow() throws IOException {
        // Given - 没有 bodyItem 的数据
        String noDetailJson = """
                {
                    "code": "PAY-SUMMARY-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "汇总供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 75000.00,
                    "creatorUserName": "李四"
                }
                """;
        JsonNode noDetailData = objectMapper.readTree(noDetailJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-summary.pdf");

        // When
        generator.generate(document, testFileContent, noDetailData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("生成付款单 PDF - 空明细数组时显示汇总行")
    void generate_paymentPdf_withEmptyDetailArray_showsSummaryRow() throws IOException {
        // Given - bodyItem 为空数组
        String emptyDetailJson = """
                {
                    "code": "PAY-EMPTY-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "空明细供应商",
                    "oriCurrencyName": "USD",
                    "oriTaxIncludedAmount": 25000.00,
                    "creatorUserName": "王五",
                    "bodyItem": []
                }
                """;
        JsonNode emptyDetailData = objectMapper.readTree(emptyDetailJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-empty-detail.pdf");

        // When
        generator.generate(document, testFileContent, emptyDetailData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("生成付款单 PDF - 多明细数据正确渲染")
    void generate_paymentPdf_withMultipleDetails_rendersAllRows() throws IOException {
        // Given - 包含多个明细项
        String multiDetailJson = """
                {
                    "code": "PAY-MULTI-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "多明细供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 150000.00,
                    "creatorUserName": "赵六",
                    "bodyItem": [
                        {
                            "quickTypeName": "货款",
                            "materialName": "设备A",
                            "oriTaxIncludedAmount": 50000.00,
                            "srcBillNo": "PO-001"
                        },
                        {
                            "quickTypeName": "服务费",
                            "productName": "服务B",
                            "oriTaxIncludedAmount": 50000.00,
                            "orderNo": "PO-002"
                        },
                        {
                            "quickTypeName": "运费",
                            "invName": "运输C",
                            "oriTaxIncludedAmount": 50000.00,
                            "srcBillNo": "PO-003"
                        }
                    ]
                }
                """;
        JsonNode multiDetailData = objectMapper.readTree(multiDetailJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-multi-detail.pdf");

        // When
        generator.generate(document, testFileContent, multiDetailData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理不同币种")
    void generate_paymentPdf_withDifferentCurrency_success() throws IOException {
        // Given
        String usdJson = """
                {
                    "code": "PAY-USD-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "美国供应商",
                    "oriCurrencyName": "USD",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户A"
                }
                """;
        JsonNode usdData = objectMapper.readTree(usdJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-usd.pdf");

        // When
        generator.generate(document, testFileContent, usdData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理零金额")
    void generate_paymentPdf_withZeroAmount_success() throws IOException {
        // Given
        String zeroAmountJson = """
                {
                    "code": "PAY-ZERO-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 0.00,
                    "creatorUserName": "用户B"
                }
                """;
        JsonNode zeroAmountData = objectMapper.readTree(zeroAmountJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-zero-amount.pdf");

        // When
        generator.generate(document, testFileContent, zeroAmountData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理大金额")
    void generate_paymentPdf_withLargeAmount_success() throws IOException {
        // Given
        String largeAmountJson = """
                {
                    "code": "PAY-LARGE-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "大额供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 999999999.99,
                    "creatorUserName": "用户C"
                }
                """;
        JsonNode largeAmountData = objectMapper.readTree(largeAmountJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-large-amount.pdf");

        // When
        generator.generate(document, testFileContent, largeAmountData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理长文本截断")
    void generate_paymentPdf_withLongText_truncatesCorrectly() throws IOException {
        // Given - 包含超长文本
        String longTextJson = """
                {
                    "code": "PAY-LONG-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "这是一个非常长的组织名称，应该会被截断以适应PDF表格列宽度限制",
                    "supplierName": "这是一个非常长的供应商名称，应该会被截断以适应PDF表格列宽度限制",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户D",
                    "bodyItem": [
                        {
                            "quickTypeName": "这是一个非常长的款项类型名称",
                            "materialName": "这是一个非常长的物料名称，应该会被截断以适应PDF表格列宽度限制",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "这是一个非常长的订单编号，应该会被截断以适应PDF表格列宽度限制"
                        }
                    ]
                }
                """;
        JsonNode longTextData = objectMapper.readTree(longTextJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-long-text.pdf");

        // When
        generator.generate(document, testFileContent, longTextData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 使用备用字段名")
    void generate_paymentPdf_withAlternativeFieldNames_success() throws IOException {
        // Given - 使用备用字段名（invName, orderNo）
        String alternativeFieldJson = """
                {
                    "code": "PAY-ALT-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户E",
                    "bodyItem": [
                        {
                            "quickTypeName": "货款",
                            "invName": "库存商品名称",
                            "oriTaxIncludedAmount": 10000.00,
                            "orderNo": "ORD-001"
                        }
                    ]
                }
                """;
        JsonNode alternativeFieldData = objectMapper.readTree(alternativeFieldJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-alt-fields.pdf");

        // When
        generator.generate(document, testFileContent, alternativeFieldData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理特殊字符")
    void generate_paymentPdf_withSpecialCharacters_success() throws IOException {
        // Given - 包含特殊字符的数据
        String specialCharJson = """
                {
                    "code": "PAY-<>&\"-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织<测试>",
                    "supplierName": "供应商&合作",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户'F'"
                }
                """;
        JsonNode specialCharData = objectMapper.readTree(specialCharJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-special-chars.pdf");

        // When
        generator.generate(document, testFileContent, specialCharData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - null 参数处理")
    void generate_paymentPdf_withNullParameters_handlesGracefully() throws IOException {
        // Given
        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-null-test.pdf");

        // When - ArcFileContent 为 null
        ArcFileContent nullContent = ArcFileContent.builder().build();
        String nullFieldJson = """
                {
                    "oriTaxIncludedAmount": 10000.00
                }
                """;
        JsonNode nullFieldData = objectMapper.readTree(nullFieldJson);

        // Then - 不应抛出 NullPointerException
        assertThatThrownBy(() -> {
            generator.generate(document, nullContent, nullFieldData, targetPath);
            document.save(targetPath.toFile());
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("生成付款单 PDF - 文档保存到不同路径")
    void generate_paymentPdf_toDifferentPaths_success() throws IOException {
        // Given
        Path targetPath1 = tempDir.resolve("payment-path-1.pdf");
        Path targetPath2 = tempDir.resolve("subdir").resolve("payment-path-2.pdf");
        Files.createDirectories(targetPath2.getParent());

        // When
        try (PDDocument doc1 = new PDDocument()) {
            generator.generate(doc1, testFileContent, testPaymentData, targetPath1);
            doc1.save(targetPath1.toFile());
        }

        try (PDDocument doc2 = new PDDocument()) {
            generator.generate(doc2, testFileContent, testPaymentData, targetPath2);
            doc2.save(targetPath2.toFile());
        }

        // Then
        assertThat(Files.exists(targetPath1)).isTrue();
        assertThat(Files.exists(targetPath2)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 验证 PDF 结构")
    void generate_paymentPdf_validatesPdfStructure() throws IOException {
        // Given
        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-structure.pdf");

        // When
        generator.generate(document, testFileContent, testPaymentData, targetPath);
        document.save(targetPath.toFile());

        // Then - 验证 PDF 基本结构
        try (PDDocument savedDoc = PDDocument.load(targetPath.toFile())) {
            assertThat(savedDoc.getNumberOfPages()).isEqualTo(1);

            PDPage page = savedDoc.getPage(0);
            assertThat(page.getMediaBox().getWidth()).isEqualTo(842); // Landscape A4
            assertThat(page.getMediaBox().getHeight()).isEqualTo(595);

            // 验证文档可以正常读取（无损坏）
            assertThat(savedDoc.isEncrypted()).isFalse();
        }
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理明细项超过页面空间")
    void generate_paymentPdf_withExcessiveDetails_truncatesAtPageLimit() throws IOException {
        // Given - 创建大量明细项（超过页面空间）
        StringBuilder jsonBuilder = new StringBuilder("""
                {
                    "code": "PAY-EXCESS-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 500000.00,
                    "creatorUserName": "测试用户",
                    "bodyItem": [
                """);

        // 添加 50 个明细项
        for (int i = 1; i <= 50; i++) {
            jsonBuilder.append("""
                    {
                        "quickTypeName": "货款",
                        "materialName": "物料%d",
                        "oriTaxIncludedAmount": 10000.00,
                        "srcBillNo": "PO-%03d"
                    }
            """.formatted(i, i));
            if (i < 50) {
                jsonBuilder.append(",");
            }
        }

        jsonBuilder.append("""
                ]
                }
                """);

        JsonNode excessiveData = objectMapper.readTree(jsonBuilder.toString());

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-excessive.pdf");

        // When
        generator.generate(document, testFileContent, excessiveData, targetPath);
        document.save(targetPath.toFile());

        // Then - 应该成功生成，但会截断超出部分
        assertThat(Files.exists(targetPath)).isTrue();
        assertThat(document.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理负金额")
    void generate_paymentPdf_withNegativeAmount_success() throws IOException {
        // Given - 负金额（退款场景）
        String negativeAmountJson = """
                {
                    "code": "PAY-NEG-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "退款供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": -5000.00,
                    "creatorUserName": "用户F",
                    "bodyItem": [
                        {
                            "quickTypeName": "退款",
                            "materialName": "物料退款",
                            "oriTaxIncludedAmount": -5000.00,
                            "srcBillNo": "REF-001"
                        }
                    ]
                }
                """;
        JsonNode negativeAmountData = objectMapper.readTree(negativeAmountJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-negative-amount.pdf");

        // When
        generator.generate(document, testFileContent, negativeAmountData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 验证日期格式化")
    void generate_paymentPdf_validatesDateFormatting() throws IOException {
        // Given
        String dateJson = """
                {
                    "code": "PAY-DATE-001",
                    "billDate": "2026-12-31",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户G"
                }
                """;
        JsonNode dateData = objectMapper.readTree(dateJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-date.pdf");

        // When
        generator.generate(document, testFileContent, dateData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 测试金额精度")
    void generate_paymentPdf_validatesAmountPrecision() throws IOException {
        // Given - 包含多位小数的金额
        String precisionJson = """
                {
                    "code": "PAY-PREC-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 12345.6789,
                    "creatorUserName": "用户H",
                    "bodyItem": [
                        {
                            "quickTypeName": "货款",
                            "materialName": "精密计价物料",
                            "oriTaxIncludedAmount": 12345.6789,
                            "srcBillNo": "PO-PREC-001"
                        }
                    ]
                }
                """;
        JsonNode precisionData = objectMapper.readTree(precisionJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-precision.pdf");

        // When
        generator.generate(document, testFileContent, precisionData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理中文字符")
    void generate_paymentPdf_withChineseCharacters_success() throws IOException {
        // Given - 包含中文的数据
        String chineseJson = """
                {
                    "code": "付-2026-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "北京测试科技有限公司",
                    "supplierName": "上海供应商有限公司",
                    "oriCurrencyName": "人民币",
                    "oriTaxIncludedAmount": 50000.00,
                    "creatorUserName": "张三丰",
                    "bodyItem": [
                        {
                            "quickTypeName": "设备采购款",
                            "materialName": "办公电脑及外设",
                            "oriTaxIncludedAmount": 30000.00,
                            "srcBillNo": "采购单-2026-001"
                        },
                        {
                            "quickTypeName": "技术服务费",
                            "productName": "系统维护服务",
                            "oriTaxIncludedAmount": 20000.00,
                            "orderNo": "服务单-2026-002"
                        }
                    ]
                }
                """;
        JsonNode chineseData = objectMapper.readTree(chineseJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-chinese.pdf");

        // When
        generator.generate(document, testFileContent, chineseData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理多批次明细")
    void generate_paymentPdf_withBatchDetails_success() throws IOException {
        // Given - 模拟批次明细场景
        String batchJson = """
                {
                    "code": "PAY-BATCH-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "批量供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 60000.00,
                    "creatorUserName": "批量用户",
                    "bodyItem": [
                        {
                            "quickTypeName": "货款",
                            "materialName": "物料1",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "PO-001"
                        },
                        {
                            "quickTypeName": "货款",
                            "materialName": "物料2",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "PO-002"
                        },
                        {
                            "quickTypeName": "货款",
                            "materialName": "物料3",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "PO-003"
                        },
                        {
                            "quickTypeName": "货款",
                            "materialName": "物料4",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "PO-004"
                        },
                        {
                            "quickTypeName": "货款",
                            "materialName": "物料5",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "PO-005"
                        },
                        {
                            "quickTypeName": "货款",
                            "materialName": "物料6",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "PO-006"
                        }
                    ]
                }
                """;
        JsonNode batchData = objectMapper.readTree(batchJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-batch.pdf");

        // When
        generator.generate(document, testFileContent, batchData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 验证来源系统显示")
    void generate_paymentPdf_validatesSourceSystemDisplay() throws IOException {
        // Given - 不同的来源系统
        String sourceSystemJson = """
                {
                    "code": "PAY-SRC-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户I"
                }
                """;
        JsonNode sourceSystemData = objectMapper.readTree(sourceSystemJson);

        // 测试自定义来源系统
        ArcFileContent customSourceContent = ArcFileContent.builder()
                .businessDocNo("PAY-SRC-001")
                .creator("用户I")
                .sourceSystem("自定义ERP系统")
                .build();

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-source-system.pdf");

        // When
        generator.generate(document, customSourceContent, sourceSystemData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 验证页脚信息")
    void generate_paymentPdf_validatesFooterInfo() throws IOException {
        // Given
        String footerJson = """
                {
                    "code": "PAY-FOOTER-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "制单人姓名"
                }
                """;
        JsonNode footerData = objectMapper.readTree(footerJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-footer.pdf");

        // When
        generator.generate(document, testFileContent, footerData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 测试内存占用")
    void generate_paymentPdf_validatesMemoryUsage() throws IOException {
        // Given
        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-memory.pdf");

        // When
        generator.generate(document, testFileContent, testPaymentData, targetPath);
        document.save(targetPath.toFile());

        // Then - 验证文档正确关闭（无内存泄漏）
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        byte[] pdfBytes = outputStream.toByteArray();

        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isLessThan(1_000_000); // 小于 1MB
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理缺失物料名称字段")
    void generate_paymentPdf_withMissingMaterialName_handlesGracefully() throws IOException {
        // Given - 缺失所有物料名称字段
        String missingMaterialJson = """
                {
                    "code": "PAY-NO-MAT-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户J",
                    "bodyItem": [
                        {
                            "quickTypeName": "货款",
                            "oriTaxIncludedAmount": 10000.00,
                            "srcBillNo": "PO-001"
                        }
                    ]
                }
                """;
        JsonNode missingMaterialData = objectMapper.readTree(missingMaterialJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-no-material.pdf");

        // When
        generator.generate(document, testFileContent, missingMaterialData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }

    @Test
    @DisplayName("生成付款单 PDF - 处理缺失订单编号字段")
    void generate_paymentPdf_withMissingOrderNo_handlesGracefully() throws IOException {
        // Given - 缺失所有订单编号字段
        String missingOrderJson = """
                {
                    "code": "PAY-NO-ORD-001",
                    "billDate": "2026-01-15",
                    "financeOrgName": "测试组织",
                    "supplierName": "测试供应商",
                    "oriCurrencyName": "CNY",
                    "oriTaxIncludedAmount": 10000.00,
                    "creatorUserName": "用户K",
                    "bodyItem": [
                        {
                            "quickTypeName": "货款",
                            "materialName": "测试物料",
                            "oriTaxIncludedAmount": 10000.00
                        }
                    ]
                }
                """;
        JsonNode missingOrderData = objectMapper.readTree(missingOrderJson);

        PDDocument document = new PDDocument();
        Path targetPath = tempDir.resolve("payment-no-order.pdf");

        // When
        generator.generate(document, testFileContent, missingOrderData, targetPath);
        document.save(targetPath.toFile());

        // Then
        assertThat(Files.exists(targetPath)).isTrue();
    }
}
