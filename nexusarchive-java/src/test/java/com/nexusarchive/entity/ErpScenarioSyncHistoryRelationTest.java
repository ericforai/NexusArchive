// Input: JUnit 5、MyBatis-Plus、Spring Boot Test
// Output: ErpScenario 与 SyncHistory 关联关系测试
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpScenario 与 SyncHistory 关联关系测试
 * <p>
 * 验证 ErpScenario.id (Long) 与 SyncHistory.scenarioId (Long) 之间的类型一致性
 * </p>
 */
@Tag("unit")
@Tag("entity")
@DisplayName("ErpScenario-SyncHistory 关联关系测试")
class ErpScenarioSyncHistoryRelationTest {

    @Test
    @DisplayName("应能够通过 Long ID 关联 ErpScenario 和 SyncHistory")
    void shouldLinkEntitiesViaLongId() {
        // Given
        Long scenarioId = 12345L;

        ErpScenario scenario = new ErpScenario();
        scenario.setId(scenarioId);
        scenario.setScenarioKey("TEST_SYNC");
        scenario.setName("测试同步场景");
        scenario.setIsActive(true);

        // When - 创建关联的 SyncHistory
        SyncHistory syncHistory = new SyncHistory();
        syncHistory.setScenarioId(scenarioId); // 使用相同的 Long ID
        syncHistory.setStatus("SUCCESS");
        syncHistory.setSyncStartTime(LocalDateTime.now());

        // Then - 验证关联
        assertEquals(scenario.getId(), syncHistory.getScenarioId(),
                "SyncHistory.scenarioId 应等于 ErpScenario.id");

        // 验证类型一致
        assertSame(Long.class, scenario.getId().getClass(),
                "ErpScenario.id 应为 Long 类型");
        assertSame(Long.class, syncHistory.getScenarioId().getClass(),
                "SyncHistory.scenarioId 应为 Long 类型");
    }

    @Test
    @DisplayName("应能够创建一对多的关联关系")
    void shouldSupportOneToManyRelation() {
        // Given
        Long scenarioId = 200L;

        ErpScenario scenario = new ErpScenario();
        scenario.setId(scenarioId);
        scenario.setScenarioKey("MULTI_SYNC");
        scenario.setName("多次同步场景");

        // When - 一个场景对应多条同步历史
        List<SyncHistory> histories = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            SyncHistory history = new SyncHistory();
            history.setId((long) i);
            history.setScenarioId(scenarioId); // 都关联到同一个场景
            history.setStatus(i % 2 == 0 ? "SUCCESS" : "FAIL");
            history.setSyncStartTime(LocalDateTime.now().minusHours(i));
            histories.add(history);
        }

        // Then - 验证所有历史记录都关联到正确的场景
        assertEquals(5, histories.size());
        for (SyncHistory history : histories) {
            assertEquals(scenarioId, history.getScenarioId(),
                    "所有 SyncHistory 应关联到同一个 ErpScenario");
        }

        // 验证类型一致性
        histories.forEach(h -> {
            assertTrue(h.getScenarioId() instanceof Long,
                    "scenarioId 应为 Long 类型");
        });
    }

    @Test
    @DisplayName("应支持通过 ID 查找关联的实体")
    void shouldFindRelatedEntityById() {
        // Given
        Long scenarioId = 300L;

        ErpScenario scenario = new ErpScenario();
        scenario.setId(scenarioId);
        scenario.setScenarioKey("LOOKUP_TEST");
        scenario.setName("查找测试场景");

        // When - 模拟通过 ID 查找
        Long lookupId = scenario.getId();
        SyncHistory history = new SyncHistory();
        history.setId(1000L);
        history.setScenarioId(lookupId);
        history.setStatus("SUCCESS");

        // Then - 验证查找结果
        assertEquals(lookupId, scenario.getId());
        assertEquals(lookupId, history.getScenarioId());
        assertEquals(scenarioId, lookupId);
    }

    @Test
    @DisplayName("应支持空值处理")
    void shouldHandleNullValues() {
        // Given
        ErpScenario scenario = new ErpScenario();
        SyncHistory history = new SyncHistory();

        // When - 不设置 ID
        // Then - 默认应为 null
        assertNull(scenario.getId(), "新建实体的 ID 应为 null");
        assertNull(history.getScenarioId(), "新建实体的 scenarioId 应为 null");

        // When - 设置 null
        scenario.setId(null);
        history.setScenarioId(null);

        // Then
        assertNull(scenario.getId());
        assertNull(history.getScenarioId());
    }

    @Test
    @DisplayName("应支持 ID 更新")
    void shouldSupportIdUpdate() {
        // Given
        ErpScenario scenario = new ErpScenario();
        scenario.setId(100L);

        SyncHistory history = new SyncHistory();
        history.setId(1000L);
        history.setScenarioId(100L);

        // When - 更新 ID
        Long newScenarioId = 200L;
        scenario.setId(newScenarioId);
        history.setScenarioId(newScenarioId);

        // Then
        assertEquals(newScenarioId, scenario.getId());
        assertEquals(newScenarioId, history.getScenarioId());
        assertEquals(200L, scenario.getId());
        assertEquals(200L, history.getScenarioId());
    }

    @Test
    @DisplayName("验证 ID 类型一致性 - 两者都应为 Long")
    void shouldHaveConsistentIdTypes() {
        // Given
        ErpScenario scenario = new ErpScenario();
        SyncHistory history = new SyncHistory();
        Long testId = 500L;

        // When
        scenario.setId(testId);
        history.setScenarioId(testId);

        // Then - 验证类型完全一致
        assertTrue(scenario.getId() instanceof Long,
                "ErpScenario.id 应为 Long 类型");
        assertTrue(history.getScenarioId() instanceof Long,
                "SyncHistory.scenarioId 应为 Long 类型");

        // 验证可以使用 Long 方法
        assertEquals(Long.valueOf(500L), scenario.getId());
        assertEquals(Long.valueOf(500L), history.getScenarioId());

        // 验证可以进行 Long 运算
        Long incrementedId = scenario.getId() + 1;
        assertEquals(501L, incrementedId);
    }

    @Test
    @DisplayName("应支持在集合中通过 Long ID 查找")
    void shouldFindInCollectionByLongId() {
        // Given
        Long targetScenarioId = 777L;

        List<ErpScenario> scenarios = List.of(
                createScenario(100L, "SCENARIO_A"),
                createScenario(200L, "SCENARIO_B"),
                createScenario(targetScenarioId, "TARGET_SCENARIO"),
                createScenario(300L, "SCENARIO_C")
        );

        List<SyncHistory> histories = List.of(
                createHistory(1L, 100L),
                createHistory(2L, 200L),
                createHistory(3L, targetScenarioId),  // 关联到目标场景
                createHistory(4L, 300L)
        );

        // When - 通过 Long ID 查找
        ErpScenario foundScenario = scenarios.stream()
                .filter(s -> s.getId().equals(targetScenarioId))
                .findFirst()
                .orElse(null);

        List<SyncHistory> relatedHistories = histories.stream()
                .filter(h -> h.getScenarioId().equals(targetScenarioId))
                .toList();

        // Then
        assertNotNull(foundScenario);
        assertEquals("TARGET_SCENARIO", foundScenario.getScenarioKey());
        assertEquals(1, relatedHistories.size());
        assertEquals(targetScenarioId, relatedHistories.get(0).getScenarioId());
    }

    private ErpScenario createScenario(Long id, String key) {
        ErpScenario scenario = new ErpScenario();
        scenario.setId(id);
        scenario.setScenarioKey(key);
        scenario.setName("场景" + key);
        scenario.setIsActive(true);
        return scenario;
    }

    private SyncHistory createHistory(Long id, Long scenarioId) {
        SyncHistory history = new SyncHistory();
        history.setId(id);
        history.setScenarioId(scenarioId);
        history.setStatus("SUCCESS");
        history.setSyncStartTime(LocalDateTime.now());
        return history;
    }
}
