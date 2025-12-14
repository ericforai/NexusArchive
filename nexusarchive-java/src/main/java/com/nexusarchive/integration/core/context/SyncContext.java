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
