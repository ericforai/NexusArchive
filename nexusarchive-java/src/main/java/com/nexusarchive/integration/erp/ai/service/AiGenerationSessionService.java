// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java
package com.nexusarchive.integration.erp.ai.service;

import com.nexusarchive.integration.erp.ai.dto.AiGenerationSession;
import com.nexusarchive.integration.erp.ai.generator.AiCodeGenerationService;
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
    private final CodeParser codeParser;
    private final JavaSyntaxValidator syntaxValidator;
    private final Map<String, AiGenerationSession> sessions = new ConcurrentHashMap<>();

    public AiGenerationSessionService(
            AiCodeGenerationService aiCodeGenerationService,
            CodeParser codeParser,
            JavaSyntaxValidator syntaxValidator) {
        this.aiCodeGenerationService = aiCodeGenerationService;
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

        // AI regeneration functionality has been removed
        throw new UnsupportedOperationException(
            "AI regeneration is not available. External LLM API clients have been removed."
        );
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
}
