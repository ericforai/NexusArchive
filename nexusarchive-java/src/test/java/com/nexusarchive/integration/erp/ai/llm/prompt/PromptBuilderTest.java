// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptBuilderTest.java
package com.nexusarchive.integration.erp.ai.llm.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PromptBuilderTest {

    @Autowired
    private CodeGenerationPromptBuilder promptBuilder;

    @Test
    void testBuildPrompt() {
        PromptContext context = PromptContext.builder()
            .erpType("yonsuite")
            .erpName("用友 YonSuite")
            .className("YonsuiteErpAdapter")
            .packageName("com.nexusarchive.integration.erp.adapter.yonsuite")
            .baseUrl("https://api.yonyoucloud.com")
            .authType("appkey")
            .apiDefinitions(List.of(
                PromptContext.ApiDefinition.builder()
                    .operationId("salesOutList")
                    .method("GET")
                    .path("/yiyan/salesOut/list")
                    .summary("销售出库单列表")
                    .requestSchema("{startDate: string, endDate: string}")
                    .responseSchema("{code: number, data: {records: []}}")
                    .build()
            ))
            .build();

        String prompt = promptBuilder.buildPrompt(context);

        assertNotNull(prompt);
        assertTrue(prompt.contains("YonsuiteErpAdapter"));
        assertTrue(prompt.contains("salesOutList"));
        assertTrue(prompt.contains("AppKey"));
        System.out.println(prompt);
    }
}
