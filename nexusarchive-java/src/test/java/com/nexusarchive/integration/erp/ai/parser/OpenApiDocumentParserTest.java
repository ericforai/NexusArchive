// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDocumentParserTest.java
// Input: -
// Output: Test results
// Pos: AI 模块 - OpenAPI 文档解析器测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.parser;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenApiDocumentParser 测试
 */
class OpenApiDocumentParserTest {

    @Test
    void shouldParseValidOpenApiJson() throws IOException {
        // Given
        OpenApiDocumentParser parser = new OpenApiDocumentParser();

        String openApiJson = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {
                "/api/vouchers": {
                  "get": {
                    "operationId": "listVouchers",
                    "summary": "获取凭证列表",
                    "tags": ["vouchers"]
                  }
                }
              }
            }
            """;

        Path tempFile = Files.createTempFile("test-", ".json");
        Files.writeString(tempFile, openApiJson);

        MultipartFile multipartFile = new MockMultipartFile(
            "openapi.json",
            "openapi.json",
            "application/json",
            Files.readAllBytes(tempFile)
        );

        // When
        var result = parser.parse(multipartFile);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getDefinitions());
        assertEquals(1, result.getDefinitions().size());
        assertEquals("listVouchers", result.getDefinitions().get(0).getOperationId());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void shouldReturnFailureForInvalidJson() throws IOException {
        // Given
        OpenApiDocumentParser parser = new OpenApiDocumentParser();

        MultipartFile invalidFile = new MockMultipartFile(
            "invalid.json",
            "invalid.json",
            "application/json",
            "{ invalid json".getBytes()
        );

        // When
        var result = parser.parse(invalidFile);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
}
