// Input: JUnit 5、MyBatis-Plus、Spring Boot Test
// Output: SyncHistory ID 类型验证测试
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SyncHistory 实体 ID 类型验证测试
 * <p>
 * 验证 bug 修复: @TableId(type = IdType.ASSIGN_ID) 与 Long id 类型不匹配
 * 修复后应使用: @TableId(type = IdType.AUTO) 与 Long id
 * </p>
 */
@Tag("unit")
@Tag("entity")
@DisplayName("SyncHistory ID 类型验证测试")
class SyncHistoryIdTypeTest {

    @Test
    @DisplayName("应使用 AUTO 策略而非 ASSIGN_ID")
    void shouldUseAutoIdType() {
        // Given & When - @TableId 注解在 id 字段上
        IdType idType = getIdFieldTableIdType();

        // Then
        assertEquals(IdType.AUTO, idType,
                "SyncHistory 应使用 IdType.AUTO 与数据库序列自增保持一致，" +
                        "ASSIGN_ID 会生成 String ID 导致类型不匹配");
    }

    /**
     * 获取 id 字段的 @TableId 注解的 type 属性
     */
    private IdType getIdFieldTableIdType() {
        try {
            Field idField = SyncHistory.class.getDeclaredField("id");
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
    @DisplayName("scenarioId 字段类型应为 Long")
    void scenarioIdFieldTypeShouldBeLong() {
        // Given & When
        Class<?> scenarioIdFieldType = getFieldType("scenarioId");

        // Then
        assertEquals(Long.class, scenarioIdFieldType,
                "scenarioId 字段应为 Long 类型以关联 sys_erp_scenario.id");
    }

    @Test
    @DisplayName("应能够创建实体并设置 Long ID")
    void shouldCreateEntityWithLongId() {
        // Given
        Long testId = 54321L;
        Long testScenarioId = 300L;

        // When
        SyncHistory syncHistory = new SyncHistory();
        syncHistory.setId(testId);
        syncHistory.setScenarioId(testScenarioId);

        // Then
        assertEquals(testId, syncHistory.getId(),
                "应能够正确设置和获取 Long 类型的 id");
        assertEquals(testScenarioId, syncHistory.getScenarioId(),
                "应能够正确设置和获取 Long 类型的 scenarioId");
    }

    @Test
    @DisplayName("应能够创建完整的 SyncHistory 实体")
    void shouldCreateCompleteEntity() {
        // Given
        Long testId = 99999L;
        Long testScenarioId = 500L;
        LocalDateTime now = LocalDateTime.now();

        // When
        SyncHistory syncHistory = new SyncHistory();
        syncHistory.setId(testId);
        syncHistory.setScenarioId(testScenarioId);
        syncHistory.setSyncStartTime(now);
        syncHistory.setSyncEndTime(now.plusMinutes(5));
        syncHistory.setStatus("SUCCESS");
        syncHistory.setTotalCount(100);
        syncHistory.setSuccessCount(98);
        syncHistory.setFailCount(2);
        syncHistory.setOperatorId("user123");
        syncHistory.setClientIp("192.168.1.1");

        // Then
        assertEquals(testId, syncHistory.getId());
        assertEquals(testScenarioId, syncHistory.getScenarioId());
        assertEquals(now, syncHistory.getSyncStartTime());
        assertEquals("SUCCESS", syncHistory.getStatus());
        assertEquals(100, syncHistory.getTotalCount());
        assertEquals(98, syncHistory.getSuccessCount());
        assertEquals(2, syncHistory.getFailCount());
        assertEquals("user123", syncHistory.getOperatorId());
        assertEquals("192.168.1.1", syncHistory.getClientIp());
    }

    @Test
    @DisplayName("验证 ID 值域 - 应接受合理的 Long 值")
    void shouldAcceptValidLongValues() {
        // Given
        SyncHistory syncHistory = new SyncHistory();

        // When & Then - 测试各种边界值
        Long[] testValues = {
                1L,                    // 最小有效值
                100L,
                1000L,
                Long.MAX_VALUE,        // 最大 Long 值
                9223372036854775807L   // 数据库 bigint 最大值
        };

        for (Long testValue : testValues) {
            syncHistory.setId(testValue);
            assertEquals(testValue, syncHistory.getId(),
                    "应接受 Long 值: " + testValue);
        }
    }

    @Test
    @DisplayName("scenarioId 也应接受合理的 Long 值")
    void shouldAcceptValidLongValuesForScenarioId() {
        // Given
        SyncHistory syncHistory = new SyncHistory();

        // When & Then
        Long[] testValues = {1L, 100L, 1000L, Long.MAX_VALUE};

        for (Long testValue : testValues) {
            syncHistory.setScenarioId(testValue);
            assertEquals(testValue, syncHistory.getScenarioId(),
                    "scenarioId 应接受 Long 值: " + testValue);
        }
    }

    @Test
    @DisplayName("不应有 JsonSerialize(ToStringSerializer) 注解")
    void shouldNotHaveToStringSerializer() {
        // Given & When
        boolean hasToStringSerializer = java.util.Arrays.stream(SyncHistory.class.getDeclaredFields())
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
            Field field = SyncHistory.class.getDeclaredField(fieldName);
            return field.getType();
        } catch (NoSuchFieldException e) {
            fail("字段 " + fieldName + " 不存在");
            return null;
        }
    }
}
