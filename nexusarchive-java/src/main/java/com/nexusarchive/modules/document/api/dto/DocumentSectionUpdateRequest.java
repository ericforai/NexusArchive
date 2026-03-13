package com.nexusarchive.modules.document.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentSectionUpdateRequest {

    @NotBlank(message = "章节标题不能为空")
    private String title;

    private String content;

    private Integer sortOrder;
}
