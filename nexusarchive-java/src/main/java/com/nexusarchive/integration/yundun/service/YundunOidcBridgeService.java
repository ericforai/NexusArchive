// Input: OIDC 回调参数
// Output: YundunOidcBridgeService 接口
// Pos: 云盾 OIDC 服务层

package com.nexusarchive.integration.yundun.service;

import com.nexusarchive.dto.sso.YundunOidcCallbackResponse;

public interface YundunOidcBridgeService {

    YundunOidcCallbackResponse consumeAuthCode(String code, String state, String cookieNonce);
}
