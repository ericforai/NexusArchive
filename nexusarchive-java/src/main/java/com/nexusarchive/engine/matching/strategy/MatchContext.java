// Input: Java 标准库
// Output: MatchContext 类
// Pos: 匹配引擎 - 匹配上下文

package com.nexusarchive.engine.matching.strategy;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 匹配上下文
 * <p>
 * 传递匹配过程中的上下文信息
 * </p>
 */
@Data
@Builder
public class MatchContext {
    /**
     * 业务场景
     */
    private String scene;

    /**
     * 证据角色
     */
    private String evidenceRole;

    /**
     * 规则配置
     */
    private Map<String, Object> ruleConfig;

    /**
     * 容差配置
     */
    private Map<String, Object> toleranceConfig;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
}
