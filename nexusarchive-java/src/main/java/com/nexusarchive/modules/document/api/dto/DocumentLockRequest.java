package com.nexusarchive.modules.document.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentLockRequest {

    @NotBlank(message = "sectionId 不能为空")
    private String sectionId;

    private String reason;

    private Boolean active = Boolean.TRUE;

    private LocalDateTime expiresAt;
}
