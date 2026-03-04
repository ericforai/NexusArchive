package com.nexusarchive.service.sso.impl;

import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.service.sso.SsoErrorCodes;
import com.nexusarchive.service.sso.YonSuiteSsoTokenStore;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryYonSuiteSsoTokenStore implements YonSuiteSsoTokenStore {

    private static final long TOKEN_TTL_SECONDS = 60L;

    private final Map<String, TokenRecord> tokenMap = new ConcurrentHashMap<>();

    @Override
    public IssuedToken issue(String clientId, String loginId) {
        String requestId = UUID.randomUUID().toString();
        String ssoToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        tokenMap.put(requestId, new TokenRecord(ssoToken, clientId, loginId, expiresAt, false));
        evictExpired();
        return new IssuedToken(requestId, ssoToken, clientId, loginId, TOKEN_TTL_SECONDS);
    }

    @Override
    public IssuedToken consume(String requestId, String ssoToken) {
        TokenRecord record = tokenMap.get(requestId);
        if (record == null) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "ssoToken 无效", 401);
        }
        if (record.used) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_ALREADY_USED, "ssoToken 已使用", 409);
        }
        long now = Instant.now().getEpochSecond();
        if (record.expiresAt < now) {
            tokenMap.remove(requestId);
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_EXPIRED, "ssoToken 已过期", 410);
        }
        if (!constantTimeEquals(record.ssoToken, ssoToken)) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "ssoToken 无效", 401);
        }

        record.used = true;
        tokenMap.put(requestId, record);

        return new IssuedToken(requestId, ssoToken, record.clientId, record.loginId, Math.max(0L, record.expiresAt - now));
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private void evictExpired() {
        long now = Instant.now().getEpochSecond();
        tokenMap.entrySet().removeIf(entry -> entry.getValue().expiresAt < now || entry.getValue().used);
    }

    private static class TokenRecord {
        private final String ssoToken;
        private final String clientId;
        private final String loginId;
        private final long expiresAt;
        private boolean used;

        private TokenRecord(String ssoToken, String clientId, String loginId, long expiresAt, boolean used) {
            this.ssoToken = ssoToken;
            this.clientId = clientId;
            this.loginId = loginId;
            this.expiresAt = expiresAt;
            this.used = used;
        }
    }
}
