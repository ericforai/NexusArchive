// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java
package com.nexusarchive.integration.erp.ai.service;

import com.nexusarchive.integration.erp.ai.dto.AiGenerationSession;
import com.nexusarchive.integration.erp.ai.generator.AiCodeGenerationService;
import com.nexusarchive.integration.erp.ai.llm.ClaudeApiClient;
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
    private final Map<String, AiGenerationSession> sessions = new ConcurrentHashMap<>();

    public AiGenerationSessionService(
            AiCodeGenerationService aiCodeGenerationService,
            ClaudeApiClient claudeApiClient) {
        this.aiCodeGenerationService = aiCodeGenerationService;
        this.claudeApiClient = claudeApiClient;
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

        // 创建会话
        AiGenerationSession session = AiGenerationSession.builder()
            .sessionId(UUID.randomUUID().toString())
            .erpType(erpType)
            .erpName(erpName)
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

        // 构建 refined prompt（包含用户反馈）
        String refinedPrompt = buildRefinedPrompt(existing, userFeedback);

        // 重新调用 AI
        String newCode = claudeApiClient.complete(refinedPrompt);

        // 更新会话
        existing.setGeneratedCode(newCode);
        existing.setUserFeedback(userFeedback);
        existing.setIterationCount(existing.getIterationCount() + 1);
        existing.setLastModifiedAt(LocalDateTime.now());
        existing.setStatus(AiGenerationSession.AiGenerationStatus.REGENERATING);

        sessions.put(sessionId, existing);

        return existing;
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
