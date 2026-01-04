// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java
// Input: OpenAPI definitions + ERP metadata
// Output: GeneratedCode with AI-generated adapter
// Pos: AI 模块 - AI 代码生成服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 代码生成服务
 * <p>
 * 外部 LLM API 客户端已移除，此服务现在仅用于代码生成的基础结构。
 * </p>
 */
@Slf4j
@Service
public class AiCodeGenerationService {

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
}
