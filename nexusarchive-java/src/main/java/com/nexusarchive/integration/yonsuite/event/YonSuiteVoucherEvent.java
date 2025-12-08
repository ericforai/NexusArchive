package com.nexusarchive.integration.yonsuite.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class YonSuiteVoucherEvent extends ApplicationEvent {
    private final String voucherId;
    private final String orgId;

    public YonSuiteVoucherEvent(Object source, String voucherId, String orgId) {
        super(source);
        this.voucherId = voucherId;
        this.orgId = orgId;
    }
}
