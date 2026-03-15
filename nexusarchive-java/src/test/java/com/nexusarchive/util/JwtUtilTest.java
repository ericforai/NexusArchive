// Input: JUnit 5、Spring Test、JwtUtil
// Output: JwtUtilTest 类
// Pos: 测试模块

package com.nexusarchive.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtUtil 单元测试
 *
 * 测试 JWT 工具类的 Token 生成、验证、解析和刷新功能
 */
@Tag("unit")
@DisplayName("JWT 工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() throws Exception {
        // 创建临时的密钥对用于测试
        KeyPair keyPair = generateKeyPair();
        String publicKeyPem = publicKeyToPem(keyPair.getPublic());
        String privateKeyPem = privateKeyToPem(keyPair.getPrivate());

        // 创建临时密钥文件
        Path tempDir = Files.createTempDirectory("jwt-test");
        Path publicKeyFile = tempDir.resolve("public.pem");
        Path privateKeyFile = tempDir.resolve("private.pem");
        Files.writeString(publicKeyFile, publicKeyPem);
        Files.writeString(privateKeyFile, privateKeyPem);

        // 使用 Spring 的 DefaultResourceLoader
        ResourceLoader resourceLoader = new DefaultResourceLoader();

        // 创建 JwtUtil 实例并配置
        jwtUtil = new JwtUtil(resourceLoader);
        ReflectionTestUtils.setField(jwtUtil, "publicKeyLocation", "file:" + publicKeyFile.toAbsolutePath());
        ReflectionTestUtils.setField(jwtUtil, "privateKeyLocation", "file:" + privateKeyFile.toAbsolutePath());
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_MS);
        jwtUtil.initKeys();
    }

    @Test
    @DisplayName("应该生成有效的 JWT token")
    void shouldGenerateValidJwtToken() {
        // Given
        String username = "testuser";
        String userId = "user123";

        // When
        String token = jwtUtil.generateToken(username, userId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token).doesNotContain("Bearer");
    }

    @Test
    @DisplayName("应该从 token 中正确提取用户名")
    void shouldExtractUsernameFromToken() {
        // Given
        String username = "testuser";
        String userId = "user123";
        String token = jwtUtil.generateToken(username, userId);

        // When
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("应该从 token 中正确提取用户ID")
    void shouldExtractUserIdFromToken() {
        // Given
        String username = "testuser";
        String userId = "user123";
        String token = jwtUtil.generateToken(username, userId);

        // When
        String extractedUserId = jwtUtil.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("应该从 token 中提取过期时间")
    void shouldExtractExpirationFromToken() {
        // Given
        String username = "testuser";
        String userId = "user123";
        long beforeGenerate = System.currentTimeMillis();
        String token = jwtUtil.generateToken(username, userId);
        long afterGenerate = System.currentTimeMillis();

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        long expirationTime = expiration.getTime();
        assertThat(expirationTime).isGreaterThan(beforeGenerate + EXPIRATION_MS - 1000);
        assertThat(expirationTime).isLessThan(afterGenerate + EXPIRATION_MS + 1000);
    }

    @Test
    @DisplayName("应该验证有效的 token")
    void shouldValidateValidToken() {
        // Given
        String token = jwtUtil.generateToken("testuser", "user123");

        // When
        Boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("应该拒绝无效的 token")
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.token.string";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.validateToken(invalidToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("应该拒绝签名错误的 token")
    void shouldRejectTokenWithWrongSignature() throws Exception {
        // Given - 使用不同的密钥对生成 token
        KeyPair otherKeyPair = generateKeyPair();
        String otherPrivateKeyPem = privateKeyToPem(otherKeyPair.getPrivate());
        String otherPublicKeyPem = publicKeyToPem(otherKeyPair.getPublic());

        // 创建另一个 JwtUtil 来签名
        Path tempDir = Files.createTempDirectory("jwt-test-2");
        Path publicKeyFile = tempDir.resolve("public.pem");
        Path privateKeyFile = tempDir.resolve("private.pem");
        Files.writeString(publicKeyFile, otherPublicKeyPem);
        Files.writeString(privateKeyFile, otherPrivateKeyPem);

        ResourceLoader resourceLoader2 = new DefaultResourceLoader();

        JwtUtil otherJwtUtil = new JwtUtil(resourceLoader2);
        ReflectionTestUtils.setField(otherJwtUtil, "publicKeyLocation", "file:" + publicKeyFile.toAbsolutePath());
        ReflectionTestUtils.setField(otherJwtUtil, "privateKeyLocation", "file:" + privateKeyFile.toAbsolutePath());
        ReflectionTestUtils.setField(otherJwtUtil, "expiration", EXPIRATION_MS);
        otherJwtUtil.initKeys();

        String token = otherJwtUtil.generateToken("testuser", "user123");

        // When & Then - 使用原始的 JwtUtil 验证（不同的公钥）
        assertThatThrownBy(() -> jwtUtil.validateToken(token))
            .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("应该刷新过期的 token")
    void shouldRefreshExpiredToken() throws Exception {
        // Given
        String username = "testuser";
        String userId = "user123";

        // Generate a token that will expire soon
        ReflectionTestUtils.setField(jwtUtil, "expiration", 10L);
        String token = jwtUtil.generateToken(username, userId);
        Thread.sleep(20); // Wait for token to expire

        // When - reset expiration for new token
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_MS);
        String newToken = jwtUtil.refreshToken(token);

        // Then
        assertThat(newToken).isNotNull();
        assertThat(jwtUtil.extractUsername(newToken)).isEqualTo(username);
        assertThat(jwtUtil.extractUserId(newToken)).isEqualTo(userId);
    }

    @Test
    @DisplayName("应该刷新有效的 token")
    void shouldRefreshValidToken() {
        // Given
        String username = "testuser";
        String userId = "user123";
        String token = jwtUtil.generateToken(username, userId);

        // When
        String newToken = jwtUtil.refreshToken(token);

        // Then
        assertThat(newToken).isNotNull();
        assertThat(jwtUtil.extractUsername(newToken)).isEqualTo(username);
        assertThat(jwtUtil.extractUserId(newToken)).isEqualTo(userId);
    }

    @Test
    @DisplayName("应该检测即将过期的 token")
    void shouldDetectTokenExpiringSoon() throws Exception {
        // Given
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1000L); // 1 秒过期
        jwtUtil.initKeys();
        String token = jwtUtil.generateToken("testuser", "user123");

        // When
        boolean isExpiring = jwtUtil.isTokenExpiringSoon(token, 1); // 1 分钟内

        // Then
        assertThat(isExpiring).isTrue();
    }

    @Test
    @DisplayName("应该检测未即将过期的 token")
    void shouldDetectTokenNotExpiringSoon() {
        // Given
        String token = jwtUtil.generateToken("testuser", "user123");

        // When
        boolean isExpiring = jwtUtil.isTokenExpiringSoon(token, 1); // 1 分钟内

        // Then
        assertThat(isExpiring).isFalse();
    }

    @Test
    @DisplayName("应该拒绝格式错误的 token")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "not-a-valid-jwt";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.validateToken(malformedToken))
            .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("应该从刷新的 token 中保留原始 claims")
    void shouldPreserveClaimsWhenRefreshing() {
        // Given
        String username = "testuser";
        String userId = "user123";
        String token = jwtUtil.generateToken(username, userId);

        // When
        String newToken = jwtUtil.refreshToken(token);

        // Then
        assertThat(jwtUtil.extractUsername(newToken)).isEqualTo(username);
        assertThat(jwtUtil.extractUserId(newToken)).isEqualTo(userId);
    }

    // ==================== Helper Methods ====================

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private String publicKeyToPem(java.security.PublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(publicKey.getEncoded()) +
                "\n-----END PUBLIC KEY-----";
    }

    private String privateKeyToPem(java.security.PrivateKey privateKey) {
        return "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(privateKey.getEncoded()) +
                "\n-----END PRIVATE KEY-----";
    }
}
