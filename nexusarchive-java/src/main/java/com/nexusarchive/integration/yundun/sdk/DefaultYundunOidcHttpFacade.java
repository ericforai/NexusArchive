// Input: 云盾 SDK HttpUtils
// Output: DefaultYundunOidcHttpFacade 默认实现
// Pos: 云盾 OIDC HTTP 适配层

package com.nexusarchive.integration.yundun.sdk;

import com.dbappsecurity.aitrust.appSecSso.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultYundunOidcHttpFacade implements YundunOidcHttpFacade {

    @Override
    public String requestAccessToken(String accessTokenUrl, String code, String clientId, String clientSecret,
                                     String redirectUri) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));
        if (StringUtils.isNotBlank(redirectUri)) {
            params.add(new BasicNameValuePair("redirect_uri", redirectUri));
        }
        return HttpUtils.getJsonFromServer(accessTokenUrl, params);
    }

    @Override
    public String requestUserInfo(String userInfoUrl, String authorizationHeaderValue) {
        List<NameValuePair> headers = new ArrayList<>();
        headers.add(new BasicNameValuePair("Authorization", authorizationHeaderValue));
        return HttpUtils.getJsonFromServer(userInfoUrl, headers);
    }
}
