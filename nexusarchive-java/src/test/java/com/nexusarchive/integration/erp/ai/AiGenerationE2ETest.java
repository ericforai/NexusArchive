// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/AiGenerationE2ETest.java
package com.nexusarchive.integration.erp.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "CLAUDE_API_KEY", matches = ".+")
class AiGenerationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullAiGenerationFlow() throws Exception {
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-yonsuite-api.json",
            "application/json",
            getClass().getResourceAsStream("/test-yonsuite-api.json").readAllBytes()
        );

        // 调用生成接口
        mockMvc.perform(multipart("/erp-ai/generate-ai")
                .file(file)
                .param("erpType", "yonsuite")
                .param("erpName", "测试 YonSuite")
                .param("baseUrl", "https://api.test.com")
                .param("authType", "appkey")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.generatedCode").isString())
                .andExpect(jsonPath("$.data.status").value("GENERATED"));

        // TODO: 测试反馈和重新生成
        // TODO: 测试部署流程
    }
}
