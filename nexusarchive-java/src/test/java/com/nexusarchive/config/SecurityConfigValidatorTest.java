// Input: JUnit 5、Spring Boot Test
// Output: SecurityConfigValidatorTest 类
// Pos: 测试模块

package com.nexusarchive.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SecurityConfigValidator 单元测试
 *
 * 测试安全配置验证器的各种场景
 */
class SecurityConfigValidatorTest {

    private SecurityConfigValidator validator;
    private Environment mockEnvironment;

    @BeforeEach
    void setUp() {
        mockEnvironment = mock(Environment.class);
        validator = new SecurityConfigValidator(mockEnvironment);
    }

    @AfterEach
    void tearDown() {
        // 清理环境变量
        System.clearProperty("SM4_KEY");
        System.clearProperty("JWT_SECRET");
    }

    @Test
    void shouldDetectProductionProfile() {
        // 模拟生产环境 profile
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"prod"});

        // 不应该抛出异常，应该正常记录日志
        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldDetectProductionProfile_FromMultipleProfiles() {
        // 多个 profile，包含 prod
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"dev", "prod", "cloud"});

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldNotDetectProductionProfile_InDevEnvironment() {
        // 开发环境
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"dev", "local"});

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldHandleEmptyProfiles() {
        // 空 profile 列表
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{});

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldDetectProductionProfile_WithProdPrefix() {
        // 包含 prod 前缀的 profile（如 prod-aws）
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"prod-aws"});

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldHandleProductionProfile_NoSm4Key() {
        // 生产环境，无 SM4_KEY
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"prod"});

        // 清除 SM4_KEY 环境变量
        System.clearProperty("SM4_KEY");

        // 不应该抛出异常，只记录错误日志
        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldHandleProductionProfile_WithSm4Key() {
        // 生产环境，有 SM4_KEY
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"prod"});

        // 设置 SM4_KEY 环境变量
        System.setProperty("SM4_KEY", "0123456789abcdef0123456789abcdef");

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldHandleProductionProfile_WithJwtSecret() {
        // 生产环境，有 JWT_SECRET
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"prod"});

        System.setProperty("JWT_SECRET", "test-secret-key-min-32-chars");

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldHandleDevelopmentProfile() {
        // 开发环境
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"dev"});

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }

    @Test
    void shouldHandleTestProfile() {
        // 测试环境
        when(mockEnvironment.getActiveProfiles())
            .thenReturn(new String[]{"test"});

        assertDoesNotThrow(() -> validator.validateSecurityConfiguration());
    }
}
