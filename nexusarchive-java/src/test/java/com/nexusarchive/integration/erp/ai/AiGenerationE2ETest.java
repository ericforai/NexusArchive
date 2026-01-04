// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/AiGenerationE2ETest.java
package com.nexusarchive.integration.erp.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 代码生成 E2E 测试
 * <p>
 * 外部 LLM API 客户端已移除，此测试验证 API 正确返回已弃用错误。
 * 原 AI 生成功能已被替换为模板生成（参见 ErpAdaptationOrchestrator）。
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiGenerationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAiGenerationEndpointReturnsDeprecationError() throws Exception {
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-yonsuite-api.json",
            "application/json",
            """
            {
              "openapi": "3.0.0",
              "info": {"title": "Test API", "version": "1.0.0"},
              "paths": {
                "/api/test": {
                  "get": {
                    "operationId": "testOperation",
                    "responses": {"200": {"description": "OK"}}
                  }
                }
              }
            }
            """.getBytes()
        );

        // 验证 /generate-ai 端点返回已弃用错误
        mockMvc.perform(multipart("/erp-ai/generate-ai")
                .file(file)
                .param("erpType", "yonsuite")
                .param("erpName", "测试 YonSuite")
                .param("baseUrl", "https://api.test.com")
                .param("authType", "appkey")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                    "AI code generation has been removed. " +
                    "Please use the template-based code generation via the /adapt-with-deploy endpoint."
                ));
    }
}
