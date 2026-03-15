// Input: JUnit 5、Java 标准库
// Output: DataSourceTypeTest 类 - 数据源类型枚举测试
// Pos: 测试层 - 基础设施
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 数据源类型枚举测试
 *
 * @since 2.1.0
 */
@Tag("unit")
@DisplayName("数据源类型枚举测试")
class DataSourceTypeTest {

    @Test
    @DisplayName("枚举值应该包含 MASTER 和 SLAVE")
    void testEnumValues() {
        DataSourceType[] values = DataSourceType.values();
        assertEquals(2, values.length);
    }

    @Test
    @DisplayName("MASTER 枚举值应该存在")
    void testMasterExists() {
        DataSourceType master = DataSourceType.MASTER;
        assertNotNull(master);
        assertEquals("MASTER", master.name());
    }

    @Test
    @DisplayName("SLAVE 枚举值应该存在")
    void testSlaveExists() {
        DataSourceType slave = DataSourceType.SLAVE;
        assertNotNull(slave);
        assertEquals("SLAVE", slave.name());
    }
}
