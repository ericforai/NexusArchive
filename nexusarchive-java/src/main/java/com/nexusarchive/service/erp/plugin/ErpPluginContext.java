// Input: Lombok, Java 标准库
// Output: ErpPluginContext 类
// Pos: 服务层 - ERP 插件上下文

package com.nexusarchive.service.erp.plugin;

import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * ERP 插件上下文
 * <p>
 * 封装插件执行所需的上下文信息
 * </p>
 */
@Data
@Builder
public class ErpPluginContext {
    /**
     * ERP 配置
     */
    private ErpConfig config;

    /**
     * 同步场景
     */
    private ErpScenario scenario;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 操作人 ID
     */
    private String operatorId;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
}
