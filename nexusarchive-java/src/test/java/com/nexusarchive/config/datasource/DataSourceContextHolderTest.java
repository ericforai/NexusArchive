// Input: JUnit 5、Java 标准库
// Output: DataSourceContextHolderTest 类 - 数据源上下文持有者测试
// Pos: 测试层 - 基础设施
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 数据源上下文持有者测试
 *
 * @since 2.1.0
 */
@Tag("unit")
@DisplayName("数据源上下文持有者测试")
class DataSourceContextHolderTest {

    @BeforeEach
    void setUp() {
        // 每个测试前清理上下文
        DataSourceContextHolder.clearDataSourceType();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清理上下文
        DataSourceContextHolder.clearDataSourceType();
    }

    @Test
    @DisplayName("设置并获取 MASTER 数据源类型")
    void testSetAndGetMasterDataSource() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("设置并获取 SLAVE 数据源类型")
    void testSetAndGetSlaveDataSource() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("未设置时默认返回 SLAVE")
    void testDefaultReturnsSlave() {
        // 不设置任何值，直接获取
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("清理后获取应返回默认值 SLAVE")
    void testClearReturnsDefault() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        DataSourceContextHolder.clearDataSourceType();
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("设置 null 应该清理上下文")
    void testSetNullClearsContext() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        DataSourceContextHolder.setDataSourceType(null);
        assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    @DisplayName("线程隔离测试 - 不同线程应独立")
    void testThreadIsolation() throws InterruptedException {
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);

        Thread otherThread = new Thread(() -> {
            // 新线程应该看不到主线程的设置
            assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.getDataSourceType());
            DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        });

        otherThread.start();
        otherThread.join();

        // 主线程的设置不应受影响
        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.getDataSourceType());
    }
}
