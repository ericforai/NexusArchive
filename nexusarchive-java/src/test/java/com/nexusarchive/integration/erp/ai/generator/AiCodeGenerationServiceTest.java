// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationServiceTest.java
// Input: OpenAPI definitions
// Output: GeneratedCode validation
// Pos: AI 模块 - AI 代码生成服务测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.config.AiProperties;
import com.nexusarchive.integration.erp.ai.llm.ClaudeApiClient;
import com.nexusarchive.integration.erp.ai.llm.parser.CodeParser;
import com.nexusarchive.integration.erp.ai.llm.parser.JavaSyntaxValidator;
import com.nexusarchive.integration.erp.ai.llm.prompt.CodeGenerationPromptBuilder;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 代码生成服务测试
 * <p>
 * 需要设置系统属性 -DCLAUDE_API_KEY=your_key 才能运行
 * </p>
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "CLAUDE_API_KEY", matches = ".+")
class AiCodeGenerationServiceTest {

    @Autowired
    private AiCodeGenerationService aiCodeGenerationService;

    @Test
    void testGenerateWithAI() {
        // 准备测试数据
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .operationId("salesOutList")
            .method("GET")
            .path("/yiyan/salesOut/list")
            .summary("销售出库单列表")
            .build();

        // 执行生成
        GeneratedCode result = aiCodeGenerationService.generateWithAI(
            List.of(definition),
            "yonsuite",
            "用友 YonSuite",
            "https://api.yonyoucloud.com",
            "appkey"
        );

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getAdapterClass());
        assertTrue(result.getAdapterClass().contains("public class"));
        assertTrue(result.getAdapterClass().contains("syncVouchers"));

        System.out.println("Generated class name: " + result.getClassName());
        System.out.println("Generated code length: " + result.getAdapterClass().length());
    }
}
