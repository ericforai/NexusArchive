// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java
package com.nexusarchive.integration.erp.ai.service;

import com.nexusarchive.integration.erp.ai.dto.AiGenerationSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 生成会话服务
 * <p>
 * 注意：此服务现在主要用于会话管理。AI 代码生成功能已被移除。
 * 系统现在使用模板化的代码生成（ErpAdapterCodeGenerator）。
 * </p>
 */
@Slf4j
@Service
public class AiGenerationSessionService {

    private final Map<String, AiGenerationSession> sessions = new ConcurrentHashMap<>();

    /**
     * 创建会话
     * <p>
     * AI 生成功能已被移除，此方法现在抛出不支持异常。
     * 请使用 ErpAdaptationOrchestrator 进行基于模板的代码生成。
     * </p>
     */
    public AiGenerationSession createSession(
            String erpType,
            String erpName) {

        // AI code generation functionality has been removed
        throw new UnsupportedOperationException(
            "AI session creation is not available. External LLM API clients have been removed. " +
            "Please use ErpAdaptationOrchestrator for template-based code generation."
        );
    }

    /**
     * 重新生成
     * <p>
     * AI 重新生成功能已被移除。
     * </p>
     */
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

    /**
     * 批准会话
     */
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

    /**
     * 保存会话
     */
    public void saveSession(AiGenerationSession session) {
        sessions.put(session.getSessionId(), session);
    }
}
