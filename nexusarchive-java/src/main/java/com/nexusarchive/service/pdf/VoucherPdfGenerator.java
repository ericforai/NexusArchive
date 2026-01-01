// Input: Apache PDFBox、Jackson JsonNode
// Output: VoucherPdfGenerator 类
// Pos: PDF 生成器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import com.fasterxml.jackson.databind.JsonNode;
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
 * 会计凭证 PDF 生成器
 * <p>
 * 生成 A4 竖向会计凭证 PDF，包含凭证分录表
 * </p>
 */
@Slf4j
public class VoucherPdfGenerator {

    /**
     * 生成凭证 PDF 文件
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
            int margin = 50;
            float yPosition = 750;

            // 从 JSON 解析头部信息
            JsonNode header = voucherData.has("header") ? voucherData.get("header") : voucherData;
            VoucherHeaderData headerData = parseHeader(header, fileContent);

            // 标题
            renderTitle(contentStream, yPosition, boldFont, useChinese);
            yPosition -= 30;

            // 头部信息
            yPosition = renderHeader(contentStream, headerData, margin, yPosition, regularFont, useChinese);
            yPosition -= 25;

            // 分录表格
            yPosition = renderEntriesTable(contentStream, voucherData, yPosition, margin, regularFont,
                    boldFont, useChinese);

            // 底部信息
            renderFooter(contentStream, headerData, fileContent, margin, regularFont, useChinese);
        }

        log.info("凭证PDF生成成功: {}", targetPath);
    }

    /**
     * 解析凭证头部数据
     */
    private VoucherHeaderData parseHeader(JsonNode header, ArcFileContent fileContent) {
        VoucherHeaderData data = new VoucherHeaderData();

        // 账簿/组织名称
        if (header.has("accbook") && header.get("accbook").has("pk_org")) {
            data.orgName = header.path("accbook").path("pk_org").path("name").asText("");
        }
        if (data.orgName.isEmpty() && header.has("accbook")) {
            data.orgName = header.path("accbook").path("name").asText("");
        }

        // 凭证字号
        String voucherStr = header.path("vouchertype").path("voucherstr").asText("");
        String displayName = header.path("displayname").asText(
                fileContent.getErpVoucherNo() != null ? fileContent.getErpVoucherNo() : "");
        data.fullVoucherNo = (voucherStr.isEmpty() ? "" : voucherStr + " ") + displayName + "号";

        // 期间
        data.period = header.path("period").asText(
                fileContent.getFiscalYear() != null ? fileContent.getFiscalYear() : "");

        // 制单日期
        data.makeTime = header.path("maketime").asText("");

        // 附单据数
        data.attachmentQty = header.path("attachmentQuantity").asText("--");

        return data;
    }

    /**
     * 渲染标题
     */
    private void renderTitle(PDPageContentStream contentStream, float yPosition, PDFont boldFont,
                             boolean useChinese) throws IOException {
        contentStream.beginText();
        contentStream.setFont(boldFont, 16);
        contentStream.newLineAtOffset(180, yPosition);
        contentStream.showText(useChinese ? "会计凭证 - Accounting Voucher" : "ACCOUNTING VOUCHER");
        contentStream.endText();
    }

    /**
     * 渲染凭证头部
     */
    private float renderHeader(PDPageContentStream contentStream, VoucherHeaderData data, int margin,
                                float yPosition, PDFont regularFont, boolean useChinese) throws IOException {
        // 账簿名称
        if (!data.orgName.isEmpty()) {
            contentStream.beginText();
            contentStream.setFont(regularFont, 10);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText((useChinese ? "账簿: " : "Book: ") + PdfUtils.safeText(data.orgName, useChinese));
            contentStream.endText();
            yPosition -= 18;
        }

        // 凭证号、制单日期、期间
        contentStream.beginText();
        contentStream.setFont(regularFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText((useChinese ? "凭证号: " : "Voucher: ") + PdfUtils.safeText(data.fullVoucherNo, useChinese));
        contentStream.newLineAtOffset(150, 0);
        contentStream.showText((useChinese ? "制单日期: " : "Date: ") + data.makeTime);
        contentStream.newLineAtOffset(150, 0);
        contentStream.showText((useChinese ? "期间: " : "Period: ") + data.period);
        contentStream.endText();
        yPosition -= 18;

        // 附单据数
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText((useChinese ? "附单据数: " : "Attachments: ") + data.attachmentQty);
        contentStream.endText();

        return yPosition;
    }

    /**
     * 渲染分录表格
     */
    private float renderEntriesTable(PDPageContentStream contentStream, JsonNode voucherData,
                                      float yPosition, int margin, PDFont regularFont, PDFont boldFont,
                                      boolean useChinese) throws IOException {
        // 表头
        contentStream.beginText();
        contentStream.setFont(boldFont, 9);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(useChinese ? "分录" : "No.");
        contentStream.newLineAtOffset(30, 0);
        contentStream.showText(useChinese ? "摘要" : "Description");
        contentStream.newLineAtOffset(120, 0);
        contentStream.showText(useChinese ? "科目" : "Subject");
        contentStream.newLineAtOffset(160, 0);
        contentStream.showText(useChinese ? "币种" : "Cur");
        contentStream.newLineAtOffset(40, 0);
        contentStream.showText(useChinese ? "借方" : "Debit");
        contentStream.newLineAtOffset(70, 0);
        contentStream.showText(useChinese ? "贷方" : "Credit");
        contentStream.endText();

        yPosition -= 18;
        contentStream.moveTo(margin, yPosition + 12);
        contentStream.lineTo(560, yPosition + 12);
        contentStream.stroke();

        // 分录数据
        boolean hasEntries = renderVoucherEntries(contentStream, voucherData, yPosition, margin, regularFont,
                useChinese);

        if (!hasEntries) {
            contentStream.beginText();
            contentStream.setFont(regularFont, 10);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(useChinese ? "暂无详细分录信息" : "No detailed entries available.");
            contentStream.endText();
        }

        return yPosition;
    }

    /**
     * 渲染凭证分录
     */
    private boolean renderVoucherEntries(PDPageContentStream contentStream, JsonNode voucherData,
                                          float startY, int margin, PDFont regularFont,
                                          boolean useChinese) throws IOException {
        float yPosition = startY;
        boolean hasDetails = false;

        JsonNode bodies = findBodiesArray(voucherData);

        if (bodies != null && bodies.isArray() && bodies.size() > 0) {
            hasDetails = true;
            contentStream.setFont(regularFont, 9);

            double totalDebit = 0.0;
            double totalCredit = 0.0;

            for (JsonNode body : bodies) {
                VoucherEntry entry = parseEntry(body);

                totalDebit += entry.debit;
                totalCredit += entry.credit;

                yPosition = renderEntry(contentStream, entry, margin, yPosition, regularFont, useChinese);

                // 辅助核算附加行
                if (!entry.auxiliaryInfo.isEmpty()) {
                    yPosition = renderAuxiliaryLine(contentStream, entry.auxiliaryInfo, margin, yPosition,
                            regularFont, useChinese);
                }

                // 现金流量附加行
                if (!entry.cashFlowInfo.isEmpty()) {
                    yPosition = renderCashFlowLine(contentStream, entry.cashFlowInfo, margin, yPosition,
                            regularFont, useChinese);
                }

                if (yPosition < 130) {
                    break;
                }
            }

            // 绘制合计行
            yPosition = renderTotalLine(contentStream, totalDebit, totalCredit, margin, yPosition, regularFont,
                    useChinese);
        }

        return hasDetails;
    }

    /**
     * 查找分录数组
     */
    private JsonNode findBodiesArray(JsonNode voucherData) {
        if (voucherData.has("body") && voucherData.get("body").isArray()) {
            return voucherData.get("body");
        } else if (voucherData.has("bodies") && voucherData.get("bodies").isArray()) {
            return voucherData.get("bodies");
        } else if (voucherData.isArray()) {
            return voucherData;
        }
        return null;
    }

    /**
     * 解析单条分录
     */
    private VoucherEntry parseEntry(JsonNode body) {
        VoucherEntry entry = new VoucherEntry();

        entry.recordNumber = body.path("recordnumber").asInt(0);
        if (entry.recordNumber == 0) {
            entry.recordNumber = body.path("recordNumber").asInt(0);
        }

        entry.description = PdfDataParser.getTextValue(body, "description", "digest", "摘要", "desc");

        // 科目
        if (body.has("accsubject") && !body.get("accsubject").isNull()) {
            JsonNode accSubjectNode = body.get("accsubject");
            entry.subjectCode = accSubjectNode.path("code").asText("");
            entry.subjectName = accSubjectNode.path("name").asText("");
        }
        if (entry.subjectName.isEmpty()) {
            entry.subjectName = PdfDataParser.getTextValue(body, "accSubject", "subjectName", "科目名称", "subject");
        }
        entry.subjectDisplay = entry.subjectCode.isEmpty() ? entry.subjectName
                : entry.subjectCode + " " + entry.subjectName;

        // 币种
        if (body.has("currency") && !body.get("currency").isNull()) {
            JsonNode currencyNode = body.get("currency");
            entry.currencyCode = currencyNode.isObject() ? currencyNode.path("code").asText("CNY")
                    : currencyNode.asText("CNY");
        }

        entry.debit = PdfDataParser.getAmountValue(body, "debit_original", "debitOriginal", "debitOrg",
                "debit_org", "debit");
        entry.credit = PdfDataParser.getAmountValue(body, "credit_original", "creditOriginal", "creditOrg",
                "credit_org", "credit");

        entry.auxiliaryInfo = PdfDataParser.parseAuxiliaryItems(body);
        entry.cashFlowInfo = PdfDataParser.parseCashFlowItems(body);

        return entry;
    }

    /**
     * 渲染单条分录
     */
    private float renderEntry(PDPageContentStream contentStream, VoucherEntry entry, int margin,
                               float yPosition, PDFont regularFont, boolean useChinese) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(entry.recordNumber > 0 ? String.valueOf(entry.recordNumber) : "");
        contentStream.newLineAtOffset(30, 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(entry.description, 18), useChinese));
        contentStream.newLineAtOffset(120, 0);
        contentStream.showText(PdfUtils.safeText(PdfUtils.truncateText(entry.subjectDisplay, 22), useChinese));
        contentStream.newLineAtOffset(160, 0);
        contentStream.showText(entry.currencyCode);
        contentStream.newLineAtOffset(40, 0);
        if (entry.debit != 0) {
            contentStream.showText(PdfUtils.formatAmount(entry.debit));
        }
        contentStream.newLineAtOffset(70, 0);
        if (entry.credit != 0) {
            contentStream.showText(PdfUtils.formatAmount(entry.credit));
        }
        contentStream.endText();

        return yPosition - 13;
    }

    /**
     * 渲染辅助核算附加行
     */
    private float renderAuxiliaryLine(PDPageContentStream contentStream, String auxiliaryInfo, int margin,
                                       float yPosition, PDFont regularFont, boolean useChinese) throws IOException {
        contentStream.beginText();
        contentStream.setFont(regularFont, 8);
        contentStream.newLineAtOffset(margin + 30, yPosition);
        contentStream.showText(PdfUtils.safeText("  [辅助] " + PdfUtils.truncateText(auxiliaryInfo, 60), useChinese));
        contentStream.endText();
        contentStream.setFont(regularFont, 9);
        return yPosition - 11;
    }

    /**
     * 渲染现金流量附加行
     */
    private float renderCashFlowLine(PDPageContentStream contentStream, String cashFlowInfo, int margin,
                                      float yPosition, PDFont regularFont, boolean useChinese) throws IOException {
        contentStream.beginText();
        contentStream.setFont(regularFont, 8);
        contentStream.newLineAtOffset(margin + 30, yPosition);
        contentStream.showText(PdfUtils.safeText("  [现金流量] " + PdfUtils.truncateText(cashFlowInfo, 55), useChinese));
        contentStream.endText();
        contentStream.setFont(regularFont, 9);
        return yPosition - 11;
    }

    /**
     * 渲染合计行
     */
    private float renderTotalLine(PDPageContentStream contentStream, double totalDebit, double totalCredit,
                                   int margin, float yPosition, PDFont regularFont, boolean useChinese)
            throws IOException {
        yPosition -= 5;
        contentStream.moveTo(margin, yPosition + 10);
        contentStream.lineTo(560, yPosition + 10);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(regularFont, 9);
        contentStream.newLineAtOffset(margin + 30 + 120 + 160, yPosition);
        contentStream.showText(useChinese ? "合计" : "Total");
        contentStream.newLineAtOffset(40, 0);
        contentStream.showText(PdfUtils.formatAmount(totalDebit));
        contentStream.newLineAtOffset(70, 0);
        contentStream.showText(PdfUtils.formatAmount(totalCredit));
        contentStream.endText();

        return yPosition;
    }

    /**
     * 渲染页脚
     */
    private void renderFooter(PDPageContentStream contentStream, VoucherHeaderData headerData,
                              ArcFileContent fileContent, int margin, PDFont regularFont, boolean useChinese)
            throws IOException {
        float yPosition = 100;

        contentStream.moveTo(margin, yPosition + 25);
        contentStream.lineTo(550, yPosition + 25);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(regularFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        String creator = fileContent.getCreator() != null ? fileContent.getCreator() : "System";
        String sourceSystem = fileContent.getSourceSystem() != null ? fileContent.getSourceSystem() : "Unknown";
        contentStream.showText((useChinese ? "制单人: " : "Creator: ") + PdfUtils.safeText(creator, useChinese));
        contentStream.newLineAtOffset(250, 0);
        contentStream.showText((useChinese ? "来源系统: " : "Source: ") + sourceSystem);
        contentStream.endText();

        // 脚注
        contentStream.beginText();
        contentStream.setFont(regularFont, 8);
        contentStream.newLineAtOffset(margin, 30);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        contentStream.showText(
                (useChinese ? "由NexusArchive系统自动生成 - " : "Generated by NexusArchive - ") + timestamp);
        contentStream.endText();
    }

    /**
     * 凭证头部数据
     */
    private static class VoucherHeaderData {
        String orgName = "";
        String fullVoucherNo = "";
        String period = "";
        String makeTime = "";
        String attachmentQty = "--";
    }

    /**
     * 凭证分录数据
     */
    private static class VoucherEntry {
        int recordNumber;
        String description = "";
        String subjectCode = "";
        String subjectName = "";
        String subjectDisplay = "";
        String currencyCode = "CNY";
        double debit;
        double credit;
        String auxiliaryInfo = "";
        String cashFlowInfo = "";
    }
}
