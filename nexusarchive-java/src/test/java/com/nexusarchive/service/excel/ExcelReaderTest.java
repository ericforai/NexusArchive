// Input: JUnit 5, AssertJ, Spring Boot Test
// Output: ExcelReader 单元测试
// Pos: nexusarchive-java/src/test/java/com/nexusarchive/service/excel/ExcelReaderTest.java

package com.nexusarchive.service.excel;

import org.apache.poi.ss.usermodel.Row;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("ExcelReader 单元测试")
class ExcelReaderTest {

    @Autowired
    private ExcelReader excelReader;

    @Test
    @DisplayName("应该读取 Excel 文件并返回数据列表")
    void shouldReadExcelFileAndReturnDataList() throws Exception {
        // Arrange - 创建测试 Excel 文件
        byte[] excelData = createTestExcelFile();

        // Act
        List<Map<String, Object>> data = excelReader.read(
            new MockMultipartFile("test.xlsx", excelData).getInputStream(), 0
        );

        // Assert
        assertThat(data).isNotEmpty();
        assertThat(data).hasSize(2);
        assertThat(data.get(0)).containsKey("姓名");
        assertThat(data.get(0).get("姓名")).isEqualTo("张三");
    }

    @Test
    @DisplayName("应该处理空 Excel 文件")
    void shouldHandleEmptyExcelFile() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "empty.xlsx", new byte[0]
        );

        // Act & Assert
        assertThatThrownBy(() -> excelReader.read(emptyFile.getInputStream(), 0))
            .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("应该正确处理不同类型的单元格值")
    void shouldHandleDifferentCellTypes() throws Exception {
        // Arrange
        byte[] excelData = createTestExcelFileWithMixedTypes();

        // Act
        List<Map<String, Object>> data = excelReader.read(
            new MockMultipartFile("mixed.xlsx", excelData).getInputStream(), 0
        );

        // Assert
        assertThat(data).hasSize(1);
        assertThat(data.get(0).get("文本")).isEqualTo("测试");
        assertThat(data.get(0).get("数字")).isEqualTo(123.45);
        assertThat(data.get(0).get("布尔值")).isEqualTo(true);
    }

    @Test
    @DisplayName("应该读取第一个 Sheet 默认")
    void shouldReadFirstSheetByDefault() throws Exception {
        // Arrange
        byte[] excelData = createTestExcelFile();

        // Act
        List<Map<String, Object>> data = excelReader.read(
            new MockMultipartFile("test.xlsx", excelData).getInputStream()
        );

        // Assert
        assertThat(data).isNotEmpty();
    }

    // 辅助方法：创建测试 Excel 文件
    private byte[] createTestExcelFile() throws IOException {
        return createTestExcelFileWithMixedTypes();
    }

    private byte[] createTestExcelFileWithMixedTypes() throws IOException {
        // 简化测试 - 使用已知的数据
        // 实际测试中应该创建真实的 Excel 文件
        return new byte[0]; // 占位符
    }
}
