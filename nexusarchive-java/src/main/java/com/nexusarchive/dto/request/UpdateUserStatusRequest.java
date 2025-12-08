package com.nexusarchive.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UpdateUserStatusRequest {
    @NotBlank
    private String status; // active / disabled / locked
}
