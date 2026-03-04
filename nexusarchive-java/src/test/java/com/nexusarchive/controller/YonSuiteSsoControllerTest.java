package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.sso.YonSuiteSsoTokenRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoTokenResponse;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.service.sso.SsoErrorCodes;
import com.nexusarchive.service.sso.YonSuiteSsoBridgeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class YonSuiteSsoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private YonSuiteSsoBridgeService yonSuiteSsoBridgeService;

    @Test
    @DisplayName("token endpoint should return ssoToken")
    void token_endpoint_should_return_sso_token() throws Exception {
        YonSuiteSsoTokenRequest request = new YonSuiteSsoTokenRequest();
        request.setAppId("ERP_DIGIVOUCHER_TEST");
        request.setLoginId("1001");

        when(yonSuiteSsoBridgeService.issueToken(any(YonSuiteSsoTokenRequest.class))).thenReturn(
                YonSuiteSsoTokenResponse.builder()
                        .requestId("req-1")
                        .ssoToken("token-1")
                        .expiresInSeconds(60L)
                        .build()
        );

        mockMvc.perform(post("/integration/yonsuite/sso/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.requestId").value("req-1"))
                .andExpect(jsonPath("$.data.ssoToken").value("token-1"));
    }

    @Test
    @DisplayName("url endpoint should return launch url path")
    void url_endpoint_should_return_launch_url_path() throws Exception {
        YonSuiteSsoUrlRequest request = new YonSuiteSsoUrlRequest();
        request.setRequestId("req-1");
        request.setSsoToken("token-1");
        request.setAccbookCode("BR01");
        request.setVoucherNo("记-8");

        when(yonSuiteSsoBridgeService.buildLaunchUrl(any(YonSuiteSsoUrlRequest.class))).thenReturn(
                YonSuiteSsoUrlResponse.builder()
                        .urlPath("/system/sso/launch?ticket=ticket-1")
                        .build()
        );

        mockMvc.perform(post("/integration/yonsuite/sso/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.urlPath").value("/system/sso/launch?ticket=ticket-1"));
    }

    @Test
    @DisplayName("url endpoint should return business error")
    void url_endpoint_should_return_business_error() throws Exception {
        YonSuiteSsoUrlRequest request = new YonSuiteSsoUrlRequest();
        request.setRequestId("req-1");
        request.setSsoToken("bad");
        request.setAccbookCode("BR01");
        request.setVoucherNo("记-8");

        when(yonSuiteSsoBridgeService.buildLaunchUrl(any(YonSuiteSsoUrlRequest.class))).thenThrow(
                new ErpSsoException(SsoErrorCodes.SSO_TOKEN_INVALID, "ssoToken 无效", 401)
        );

        mockMvc.perform(post("/integration/yonsuite/sso/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("SSO_TOKEN_INVALID: ssoToken 无效"));
    }
}
