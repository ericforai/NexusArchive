// Input: Lombok、Java 标准库
// Output: YundunAppTokenResponse
// Pos: SSO DTO

package com.nexusarchive.dto.sso;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class YundunAppTokenResponse {
    String token;
    String provider;
    Long issuedAt;
}
