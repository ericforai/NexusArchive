// Input: Apache PDFBox、Jackson JsonNode
// Output: PaymentPdfGenerator 类
// Pos: PDF 生成器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusarchive.entity.ArcFileContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 付款单 PDF 生成器
 * <p>
 * 生成横向 A4 付款单 PDF，包含表头信息和付款明细表
 * </p>
 */
@Slf4j
public class PaymentPdfGenerator {

    /**
     * 生成付款单 PDF 文件（横向简洁版）
     *
     * @param document   PDF 文档
     * @param fileContent 文件内容
     * @param data       凭证数据
     * @param targetPath 目标路径
     * @throws IOException 生成失败时抛出
     */
    public void generate(PDDocument document, ArcFileContent fileContent, JsonNode data, Path targetPath)
            throws IOException {
        // 设置为横向 A4 页面 (842 x 595)
        PDPage page = new PDPage(new PDRectangle(842, 595));
        document.addPage(page);

        // 加载中文字体
        PDFont chineseFont = PdfFontLoader.loadChineseFont(document);
        boolean useChinese = chineseFont != null;
        PDFont regularFont = useChinese ? chineseFont : PDType1Font.HELVETICA;
        PDFont boldFont = useChinese ? chineseFont : PDType1Font.HELVETICA_BOLD;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            int margin = 40;
            float pageWidth = 842; // Landscape A4 width
            float yPosition = 555; // Start from top (595 - 40)

            // === 标题 ===
            contentStream.beginText();
            contentStream.setFont(boldFont, 18);
            contentStream.newLineAtOffset((pageWidth / 2) - 40, yPosition);
            contentStream.showText(useChinese ? "付 款 单" : "Payment Bill");
            contentStream.endText();
            yPosition -= 35;

            // === 表头信息 ===
            renderHeader(contentStream, data, fileContent, margin, yPosition, regularFont, boldFont, useChinese);
            yPosition = getYPositionAfterHeader(yPosition);

            // === 分隔线 ===
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(pageWidth - margin, yPosition);
            contentStream.stroke();
            yPosition -= 10;

            // === 付款明细表 ===
            yPosition = renderDetailTable(contentStream, data, margin, yPosition, pageWidth, regularFont,
                    boldFont, useChinese);

            // === 合计行 ===
            renderTotalRow(contentStream, data, margin, yPosition, regularFont, boldFont, useChinese);
            yPosition -= 30;

            // === 页脚信息 ===
            renderFooter(contentStream, data, fileContent, margin, regularFont, useChinese);
        }

        log.info("付款单PDF生成成功(Landscape简洁版): {}", targetPath);
    }

    /**
     * 渲染表头信息
     */
    private void renderHeader(PDPageContentStream contentStream, JsonNode data, ArcFileContent fileContent,
                              int margin, float yPosition, PDFont regularFont, PDFont boldFont,
                              boolean useChinese) throws IOException {
        String code = data.path("code").asText(fileContent.getBusinessDocNo());
        String billDate = data.path("billDate").asText(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        String orgName = data.path("financeOrgName").asText("-");
        String supplier = data.path("supplierName").asText("-");
        String currency = data.path("oriCurrencyName").asText("CNY");
        double totalAmount = data.path("oriTaxIncludedAmount").asDouble(0.0);
        String creator = data.path("creatorUserName").asText(
                fileContent.getCreator() != null ? fileContent.getCreator() : "");

        contentStream.setFont(regularFont, 10);

        // 第一行: 单据编号、交易类型、单据日期
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("单据编号: " + code, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("交易类型: 采购付款", useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("单据日期: " + billDate, useChinese));
        contentStream.endText();
        yPosition -= 18;

        // 第二行: 付款组织、供应商、币种
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("付款组织: " + orgName, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("供应商: " + supplier, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("币种: " + currency, useChinese));
        contentStream.endText();
        yPosition -= 18;

        // 第三行: 付款金额（高亮）
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.setFont(boldFont, 11);
        contentStream.showText(PdfUtils.safeText(
                "付款金额: " + PdfUtils.formatAmountWithCurrency(totalAmount, currency), useChinese));
        contentStream.setFont(regularFont, 10);
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("往来对象类型: 供应商", useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("创建人: " + creator, useChinese));
        contentStream.endText();
        yPosition -= 18;

        // 第四行: 来源系统
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("来源系统: "
                + (fileContent.getSourceSystem() != null ? fileContent.getSourceSystem() : "用友YonSuite"),
                useChinese));
        contentStream.endText();
    }

    private float getYPositionAfterHeader(float yPosition) {
        return yPosition - 18 - 30;
    }

    /**
     * 渲染明细表
     */
    private float renderDetailTable(PDPageContentStream contentStream, JsonNode data, int margin,
                                     float yPosition, float pageWidth, PDFont regularFont, PDFont boldFont,
                                     boolean useChinese) throws IOException {
        // 表头
        contentStream.beginText();
        contentStream.setFont(boldFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("付款明细", useChinese));
        contentStream.endText();
        yPosition -= 20;

        // 表格表头 (8列)
        float[] columnPositions = { 0, 40, 120, 220, 320, 420, 540, 660 };
        String[] headers = { "序号", "款项类型", "物料名称", "应结算款", "付款金额", "本币金额", "供应商", "订单编号" };

        contentStream.beginText();
        contentStream.setFont(boldFont, 9);
        for (int i = 0; i < headers.length; i++) {
            contentStream.newLineAtOffset(
                    margin + columnPositions[i] - (i == 0 ? 0 : columnPositions[i - 1]),
                    yPosition);
            contentStream.showText(PdfUtils.safeText(headers[i], useChinese));
        }
        contentStream.endText();
        yPosition -= 15;

        // 表格线
        contentStream.moveTo(margin, yPosition + 10);
        contentStream.lineTo(pageWidth - margin, yPosition + 10);
        contentStream.stroke();

        // 表体数据
        yPosition = renderTableBody(contentStream, data, margin, yPosition, columnPositions, regularFont,
                useChinese);

        // 表格底线
        contentStream.moveTo(margin, yPosition + 10);
        contentStream.lineTo(pageWidth - margin, yPosition + 10);
        contentStream.stroke();
        yPosition -= 15;

        return yPosition;
    }

    /**
     * 渲染表体数据
     */
    private float renderTableBody(PDPageContentStream contentStream, JsonNode data, int margin,
                                   float yPosition, float[] columnPositions, PDFont regularFont,
                                   boolean useChinese) throws IOException {
        contentStream.setFont(regularFont, 9);
        JsonNode bodyItems = data.path("bodyItem");
        String supplier = data.path("supplierName").asText("-");

        if (bodyItems.isArray() && bodyItems.size() > 0) {
            int index = 1;
            for (JsonNode item : bodyItems) {
                yPosition = renderTableRow(contentStream, item, margin, yPosition, columnPositions,
                        index++, supplier, useChinese);
                if (yPosition < 100) {
                    break;
                }
            }
        } else {
            // 无明细数据时显示汇总行
            yPosition = renderSummaryRow(contentStream, data, margin, yPosition, columnPositions,
                    supplier, regularFont, useChinese);
        }

        return yPosition;
    }

    /**
     * 渲染单行数据
     */
    private float renderTableRow(PDPageContentStream contentStream, JsonNode item, int margin,
                                  float yPosition, float[] columnPositions, int index, String supplier,
                                  boolean useChinese) throws IOException {
        String typeName = item.path("quickTypeName").asText("-");
        String materialName = PdfDataParser.getTextValue(item, "materialName", "productName", "invName");
        if (materialName.isEmpty()) {
            materialName = "-";
        }
        double itemAmount = item.path("oriTaxIncludedAmount").asDouble(0.0);
        String orderNo = PdfDataParser.getTextValue(item, "srcBillNo", "orderNo", "订单编号");

        contentStream.beginText();
        contentStream.newLineAtOffset(margin + columnPositions[0], yPosition);
        contentStream.showText(String.valueOf(index));
        contentStream.newLineAtOffset(columnPositions[1] - columnPositions[0], 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(typeName, 10), useChinese));
        contentStream.newLineAtOffset(columnPositions[2] - columnPositions[1], 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(materialName, 12), useChinese));
        contentStream.newLineAtOffset(columnPositions[3] - columnPositions[2], 0);
        contentStream.showText(PdfUtils.formatAmount(itemAmount));
        contentStream.newLineAtOffset(columnPositions[4] - columnPositions[3], 0);
        contentStream.showText(PdfUtils.formatAmount(itemAmount));
        contentStream.newLineAtOffset(columnPositions[5] - columnPositions[4], 0);
        contentStream.showText(PdfUtils.formatAmount(itemAmount));
        contentStream.newLineAtOffset(columnPositions[6] - columnPositions[5], 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(supplier, 12), useChinese));
        contentStream.newLineAtOffset(columnPositions[7] - columnPositions[6], 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(orderNo, 15), useChinese));
        contentStream.endText();

        return yPosition - 15;
    }

    /**
     * 渲染汇总行
     */
    private float renderSummaryRow(PDPageContentStream contentStream, JsonNode data, int margin,
                                    float yPosition, float[] columnPositions, String supplier,
                                    PDFont regularFont, boolean useChinese) throws IOException {
        String currency = data.path("oriCurrencyName").asText("CNY");
        double totalAmount = data.path("oriTaxIncludedAmount").asDouble(0.0);

        contentStream.beginText();
        contentStream.newLineAtOffset(margin + columnPositions[0], yPosition);
        contentStream.showText("1");
        contentStream.newLineAtOffset(columnPositions[1] - columnPositions[0], 0);
        contentStream.showText(PdfUtils.safeText("应付款", useChinese));
        contentStream.newLineAtOffset(columnPositions[2] - columnPositions[1], 0);
        contentStream.showText("-");
        contentStream.newLineAtOffset(columnPositions[3] - columnPositions[2], 0);
        contentStream.showText(PdfUtils.formatAmount(totalAmount));
        contentStream.newLineAtOffset(columnPositions[4] - columnPositions[3], 0);
        contentStream.showText(PdfUtils.formatAmount(totalAmount));
        contentStream.newLineAtOffset(columnPositions[5] - columnPositions[4], 0);
        contentStream.showText(PdfUtils.formatAmount(totalAmount));
        contentStream.newLineAtOffset(columnPositions[6] - columnPositions[5], 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(supplier, 12), useChinese));
        contentStream.newLineAtOffset(columnPositions[7] - columnPositions[6], 0);
        contentStream.showText("-");
        contentStream.endText();

        return yPosition - 15;
    }

    /**
     * 渲染合计行
     */
    private void renderTotalRow(PDPageContentStream contentStream, JsonNode data, int margin,
                                float yPosition, PDFont regularFont, PDFont boldFont,
                                boolean useChinese) throws IOException {
        String currency = data.path("oriCurrencyName").asText("CNY");
        double totalAmount = data.path("oriTaxIncludedAmount").asDouble(0.0);
        float[] columnPositions = { 0, 40, 120, 220, 320, 420, 540, 660 };

        contentStream.beginText();
        contentStream.setFont(boldFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("合计", useChinese));
        contentStream.newLineAtOffset(columnPositions[3], 0);
        contentStream.showText(PdfUtils.formatAmount(totalAmount));
        contentStream.newLineAtOffset(columnPositions[4] - columnPositions[3], 0);
        contentStream.showText(PdfUtils.formatAmount(totalAmount));
        contentStream.newLineAtOffset(columnPositions[5] - columnPositions[4], 0);
        contentStream.showText(PdfUtils.formatAmount(totalAmount));
        contentStream.endText();
    }

    /**
     * 渲染页脚
     */
    private void renderFooter(PDPageContentStream contentStream, JsonNode data, ArcFileContent fileContent,
                              int margin, PDFont regularFont, boolean useChinese) throws IOException {
        String creator = data.path("creatorUserName").asText(
                fileContent.getCreator() != null ? fileContent.getCreator() : "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        contentStream.setFont(regularFont, 8);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, 30);
        contentStream.showText(PdfUtils.safeText("生成时间: " + timestamp, useChinese));
        contentStream.newLineAtOffset(300, 0);
        contentStream.showText(PdfUtils.safeText("制单人: " + creator, useChinese));
        contentStream.endText();
    }
}
