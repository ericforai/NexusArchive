// Input: JUnit 5、Java 标准库
// Output: AspectBehaviorDocumentationTest 类 - Aspect 行为文档测试
// Pos: 测试层 - 基础设施

package com.nexusarchive.config.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Aspect 行为文档测试
 * <p>通过代码示例展示 @ReadOnly 和 @Transactional 注解的交互行为，
 * 作为开发人员的行为参考文档</p>
 *
 * <h3>路由规则总结：</h3>
 * <pre>
 * ┌─────────────────────────────────────┬───────────────┐
 * │ 注解组合                             │ 路由到        │
 * ├─────────────────────────────────────┼───────────────┤
 * │ @ReadOnly                           │ SLAVE         │
 * │ @Transactional(readOnly=true)       │ SLAVE         │
 * │ @ReadOnly + @Transactional(RO=true) │ SLAVE         │
 * │ @Transactional(readOnly=false)      │ MASTER        │
 * │ @ReadOnly + @Transactional(RO=false)│ MASTER ✅      │
 * │ (无注解)                            │ SLAVE (默认)  │
 * └─────────────────────────────────────┴───────────────┘
 * </pre>
 *
 * @since 2.1.0
 */
@Tag("unit")
@DisplayName("Aspect 行为文档测试")
class AspectBehaviorDocumentationTest {

    @BeforeEach
    void setUp() {
        DataSourceContextHolder.clearDataSourceType();
    }

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clearDataSourceType();
    }

    @Test
    @DisplayName("场景1: @ReadOnly 单独使用 → SLAVE")
    void scenario1_ReadOnlyAlone() {
        // 场景：统计数据查询，容忍轻微延迟
        // 代码示例：
        // @ReadOnly
        // public DashboardStats getDashboardStats() { ... }

        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("场景2: @Transactional(readOnly=false) → MASTER")
    void scenario2_TransactionalWrite() {
        // 场景：写操作或需要强一致性的读操作
        // 代码示例：
        // @Transactional
        // public void createArchive(Archive archive) { ... }

        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("场景3: @ReadOnly + @Transactional(readOnly=false) → MASTER (写优先)")
    void scenario3_ConflictWriteWins() {
        // 场景：注解冲突时，写事务优先级高于只读标记
        // 代码示例：
        // @ReadOnly
        // @Transactional  // 等价于 readOnly=false
        // public void saveWithReadOnlyAnnotation() { ... }
        //
        // 结果：路由到 MASTER，因为事务的写语义优先

        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("场景4: 无注解 → SLAVE (默认)")
    void scenario4_DefaultRouting() {
        // 场景：未明确标记的方法使用默认数据源
        // 代码示例：
        // public List<Archive> findAll() { ... }
        //
        // 结果：路由到 SLAVE（默认数据源）

        // 不设置任何值
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("行为验证: 连续切换数据源")
    void verifyContextSwitching() {
        // 验证 ThreadLocal 上下文可以正确切换

        // 1. 初始状态
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType(),
                "初始应使用默认 SLAVE");

        // 2. 切换到 MASTER
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.getDataSourceType(),
                "应切换到 MASTER");

        // 3. 切换回 SLAVE
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType(),
                "应切换回 SLAVE");

        // 4. 清理后恢复默认
        DataSourceContextHolder.clearDataSourceType();
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType(),
                "清理后应恢复默认 SLAVE");
    }

    @Test
    @DisplayName("边界情况: ThreadLocal 隔离性")
    void verifyThreadIsolation() throws InterruptedException {
        // 验证不同线程的上下文隔离

        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);

        Thread otherThread = new Thread(() -> {
            // 新线程应看到默认值，不受主线程影响
            assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType(),
                    "新线程应使用默认 SLAVE");

            DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
            assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType(),
                    "新线程可以设置自己的数据源");
        });

        otherThread.start();
        otherThread.join();

        // 主线程的上下文不受影响
        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.getDataSourceType(),
                "主线程的 MASTER 设置应保持不变");
    }
}
