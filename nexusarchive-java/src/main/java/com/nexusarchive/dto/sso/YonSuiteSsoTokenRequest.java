package com.nexusarchive.dto.sso;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class YonSuiteSsoTokenRequest {

    @NotBlank(message = "appId 不能为空")
    private String appId;

    @NotBlank(message = "loginId 不能为空")
    private String loginId;
}
