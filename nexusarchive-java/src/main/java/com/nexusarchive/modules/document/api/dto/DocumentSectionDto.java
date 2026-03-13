package com.nexusarchive.modules.document.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DocumentSectionDto {
    private String id;
    private String projectId;
    private String title;
    private String content;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private DocumentAssignmentDto assignment;
    private DocumentLockDto lock;
    private List<DocumentReminderDto> reminders;
}
