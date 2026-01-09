// Input: Spring Framework、JUnit 5、Embedded Redis、Java 标准库
// Output: EmbeddedRedisConfig 测试配置类
// Pos: 测试配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import jakarta.annotation.PreDestroy;
import java.io.IOException;

/**
 * Embedded Redis 测试配置
 * <p>
 * 在测试环境中启动内嵌 Redis 服务器，无需外部 Redis 实例。
 * 使用 @Import 注入到测试类中启用。
 * </p>
 *
 * <p>注意：</p>
 * <ul>
 *   <li>默认端口：6370（避免与开发环境 Redis 冲突）</li>
 *   <li>可通过环境变量 TEST_REDIS_PORT 覆盖</li>
 *   <li>测试结束后自动关闭</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @SpringBootTest
 * @Import(EmbeddedRedisConfig.class)
 * class MyCacheTest { }
 * }</pre>
 */
@TestConfiguration
@ConditionalOnProperty(name = "test.redis.embedded", havingValue = "true", matchIfMissing = false)
public class EmbeddedRedisConfig {

    private static final int DEFAULT_PORT = 6370;
    private RedisServer redisServer;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() throws IOException {
        int port = getTestRedisPort();

        // 启动内嵌 Redis
        redisServer = new RedisServer(port);
        redisServer.start();

        System.out.println("========================================");
        System.out.println("Embedded Redis 已启动，端口: " + port);
        System.out.println("========================================");

        // 创建连接工厂
        return new LettuceConnectionFactory("localhost", port);
    }

    @PreDestroy
    public void destroy() {
        if (redisServer != null) {
            redisServer.stop();
            System.out.println("========================================");
            System.out.println("Embedded Redis 已停止");
            System.out.println("========================================");
        }
    }

    private static int getTestRedisPort() {
        String port = System.getenv("TEST_REDIS_PORT");
        if (port != null && !port.isEmpty()) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                System.err.println("无效的 TEST_REDIS_PORT，使用默认值: " + DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }

    /**
     * 检查是否可以使用内嵌 Redis
     * <p>
     * 某些环境（如 macOS ARM）可能需要额外配置
     * </p>
     *
     * @return 是否可以使用
     */
    public static boolean isAvailable() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();

            // macOS ARM (Apple Silicon) 需要特殊处理
            if (os.contains("mac") && arch.contains("aarch64")) {
                System.out.println("警告: Embedded Redis 在 macOS ARM 上可能需要手动安装 Redis");
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
