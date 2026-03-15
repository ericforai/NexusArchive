// Input: Spring Boot、Spring Framework、Java 标准库
// Output: DataSourceConfig 类 - 读写分离数据源配置
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读写分离数据源配置
 * <p>当 {@code rw-split.enabled=true} 时，配置主从数据源和动态路由</p>
 * <p>排除 Spring Boot 默认的 {@link DataSourceAutoConfiguration}，避免 Bean 冲突</p>
 *
 * <p>配置结构：</p>
 * <pre>
 * rw-split:
 *   enabled: true
 *   master:
 *     url: jdbc:postgresql://master-host:5432/nexusarchive
 *   slaves:
 *     - url: jdbc:postgresql://slave1-host:5432/nexusarchive
 *     - url: jdbc:postgresql://slave2-host:5432/nexusarchive
 * </pre>
 *
 * @since 2.1.0
 */
@Configuration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = "rw-split", name = "enabled", havingValue = "true")
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * 主库数据源配置属性
     */
    @Value("${rw-split.master.url}")
    private String masterUrl;

    @Value("${rw-split.master.username}")
    private String masterUsername;

    @Value("${rw-split.master.password}")
    private String masterPassword;

    @Value("${rw-split.master.hikari.minimum-idle:5}")
    private int masterMinIdle;

    @Value("${rw-split.master.hikari.maximum-pool-size:20}")
    private int masterMaxPoolSize;

    @Value("${rw-split.master.hikari.connection-timeout:1000}")
    private long masterConnectionTimeout;

    /**
     * 从库数据源配置属性（支持多个）
     */
    @Value("${rw-split.slaves[0].url}")
    private String slaveUrl;

    @Value("${rw-split.slaves[0].username}")
    private String slaveUsername;

    @Value("${rw-split.slaves[0].password}")
    private String slavePassword;

    @Value("${rw-split.slaves[0].hikari.minimum-idle:5}")
    private int slaveMinIdle;

    @Value("${rw-split.slaves[0].hikari.maximum-pool-size:30}")
    private int slaveMaxPoolSize;

    @Value("${rw-split.slaves[0].hikari.connection-timeout:1000}")
    private long slaveConnectionTimeout;

    /**
     * 创建主库数据源
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        log.info("[RW_SPLIT] Initializing master data source: {}", maskUrl(masterUrl));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterUrl);
        config.setUsername(masterUsername);
        config.setPassword(masterPassword);
        config.setMinimumIdle(masterMinIdle);
        config.setMaximumPoolSize(masterMaxPoolSize);
        config.setConnectionTimeout(masterConnectionTimeout);
        config.setPoolName("nexus-master-pool");

        // PostgreSQL 优化配置
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }

    /**
     * 创建从库数据源
     * <p>当前实现使用第一个从库配置，未来可扩展为负载均衡</p>
     */
    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        log.info("[RW_SPLIT] Initializing slave data source: {}", maskUrl(slaveUrl));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(slaveUrl);
        config.setUsername(slaveUsername);
        config.setPassword(slavePassword);
        config.setMinimumIdle(slaveMinIdle);
        config.setMaximumPoolSize(slaveMaxPoolSize);
        config.setConnectionTimeout(slaveConnectionTimeout);
        config.setPoolName("nexus-slave-pool");

        // PostgreSQL 优化配置
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }

    /**
     * 创建动态路由数据源
     * <p>这是主数据源，会被 MyBatis 和事务管理器使用</p>
     */
    @Bean(name = "routingDataSource")
    @Primary
    public DataSource routingDataSource() {
        log.info("[RW_SPLIT] Initializing routing data source");

        RoutingDataSource routingDataSource = new RoutingDataSource();

        // 设置目标数据源映射
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DataSourceType.MASTER, masterDataSource());
        dataSourceMap.put(DataSourceType.SLAVE, slaveDataSource());

        routingDataSource.setTargetDataSources(dataSourceMap);

        // 设置默认数据源为从库（读多写少场景）
        routingDataSource.setDefaultTargetDataSource(slaveDataSource());

        return routingDataSource;
    }

    /**
     * 隐藏 URL 中的敏感信息（仅用于日志）
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "null";
        }
        // 隐藏密码部分
        return url.replaceAll("password=[^&]*", "password=***");
    }
}
