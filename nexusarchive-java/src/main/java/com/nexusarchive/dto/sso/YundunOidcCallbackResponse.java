// Input: LoginResponse、Lombok
// Output: YundunOidcCallbackResponse
// Pos: SSO DTO

package com.nexusarchive.dto.sso;

import com.nexusarchive.dto.response.LoginResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YundunOidcCallbackResponse {

    private String token;

    private LoginResponse.UserInfo user;

    private String provider;

    private String externalUserId;
}
