// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java
// Input: OpenAPI definitions + ERP metadata
// Output: GeneratedCode with AI-generated adapter
// Pos: AI 模块 - AI 代码生成服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.llm.deepseek.DeepSeekApiClient;
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
 * 使用 LLM 生成 ERP 适配器代码
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCodeGenerationService {

    private final DeepSeekApiClient deepSeekClient;
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
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int tokensUsed = 0;

        try {
            log.info("Starting AI code generation for {} APIs", definitions.size());

            // 1. 构建 Prompt 上下文
            PromptContext context = buildContext(definitions, erpType, erpName, baseUrl, authType);

            // 2. 生成 Prompt
            String prompt = promptBuilder.buildPrompt(context);
            log.debug("Generated prompt ({} chars)", prompt.length());

            // 3. 调用 DeepSeek API
            String aiResponse = deepSeekClient.complete(prompt);
            success = true;
            // Rough token estimate: 1 token ≈ 4 characters (prompt + response)
            tokensUsed = (prompt.length() + aiResponse.length()) / 4;
            log.info("Received AI response ({} chars)", aiResponse.length());

            // 4. 提取 Java 代码
            String javaCode = codeParser.extractJavaCode(aiResponse);
            log.info("Extracted Java code ({} chars)", javaCode.length());

            // 5. 验证语法
            syntaxValidator.validate(javaCode);
            log.info("Code syntax validation passed");

            // 6. 解析元信息
            CodeParser.ParsedCodeMetadata metadata = codeParser.parseMetadata(javaCode);

            // 7. 构建 GeneratedCode 对象
            return GeneratedCode.builder()
                .adapterClass(javaCode)
                .className(metadata.getClassName())
                .packageName(metadata.getPackageName())
                .erpType(erpType)
                .erpName(erpName)
                .build();

        } catch (CodeValidationException e) {
            log.error("AI code generation validation failed", e);
            throw new RuntimeException("Failed to generate valid code: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI code generation failed", e);
            throw new RuntimeException("AI generation error: " + e.getMessage(), e);
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            metrics.recordRequest(success, tokensUsed, responseTime);
            log.debug("Recorded metrics: success={}, tokens={}, time={}ms", success, tokensUsed, responseTime);
        }
    }

    /**
     * 检查 AI 生成是否可用
     */
    public boolean isAvailable() {
        try {
            return deepSeekClient != null;
        } catch (Exception e) {
            log.warn("AI generation not available", e);
            return false;
        }
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
