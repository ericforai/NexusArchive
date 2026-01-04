// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java
package com.nexusarchive.integration.erp.ai.service;

import com.nexusarchive.integration.erp.ai.dto.AiGenerationSession;
import com.nexusarchive.integration.erp.ai.generator.AiCodeGenerationService;
import com.nexusarchive.integration.erp.ai.llm.ClaudeApiClient;
import com.nexusarchive.integration.erp.ai.llm.parser.CodeParser;
import com.nexusarchive.integration.erp.ai.llm.parser.CodeValidationException;
import com.nexusarchive.integration.erp.ai.llm.parser.JavaSyntaxValidator;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AiGenerationSessionService {

    private final AiCodeGenerationService aiCodeGenerationService;
    private final ClaudeApiClient claudeApiClient;
    private final CodeParser codeParser;
    private final JavaSyntaxValidator syntaxValidator;
    private final Map<String, AiGenerationSession> sessions = new ConcurrentHashMap<>();

    public AiGenerationSessionService(
            AiCodeGenerationService aiCodeGenerationService,
            ClaudeApiClient claudeApiClient,
            CodeParser codeParser,
            JavaSyntaxValidator syntaxValidator) {
        this.aiCodeGenerationService = aiCodeGenerationService;
        this.claudeApiClient = claudeApiClient;
        this.codeParser = codeParser;
        this.syntaxValidator = syntaxValidator;
    }

    public AiGenerationSession createSession(
            List<OpenApiDefinition> definitions,
            String erpType,
            String erpName,
            String baseUrl,
            String authType) {

        // 生成代码
        var generatedCode = aiCodeGenerationService.generateWithAI(
            definitions, erpType, erpName, baseUrl, authType
        );

        // 创建原始提示词记录
        String originalPrompt = String.format(
            "Generate ERP adapter for %s (%s) with %d API definitions",
            erpName, erpType, definitions.size()
        );

        // 创建会话
        AiGenerationSession session = AiGenerationSession.builder()
            .sessionId(UUID.randomUUID().toString())
            .erpType(erpType)
            .erpName(erpName)
            .originalPrompt(originalPrompt)
            .generatedCode(generatedCode.getAdapterClass())
            .iterationCount(1)
            .createdAt(LocalDateTime.now())
            .lastModifiedAt(LocalDateTime.now())
            .status(AiGenerationSession.AiGenerationStatus.GENERATED)
            .build();

        sessions.put(session.getSessionId(), session);

        return session;
    }

    public AiGenerationSession regenerate(String sessionId, String userFeedback) {
        AiGenerationSession existing = sessions.get(sessionId);
        if (existing == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        try {
            // 构建 refined prompt（包含用户反馈）
            String refinedPrompt = buildRefinedPrompt(existing, userFeedback);

            // 重新调用 AI
            String aiResponse = claudeApiClient.complete(refinedPrompt);
            log.info("Received AI regeneration response ({} chars)", aiResponse.length());

            // 提取 Java 代码
            String newCode = codeParser.extractJavaCode(aiResponse);
            log.info("Extracted Java code from regeneration ({} chars)", newCode.length());

            // 验证语法
            syntaxValidator.validate(newCode);
            log.info("Regenerated code syntax validation passed");

            // 使用 builder 模式创建新的会话对象（避免并发问题）
            AiGenerationSession updatedSession = AiGenerationSession.builder()
                .sessionId(existing.getSessionId())
                .erpType(existing.getErpType())
                .erpName(existing.getErpName())
                .originalPrompt(existing.getOriginalPrompt())
                .generatedCode(newCode)
                .userFeedback(userFeedback)
                .iterationCount(existing.getIterationCount() + 1)
                .createdAt(existing.getCreatedAt())
                .lastModifiedAt(LocalDateTime.now())
                .status(AiGenerationSession.AiGenerationStatus.GENERATED)
                .build();

            // 原子性地替换会话
            sessions.put(sessionId, updatedSession);

            return updatedSession;

        } catch (CodeValidationException e) {
            log.error("Code validation failed during regeneration", e);
            throw new RuntimeException("Failed to validate regenerated code: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Regeneration failed", e);
            throw new RuntimeException("Regeneration error: " + e.getMessage(), e);
        }
    }

    public void approve(String sessionId) {
        AiGenerationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.setStatus(AiGenerationSession.AiGenerationStatus.APPROVED);
        session.setLastModifiedAt(LocalDateTime.now());

        // TODO: 将代码保存到文件系统并部署

        log.info("Session {} approved, proceeding with deployment", sessionId);
    }

    public void saveSession(AiGenerationSession session) {
        sessions.put(session.getSessionId(), session);
    }

    private String buildRefinedPrompt(AiGenerationSession session, String feedback) {
        return String.format("""
            Original prompt generated the following code.

            User feedback: %s

            Please refine the generated code addressing the feedback above.
            Keep the same class structure and package name.
            """, feedback);
    }
}
