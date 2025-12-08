package com.nexusarchive.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String newPassword;
}
