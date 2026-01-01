// Input: Spring Framework, Lombok
// Output: IngestEventPublisher 类
// Pos: 服务层 - SIP 事件发布器

package com.nexusarchive.service.ingest;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.event.VoucherReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SIP 事件发布器
 * <p>
 * 封装 Spring 事件发布机制，统一归档事件发布
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布凭证接收事件
     *
     * @param sipDto SIP 请求
     * @param tempPath 临时文件路径
     * @param fileStreams 文件流映射
     */
    public void publishVoucherReceivedEvent(AccountingSipDto sipDto, String tempPath, Map<String, byte[]> fileStreams) {
        VoucherReceivedEvent event = new VoucherReceivedEvent(this, sipDto, tempPath, fileStreams);
        eventPublisher.publishEvent(event);
        log.info("已发布 VoucherReceivedEvent: requestId={}", sipDto.getRequestId());
    }
}
