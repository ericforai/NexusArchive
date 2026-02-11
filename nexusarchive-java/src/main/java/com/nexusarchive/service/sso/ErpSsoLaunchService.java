// Input: DTO
// Output: ErpSsoLaunchService 接口
// Pos: SSO 服务层

package com.nexusarchive.service.sso;

import com.nexusarchive.dto.sso.ConsumeTicketResponse;
import com.nexusarchive.dto.sso.ErpLaunchRequest;
import com.nexusarchive.dto.sso.ErpLaunchResponse;

public interface ErpSsoLaunchService {

    ErpLaunchResponse launch(String clientId, String signature, ErpLaunchRequest request);

    ConsumeTicketResponse consume(String ticket);
}
