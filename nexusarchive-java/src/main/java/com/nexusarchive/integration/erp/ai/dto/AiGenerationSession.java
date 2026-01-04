// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/dto/AiGenerationSession.java
package com.nexusarchive.integration.erp.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiGenerationSession {
    private String sessionId;
    private String erpType;
    private String erpName;
    private String originalPrompt;
    private String generatedCode;
    private String userFeedback;
    private int iterationCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private AiGenerationStatus status;

    public enum AiGenerationStatus {
        GENERATED,      // AI 已生成
        REVIEWING,      // 等待人工审核
        APPROVED,       // 已批准
        REJECTED,       // 已拒绝
        REGENERATING    // 重新生成中
    }
}
