// Input: Lombok、LoginResponse
// Output: ConsumeTicketResponse DTO
// Pos: SSO DTO

package com.nexusarchive.dto.sso;

import com.nexusarchive.dto.response.LoginResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsumeTicketResponse {

    private String token;

    private LoginResponse.UserInfo user;

    private String voucherNo;
}
