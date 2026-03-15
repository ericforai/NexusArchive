// Input: JUnit 5、FileExtensions 常量类
// Output: FileExtensionsTest 测试类
// Pos: 测试模块/常量测试

package com.nexusarchive.common.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileExtensions 常量类单元测试
 *
 * 验证文件扩展名常量值的正确性
 */
@DisplayName("FileExtensions 常量测试")
@Tag("unit")
class FileExtensionsTest {

    @Test
    @DisplayName("PDF 常量值应为 '.pdf'")
    void pdfConstantShouldBeCorrect() {
        assertEquals(".pdf", FileExtensions.PDF);
    }

    @Test
    @DisplayName("OFD 常量值应为 '.ofd'")
    void ofdConstantShouldBeCorrect() {
        assertEquals(".ofd", FileExtensions.OFD);
    }

    @Test
    @DisplayName("JSON 常量值应为 '.json'")
    void jsonConstantShouldBeCorrect() {
        assertEquals(".json", FileExtensions.JSON);
    }

    @Test
    @DisplayName("XML 常量值应为 '.xml'")
    void xmlConstantShouldBeCorrect() {
        assertEquals(".xml", FileExtensions.XML);
    }

    @Test
    @DisplayName("PNG 常量值应为 '.png'")
    void pngConstantShouldBeCorrect() {
        assertEquals(".png", FileExtensions.PNG);
    }

    @Test
    @DisplayName("JPG 常量值应为 '.jpg'")
    void jpgConstantShouldBeCorrect() {
        assertEquals(".jpg", FileExtensions.JPG);
    }

    @Test
    @DisplayName("ZIP 常量值应为 '.zip'")
    void zipConstantShouldBeCorrect() {
        assertEquals(".zip", FileExtensions.ZIP);
    }

    @Test
    @DisplayName("所有文件扩展名常量应以点开头")
    void allExtensionsShouldStartWithDot() {
        assertTrue(FileExtensions.PDF.startsWith("."));
        assertTrue(FileExtensions.OFD.startsWith("."));
        assertTrue(FileExtensions.JSON.startsWith("."));
        assertTrue(FileExtensions.XML.startsWith("."));
        assertTrue(FileExtensions.PNG.startsWith("."));
        assertTrue(FileExtensions.JPG.startsWith("."));
        assertTrue(FileExtensions.ZIP.startsWith("."));
    }

    @Test
    @DisplayName("版式文档扩展名应包含 PDF 和 OFD")
    void shouldContainArchiveFormats() {
        assertEquals(".pdf", FileExtensions.PDF);
        assertEquals(".ofd", FileExtensions.OFD);
    }

    @Test
    @DisplayName("数据格式扩展名应包含 JSON 和 XML")
    void shouldContainDataFormats() {
        assertEquals(".json", FileExtensions.JSON);
        assertEquals(".xml", FileExtensions.XML);
    }

    @Test
    @DisplayName("图像格式扩展名应包含 PNG 和 JPG")
    void shouldContainImageFormats() {
        assertEquals(".png", FileExtensions.PNG);
        assertEquals(".jpg", FileExtensions.JPG);
    }

    @Test
    @DisplayName("压缩格式扩展名应包含 ZIP")
    void shouldContainCompressionFormat() {
        assertEquals(".zip", FileExtensions.ZIP);
    }

    @Test
    @DisplayName("所有常量值应为小写")
    void allConstantsShouldBeLowerCase() {
        assertEquals(FileExtensions.PDF, FileExtensions.PDF.toLowerCase());
        assertEquals(FileExtensions.OFD, FileExtensions.OFD.toLowerCase());
        assertEquals(FileExtensions.JSON, FileExtensions.JSON.toLowerCase());
        assertEquals(FileExtensions.XML, FileExtensions.XML.toLowerCase());
        assertEquals(FileExtensions.PNG, FileExtensions.PNG.toLowerCase());
        assertEquals(FileExtensions.JPG, FileExtensions.JPG.toLowerCase());
        assertEquals(FileExtensions.ZIP, FileExtensions.ZIP.toLowerCase());
    }

    @Test
    @DisplayName("常量值不应为空")
    void constantsShouldNotBeBlank() {
        assertFalse(FileExtensions.PDF.isBlank());
        assertFalse(FileExtensions.OFD.isBlank());
        assertFalse(FileExtensions.JSON.isBlank());
        assertFalse(FileExtensions.XML.isBlank());
        assertFalse(FileExtensions.PNG.isBlank());
        assertFalse(FileExtensions.JPG.isBlank());
        assertFalse(FileExtensions.ZIP.isBlank());
    }

    @Test
    @DisplayName("扩展名常量应只包含点和字母")
    void extensionsShouldContainOnlyDotAndLetters() {
        assertTrue(FileExtensions.PDF.matches("^.[a-z]+$"));
        assertTrue(FileExtensions.OFD.matches("^.[a-z]+$"));
        assertTrue(FileExtensions.JSON.matches("^.[a-z]+$"));
        assertTrue(FileExtensions.XML.matches("^.[a-z]+$"));
        assertTrue(FileExtensions.PNG.matches("^.[a-z]+$"));
        assertTrue(FileExtensions.JPG.matches("^.[a-z]+$"));
        assertTrue(FileExtensions.ZIP.matches("^.[a-z]+$"));
    }

    @Test
    @DisplayName("所有扩展名应为 DA/T 94-2022 允许的格式")
    void allExtensionsShouldBeAllowedByArchiveStandard() {
        // DA/T 94-2022 允许的电子档案格式
        String[] allowedExtensions = {".pdf", ".ofd", ".xml", ".json", ".png", ".jpg", ".zip"};

        assertEquals(".pdf", FileExtensions.PDF);
        assertEquals(".ofd", FileExtensions.OFD);
        assertEquals(".xml", FileExtensions.XML);
        assertEquals(".json", FileExtensions.JSON);
        assertEquals(".png", FileExtensions.PNG);
        assertEquals(".jpg", FileExtensions.JPG);
        assertEquals(".zip", FileExtensions.ZIP);
    }
}
