// Input: Lombok
// Output: ErpLaunchResponse DTO
// Pos: SSO DTO

package com.nexusarchive.dto.sso;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErpLaunchResponse {

    private String launchTicket;

    private Long expiresInSeconds;

    private String launchUrl;
}
