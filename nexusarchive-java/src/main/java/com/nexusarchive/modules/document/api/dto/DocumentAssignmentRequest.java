package com.nexusarchive.modules.document.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentAssignmentRequest {

    @NotBlank(message = "sectionId 不能为空")
    private String sectionId;

    @NotBlank(message = "assigneeId 不能为空")
    private String assigneeId;

    private String assigneeName;

    private String note;

    private Boolean active = Boolean.TRUE;
}
