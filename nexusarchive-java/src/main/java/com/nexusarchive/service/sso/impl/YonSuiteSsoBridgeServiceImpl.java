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
import com.nexusarchive.service.sso.YonSuiteSsoBridgeService;
import com.nexusarchive.service.sso.YonSuiteSsoTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class YonSuiteSsoBridgeServiceImpl implements YonSuiteSsoBridgeService {

    private final ErpSsoClientMapper erpSsoClientMapper;
    private final ErpSsoLaunchService erpSsoLaunchService;
    private final ErpSsoSignatureService signatureService;
    private final YonSuiteSsoTokenStore tokenStore;

    @Override
    public YonSuiteSsoTokenResponse issueToken(YonSuiteSsoTokenRequest request) {
        ErpSsoClient client = erpSsoClientMapper.findByClientId(request.getAppId());
        if (client == null || !"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            throw new ErpSsoException(SsoErrorCodes.CLIENT_NOT_FOUND, "SSO 客户端不存在或未启用", 401);
        }

        YonSuiteSsoTokenStore.IssuedToken token = tokenStore.issue(request.getAppId(), request.getLoginId());
        return YonSuiteSsoTokenResponse.builder()
                .requestId(token.requestId())
                .ssoToken(token.ssoToken())
                .expiresInSeconds(token.expiresInSeconds())
                .build();
    }

    @Override
    public YonSuiteSsoUrlResponse buildLaunchUrl(YonSuiteSsoUrlRequest request) {
        YonSuiteSsoTokenStore.IssuedToken token = tokenStore.consume(request.getRequestId(), request.getSsoToken());

        ErpSsoClient client = erpSsoClientMapper.findByClientId(token.clientId());
        if (client == null || !"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            throw new ErpSsoException(SsoErrorCodes.CLIENT_NOT_FOUND, "SSO 客户端不存在或未启用", 401);
        }

        ErpLaunchRequest erpLaunchRequest = new ErpLaunchRequest();
        erpLaunchRequest.setAccbookCode(request.getAccbookCode());
        erpLaunchRequest.setErpUserJobNo(token.loginId());
        erpLaunchRequest.setVoucherNo(request.getVoucherNo());
        erpLaunchRequest.setTimestamp(Instant.now().getEpochSecond());
        erpLaunchRequest.setNonce(UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        String payload = String.join("|",
                token.clientId(),
                String.valueOf(erpLaunchRequest.getTimestamp()),
                erpLaunchRequest.getNonce(),
                erpLaunchRequest.getAccbookCode(),
                erpLaunchRequest.getErpUserJobNo(),
                erpLaunchRequest.getVoucherNo());
        String signature = signatureService.sign(payload, client.getClientSecret());

        ErpLaunchResponse launchResponse = erpSsoLaunchService.launch(token.clientId(), signature, erpLaunchRequest);
        return YonSuiteSsoUrlResponse.builder()
                .urlPath(launchResponse.getLaunchUrl())
                .build();
    }
}
