// Input: OIDC 接口参数
// Output: YundunOidcHttpFacade 接口
// Pos: 云盾 OIDC HTTP 适配层

package com.nexusarchive.integration.yundun.sdk;

public interface YundunOidcHttpFacade {

    String requestAccessToken(String accessTokenUrl, String code, String clientId, String clientSecret, String redirectUri);

    String requestUserInfo(String userInfoUrl, String authorizationHeaderValue);
}
