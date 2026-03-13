package com.nexusarchive.modules.document.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentVersionDto {
    private String id;
    private String projectId;
    private String versionName;
    private String description;
    private String createdBy;
    private String rolledBackBy;
    private LocalDateTime rolledBackAt;
    private LocalDateTime createdAt;
}
