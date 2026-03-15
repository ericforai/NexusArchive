// Input: Spring Framework、Java 标准库
// Output: RoutingDataSource 类 - 动态路由数据源
// Pos: 基础设施层 - 数据源路由
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源
 * <p>基于 {@link AbstractRoutingDataSource} 实现，根据 {@link DataSourceContextHolder}
 * 中存储的数据源类型动态选择主库或从库</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>SQL 执行前，Spring 调用 {@code determineCurrentLookupKey()}</li>
 *   <li>从 {@link DataSourceContextHolder} 获取当前线程的数据源类型</li>
 *   <li>根据类型返回对应的数据源 key（MASTER 或 SLAVE）</li>
 *   <li>Spring 使用该数据源执行 SQL</li>
 * </ol>
 *
 * @since 2.1.0
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

    /**
     * 确定当前数据源的查找键
     * <p>从 {@link DataSourceContextHolder} 获取当前线程的数据源类型</p>
     *
     * @return 数据源类型（MASTER 或 SLAVE）
     */
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType dataSourceType = DataSourceContextHolder.getDataSourceType();

        if (log.isTraceEnabled()) {
            log.trace("[RW_SPLIT] Current data source routing to: {}", dataSourceType);
        }

        return dataSourceType;
    }
}
