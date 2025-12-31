// Input: 电子档案元数据与文件实体
// Output: 检测结果封装
// Pos: NexusCore preservation/base
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckResult {
    private boolean passed;
    private String checkName; // e.g., "INTEGRITY", "AUTHENTICITY"
    private String message;
    private LocalDateTime checkTime;
    private String details; // JSON or detailed text

    public static CheckResult pass(String name, String message) {
        return CheckResult.builder()
                .passed(true)
                .checkName(name)
                .message(message)
                .checkTime(LocalDateTime.now())
                .build();
    }

    public static CheckResult fail(String name, String message, String details) {
        return CheckResult.builder()
                .passed(false)
                .checkName(name)
                .message(message)
                .details(details)
                .checkTime(LocalDateTime.now())
                .build();
    }
}
