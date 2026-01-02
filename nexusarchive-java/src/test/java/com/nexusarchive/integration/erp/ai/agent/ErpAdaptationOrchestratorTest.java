// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestratorTest.java
// Input: -
// Output: Test results
// Pos: AI 模块 - Agent 编排器测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.agent;

import com.nexusarchive.integration.erp.ai.generator.ErpAdapterCodeGenerator;
import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDocumentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpAdaptationOrchestrator 测试
 */
class ErpAdaptationOrchestratorTest {

    private ErpAdaptationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        OpenApiDocumentParser parser = new OpenApiDocumentParser();
        BusinessSemanticMapper mapper = new BusinessSemanticMapper();
        ErpAdapterCodeGenerator generator = new ErpAdapterCodeGenerator();
        orchestrator = new ErpAdaptationOrchestrator(parser, mapper, generator);
    }

    @Test
    void shouldAdaptErpFromOpenApiFile() throws IOException {
        // Given
        String openApiJson = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test ERP API",
                "version": "1.0.0"
              },
              "paths": {
                "/api/v1/vouchers": {
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

        ErpAdaptationOrchestrator.AdaptationRequest request = ErpAdaptationOrchestrator.AdaptationRequest.builder()
            .erpType("testerp")
            .erpName("Test ERP")
            .apiFiles(List.of(multipartFile))
            .build();

        // When
        var result = orchestrator.adapt(request);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getCode());
        assertNotNull(result.getMappings());
        assertFalse(result.getMappings().isEmpty());
        assertEquals("testerp", result.getAdapterId());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void shouldReturnFailureForInvalidFile() throws IOException {
        // Given
        MultipartFile invalidFile = new MockMultipartFile(
            "invalid.json",
            "invalid.json",
            "application/json",
            "{ invalid json".getBytes()
        );

        ErpAdaptationOrchestrator.AdaptationRequest request = ErpAdaptationOrchestrator.AdaptationRequest.builder()
            .erpType("testerp")
            .erpName("Test ERP")
            .apiFiles(List.of(invalidFile))
            .build();

        // When
        var result = orchestrator.adapt(request);

        // Then
        assertFalse(result.isSuccess());
    }
}
