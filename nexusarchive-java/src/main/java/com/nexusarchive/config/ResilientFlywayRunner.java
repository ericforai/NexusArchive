// Input: Jakarta EE、Lombok、org.flywaydb、Spring Framework、等
// Output: ResilientFlywayRunner 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.nexusarchive.common.result.Result;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 弹性 Flyway 迁移运行器
 * 允许应用在数据库不可用时启动，并在后台异步重试迁移。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ResilientFlywayRunner implements ApplicationRunner {

    private final DataSource dataSource;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled;

    private final AtomicBoolean isReady = new AtomicBoolean(false);
    private final AtomicBoolean isMigrating = new AtomicBoolean(false);
    
    // 状态机：STARTING -> MIGRATING -> READY
    // 简化为 isReady 标识最终态，启动时即视为 MIGRATING

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "flyway-migrator");
        t.setDaemon(true);
        return t;
    });

    public boolean isReady() {
        return isReady.get();
    }

    public String getStatus() {
        return isReady.get() ? "READY" : "MIGRATING";
    }

    @Override
    public void run(ApplicationArguments args) {
        // 支持通过配置禁用Flyway迁移
        if (!flywayEnabled) {
            log.info("Flyway is disabled (spring.flyway.enabled=false), skipping migration");
            isReady.set(true);
            scheduler.shutdown();
            return;
        }
        
        log.info("Starting Resilient Flyway Runner...");
        
        // Initial delay 0, start immediately
        scheduleMigration(0);
    }

    private void scheduleMigration(int consecutiveFailures) {
        if (isReady.get()) {
            return;
        }

        // Exponential backoff: 1s, 2s, 4s... max 30s
        long delaySeconds = (long) Math.min(30, Math.pow(2, consecutiveFailures));
        if (consecutiveFailures == 0) delaySeconds = 0; // First try immediate

        log.info("Scheduling migration attempt #{} in {} seconds...", consecutiveFailures + 1, delaySeconds);

        scheduler.schedule(() -> {
            try {
                if (isReady.get()) return;

                log.info("Attempting database migration...");
                
                // 1. Connection Check (Fast Fail)
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(2)) {
                        throw new Exception("Connection invalid");
                    }
                }

                // 2. Perform Migration
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .locations(flywayLocations.split(","))
                        .baselineOnMigrate(true)
                        .validateOnMigrate(false)
                        .load();
                
                // 自动修复元数据表 (清除 Failed 状态的记录)
                flyway.repair();
                flyway.migrate();
                
                isReady.set(true);
                log.info("Database migration completed successfully. System is READY.");
                scheduler.shutdown();

            } catch (Exception e) {
                log.error("Migration failed (Attempt #{}): {}", consecutiveFailures + 1, e.getMessage());
                // Re-schedule with backoff
                scheduleMigration(consecutiveFailures + 1);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }
}
