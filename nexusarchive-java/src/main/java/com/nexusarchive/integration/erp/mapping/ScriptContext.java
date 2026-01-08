// Input: Lombok, Java 标准库
// Output: ScriptContext 类
// Pos: 集成模块 - ERP 映射脚本上下文

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.integration.erp.dto.ErpConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Groovy 脚本执行上下文
 */
@Data
@Builder
@AllArgsConstructor
public class ScriptContext {
    /**
     * 当前 ERP 响应对象
     */
    private Object ctx;

    /**
     * ERP 配置
     */
    private ErpConfig config;

    /**
     * 工具类
     */
    private Map<String, Object> utils;

    /**
     * 获取绑定的变量用于 GroovyShell
     */
    public Map<String, Object> getBindings() {
        return Map.of(
            "ctx", ctx,
            "config", config,
            "utils", utils
        );
    }
}
