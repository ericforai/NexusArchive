package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.dto.sso.YundunOidcCallbackResponse;
import com.nexusarchive.dto.sso.YundunOidcStateResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.service.YundunOidcBridgeService;
import com.nexusarchive.integration.yundun.service.YundunOidcStateService;
import com.nexusarchive.service.sso.SsoErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class YundunOidcControllerTest {

    @Mock
    private YundunOidcBridgeService yundunOidcBridgeService;

    @Mock
    private YundunOidcStateService yundunOidcStateService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private YundunOidcController controller;

    @Test
    @DisplayName("callback endpoint should return local login response")
    void callback_endpoint_should_return_local_login_response() {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId("u-1");
        userInfo.setUsername("zhangsan");

        when(yundunOidcStateService.readNonceFromCookie(request)).thenReturn("nonce-1");
        when(yundunOidcBridgeService.consumeAuthCode("code-1", "state-1", "nonce-1")).thenReturn(
                YundunOidcCallbackResponse.builder()
                        .token("jwt-1")
                        .user(userInfo)
                        .provider("YUNDUN_OIDC")
                        .externalUserId("job1001")
                        .build()
        );

        Result<YundunOidcCallbackResponse> result = controller.callback("code-1", "state-1", request, response);
        assertEquals(200, result.getCode());
        assertEquals("jwt-1", result.getData().getToken());
        assertEquals("job1001", result.getData().getExternalUserId());
    }

    @Test
    @DisplayName("callback endpoint should map business error")
    void callback_endpoint_should_map_business_error() {
        when(yundunOidcStateService.readNonceFromCookie(request)).thenReturn("nonce-1");
        when(yundunOidcBridgeService.consumeAuthCode("bad", "state-1", "nonce-1")).thenThrow(
                new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED, "获取 accessToken 失败", 502)
        );

        Result<YundunOidcCallbackResponse> result = controller.callback("bad", "state-1", request, response);
        assertEquals(502, result.getCode());
        assertEquals("YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED: 获取 accessToken 失败", result.getMessage());
    }

    @Test
    @DisplayName("state endpoint should issue signed state")
    void state_endpoint_should_issue_signed_state() {
        when(yundunOidcStateService.issueState(response)).thenReturn(
                YundunOidcStateResponse.builder()
                        .state("v1.payload.signature")
                        .expiresInSeconds(300)
                        .expiresAtEpochSeconds(1730000000L)
                        .build()
        );

        Result<YundunOidcStateResponse> result = controller.issueState(response);
        assertEquals(200, result.getCode());
        assertEquals("v1.payload.signature", result.getData().getState());
    }
}
