package com.nexusarchive.integration.yundun.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.dto.sso.YundunOidcCallbackResponse;
import com.nexusarchive.entity.ErpSsoClient;
import com.nexusarchive.entity.ErpUserMapping;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.config.YundunOidcProperties;
import com.nexusarchive.integration.yundun.sdk.YundunOidcHttpFacade;
import com.nexusarchive.integration.yundun.service.YundunOidcStateService;
import com.nexusarchive.integration.yundun.service.impl.YundunOidcBridgeServiceImpl;
import com.nexusarchive.mapper.ErpSsoClientMapper;
import com.nexusarchive.mapper.ErpUserMappingMapper;
import com.nexusarchive.service.AuthService;
import com.nexusarchive.service.sso.SsoErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class YundunOidcBridgeServiceImplTest {

    @Mock
    private YundunOidcProperties properties;

    @Mock
    private YundunOidcHttpFacade oidcHttpFacade;

    @Mock
    private ErpSsoClientMapper erpSsoClientMapper;

    @Mock
    private ErpUserMappingMapper erpUserMappingMapper;

    @Mock
    private AuthService authService;

    @Mock
    private YundunOidcStateService oidcStateService;

    private YundunOidcBridgeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new YundunOidcBridgeServiceImpl(
                properties,
                oidcHttpFacade,
                erpSsoClientMapper,
                erpUserMappingMapper,
                authService,
                new ObjectMapper(),
                oidcStateService
        );
    }

    @Test
    @DisplayName("should throw when oidc integration disabled")
    void should_throw_when_oidc_disabled() {
        when(properties.isEnabled()).thenReturn(false);

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.consumeAuthCode("code-1", "state-1", "nonce-1"));
        assertEquals(SsoErrorCodes.YUNDUN_OIDC_DISABLED, ex.getErrorCode());
    }

    @Test
    @DisplayName("should throw when oidc config invalid")
    void should_throw_when_oidc_config_invalid() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getClientId()).thenReturn("ERP_YUNDUN");
        when(properties.getAccessTokenUrl()).thenReturn("https://idp/iam/auth/oidc/accessToken");
        when(properties.getUserInfoUrl()).thenReturn(" ");

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.consumeAuthCode("code-1", "state-1", "nonce-1"));
        assertEquals(SsoErrorCodes.YUNDUN_OIDC_CONFIG_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("should throw when token exchange failed")
    void should_throw_when_token_exchange_failed() {
        mockValidConfig();
        mockActiveClient();
        when(oidcHttpFacade.requestAccessToken(
                eq("https://idp/iam/auth/oidc/accessToken"),
                eq("bad-code"),
                eq("ERP_YUNDUN"),
                eq("secret-1"),
                eq("https://app/callback")
        )).thenReturn("{\"code\":1,\"msg\":\"invalid code\"}");

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.consumeAuthCode("bad-code", "state-1", "nonce-1"));
        assertEquals(SsoErrorCodes.YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("should return local jwt when oidc callback success")
    void should_return_local_jwt_when_oidc_callback_success() {
        mockValidConfig();
        mockActiveClient();

        when(oidcHttpFacade.requestAccessToken(
                eq("https://idp/iam/auth/oidc/accessToken"),
                eq("good-code"),
                eq("ERP_YUNDUN"),
                eq("secret-1"),
                eq("https://app/callback")
        )).thenReturn("{\"code\":0,\"msg\":\"ok\",\"content\":\"access-1\"}");

        when(oidcHttpFacade.requestUserInfo(
                eq("https://idp/iam/auth/oidc/userInfo"),
                eq("access-1")
        )).thenReturn("{\"sub\":\"job1001\",\"name\":\"张三\"}");

        ErpUserMapping mapping = new ErpUserMapping();
        mapping.setNexusUserId("u-1");
        when(erpUserMappingMapper.findActive("ERP_YUNDUN", "job1001")).thenReturn(mapping);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId("u-1");
        userInfo.setUsername("zhangsan");
        when(authService.issueTokenByUserId("u-1")).thenReturn(new LoginResponse("jwt-1", userInfo));

        YundunOidcCallbackResponse response = service.consumeAuthCode("good-code", "state-1", "nonce-1");
        assertEquals("jwt-1", response.getToken());
        assertEquals("YUNDUN_OIDC", response.getProvider());
        assertEquals("job1001", response.getExternalUserId());
        assertEquals("u-1", response.getUser().getId());
    }

    private void mockValidConfig() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getClientId()).thenReturn("ERP_YUNDUN");
        when(properties.getAccessTokenUrl()).thenReturn("https://idp/iam/auth/oidc/accessToken");
        when(properties.getUserInfoUrl()).thenReturn("https://idp/iam/auth/oidc/userInfo");
        when(properties.getRedirectUri()).thenReturn("https://app/callback");
        when(properties.getUserIdField()).thenReturn("sub");
    }

    private void mockActiveClient() {
        ErpSsoClient client = new ErpSsoClient();
        client.setClientId("ERP_YUNDUN");
        client.setClientSecret("secret-1");
        client.setStatus("ACTIVE");
        when(erpSsoClientMapper.findByClientId("ERP_YUNDUN")).thenReturn(client);
    }
}
