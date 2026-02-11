// Input: Lombok
// Output: AccbookResolutionResult DTO
// Pos: SSO 服务层

package com.nexusarchive.service.sso;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccbookResolutionResult {

    private String accbookCode;

    private String fondsCode;
}
