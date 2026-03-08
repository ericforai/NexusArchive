// Input: OIDC 配置、Redis、Servlet Cookie
// Output: YundunOidcStateService
// Pos: 云盾 OIDC state 安全服务

package com.nexusarchive.integration.yundun.service;

import com.nexusarchive.dto.sso.YundunOidcStateResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.config.YundunOidcProperties;
import com.nexusarchive.service.sso.SsoErrorCodes;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class YundunOidcStateService {

    private static final String STATE_VERSION = "v1";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final YundunOidcProperties properties;
    private final StringRedisTemplate redisTemplate;

    // Redis 不可用时的降级（进程内）防重放
    private final Map<String, Long> fallbackReplayGuard = new ConcurrentHashMap<>();

    public YundunOidcStateResponse issueState(HttpServletResponse response) {
        validateStateConfig();

        long now = Instant.now().getEpochSecond();
        long exp = now + properties.getStateTtlSeconds();
        String nonce = generateNonce();
        String payload = nonce + "." + now + "." + exp;
        String payloadB64 = base64UrlEncode(payload);
        String signature = sign(payloadB64);
        String state = STATE_VERSION + "." + payloadB64 + "." + signature;

        writeNonceCookie(response, nonce);
        return YundunOidcStateResponse.builder()
                .state(state)
                .expiresInSeconds(properties.getStateTtlSeconds())
                .expiresAtEpochSeconds(exp)
                .build();
    }

    public void validateAndConsume(String state, String cookieNonce) {
        if (!properties.isRequireState()) {
            return;
        }
        validateStateConfig();
        ParsedState parsed = parseAndVerify(state);

        if (parsed.expiresAtEpochSeconds < Instant.now().getEpochSecond()) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_EXPIRED, "OIDC state 已过期", 400);
        }
        if (!StringUtils.equals(parsed.nonce, StringUtils.trimToEmpty(cookieNonce))) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 与浏览器上下文不匹配", 400);
        }
        long ttl = Math.max(1, parsed.expiresAtEpochSeconds - Instant.now().getEpochSecond());
        boolean consumed = markStateAsConsumed(parsed.nonce, ttl);
        if (!consumed) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_ALREADY_USED, "OIDC state 已被使用", 409);
        }
    }

    public String readNonceFromCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return "";
        }
        for (Cookie cookie : request.getCookies()) {
            if (properties.getStateCookieName().equals(cookie.getName())) {
                return StringUtils.trimToEmpty(cookie.getValue());
            }
        }
        return "";
    }

    public void clearNonceCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(properties.getStateCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.isStateCookieSecure());
        cookie.setPath(properties.getStateCookiePath());
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private ParsedState parseAndVerify(String state) {
        if (StringUtils.isBlank(state)) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 缺失", 400);
        }
        String[] parts = state.split("\\.");
        if (parts.length != 3 || !STATE_VERSION.equals(parts[0])) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 格式非法", 400);
        }
        String payloadB64 = parts[1];
        String signature = parts[2];
        String expected = sign(payloadB64);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 签名无效", 400);
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 解析失败", 400);
        }
        String[] payloadParts = payload.split("\\.");
        if (payloadParts.length != 3) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 载荷非法", 400);
        }
        String nonce = payloadParts[0];
        if (nonce.length() < properties.getStateMinLength() || !nonce.matches("^[A-Za-z0-9_-]+$")) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state nonce 非法", 400);
        }
        try {
            long issuedAt = Long.parseLong(payloadParts[1]);
            long expiresAt = Long.parseLong(payloadParts[2]);
            return new ParsedState(nonce, issuedAt, expiresAt);
        } catch (NumberFormatException e) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 时间字段非法", 400);
        }
    }

    private String sign(String payloadB64) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(properties.getStateSigningKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(key);
            byte[] signature = hmac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "OIDC state 签名失败", 500);
        }
    }

    private String generateNonce() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String base64UrlEncode(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeNonceCookie(HttpServletResponse response, String nonce) {
        Cookie cookie = new Cookie(properties.getStateCookieName(), nonce);
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.isStateCookieSecure());
        cookie.setPath(properties.getStateCookiePath());
        cookie.setMaxAge((int) properties.getStateTtlSeconds());
        response.addCookie(cookie);
    }

    private boolean markStateAsConsumed(String nonce, long ttlSeconds) {
        String key = "sso:yundun:oidc:state:used:" + nonce;
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", java.time.Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            fallbackReplayGuard.entrySet().removeIf(entry -> entry.getValue() <= now);
            Long previous = fallbackReplayGuard.putIfAbsent(nonce, now + ttlSeconds * 1000);
            return previous == null;
        }
    }

    private void validateStateConfig() {
        if (StringUtils.isBlank(properties.getStateSigningKey())) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_CONFIG_INVALID, "OIDC state 签名密钥未配置", 500);
        }
        if (properties.getStateTtlSeconds() <= 0) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_CONFIG_INVALID, "OIDC state 过期时间配置非法", 500);
        }
    }

    private record ParsedState(String nonce, long issuedAtEpochSeconds, long expiresAtEpochSeconds) {
    }
}
