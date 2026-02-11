// Input: Mapper、AuthService、ErpConfigService、签名与防重放服务
// Output: ErpSsoLaunchServiceImpl
// Pos: SSO 服务层实现

package com.nexusarchive.service.sso.impl;

import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.dto.sso.ConsumeTicketResponse;
import com.nexusarchive.dto.sso.ErpLaunchRequest;
import com.nexusarchive.dto.sso.ErpLaunchResponse;
import com.nexusarchive.entity.ErpSsoClient;
import com.nexusarchive.entity.ErpSsoLaunchTicket;
import com.nexusarchive.entity.ErpUserMapping;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.mapper.ErpSsoClientMapper;
import com.nexusarchive.mapper.ErpSsoLaunchTicketMapper;
import com.nexusarchive.mapper.ErpUserMappingMapper;
import com.nexusarchive.service.AuthService;
import com.nexusarchive.service.ErpConfigService;
import com.nexusarchive.service.sso.ErpSsoLaunchService;
import com.nexusarchive.service.sso.ErpSsoSignatureService;
import com.nexusarchive.service.sso.NonceReplayGuard;
import com.nexusarchive.service.sso.SsoErrorCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ErpSsoLaunchServiceImpl implements ErpSsoLaunchService {

    private static final long LAUNCH_TICKET_TTL_SECONDS = 60L;
    private static final long TIMESTAMP_ALLOWED_SKEW_SECONDS = 300L;

    private final ErpSsoClientMapper erpSsoClientMapper;
    private final ErpUserMappingMapper erpUserMappingMapper;
    private final ErpSsoLaunchTicketMapper erpSsoLaunchTicketMapper;
    private final ErpSsoSignatureService signatureService;
    private final NonceReplayGuard nonceReplayGuard;
    private final ErpConfigService erpConfigService;
    private final AuthService authService;

    @Value("${app.sso.launch-path:/system/sso/launch}")
    private String launchPath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpLaunchResponse launch(String clientId, String signature, ErpLaunchRequest request) {
        ErpSsoClient client = erpSsoClientMapper.findByClientId(clientId);
        if (client == null || !"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            throw new ErpSsoException(SsoErrorCodes.CLIENT_NOT_FOUND, "SSO 客户端不存在或未启用", 401);
        }

        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        signatureService.validateTimestamp(request.getTimestamp(), now, TIMESTAMP_ALLOWED_SKEW_SECONDS);

        String payload = buildPayload(clientId, request);
        if (!signatureService.verify(payload, client.getClientSecret(), signature)) {
            throw new ErpSsoException(SsoErrorCodes.INVALID_SIGNATURE, "签名校验失败", 401);
        }

        boolean nonceOk = nonceReplayGuard.tryAcquire(clientId, request.getNonce(), TIMESTAMP_ALLOWED_SKEW_SECONDS);
        if (!nonceOk) {
            throw new ErpSsoException(SsoErrorCodes.NONCE_REPLAYED, "重复请求", 401);
        }

        ErpUserMapping mapping = erpUserMappingMapper.findActive(clientId, request.getErpUserJobNo());
        if (mapping == null) {
            throw new ErpSsoException(SsoErrorCodes.USER_MAPPING_NOT_FOUND,
                    "ERP 工号未映射 NexusArchive 用户", 400);
        }

        String fondsCode = erpConfigService.resolveFondsCodeStrict(request.getAccbookCode());

        String ticketId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(LAUNCH_TICKET_TTL_SECONDS);

        ErpSsoLaunchTicket ticket = new ErpSsoLaunchTicket();
        ticket.setId(ticketId);
        ticket.setClientId(clientId);
        ticket.setErpUserJobNo(request.getErpUserJobNo());
        ticket.setNexusUserId(mapping.getNexusUserId());
        ticket.setAccbookCode(request.getAccbookCode());
        ticket.setFondsCode(fondsCode);
        ticket.setVoucherNo(request.getVoucherNo());
        ticket.setExpiresAt(expiresAt);
        ticket.setUsed(0);
        ticket.setCreatedTime(LocalDateTime.now());
        ticket.setLastModifiedTime(LocalDateTime.now());
        erpSsoLaunchTicketMapper.insert(ticket);

        String launchUrl = launchPath + "?ticket=" + ticketId;
        return ErpLaunchResponse.builder()
                .launchTicket(ticketId)
                .expiresInSeconds(LAUNCH_TICKET_TTL_SECONDS)
                .launchUrl(launchUrl)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumeTicketResponse consume(String ticketId) {
        ErpSsoLaunchTicket ticket = erpSsoLaunchTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new ErpSsoException(SsoErrorCodes.TICKET_NOT_FOUND, "ticket 不存在", 404);
        }
        if (Integer.valueOf(1).equals(ticket.getUsed())) {
            throw new ErpSsoException(SsoErrorCodes.TICKET_ALREADY_USED, "ticket 已使用", 409);
        }
        if (ticket.getExpiresAt() != null && ticket.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ErpSsoException(SsoErrorCodes.TICKET_EXPIRED, "ticket 已过期", 410);
        }

        ticket.setUsed(1);
        ticket.setUsedAt(LocalDateTime.now());
        ticket.setLastModifiedTime(LocalDateTime.now());
        erpSsoLaunchTicketMapper.updateById(ticket);

        LoginResponse loginResponse = authService.issueTokenByUserId(ticket.getNexusUserId());
        return ConsumeTicketResponse.builder()
                .token(loginResponse.getToken())
                .user(loginResponse.getUser())
                .voucherNo(ticket.getVoucherNo())
                .build();
    }

    private String buildPayload(String clientId, ErpLaunchRequest request) {
        return String.join("|",
                clientId,
                String.valueOf(request.getTimestamp()),
                request.getNonce(),
                request.getAccbookCode(),
                request.getErpUserJobNo(),
                request.getVoucherNo());
    }
}
