// Input: Lombok、Spring Framework、Java 标准库
// Output: YonSuiteVoucherEvent 类
// Pos: 事件模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
