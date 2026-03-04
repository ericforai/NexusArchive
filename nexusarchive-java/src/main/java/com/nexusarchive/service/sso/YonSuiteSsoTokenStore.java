package com.nexusarchive.service.sso;

public interface YonSuiteSsoTokenStore {

    IssuedToken issue(String clientId, String loginId);

    IssuedToken consume(String requestId, String ssoToken);

    record IssuedToken(String requestId, String ssoToken, String clientId, String loginId, long expiresInSeconds) {
    }
}
