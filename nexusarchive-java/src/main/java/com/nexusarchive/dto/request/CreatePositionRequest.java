package com.nexusarchive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePositionRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String code;
    private String departmentId;
    private String description;
    private String status;
}
