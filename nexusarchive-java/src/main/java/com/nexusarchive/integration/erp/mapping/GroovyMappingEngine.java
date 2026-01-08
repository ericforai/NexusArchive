// Input: Groovy, Lombok, Java 标准库, SLF4J
// Output: GroovyMappingEngine 类
// Pos: 集成模块 - ERP 映射脚本引擎

package com.nexusarchive.integration.erp.mapping;

import groovy.lang.GroovyShell;
import groovy.lang.Binding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Groovy 脚本执行引擎
 * 用于执行字段映射中的动态脚本
 */
@Slf4j
@Component
public class GroovyMappingEngine {

    public GroovyMappingEngine() {
    }

    /**
     * 执行脚本并返回结果
     *
     * @param script Groovy 脚本
     * @param context 执行上下文
     * @return 脚本执行结果
     */
    public Object execute(String script, ScriptContext context) {
        try {
            Binding binding = new Binding(context.getBindings());
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(script);
            log.debug("Script executed successfully: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Script execution failed: {}", script, e);
            throw new MappingScriptException("Script execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * 安全地执行脚本，返回默认值如果失败
     */
    public Object executeSafe(String script, ScriptContext context, Object defaultValue) {
        try {
            return execute(script, context);
        } catch (Exception e) {
            log.warn("Script failed, returning default value: {}", defaultValue);
            return defaultValue;
        }
    }
}
