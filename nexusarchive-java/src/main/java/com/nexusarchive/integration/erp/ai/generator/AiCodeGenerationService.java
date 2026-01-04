// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java
// Input: OpenAPI definitions + ERP metadata
// Output: GeneratedCode with AI-generated adapter
// Pos: AI 模块 - AI 代码生成服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.llm.parser.CodeParser;
import com.nexusarchive.integration.erp.ai.llm.parser.CodeValidationException;
import com.nexusarchive.integration.erp.ai.llm.parser.JavaSyntaxValidator;
import com.nexusarchive.integration.erp.ai.llm.prompt.CodeGenerationPromptBuilder;
import com.nexusarchive.integration.erp.ai.llm.prompt.PromptContext;
import com.nexusarchive.integration.erp.ai.monitoring.AiGenerationMetrics;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 代码生成服务
 * <p>
 * 外部 LLM API 客户端已移除，此服务现在仅用于代码生成的基础结构。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCodeGenerationService {

    private final CodeGenerationPromptBuilder promptBuilder;
    private final CodeParser codeParser;
    private final JavaSyntaxValidator syntaxValidator;

    @Autowired
    private AiGenerationMetrics metrics;

    /**
     * 使用 AI 生成完整的适配器代码
     */
    public GeneratedCode generateWithAI(List<OpenApiDefinition> definitions,
                                     String erpType,
                                     String erpName,
                                     String baseUrl,
                                     String authType) {
        // AI generation functionality has been removed
        throw new UnsupportedOperationException(
            "AI code generation is not available. External LLM API clients (Claude, GLM, DeepSeek) have been removed."
        );
    }

    /**
     * 检查 AI 生成是否可用
     */
    public boolean isAvailable() {
        return false;
    }

    /**
     * 构建 Prompt 上下文
     */
    private PromptContext buildContext(List<OpenApiDefinition> definitions,
                                       String erpType,
                                       String erpName,
                                       String baseUrl,
                                       String authType) {
        List<PromptContext.ApiDefinition> apiDefs = definitions.stream()
            .map(d -> PromptContext.ApiDefinition.builder()
                .operationId(d.getOperationId())
                .method(d.getMethod())
                .path(d.getPath())
                .summary(d.getSummary())
                .requestSchema(d.getRequestSchema() != null ? d.getRequestSchema().toString() : "{}")
                .responseSchema(d.getResponseSchema() != null ? d.getResponseSchema().toString() : "{}")
                .build())
            .collect(Collectors.toList());

        String className = generateClassName(erpType);
        String packageName = "com.nexusarchive.integration.erp.adapter." + erpType.toLowerCase();

        return PromptContext.builder()
            .erpType(erpType)
            .erpName(erpName)
            .className(className)
            .packageName(packageName)
            .baseUrl(baseUrl)
            .authType(authType)
            .apiDefinitions(apiDefs)
            .build();
    }

    /**
     * 生成类名
     */
    private String generateClassName(String erpType) {
        return erpType.substring(0, 1).toUpperCase() + erpType.substring(1).toLowerCase() + "ErpAdapter";
    }
}
