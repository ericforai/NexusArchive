// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationServiceTest.java
// Input: OpenAPI definitions
// Output: GeneratedCode validation
// Pos: AI 模块 - AI 代码生成服务测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 代码生成服务测试
 * <p>
 * 外部 LLM API 客户端已移除，此测试验证服务状态。
 * </p>
 */
class AiCodeGenerationServiceTest {

    private final AiCodeGenerationService aiCodeGenerationService = new AiCodeGenerationService();

    @Test
    void testIsAvailable() {
        // 验证 AI 生成服务已不可用
        assertFalse(aiCodeGenerationService.isAvailable());
    }

    @Test
    void testGenerateWithAIThrowsException() {
        // 准备测试数据
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .operationId("salesOutList")
            .method("GET")
            .path("/yiyan/salesOut/list")
            .summary("销售出库单列表")
            .build();

        // 验证调用会抛出异常
        assertThrows(UnsupportedOperationException.class, () -> {
            aiCodeGenerationService.generateWithAI(
                List.of(definition),
                "yonsuite",
                "用友 YonSuite",
                "https://api.yonyoucloud.com",
                "appkey"
            );
        });
    }
}
