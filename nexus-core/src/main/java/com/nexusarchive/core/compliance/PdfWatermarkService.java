// Input: PDFBox 水印绘制（流式输出）
// Output: PDF 水印服务实现
// Pos: NexusCore compliance/watermark
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PDF 水印服务实现
 * 
 * 使用 PDFBox 3.x 为每页添加斜向水印
 */
@Service
public class PdfWatermarkService implements WatermarkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfWatermarkService.class);

    @Override
    public InputStream addWatermark(InputStream source, WatermarkConfig config) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(config, "config must not be null");

        try {
            Path sourcePath = writeToTempFile(source, "nexus-watermark-source-", ".pdf");
            Path outputPath = Files.createTempFile("nexus-watermark-output-", ".pdf");
            Files.deleteIfExists(outputPath);
            int pageCount;

            try (PDDocument doc = Loader.loadPDF(sourcePath.toFile())) {
                pageCount = doc.getNumberOfPages();
                for (int i = 0; i < pageCount; i++) {
                    PDPage page = doc.getPage(i);
                    addWatermarkToPage(doc, page, config);
                }
                doc.save(outputPath.toFile());
            }

            LOGGER.info("水印添加完成: pages={}, text={}", pageCount, config.primaryText());
            return new TempFileInputStream(outputPath, sourcePath);
        } catch (IOException ex) {
            LOGGER.error("水印添加失败", ex);
            throw new RuntimeException("水印添加失败: " + ex.getMessage(), ex);
        }
    }

    private void addWatermarkToPage(PDDocument doc, PDPage page, WatermarkConfig config) 
            throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        try (PDPageContentStream contentStream = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            // 设置透明度
            PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
            graphicsState.setNonStrokingAlphaConstant(config.opacity());
            graphicsState.setStrokingAlphaConstant(config.opacity());
            contentStream.setGraphicsStateParameters(graphicsState);

            // 设置字体和颜色
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            contentStream.setFont(font, config.fontSize());
            contentStream.setNonStrokingColor(Color.LIGHT_GRAY);

            // 计算旋转变换
            float centerX = pageWidth / 2;
            float centerY = pageHeight / 2;
            double radians = Math.toRadians(config.rotation());

            Matrix matrix = Matrix.getRotateInstance(radians, centerX, centerY);
            contentStream.transform(matrix);

            // 绘制主水印文本
            String watermarkText = config.primaryText() + " | " + config.secondaryText();
            float textWidth = font.getStringWidth(watermarkText) / 1000 * config.fontSize();
            float textX = centerX - textWidth / 2;
            float textY = centerY;

            contentStream.beginText();
            contentStream.newLineAtOffset(textX, textY);
            contentStream.showText(watermarkText);
            contentStream.endText();
        }
    }

    private Path writeToTempFile(InputStream source, String prefix, String suffix) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        try (InputStream input = source;
             OutputStream output = Files.newOutputStream(tempFile)) {
            input.transferTo(output);
        }
        return tempFile;
    }

    public static final class TempFileInputStream extends java.io.FilterInputStream {
        private final Path outputPath;
        private final Path sourcePath;

        public TempFileInputStream(Path outputPath, Path sourcePath) throws IOException {
            super(Files.newInputStream(outputPath));
            this.outputPath = outputPath;
            this.sourcePath = sourcePath;
        }

        public Path getOutputPath() {
            return outputPath;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                deleteQuietly(outputPath);
                deleteQuietly(sourcePath);
            }
        }

        private void deleteQuietly(Path path) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                LOGGER.warn("水印临时文件清理失败: {}", path);
            }
        }
    }
}
