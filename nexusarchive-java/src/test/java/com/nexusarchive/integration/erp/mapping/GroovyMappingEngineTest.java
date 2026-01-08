// Input: JUnit 5, AssertJ, Lombok, Java 标准库
// Output: GroovyMappingEngineTest 类
// Pos: 集成模块 - ERP 映射引擎单元测试

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.integration.erp.dto.ErpConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * GroovyMappingEngine 单元测试
 * 测试 Groovy 脚本执行引擎的各种场景
 */
@DisplayName("GroovyMappingEngine 单元测试")
@Tag("unit")
class GroovyMappingEngineTest {

    private GroovyMappingEngine engine;
    private ScriptContext context;
    private Map<String, Object> testErpData;
    private ErpConfig config;

    @BeforeEach
    void setUp() {
        engine = new GroovyMappingEngine();

        // 创建测试用的 ERP 数据
        testErpData = new HashMap<>();
        testErpData.put("FNumber", "AP001");
        testErpData.put("FYear", 2025);
        testErpData.put("FMonth", 1);
        testErpData.put("FAmount", new BigDecimal("1234.56"));

        // 创建测试用的配置
        config = ErpConfig.builder()
            .adapterType("kingdee")
            .baseUrl("https://test.kingdee.com")
            .build();

        context = ScriptContext.builder()
            .ctx(testErpData)
            .config(config)
            .utils(new HashMap<>())
            .build();
    }

    @Test
    @DisplayName("应该执行简单的字段访问脚本")
    void shouldExecuteSimpleFieldAccess() {
        String script = "groovy: return ctx.FNumber";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("AP001");
    }

    @Test
    @DisplayName("应该执行字符串拼接脚本")
    void shouldExecuteStringConcatenation() {
        String script = "groovy: return ctx.FYear + '-' + String.format('%02d', ctx.FMonth as Integer)";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("2025-01");
    }

    @Test
    @DisplayName("应该执行数学运算脚本")
    void shouldExecuteMathOperation() {
        String script = "groovy: return ctx.FAmount ?: 0";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("应该处理空值")
    void shouldHandleNullValue() {
        testErpData.put("EmptyField", null);
        String script = "groovy: return ctx.EmptyField ?: 'default'";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("应该访问配置对象")
    void shouldAccessConfigObject() {
        String script = "groovy: return config.getBaseUrl()";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("https://test.kingdee.com");
    }

    @Test
    @DisplayName("当脚本为null或空白时应该抛出异常")
    void shouldThrowExceptionWhenScriptIsNullOrBlank() {
        assertThatThrownBy(() -> engine.execute(null, context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Script cannot be null");

        assertThatThrownBy(() -> engine.execute("", context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Script cannot be null");

        assertThatThrownBy(() -> engine.execute("   ", context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Script cannot be null");
    }

    @Test
    @DisplayName("当上下文为null时应该抛出异常")
    void shouldThrowExceptionWhenContextIsNull() {
        assertThatThrownBy(() -> engine.execute("groovy: return 1", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Context cannot be null");
    }

    @Test
    @DisplayName("当脚本语法错误时应该抛出MappingScriptException")
    void shouldThrowExceptionWhenScriptHasSyntaxError() {
        String invalidScript = "groovy: return invalid.syntax.here";

        assertThatThrownBy(() -> engine.execute(invalidScript, context))
            .isInstanceOf(MappingScriptException.class)
            .hasMessageContaining("Script execution failed");
    }

    @Test
    @DisplayName("应该支持复杂的数据结构访问")
    void shouldSupportComplexDataStructureAccess() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("code", "ACC001");
        testErpData.put("account", nested);

        String script = "groovy: return ctx.account.code";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("应该支持FieldMapping的脚本字段")
    void shouldSupportFieldMappingScript() {
        // 测试带脚本类型的字段映射
        Map<String, Object> nested = new HashMap<>();
        nested.put("year", 2025);
        nested.put("month", 3);
        testErpData.put("period", nested);

        // 模拟一个脚本月期间格式化
        String script = "groovy: return String.format('%04d-%02d', ctx.period.year, ctx.period.month)";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("2025-03");
    }
}
