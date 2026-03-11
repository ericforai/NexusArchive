package com.nexusarchive.modules.document.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentLockDto {
    private String id;
    private String sectionId;
    private String lockedBy;
    private String lockedByName;
    private String reason;
    private Boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
