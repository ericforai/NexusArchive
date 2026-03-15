// Input: Apache PDFBox、Jackson JsonNode
// Output: CollectionPdfGenerator 类
// Pos: PDF 生成器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.entity.ArcFileContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 收款单 PDF 生成器
 * <p>
 * 生成 A4 竖向收款单 PDF，包含表头信息和收款明细表
 * </p>
 */
@Slf4j
public class CollectionPdfGenerator {

    /**
     * 生成收款单 PDF 文件
     *
     * @param document    PDF 文档
     * @param fileContent 文件内容
     * @param voucherData 凭证数据
     * @param targetPath  目标路径
     * @throws IOException 生成失败时抛出
     */
    public void generate(PDDocument document, ArcFileContent fileContent, JsonNode voucherData,
                         Path targetPath) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);

        PDFont chineseFont = PdfFontLoader.loadChineseFont(document);
        boolean useChinese = chineseFont != null;
        PDFont regularFont = useChinese ? chineseFont : PDType1Font.HELVETICA;
        PDFont boldFont = useChinese ? chineseFont : PDType1Font.HELVETICA_BOLD;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            int margin = 40;
            float yPosition = 750;

            // 解析数据
            CollectionBillData data = parseData(fileContent, voucherData);

            // 标题
            renderTitle(contentStream, yPosition, boldFont, useChinese);
            yPosition -= 35;

            // 表头信息
            yPosition = renderHeader(contentStream, data, margin, yPosition, regularFont, useChinese);

            // 分隔线
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(560, yPosition);
            contentStream.stroke();
            yPosition -= 10;

            // 明细表
            yPosition = renderDetailTable(contentStream, data, margin, yPosition, regularFont, boldFont,
                    useChinese);

            // 底部信息
            renderFooter(contentStream, data, margin, regularFont, useChinese);
        }

        log.info("收款单PDF生成成功: {}", targetPath);
    }

    /**
     * 解析收款单数据
     */
    private CollectionBillData parseData(ArcFileContent fileContent, JsonNode voucherData) {
        CollectionBillData data = new CollectionBillData();

        data.billCode = fileContent.getErpVoucherNo() != null ? fileContent.getErpVoucherNo() : "";
        data.summary = voucherData != null ? voucherData.path("summary").asText("") : "";
        data.creator = fileContent.getCreator() != null ? fileContent.getCreator() : "";
        data.accountPeriod = voucherData != null ? voucherData.path("accountPeriod").asText("") : "";
        data.voucherNo = voucherData != null ? voucherData.path("voucherNo").asText(data.billCode)
                : data.billCode;
        data.sourceSystem = fileContent.getSourceSystem() != null ? fileContent.getSourceSystem()
                : "用友YonSuite";

        // 从 summary 解析客户名和金额
        if (!data.summary.isEmpty()) {
            data.customerName = extractField(data.summary, "客户:", ",");
            data.amount = extractAmount(data.summary);
        }

        return data;
    }

    private String extractField(String summary, String prefix, String delimiter) {
        if (summary.contains(prefix)) {
            int start = summary.indexOf(prefix) + prefix.length();
            int end = summary.indexOf(delimiter, start);
            if (end > start) {
                return summary.substring(start, end).trim();
            }
        }
        return "-";
    }

    private String extractAmount(String summary) {
        if (summary.contains("金额:")) {
            int start = summary.indexOf("金额:") + 3;
            int end = summary.indexOf(" CNY", start);
            if (end > start) {
                return summary.substring(start, end).trim();
            }
        }
        return "-";
    }

    /**
     * 渲染标题
     */
    private void renderTitle(PDPageContentStream contentStream, float yPosition, PDFont boldFont,
                             boolean useChinese) throws IOException {
        contentStream.beginText();
        contentStream.setFont(boldFont, 18);
        contentStream.newLineAtOffset(220, yPosition);
        contentStream.showText(useChinese ? "收 款 单" : "Collection Bill");
        contentStream.endText();
    }

    /**
     * 渲染表头信息
     */
    private float renderHeader(PDPageContentStream contentStream, CollectionBillData data, int margin,
                                float yPosition, PDFont regularFont, boolean useChinese) throws IOException {
        String billDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateFormat.DATE));

        contentStream.setFont(regularFont, 10);

        // 第一行
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("单据编号: " + data.voucherNo, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("交易类型: 销售收款", useChinese));
        contentStream.endText();
        yPosition -= 18;

        // 第二行
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("单据日期: " + billDate, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("会计期间: " + data.accountPeriod, useChinese));
        contentStream.endText();
        yPosition -= 18;

        // 第三行
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("客户: " + data.customerName, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("往来对象类型: 客户", useChinese));
        contentStream.endText();
        yPosition -= 18;

        // 第四行
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("来源系统: " + data.sourceSystem, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText(PdfUtils.safeText("创建人: " + data.creator, useChinese));
        contentStream.endText();

        return yPosition - 30;
    }

    /**
     * 渲染明细表
     */
    private float renderDetailTable(PDPageContentStream contentStream, CollectionBillData data, int margin,
                                     float yPosition, PDFont regularFont, PDFont boldFont,
                                     boolean useChinese) throws IOException {
        // 表头
        contentStream.beginText();
        contentStream.setFont(boldFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("收款明细", useChinese));
        contentStream.endText();
        yPosition -= 20;

        // 表格表头
        contentStream.beginText();
        contentStream.setFont(boldFont, 9);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("序号", useChinese));
        contentStream.newLineAtOffset(40, 0);
        contentStream.showText(PdfUtils.safeText("款项类型", useChinese));
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText(PdfUtils.safeText("收款金额", useChinese));
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText(PdfUtils.safeText("本币金额", useChinese));
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText(PdfUtils.safeText("客户", useChinese));
        contentStream.newLineAtOffset(120, 0);
        contentStream.showText(PdfUtils.safeText("备注", useChinese));
        contentStream.endText();
        yPosition -= 15;

        // 表格线
        contentStream.moveTo(margin, yPosition + 10);
        contentStream.lineTo(560, yPosition + 10);
        contentStream.stroke();

        // 表体
        contentStream.setFont(regularFont, 9);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("1");
        contentStream.newLineAtOffset(40, 0);
        contentStream.showText(PdfUtils.safeText("预收款", useChinese));
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText(data.amount.isEmpty() ? "-" : data.amount);
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText(data.amount.isEmpty() ? "-" : data.amount);
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(data.customerName, 15), useChinese));
        contentStream.newLineAtOffset(120, 0);
        contentStream.showText("-");
        contentStream.endText();
        yPosition -= 20;

        // 合计行
        contentStream.moveTo(margin, yPosition + 10);
        contentStream.lineTo(560, yPosition + 10);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(boldFont, 9);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("合计", useChinese));
        contentStream.newLineAtOffset(120, 0);
        contentStream.showText((data.amount.isEmpty() ? "-" : data.amount + " CNY"));
        contentStream.endText();

        return yPosition - 20;
    }

    /**
     * 渲染页脚
     */
    private void renderFooter(PDPageContentStream contentStream, CollectionBillData data, int margin,
                              PDFont regularFont, boolean useChinese) throws IOException {
        float yPosition = 80;

        contentStream.moveTo(margin, yPosition + 20);
        contentStream.lineTo(560, yPosition + 20);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(regularFont, 9);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(PdfUtils.safeText("制单人: " + data.creator, useChinese));
        contentStream.newLineAtOffset(200, 0);
        contentStream.showText(PdfUtils.safeText("审核人: -", useChinese));
        contentStream.endText();

        // 脚注
        contentStream.beginText();
        contentStream.setFont(regularFont, 8);
        contentStream.newLineAtOffset(margin, 30);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateFormat.DATETIME));
        contentStream.showText(PdfUtils.safeText("由NexusArchive系统自动生成 - " + timestamp, useChinese));
        contentStream.endText();
    }

    /**
     * 收款单数据容器
     */
    private static class CollectionBillData {
        String billCode;
        String summary;
        String creator;
        String accountPeriod;
        String voucherNo;
        String sourceSystem;
        String customerName = "-";
        String amount = "-";
    }
}
