package com.nexusarchive.service.sso.impl;

import com.nexusarchive.dto.sso.ErpLaunchRequest;
import com.nexusarchive.entity.ErpSsoClient;
import com.nexusarchive.entity.ErpUserMapping;
import com.nexusarchive.mapper.ErpSsoClientMapper;
import com.nexusarchive.mapper.ErpSsoLaunchTicketMapper;
import com.nexusarchive.mapper.ErpUserMappingMapper;
import com.nexusarchive.service.AuthService;
import com.nexusarchive.service.ErpConfigService;
import com.nexusarchive.service.sso.ErpSsoSignatureService;
import com.nexusarchive.service.sso.NonceReplayGuard;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ErpSsoLaunchServiceImplTest {

    @Mock
    private ErpSsoClientMapper erpSsoClientMapper;
    @Mock
    private ErpUserMappingMapper erpUserMappingMapper;
    @Mock
    private ErpSsoLaunchTicketMapper erpSsoLaunchTicketMapper;
    @Mock
    private ErpSsoSignatureService signatureService;
    @Mock
    private NonceReplayGuard nonceReplayGuard;
    @Mock
    private ErpConfigService erpConfigService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private ErpSsoLaunchServiceImpl service;

    @Test
    void should_use_utc_epoch_seconds_for_timestamp_validation() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        try {
            ReflectionTestUtils.setField(service, "launchPath", "/system/sso/launch");

            ErpSsoClient client = new ErpSsoClient();
            client.setClientId("ERP_A");
            client.setClientSecret("secret");
            client.setStatus("ACTIVE");
            when(erpSsoClientMapper.findByClientId("ERP_A")).thenReturn(client);

            ErpUserMapping mapping = new ErpUserMapping();
            mapping.setNexusUserId("u1");
            when(erpUserMappingMapper.findActive("ERP_A", "1001")).thenReturn(mapping);
            when(signatureService.verify(any(), eq("secret"), eq("sig"))).thenReturn(true);
            when(nonceReplayGuard.tryAcquire(eq("ERP_A"), eq("nonce-1"), anyLong())).thenReturn(true);
            when(erpConfigService.resolveFondsCodeStrict("BR01")).thenReturn("BR-GROUP");

            ErpLaunchRequest request = new ErpLaunchRequest();
            request.setTimestamp(Instant.now().getEpochSecond());
            request.setNonce("nonce-1");
            request.setAccbookCode("BR01");
            request.setErpUserJobNo("1001");
            request.setVoucherNo("记-8");

            service.launch("ERP_A", "sig", request);

            ArgumentCaptor<Long> nowCaptor = ArgumentCaptor.forClass(Long.class);
            verify(signatureService).validateTimestamp(eq(request.getTimestamp()), nowCaptor.capture(), eq(300L));
            long expectedNow = Instant.now().getEpochSecond();
            assertTrue(Math.abs(nowCaptor.getValue() - expectedNow) <= 5,
                    "nowSeconds should be based on Instant epoch seconds");
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
