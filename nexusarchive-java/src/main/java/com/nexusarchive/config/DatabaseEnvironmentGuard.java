// Input: Spring Boot, JDBC, SLF4J
// Output: DatabaseEnvironmentGuard 组件
// Pos: 系统配置/启动守卫
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库环境守卫 (Environment Guard)
 * 职责：杜绝后端连接到非预期的数据库实例（如宿主机本地 Postgres 冲突）。
 * 逻辑：如果连接的是敏感端口 (5432)，则强制核验 sys_env_marker 表中的项目指纹。
 */
@Slf4j
@Configuration
@Profile("dev")
public class DatabaseEnvironmentGuard {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    private final DataSource dataSource;

    public DatabaseEnvironmentGuard(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void validateEnvironment() {
        log.info("[Guard] Audit database environment for: {}", jdbcUrl);

        // 检查是否使用了典型的本地冲突端口 5432
        boolean isSensitivePort = jdbcUrl.contains(":5432/");

        try (Connection conn = dataSource.getConnection()) {
            // 尝试读取环境安全标记
            boolean markerFound = false;
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT marker_value FROM sys_env_marker WHERE marker_key = 'INSTANCE_SIG'")) {
                if (rs.next()) {
                    String sig = rs.getString("marker_value");
                    if ("NEXUS_ARCHIVE_SAFE_INSTANCE".equals(sig)) {
                        markerFound = true;
                    }
                }
            } catch (Exception e) {
                // 如果表不存在，说明是全新库或者是没干预过的本地库
                log.debug("[Guard] sig table not found yet: {}", e.getMessage());
            }

            if (isSensitivePort && !markerFound) {
                log.error("****************************************************************");
                log.error("CRITICAL ERROR: ENVIRONMENT MISMATCH DETECTED!");
                log.error("Backend is trying to connect to a local Postgres on port 5432,");
                log.error("but the 'sys_env_marker' is missing or incorrect.");
                log.error("This often means you are connecting to your HOST OS Postgres");
                log.error("instead of the project DOCKER container.");
                log.error("ACTION REQUIRED: Check your .env file or use port 15432.");
                log.error("****************************************************************");
                // 抛出运行时异常阻止启动
                throw new IllegalStateException(
                        "Security Guard: Refused to boot on unverified local database instance.");
            }

            log.info("[Guard] Database fingerprint verified. Environment: OK.");
        } catch (Exception e) {
            if (e instanceof IllegalStateException)
                throw (IllegalStateException) e;
            log.warn(
                    "[Guard] Unable to execute pre-flight signature check (DB might be empty). Continuing to Flyway...");
        }
    }
}
