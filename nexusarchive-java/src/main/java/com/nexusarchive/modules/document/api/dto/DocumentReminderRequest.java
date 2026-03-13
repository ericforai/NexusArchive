package com.nexusarchive.modules.document.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentReminderRequest {

    @NotBlank(message = "sectionId 不能为空")
    private String sectionId;

    @NotBlank(message = "提醒内容不能为空")
    private String message;

    @NotNull(message = "提醒时间不能为空")
    private LocalDateTime remindAt;

    @NotBlank(message = "recipientId 不能为空")
    private String recipientId;

    private String recipientName;
}
