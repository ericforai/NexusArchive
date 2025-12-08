package com.nexusarchive.event;

import com.nexusarchive.dto.sip.AccountingSipDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 凭证接收事件
 * 触发时机: IngestController 接收到请求并保存到临时目录后
 */
@Getter
public class VoucherReceivedEvent extends ApplicationEvent {

    private final AccountingSipDto sipDto;
    private final String tempPath;
    private final Map<String, byte[]> fileStreams; // 传递文件流以避免重复读取

    public VoucherReceivedEvent(Object source, AccountingSipDto sipDto, String tempPath, Map<String, byte[]> fileStreams) {
        super(source);
        this.sipDto = sipDto;
        this.tempPath = tempPath;
        this.fileStreams = fileStreams;
    }
}
