package com.nexusarchive.service.sso.impl;

import com.nexusarchive.dto.sso.ErpLaunchRequest;
import com.nexusarchive.dto.sso.ErpLaunchResponse;
import com.nexusarchive.dto.sso.YonSuiteSsoTokenRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoTokenResponse;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlResponse;
import com.nexusarchive.entity.ErpSsoClient;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.mapper.ErpSsoClientMapper;
import com.nexusarchive.service.sso.ErpSsoLaunchService;
import com.nexusarchive.service.sso.ErpSsoSignatureService;
import com.nexusarchive.service.sso.SsoErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YonSuiteSsoBridgeServiceImplTest {

    @Mock
    private ErpSsoClientMapper erpSsoClientMapper;
    @Mock
    private ErpSsoLaunchService erpSsoLaunchService;
    @Mock
    private ErpSsoSignatureService signatureService;

    @Test
    @DisplayName("should issue token and build launch url")
    void should_issue_token_and_build_launch_url() {
        ErpSsoClient client = new ErpSsoClient();
        client.setClientId("ERP_DIGIVOUCHER_TEST");
        client.setClientSecret("secret");
        client.setStatus("ACTIVE");
        when(erpSsoClientMapper.findByClientId("ERP_DIGIVOUCHER_TEST")).thenReturn(client);

        when(signatureService.sign(any(), eq("secret"))).thenReturn("sig");
        when(erpSsoLaunchService.launch(eq("ERP_DIGIVOUCHER_TEST"), eq("sig"), any(ErpLaunchRequest.class))).thenReturn(
                ErpLaunchResponse.builder()
                        .launchTicket("ticket-1")
                        .expiresInSeconds(60L)
                        .launchUrl("/system/sso/launch?ticket=ticket-1")
                        .build()
        );

        YonSuiteSsoBridgeServiceImpl service = new YonSuiteSsoBridgeServiceImpl(
                erpSsoClientMapper,
                erpSsoLaunchService,
                signatureService,
                new InMemoryYonSuiteSsoTokenStore()
        );

        YonSuiteSsoTokenRequest tokenRequest = new YonSuiteSsoTokenRequest();
        tokenRequest.setAppId("ERP_DIGIVOUCHER_TEST");
        tokenRequest.setLoginId("1001");
        YonSuiteSsoTokenResponse tokenResponse = service.issueToken(tokenRequest);

        assertNotNull(tokenResponse.getRequestId());
        assertNotNull(tokenResponse.getSsoToken());

        YonSuiteSsoUrlRequest urlRequest = new YonSuiteSsoUrlRequest();
        urlRequest.setRequestId(tokenResponse.getRequestId());
        urlRequest.setSsoToken(tokenResponse.getSsoToken());
        urlRequest.setAccbookCode("BR01");
        urlRequest.setVoucherNo("记-8");

        YonSuiteSsoUrlResponse urlResponse = service.buildLaunchUrl(urlRequest);
        assertEquals("/system/sso/launch?ticket=ticket-1", urlResponse.getUrlPath());

        ArgumentCaptor<ErpLaunchRequest> requestCaptor = ArgumentCaptor.forClass(ErpLaunchRequest.class);
        verify(erpSsoLaunchService).launch(eq("ERP_DIGIVOUCHER_TEST"), eq("sig"), requestCaptor.capture());
        assertEquals("1001", requestCaptor.getValue().getErpUserJobNo());
        assertEquals("BR01", requestCaptor.getValue().getAccbookCode());
        assertEquals("记-8", requestCaptor.getValue().getVoucherNo());
    }

    @Test
    @DisplayName("should reject invalid sso token")
    void should_reject_invalid_sso_token() {
        YonSuiteSsoBridgeServiceImpl service = new YonSuiteSsoBridgeServiceImpl(
                erpSsoClientMapper,
                erpSsoLaunchService,
                signatureService,
                new InMemoryYonSuiteSsoTokenStore()
        );

        YonSuiteSsoUrlRequest request = new YonSuiteSsoUrlRequest();
        request.setRequestId("req-1");
        request.setSsoToken("bad");
        request.setAccbookCode("BR01");
        request.setVoucherNo("记-8");

        ErpSsoException ex = assertThrows(ErpSsoException.class, () -> service.buildLaunchUrl(request));
        assertEquals(SsoErrorCodes.SSO_TOKEN_INVALID, ex.getErrorCode());
    }
}
