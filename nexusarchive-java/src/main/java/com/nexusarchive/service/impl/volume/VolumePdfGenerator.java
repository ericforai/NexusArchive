// Input: Apache PDFBox、Jackson、Lombok
// Output: VolumePdfGenerator 类
// Pos: 案卷服务 - PDF生成层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.volume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 案卷PDF生成器
 * <p>
 * 生成合规的 PDF 版式文件作为占位凭证
 * </p>
 */
@Slf4j
public class VolumePdfGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成合规的 PDF 版式文件
     * 如果存在自定义元数据(包含分录详情)，则生成详细凭证；否则生成占位文件。
     */
    public static void generatePlaceholderPdf(Path targetPath, Archive archive) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // 尝试加载中文字体
            PDFont chineseFont = null;
            PDFont chineseFontBold = null;

            // 尝试从系统加载中文字体 (macOS, Linux, Windows 常见路径)
            String[] fontPaths = {
                // macOS
                "/System/Library/Fonts/STHeiti Light.ttc",
                "/System/Library/Fonts/STHeiti Medium.ttc",
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/PingFang.ttc",
                // Linux
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/noto-cjk/NotoSansSC-Regular.otf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                // Windows
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/simhei.ttf"
            };

            for (String fontPath : fontPaths) {
                try {
                    File fontFile = new File(fontPath);
                    if (fontFile.exists()) {
                        chineseFont = PDType0Font.load(document, fontFile);
                        chineseFontBold = chineseFont; // 使用相同字体作为粗体替代
                        log.debug("Loaded Chinese font from: {}", fontPath);
                        break;
                    }
                } catch (Exception e) {
                    log.trace("Failed to load font from: {}", fontPath);
                }
            }

            // 如果没有找到中文字体，回退到 ASCII 字体
            boolean useChinese = chineseFont != null;
            PDFont regularFont = useChinese ? chineseFont : PDType1Font.HELVETICA;
            PDFont boldFont = useChinese ? chineseFontBold : PDType1Font.HELVETICA_BOLD;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // 1. 标题
                contentStream.beginText();
                contentStream.setFont(boldFont, 18);
                contentStream.newLineAtOffset(160, 750);
                contentStream.showText(useChinese ? "会计凭证 - Accounting Voucher" : "ACCOUNTING VOUCHER");
                contentStream.endText();

                // 2. 头部信息
                contentStream.beginText();
                contentStream.setFont(regularFont, 10);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, 720);

                String voucherNo = archive.getArchiveCode() != null ? archive.getArchiveCode() : "";
                String docDate = archive.getDocDate() != null ? archive.getDocDate().toString() : "";
                contentStream.showText((useChinese ? "凭证号: " : "Voucher No: ") + VolumeUtils.safeText(voucherNo, useChinese));
                contentStream.newLineAtOffset(300, 0);
                contentStream.showText((useChinese ? "日期: " : "Date: ") + VolumeUtils.safeText(docDate, useChinese));
                contentStream.endText();

                // 3. 分录表格
                float yPosition = 680;
                int margin = 50;

                // 表头
                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(useChinese ? "摘要" : "Description");
                contentStream.newLineAtOffset(200, 0);
                contentStream.showText(useChinese ? "科目" : "Subject");
                contentStream.newLineAtOffset(150, 0);
                contentStream.showText(useChinese ? "借方" : "Debit");
                contentStream.newLineAtOffset(100, 0);
                contentStream.showText(useChinese ? "贷方" : "Credit");
                contentStream.endText();

                yPosition -= 20;
                contentStream.moveTo(margin, yPosition + 15);
                contentStream.lineTo(550, yPosition + 15);
                contentStream.stroke();

                // 尝试解析详细分录
                boolean hasDetails = renderEntries(contentStream, archive, margin, yPosition, useChinese, regularFont);

                if (!hasDetails) {
                    contentStream.beginText();
                    contentStream.setFont(regularFont, 10);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(useChinese ? "暂无详细分录信息" : "No detailed entries available. (Metadata missing)");
                    contentStream.endText();
                }

                // 4. 底部合计
                yPosition -= 30;
                contentStream.moveTo(margin, yPosition + 25);
                contentStream.lineTo(550, yPosition + 25);
                contentStream.stroke();

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                String amountStr = archive.getAmount() != null ? archive.getAmount().toString() : "0.00";
                String creatorStr = archive.getCreator() != null ? archive.getCreator() : "System";
                contentStream.showText((useChinese ? "合计金额: " : "Total Amount: ") + amountStr);
                contentStream.newLineAtOffset(300, 0);
                contentStream.showText((useChinese ? "制单人: " : "Creator: ") + VolumeUtils.safeText(creatorStr, useChinese));
                contentStream.endText();

                // 5. 脚注
                contentStream.beginText();
                contentStream.setFont(regularFont, 8);
                contentStream.newLineAtOffset(margin, 30);
                contentStream.showText(useChinese ? "由NexusArchive系统根据用友元数据自动生成" : "Generated by NexusArchive System based on YonSuite Metadata.");
                contentStream.endText();
            }

            document.save(targetPath.toFile());
        }
    }

    /**
     * 渲染分录详情
     */
    private static boolean renderEntries(PDPageContentStream contentStream, Archive archive,
                                          int margin, float yPosition, boolean useChinese, PDFont font) throws IOException {
        if (archive.getCustomMetadata() == null || archive.getCustomMetadata().isEmpty()) {
            return false;
        }

        try {
            JsonNode bodies = objectMapper.readTree(archive.getCustomMetadata());

            if (bodies.isArray()) {
                contentStream.setFont(font, 9);

                for (JsonNode body : bodies) {
                    String desc = body.path("description").asText("");
                    String subject = body.path("accsubject").path("name").asText("");
                    if (subject.isEmpty()) {
                        subject = body.path("accSubject").asText("");
                    }

                    double debit = body.path("debit_original").asDouble(0.0);
                    if (debit == 0.0) debit = body.path("debitOriginal").asDouble(0.0);

                    double credit = body.path("credit_original").asDouble(0.0);
                    if (credit == 0.0) credit = body.path("creditOriginal").asDouble(0.0);

                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(VolumeUtils.safeText(VolumeUtils.truncateText(desc, 30), useChinese));
                    contentStream.newLineAtOffset(200, 0);
                    contentStream.showText(VolumeUtils.safeText(VolumeUtils.truncateText(subject, 20), useChinese));
                    contentStream.newLineAtOffset(150, 0);
                    if (debit != 0) contentStream.showText(String.format("%.2f", debit));
                    contentStream.newLineAtOffset(100, 0);
                    if (credit != 0) contentStream.showText(String.format("%.2f", credit));
                    contentStream.endText();

                    yPosition -= 15;
                    if (yPosition < 50) {
                        break;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            log.debug("Failed to parse customMetadata: {}", e.getMessage());
        }
        return false;
    }
}
