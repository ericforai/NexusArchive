package com.nexusarchive.integration.yundun.service;

import com.nexusarchive.dto.sso.YundunOidcStateResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.config.YundunOidcProperties;
import com.nexusarchive.service.sso.SsoErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class YundunOidcStateServiceTest {

    @Test
    @DisplayName("should issue signed state and set nonce cookie")
    void should_issue_signed_state_and_set_nonce_cookie() {
        YundunOidcProperties properties = buildProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        YundunOidcStateService service = new YundunOidcStateService(properties, redisTemplate);
        MockHttpServletResponse response = new MockHttpServletResponse();

        YundunOidcStateResponse issued = service.issueState(response);

        assertTrue(issued.getState().startsWith("v1."));
        assertEquals(300L, issued.getExpiresInSeconds());
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(v -> v.contains("YUNDUN_OIDC_STATE_NONCE")));
    }

    @Test
    @DisplayName("should reject replayed state")
    void should_reject_replayed_state() {
        YundunOidcProperties properties = buildProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(true).thenReturn(false);

        YundunOidcStateService service = new YundunOidcStateService(properties, redisTemplate);
        MockHttpServletResponse response = new MockHttpServletResponse();
        YundunOidcStateResponse issued = service.issueState(response);
        String nonce = response.getCookie("YUNDUN_OIDC_STATE_NONCE").getValue();

        service.validateAndConsume(issued.getState(), nonce);
        ErpSsoException replay = assertThrows(ErpSsoException.class,
                () -> service.validateAndConsume(issued.getState(), nonce));
        assertEquals(SsoErrorCodes.SSO_TOKEN_ALREADY_USED, replay.getErrorCode());
    }

    @Test
    @DisplayName("should reject state when cookie nonce mismatch")
    void should_reject_state_when_cookie_nonce_mismatch() {
        YundunOidcProperties properties = buildProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        YundunOidcStateService service = new YundunOidcStateService(properties, redisTemplate);
        MockHttpServletResponse response = new MockHttpServletResponse();
        YundunOidcStateResponse issued = service.issueState(response);

        ErpSsoException ex = assertThrows(ErpSsoException.class,
                () -> service.validateAndConsume(issued.getState(), "another-nonce"));
        assertEquals(SsoErrorCodes.SSO_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("should reject tampered state signature")
    void should_reject_tampered_state_signature() {
        YundunOidcProperties properties = buildProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        YundunOidcStateService service = new YundunOidcStateService(properties, redisTemplate);
        MockHttpServletResponse response = new MockHttpServletResponse();
        YundunOidcStateResponse issued = service.issueState(response);
        String[] parts = issued.getState().split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalid-signature";

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.validateAndConsume(tampered, "test-nonce"));
        assertEquals(SsoErrorCodes.SSO_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("should clear nonce cookie")
    void should_clear_nonce_cookie() {
        YundunOidcProperties properties = buildProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        YundunOidcStateService service = new YundunOidcStateService(properties, redisTemplate);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearNonceCookie(response);

        assertFalse(response.getHeaders("Set-Cookie").isEmpty());
        assertTrue(response.getHeaders("Set-Cookie").get(0).contains("Max-Age=0"));
    }

    private YundunOidcProperties buildProperties() {
        YundunOidcProperties properties = new YundunOidcProperties();
        properties.setRequireState(true);
        properties.setStateSigningKey("unit-test-sign-key-1234567890");
        properties.setStateTtlSeconds(300);
        properties.setStateMinLength(8);
        properties.setStateCookieName("YUNDUN_OIDC_STATE_NONCE");
        properties.setStateCookiePath("/api/integration/yundun/oidc");
        return properties;
    }
}
