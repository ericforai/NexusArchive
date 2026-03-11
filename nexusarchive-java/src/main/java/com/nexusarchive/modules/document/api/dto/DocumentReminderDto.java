package com.nexusarchive.modules.document.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentReminderDto {
    private String id;
    private String sectionId;
    private String message;
    private LocalDateTime remindAt;
    private String recipientId;
    private String recipientName;
    private String createdBy;
    private Boolean delivered;
    private LocalDateTime createdAt;
}
