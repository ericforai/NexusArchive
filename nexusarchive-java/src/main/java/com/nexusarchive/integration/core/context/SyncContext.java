// Input: Lombok、Java 标准库
// Output: SyncContext 类
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.core.context;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * 同步上下文
 * 传递同步任务的配置参数
 */
@Data
@Builder
public class SyncContext {

    /**
     * 账套代码
     */
    private String accountBookCode;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 访问令牌或其他凭证
     */
    private String accessToken;

    /**
     * 其他特定配置
     */
    private Map<String, Object> extraConfig;
}
