package com.nexusarchive.integration.yundun.service;

import com.dbappsecurity.aitrust.appSecSso.InvokeResult;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.config.YundunSdkProperties;
import com.nexusarchive.integration.yundun.sdk.YundunSdkFacade;
import com.nexusarchive.integration.yundun.service.impl.YundunTokenServiceImpl;
import com.nexusarchive.service.sso.SsoErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class YundunTokenServiceImplTest {

    @Mock
    private YundunSdkProperties properties;

    @Mock
    private YundunSdkFacade sdkFacade;

    @InjectMocks
    private YundunTokenServiceImpl service;

    @Test
    @DisplayName("should throw when sdk integration disabled")
    void should_throw_when_sdk_disabled() {
        when(properties.isEnabled()).thenReturn(false);

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.fetchAppToken());
        assertEquals(SsoErrorCodes.YUNDUN_SDK_DISABLED, ex.getErrorCode());
        verify(sdkFacade, never()).applyAppToken(anyString(), anyString());
    }

    @Test
    @DisplayName("should throw when private key missing")
    void should_throw_when_private_key_missing() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getPrivateKey()).thenReturn(" ");

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.fetchAppToken());
        assertEquals(SsoErrorCodes.YUNDUN_SDK_CONFIG_INVALID, ex.getErrorCode());
        verify(sdkFacade, never()).applyAppToken(anyString(), anyString());
    }

    @Test
    @DisplayName("should throw when sdk returns failure")
    void should_throw_when_sdk_returns_failure() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getPrivateKey()).thenReturn("private-key");
        when(properties.getIdpBaseUrl()).thenReturn("https://idp.example.com");

        InvokeResult fail = new InvokeResult();
        fail.setCode(1);
        fail.setMsg("Apply token fail");
        when(sdkFacade.applyAppToken(eq("private-key"), eq("https://idp.example.com"))).thenReturn(fail);

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.fetchAppToken());
        assertEquals(SsoErrorCodes.YUNDUN_SDK_TOKEN_FETCH_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("should return token when sdk success")
    void should_return_token_when_sdk_success() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getPrivateKey()).thenReturn("private-key");
        when(properties.getIdpBaseUrl()).thenReturn("https://idp.example.com");

        InvokeResult success = new InvokeResult();
        success.setCode(0);
        success.setContent("token-value");
        when(sdkFacade.applyAppToken(eq("private-key"), eq("https://idp.example.com"))).thenReturn(success);

        String token = service.fetchAppToken();
        assertEquals("token-value", token);
    }
}
