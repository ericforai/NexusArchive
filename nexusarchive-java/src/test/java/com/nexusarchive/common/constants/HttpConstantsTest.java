// Input: JUnit 5、HttpConstants 常量类
// Output: HttpConstantsTest 测试类
// Pos: 测试模块/常量测试

package com.nexusarchive.common.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpConstants 常量类单元测试
 *
 * 验证 HTTP 常量值的正确性
 */
@DisplayName("HttpConstants 常量测试")
@Tag("unit")
class HttpConstantsTest {

    @Test
    @DisplayName("APPLICATION_JSON 常量值应为 'application/json'")
    void applicationJsonConstantShouldBeCorrect() {
        assertEquals("application/json", HttpConstants.APPLICATION_JSON);
    }

    @Test
    @DisplayName("APPLICATION_PDF 常量值应为 'application/pdf'")
    void applicationPdfConstantShouldBeCorrect() {
        assertEquals("application/pdf", HttpConstants.APPLICATION_PDF);
    }

    @Test
    @DisplayName("APPLICATION_OFD 常量值应为 'application/ofd'")
    void applicationOfdConstantShouldBeCorrect() {
        assertEquals("application/ofd", HttpConstants.APPLICATION_OFD);
    }

    @Test
    @DisplayName("CONTENT_TYPE 常量值应为 'Content-Type'")
    void contentTypeConstantShouldBeCorrect() {
        assertEquals("Content-Type", HttpConstants.CONTENT_TYPE);
    }

    @Test
    @DisplayName("AUTHORIZATION 常量值应为 'Authorization'")
    void authorizationConstantShouldBeCorrect() {
        assertEquals("Authorization", HttpConstants.AUTHORIZATION);
    }

    @Test
    @DisplayName("所有 MIME 类型常量应包含 'application/' 前缀")
    void mimeTypeConstantsShouldHaveApplicationPrefix() {
        assertTrue(HttpConstants.APPLICATION_JSON.startsWith("application/"));
        assertTrue(HttpConstants.APPLICATION_PDF.startsWith("application/"));
        assertTrue(HttpConstants.APPLICATION_OFD.startsWith("application/"));
    }

    @Test
    @DisplayName("HTTP 头常量应符合标准命名")
    void headerConstantsShouldFollowStandardNaming() {
        // HTTP 头名称对大小写不敏感，但标准写法是每个单词首字母大写
        assertEquals("Content-Type", HttpConstants.CONTENT_TYPE);
        assertEquals("Authorization", HttpConstants.AUTHORIZATION);
    }

    @Test
    @DisplayName("常量值不应为空")
    void constantsShouldNotBeBlank() {
        assertFalse(HttpConstants.APPLICATION_JSON.isBlank());
        assertFalse(HttpConstants.APPLICATION_PDF.isBlank());
        assertFalse(HttpConstants.APPLICATION_OFD.isBlank());
        assertFalse(HttpConstants.CONTENT_TYPE.isBlank());
        assertFalse(HttpConstants.AUTHORIZATION.isBlank());
    }

    @Test
    @DisplayName("MIME 类型常量应使用小写")
    void mimeTypeConstantsShouldBeLowerCase() {
        assertEquals(HttpConstants.APPLICATION_JSON, HttpConstants.APPLICATION_JSON.toLowerCase());
        assertEquals(HttpConstants.APPLICATION_PDF, HttpConstants.APPLICATION_PDF.toLowerCase());
        assertEquals(HttpConstants.APPLICATION_OFD, HttpConstants.APPLICATION_OFD.toLowerCase());
    }
}
