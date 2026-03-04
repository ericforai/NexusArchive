package com.nexusarchive.dto.sso;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YonSuiteSsoTokenResponse {

    private String requestId;

    private String ssoToken;

    private Long expiresInSeconds;
}
