package com.nexusarchive.integration.yonsuite.event;

import com.nexusarchive.integration.yonsuite.service.YonSuiteVoucherSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class YonSuiteVoucherEventListener {

    private final YonSuiteVoucherSyncService voucherSyncService;

    @Async
    @EventListener
    public void handleVoucherEvent(YonSuiteVoucherEvent event) {
        log.info("Received YonSuite voucher event: voucherId={}, orgId={}", event.getVoucherId(), event.getOrgId());
        try {
            // 这里的 accessToken 暂时传 null，由 Service 内部自动获取
            voucherSyncService.syncVoucherById(null, event.getVoucherId());
        } catch (Exception e) {
            log.error("Error processing YonSuite voucher event: {}", event.getVoucherId(), e);
        }
    }
}
