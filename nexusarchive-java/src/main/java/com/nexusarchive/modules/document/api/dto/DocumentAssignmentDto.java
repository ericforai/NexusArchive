package com.nexusarchive.modules.document.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentAssignmentDto {
    private String id;
    private String sectionId;
    private String assigneeId;
    private String assigneeName;
    private String assignedBy;
    private String note;
    private Boolean active;
    private LocalDateTime createdAt;
}
