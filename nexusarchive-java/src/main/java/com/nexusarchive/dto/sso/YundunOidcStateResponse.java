// Input: Lombok
// Output: YundunOidcStateResponse
// Pos: 云盾 OIDC state 接口响应 DTO

package com.nexusarchive.dto.sso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YundunOidcStateResponse {
    private String state;
    private long expiresInSeconds;
    private long expiresAtEpochSeconds;
}
