// Input: JUnit + PDFBox 水印服务
// Output: PdfWatermarkService 流式水印测试
// Pos: NexusCore tests/compliance
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfWatermarkServiceTests {
    private final PdfWatermarkService service = new PdfWatermarkService();

    @TempDir
    Path tempDir;

    @Test
    void shouldStreamWatermarkedPdf() throws Exception {
        Path sourcePdf = createSamplePdf(tempDir.resolve("watermark-source.pdf"));

        Path outputPath;
        try (InputStream output = service.addWatermark(
                Files.newInputStream(sourcePdf),
                WatermarkConfig.of("tester", "trace-001"))) {
            assertTrue(output instanceof PdfWatermarkService.TempFileInputStream);
            outputPath = ((PdfWatermarkService.TempFileInputStream) output).getOutputPath();

            byte[] bytes = output.readAllBytes();
            assertTrue(bytes.length > 0);

            try (PDDocument doc = Loader.loadPDF(bytes)) {
                assertNotNull(doc);
                assertTrue(doc.getNumberOfPages() > 0);
            }
        }
        assertFalse(Files.exists(outputPath));
    }

    private Path createSamplePdf(Path target) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(target.toFile());
        }
        return target;
    }
}
