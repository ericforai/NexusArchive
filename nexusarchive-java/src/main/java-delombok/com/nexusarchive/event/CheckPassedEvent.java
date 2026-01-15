// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: CheckPassedEvent 类
// Pos: 事件模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.event;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 四性检测通过事件
 * 触发时机: ComplianceListener 完成四性检测且通过后
 */
@Getter
public class CheckPassedEvent extends ApplicationEvent {

    private final AccountingSipDto sipDto;
    private final String tempPath;
    private final FourNatureReport report;
    private final Map<String, byte[]> fileStreams;

    public CheckPassedEvent(Object source, AccountingSipDto sipDto, String tempPath, FourNatureReport report, Map<String, byte[]> fileStreams) {
        super(source);
        this.sipDto = sipDto;
        this.tempPath = tempPath;
        this.report = report;
        this.fileStreams = fileStreams;
    }
}
