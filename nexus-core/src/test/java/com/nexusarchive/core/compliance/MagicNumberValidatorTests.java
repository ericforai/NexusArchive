// Input: JUnit + 测试文件
// Output: Magic Number 验证器单元测试
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicNumberValidatorTests {
    private final MagicNumberValidator validator = new MagicNumberValidator();

    @Test
    void shouldDetectPdf() throws IOException {
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        MagicNumberValidator.FileType type = validator.detectFileType(new ByteArrayInputStream(pdfHeader));
        assertEquals(MagicNumberValidator.FileType.PDF, type);
    }

    @Test
    void shouldDetectZipAsOfd() throws IOException {
        byte[] zipHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00};
        MagicNumberValidator.FileType type = validator.detectFileType(new ByteArrayInputStream(zipHeader));
        assertEquals(MagicNumberValidator.FileType.OFD, type);
    }

    @Test
    void shouldDetectXml() throws IOException {
        byte[] xmlHeader = "<?xml version=\"1.0\"?>".getBytes();
        MagicNumberValidator.FileType type = validator.detectFileType(new ByteArrayInputStream(xmlHeader));
        assertEquals(MagicNumberValidator.FileType.XML, type);
    }

    @Test
    void shouldDetectJpeg() throws IOException {
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MagicNumberValidator.FileType type = validator.detectFileType(new ByteArrayInputStream(jpegHeader));
        assertEquals(MagicNumberValidator.FileType.JPEG, type);
    }

    @Test
    void shouldReturnUnknownForEmpty() throws IOException {
        byte[] empty = new byte[0];
        MagicNumberValidator.FileType type = validator.detectFileType(new ByteArrayInputStream(empty));
        assertEquals(MagicNumberValidator.FileType.UNKNOWN, type);
    }

    @Test
    void shouldMatchExtension() {
        assertTrue(MagicNumberValidator.FileType.PDF.matchesExtension("pdf"));
        assertTrue(MagicNumberValidator.FileType.PDF.matchesExtension(".pdf"));
        assertTrue(MagicNumberValidator.FileType.JPEG.matchesExtension("jpeg"));
        assertTrue(MagicNumberValidator.FileType.JPEG.matchesExtension("jpg"));
    }
}
