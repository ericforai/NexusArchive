// Input: Lombok, Java 标准库
// Output: ScriptContext 类
// Pos: 集成模块 - ERP 映射脚本上下文

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.integration.erp.dto.ErpConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
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
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("ctx", ctx);
        bindings.put("it", ctx);  // it 是 ctx 的别名，方便脚本中使用
        if (config != null) {
            bindings.put("config", config);
        }
        if (utils != null) {
            bindings.put("utils", utils);
        }
        return bindings;
    }
}
