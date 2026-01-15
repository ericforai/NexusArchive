// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: VoucherReceivedEvent 类
// Pos: 事件模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
