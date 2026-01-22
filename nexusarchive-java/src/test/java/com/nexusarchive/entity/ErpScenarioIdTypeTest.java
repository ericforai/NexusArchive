// Input: JUnit 5、MyBatis-Plus、Spring Boot Test
// Output: ErpScenario ID 类型验证测试
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpScenario 实体 ID 类型验证测试
 * <p>
 * 验证 bug 修复: @TableId(type = IdType.ASSIGN_ID) 与 Long id 类型不匹配
 * 修复后应使用: @TableId(type = IdType.AUTO) 与 Long id
 * </p>
 */
@Tag("unit")
@Tag("entity")
@DisplayName("ErpScenario ID 类型验证测试")
class ErpScenarioIdTypeTest {

    @Test
    @DisplayName("应使用 AUTO 策略而非 ASSIGN_ID")
    void shouldUseAutoIdType() {
        // Given & When - @TableId 注解在 id 字段上
        IdType idType = getIdFieldTableIdType();

        // Then
        assertEquals(IdType.AUTO, idType,
                "ErpScenario 应使用 IdType.AUTO 与数据库序列自增保持一致，" +
                        "ASSIGN_ID 会生成 String ID 导致类型不匹配");
    }

    /**
     * 获取 id 字段的 @TableId 注解的 type 属性
     */
    private IdType getIdFieldTableIdType() {
        try {
            Field idField = ErpScenario.class.getDeclaredField("id");
            com.baomidou.mybatisplus.annotation.TableId tableId =
                    idField.getAnnotation(com.baomidou.mybatisplus.annotation.TableId.class);
            assertNotNull(tableId, "id 字段应有 @TableId 注解");
            return tableId.type();
        } catch (NoSuchFieldException e) {
            fail("找不到 id 字段");
            return null;
        }
    }

    @Test
    @DisplayName("ID 字段类型应为 Long")
    void idFieldTypeShouldBeLong() {
        // Given & When
        Class<?> idFieldType = getFieldType("id");

        // Then
        assertEquals(Long.class, idFieldType,
                "id 字段应为 Long 类型以匹配数据库 bigint 类型");
    }

    @Test
    @DisplayName("configId 字段类型应为 Long")
    void configIdFieldTypeShouldBeLong() {
        // Given & When
        Class<?> configIdFieldType = getFieldType("configId");

        // Then
        assertEquals(Long.class, configIdFieldType,
                "configId 字段应为 Long 类型以关联 sys_erp_config.id");
    }

    @Test
    @DisplayName("应能够创建实体并设置 Long ID")
    void shouldCreateEntityWithLongId() {
        // Given
        Long testId = 12345L;
        Long testConfigId = 100L;

        // When
        ErpScenario scenario = new ErpScenario();
        scenario.setId(testId);
        scenario.setConfigId(testConfigId);

        // Then
        assertEquals(testId, scenario.getId(),
                "应能够正确设置和获取 Long 类型的 id");
        assertEquals(testConfigId, scenario.getConfigId(),
                "应能够正确设置和获取 Long 类型的 configId");
    }

    @Test
    @DisplayName("应能够创建实体并通过 Builder 模式设置 Long ID")
    void shouldSupportBuilderPatternWithLongId() {
        // Given
        Long testId = 67890L;
        Long testConfigId = 200L;

        // When
        ErpScenario scenario = new ErpScenario();
        scenario.setId(testId);
        scenario.setConfigId(testConfigId);
        scenario.setScenarioKey("TEST_SCENARIO");
        scenario.setName("测试场景");
        scenario.setIsActive(true);
        scenario.setSyncStrategy("MANUAL");

        // Then
        assertEquals(testId, scenario.getId());
        assertEquals(testConfigId, scenario.getConfigId());
        assertEquals("TEST_SCENARIO", scenario.getScenarioKey());
        assertEquals("测试场景", scenario.getName());
        assertTrue(scenario.getIsActive());
        assertEquals("MANUAL", scenario.getSyncStrategy());
    }

    @Test
    @DisplayName("验证 ID 值域 - 应接受合理的 Long 值")
    void shouldAcceptValidLongValues() {
        // Given
        ErpScenario scenario = new ErpScenario();

        // When & Then - 测试各种边界值
        Long[] testValues = {
                1L,                    // 最小有效值
                100L,
                1000L,
                Long.MAX_VALUE,        // 最大 Long 值
                9223372036854775807L   // 数据库 bigint 最大值
        };

        for (Long testValue : testValues) {
            scenario.setId(testValue);
            assertEquals(testValue, scenario.getId(),
                    "应接受 Long 值: " + testValue);
        }
    }

    @Test
    @DisplayName("不应有 JsonSerialize(ToStringSerializer) 注解")
    void shouldNotHaveToStringSerializer() {
        // Given & When
        boolean hasToStringSerializer = ErpScenario.class.getDeclaredFields().length > 0 &&
                java.util.Arrays.stream(ErpScenario.class.getDeclaredFields())
                        .filter(f -> f.getName().equals("id"))
                        .findFirst()
                        .map(f -> f.getAnnotation(com.fasterxml.jackson.databind.annotation.JsonSerialize.class))
                        .map(a -> a.using() == com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
                        .orElse(false);

        // Then
        assertFalse(hasToStringSerializer,
                "修复后不应有 @JsonSerialize(ToStringSerializer) 注解，" +
                        "因为使用 Long 类型而非 String");
    }

    /**
     * 获取字段的类型
     */
    private Class<?> getFieldType(String fieldName) {
        try {
            Field field = ErpScenario.class.getDeclaredField(fieldName);
            return field.getType();
        } catch (NoSuchFieldException e) {
            fail("字段 " + fieldName + " 不存在");
            return null;
        }
    }
}
