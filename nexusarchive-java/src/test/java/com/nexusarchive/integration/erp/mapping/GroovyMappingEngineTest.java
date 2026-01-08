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
    @DisplayName("当脚本为null时应该抛出异常")
    void shouldThrowExceptionWhenScriptIsNull() {
        assertThatThrownBy(() -> engine.execute(null, context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Script cannot be null");
    }

    @Test
    @DisplayName("当脚本为空字符串时应该抛出异常")
    void shouldThrowExceptionWhenScriptIsEmpty() {
        assertThatThrownBy(() -> engine.execute("", context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Script cannot be null");
    }

    @Test
    @DisplayName("当脚本为空白字符串时应该抛出异常")
    void shouldThrowExceptionWhenScriptIsBlank() {
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
    @DisplayName("应该支持列表操作")
    void shouldSupportListOperations() {
        java.util.List<String> items = java.util.List.of("A", "B", "C");
        testErpData.put("items", items);

        String script = "groovy: return ctx.items.size()";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo(3);
    }

    @Test
    @DisplayName("应该支持条件表达式")
    void shouldSupportConditionalExpression() {
        testErpData.put("status", "ACTIVE");

        String script = "groovy: return ctx.status == 'ACTIVE' ? 'Y' : 'N'";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("Y");
    }

    @Test
    @DisplayName("executeSafe应该在执行失败时返回默认值")
    void shouldReturnDefaultValueWhenExecuteSafeFails() {
        String invalidScript = "groovy: return invalid.syntax.here";
        String defaultValue = "DEFAULT_VALUE";

        Object result = engine.executeSafe(invalidScript, context, defaultValue);

        assertThat(result).isEqualTo(defaultValue);
    }

    @Test
    @DisplayName("executeSafe应该在成功时返回正确结果")
    void shouldReturnCorrectResultWhenExecuteSafeSucceeds() {
        String script = "groovy: return ctx.FNumber";
        String defaultValue = "DEFAULT_VALUE";

        Object result = engine.executeSafe(script, context, defaultValue);

        assertThat(result).isEqualTo("AP001");
    }

    @Test
    @DisplayName("应该支持多层嵌套访问")
    void shouldSupportNestedAccess() {
        Map<String, Object> level2 = new HashMap<>();
        Map<String, Object> level1 = new HashMap<>();

        level2.put("value", "DEEP_VALUE");
        level1.put("nested", level2);
        testErpData.put("level1", level1);

        String script = "groovy: return ctx.level1.nested.value";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("DEEP_VALUE");
    }

    @Test
    @DisplayName("应该支持null安全导航")
    void shouldSupportNullSafeNavigation() {
        testErpData.put("nullable", null);

        String script = "groovy: return ctx.nullable?.value ?: 'SAFE_DEFAULT'";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("SAFE_DEFAULT");
    }

    @Test
    @DisplayName("应该支持日期操作")
    void shouldSupportDateOperations() {
        testErpData.put("year", 2025);
        testErpData.put("month", 1);
        testErpData.put("day", 15);

        String script = "groovy: return String.format('%04d-%02d-%02d', ctx.year, ctx.month, ctx.day)";

        Object result = engine.execute(script, context);

        assertThat(result).isEqualTo("2025-01-15");
    }
}
