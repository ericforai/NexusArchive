// Input: Apache PDFBox, Spring Framework
// Output: PdfWatermarkRenderer
// Pos: Service Layer

package com.nexusarchive.service.preview;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * PDF水印渲染器
 *
 * 负责使用PDFBox在PDF上添加水印并输出
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PdfWatermarkRenderer {

    private final WatermarkGenerator watermarkGenerator;

    /**
     * 渲染指定页面并添加水印
     *
     * @param pdfFile PDF文件
     * @param pageNumber 页码（从1开始）
     * @param watermarkText 水印文本
     * @param watermarkSubtext 水印副文本
     * @param traceId 追踪ID
     * @param response HTTP响应
     * @throws IOException 如果渲染失败
     */
    public void renderPageWithWatermark(File pdfFile, int pageNumber,
                                        String watermarkText, String watermarkSubtext,
                                        String traceId, HttpServletResponse response) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            validatePageNumber(pageNumber, totalPages);

            // 创建新文档，只包含指定页面
            PDDocument watermarkedDoc = new PDDocument();
            PDPage sourcePage = document.getPage(pageNumber - 1);
            PDPage newPage = new PDPage(sourcePage.getMediaBox());
            watermarkedDoc.addPage(newPage);

            // 添加水印到页面
            addWatermarkToPage(watermarkedDoc, newPage, watermarkText, watermarkSubtext);

            // 设置响应头
            setWatermarkResponseHeaders(response, traceId, watermarkText, watermarkSubtext, pageNumber, totalPages);

            // 输出到响应流
            writeDocumentToResponse(watermarkedDoc, response);

            log.info("服务端渲染带水印完成: pageNumber={}, traceId={}", pageNumber, traceId);
        }
    }

    /**
     * 验证页码是否有效
     */
    private void validatePageNumber(int pageNumber, int totalPages) throws IOException {
        if (pageNumber < 1 || pageNumber > totalPages) {
            throw new IOException("页码超出范围: " + pageNumber + ", 总页数: " + totalPages);
        }
    }

    /**
     * 向页面添加水印
     */
    private void addWatermarkToPage(PDDocument document, PDPage page,
                                    String watermarkText, String watermarkSubtext) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

            PDRectangle mediaBox = page.getMediaBox();
            float width = mediaBox.getWidth();
            float height = mediaBox.getHeight();

            // 添加主水印文本（倾斜、半透明）
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 48);
            contentStream.setNonStrokingColor(new Color(200, 200, 200, 100)); // 半透明灰色

            // 计算主文本位置（居中）
            float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(watermarkText) / 1000 * 48;
            float textX = (width - textWidth) / 2;
            float textY = height / 2;

            // 绘制主水印文本
            drawRotatedText(contentStream, watermarkText, textX, textY, 48);

            // 添加副文本
            contentStream.setFont(PDType1Font.HELVETICA, 24);
            float subtextWidth = PDType1Font.HELVETICA.getStringWidth(watermarkSubtext) / 1000 * 24;
            float subtextX = (width - subtextWidth) / 2;
            float subtextY = height / 2 - 60;

            drawRotatedText(contentStream, watermarkSubtext, subtextX, subtextY, 24);
        }
    }

    /**
     * 绘制旋转文本
     */
    private void drawRotatedText(PDPageContentStream contentStream, String text,
                                float x, float y, float fontSize) throws IOException {
        contentStream.saveGraphicsState();
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        Matrix rotationMatrix = Matrix.getRotateInstance(Math.toRadians(-45), x, y);
        contentStream.transform(rotationMatrix);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.restoreGraphicsState();
    }

    /**
     * 设置水印响应头
     */
    private void setWatermarkResponseHeaders(HttpServletResponse response, String traceId,
                                             String watermarkText, String watermarkSubtext,
                                             int pageNumber, int totalPages) {
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setHeader("X-Trace-Id", traceId);
        response.setHeader("X-Watermark-Text", watermarkText);
        response.setHeader("X-Watermark-Subtext", watermarkSubtext);
        response.setHeader("X-Watermark-Opacity", "0.3");
        response.setHeader("X-Watermark-Rotate", "-45");
        response.setHeader("X-Page-Number", String.valueOf(pageNumber));
        response.setHeader("X-Total-Pages", String.valueOf(totalPages));
    }

    /**
     * 将文档写入响应流
     */
    private void writeDocumentToResponse(PDDocument document, HttpServletResponse response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();

        byte[] pdfBytes = baos.toByteArray();
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdfBytes.length));
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }
}
