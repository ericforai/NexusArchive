// Input: JUnit 5、Java 标准库
// Output: RoutingDataSourceTest 类 - 路由数据源测试
// Pos: 测试层 - 基础设施
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 路由数据源测试
 *
 * @since 2.1.0
 */
@Tag("unit")
@DisplayName("路由数据源测试")
class RoutingDataSourceTest {

    private RoutingDataSource routingDataSource;

    @BeforeEach
    void setUp() {
        routingDataSource = new RoutingDataSource();
        DataSourceContextHolder.clearDataSourceType();
    }

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clearDataSourceType();
    }

    @Test
    @DisplayName("当上下文为 MASTER 时应返回 MASTER")
    void testDetermineMasterKey() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        Object key = routingDataSource.determineCurrentLookupKey();
        assertEquals(DataSourceType.MASTER, key);
    }

    @Test
    @DisplayName("当上下文为 SLAVE 时应返回 SLAVE")
    void testDetermineSlaveKey() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        Object key = routingDataSource.determineCurrentLookupKey();
        assertEquals(DataSourceType.SLAVE, key);
    }

    @Test
    @DisplayName("当上下文为空时应返回默认值 SLAVE")
    void testDetermineDefaultKey() {
        // 不设置任何值
        Object key = routingDataSource.determineCurrentLookupKey();
        assertEquals(DataSourceType.SLAVE, key);
    }

    @Test
    @DisplayName("连续切换数据源应正确路由")
    void testSwitchDataSource() {
        // 先设为 MASTER
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        assertEquals(DataSourceType.MASTER, routingDataSource.determineCurrentLookupKey());

        // 切换为 SLAVE
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        assertEquals(DataSourceType.SLAVE, routingDataSource.determineCurrentLookupKey());

        // 再切回 MASTER
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        assertEquals(DataSourceType.MASTER, routingDataSource.determineCurrentLookupKey());
    }
}
