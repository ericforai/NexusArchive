package com.nexusarchive.dto.sso;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class YonSuiteSsoUrlRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    @NotBlank(message = "ssoToken 不能为空")
    private String ssoToken;

    @NotBlank(message = "账套编码不能为空")
    private String accbookCode;

    @NotBlank(message = "凭证号不能为空")
    private String voucherNo;
}
