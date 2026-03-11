package com.nexusarchive.modules.document.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentVersionCreateRequest {

    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    private String description;
}
