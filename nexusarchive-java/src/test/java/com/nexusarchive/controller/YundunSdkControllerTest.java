package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sso.YundunAppTokenResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.service.YundunTokenService;
import com.nexusarchive.service.sso.SsoErrorCodes;
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
class YundunSdkControllerTest {

    @Mock
    private YundunTokenService yundunTokenService;

    @InjectMocks
    private YundunSdkController controller;

    @Test
    @DisplayName("token endpoint should return app token")
    void token_endpoint_should_return_app_token() {
        when(yundunTokenService.fetchAppToken()).thenReturn("token-1");

        Result<YundunAppTokenResponse> result = controller.token();
        assertEquals(200, result.getCode());
        assertEquals("token-1", result.getData().getToken());
        assertEquals("YUNDUN_SDK", result.getData().getProvider());
    }

    @Test
    @DisplayName("token endpoint should map business error")
    void token_endpoint_should_map_business_error() {
        when(yundunTokenService.fetchAppToken()).thenThrow(
                new ErpSsoException(SsoErrorCodes.YUNDUN_SDK_TOKEN_FETCH_FAILED, "获取 token 失败", 502)
        );

        Result<YundunAppTokenResponse> result = controller.token();
        assertEquals(502, result.getCode());
        assertEquals("YUNDUN_SDK_TOKEN_FETCH_FAILED: 获取 token 失败", result.getMessage());
    }
}
