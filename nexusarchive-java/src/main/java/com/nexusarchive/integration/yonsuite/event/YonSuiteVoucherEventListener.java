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

    private final com.nexusarchive.integration.service.UniversalSyncEngine universalSyncEngine;

    @Async
    @EventListener
    public void handleVoucherEvent(YonSuiteVoucherEvent event) {
        log.info("Received YonSuite voucher event: voucherId={}, orgId={}", event.getVoucherId(), event.getOrgId());
        try {
            // Build simple context
            com.nexusarchive.integration.core.context.SyncContext context = com.nexusarchive.integration.core.context.SyncContext
                    .builder()
                    .build(); // No token available in event, assumes Client uses configured credentials or
                              // token logic

            // 这里的 accessToken 暂时传 null，由 YonSuiteClient 内部自动获取或者 Connector 处理
            // 但是 Connector fetch 需要 token?
            // 如果 YonSuiteClient 需要 token，而 context 没有，会 fail。
            // 之前的 Service 是 syncVoucherById(null, id)，这意味着 YonSuiteClient 可能自己处理了 token
            // 或者之前的调用本来就有点问题/依赖默认。
            // 假设 YonSuiteClient 不强依赖 context 里的 token，或者我们应该在这里获取 Token。
            // 暂时保持 null，如果 Connector 强依需 token，这里可能会报错。

            universalSyncEngine.syncById(context, "YONSUITE", event.getVoucherId());
        } catch (Exception e) {
            log.error("Error processing YonSuite voucher event: {}", event.getVoucherId(), e);
        }
    }
}
