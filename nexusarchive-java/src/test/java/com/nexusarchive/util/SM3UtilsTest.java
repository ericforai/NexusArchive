// Input: JUnit 5、Spring Test、SM3Utils
// Output: SM3UtilsTest 类
// Pos: 测试模块

package com.nexusarchive.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * SM3Utils 单元测试
 *
 * 测试国密 SM3 哈希工具类的功能
 */
@Tag("unit")
@DisplayName("SM3 哈希工具类测试")
class SM3UtilsTest {

    private SM3Utils sm3Utils;

    @BeforeEach
    void setUp() {
        sm3Utils = new SM3Utils();
    }

    @Test
    @DisplayName("应该计算字符串的 SM3 哈希值")
    void shouldCalculateStringHash() {
        // Given
        String content = "Hello, NexusArchive!";

        // When
        String hash = sm3Utils.hash(content);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64); // SM3 输出 256 位 = 64 个十六进制字符
        assertThat(hash).matches("^[a-fA-F0-9]{64}$");
    }

    @Test
    @DisplayName("应该计算字节数组的 SM3 哈希值")
    void shouldCalculateByteArrayHash() {
        // Given
        byte[] data = "Hello, SM3!".getBytes();

        // When
        String hash = sm3Utils.hash(data);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
    }

    @Test
    @DisplayName("应该对相同输入产生相同哈希值")
    void shouldProduceSameHashForSameInput() {
        // Given
        String content = "Consistent Input";

        // When
        String hash1 = sm3Utils.hash(content);
        String hash2 = sm3Utils.hash(content);

        // Then
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("应该对不同输入产生不同哈希值")
    void shouldProduceDifferentHashForDifferentInput() {
        // Given
        String content1 = "Input One";
        String content2 = "Input Two";

        // When
        String hash1 = sm3Utils.hash(content1);
        String hash2 = sm3Utils.hash(content2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("应该对空字符串返回 null")
    void shouldReturnNullForEmptyString() {
        // When
        String hash = sm3Utils.hash("");

        // Then
        assertThat(hash).isNull();
    }

    @Test
    @DisplayName("应该对 null 输入返回 null")
    void shouldReturnNullForNullInput() {
        // When
        String hash = sm3Utils.hash((String) null);

        // Then
        assertThat(hash).isNull();
    }

    @Test
    @DisplayName("应该对空字节数组返回 null")
    void shouldReturnNullForEmptyByteArray() {
        // When
        String hash = sm3Utils.hash(new byte[0]);

        // Then
        assertThat(hash).isNull();
    }

    @Test
    @DisplayName("应该计算审计日志哈希链")
    void shouldCalculateAuditLogHash() {
        // Given
        String operatorId = "user123";
        String operationType = "CREATE";
        String objectDigest = "abc123";
        String createdTime = "2026-01-15T10:30:00";
        String prevLogHash = "prevHashValue";

        // When
        String logHash = sm3Utils.calculateLogHash(
                operatorId, operationType, objectDigest, createdTime, prevLogHash);

        // Then
        assertThat(logHash).isNotNull();
        assertThat(logHash).hasSize(64);
    }

    @Test
    @DisplayName("应该计算包含 null 值的审计日志哈希链")
    void shouldCalculateLogHashWithNullValues() {
        // Given - 全部使用 null 值
        // When
        String logHash = sm3Utils.calculateLogHash(null, null, null, null, null);

        // Then
        assertThat(logHash).isNotNull();
        assertThat(logHash).hasSize(64);
    }

    @Test
    @DisplayName("应该计算 SM3 HMAC")
    void shouldCalculateHmac() {
        // Given
        String key = "secret-key";
        String content = "Message to authenticate";

        // When
        String hmac = sm3Utils.hmac(key, content);

        // Then
        assertThat(hmac).isNotNull();
        assertThat(hmac).hasSize(64);
    }

    @Test
    @DisplayName("应该对相同密钥和内容产生相同 HMAC")
    void shouldProduceSameHmacForSameKeyAndContent() {
        // Given
        String key = "secret-key";
        String content = "Message";

        // When
        String hmac1 = sm3Utils.hmac(key, content);
        String hmac2 = sm3Utils.hmac(key, content);

        // Then
        assertThat(hmac1).isEqualTo(hmac2);
    }

    @Test
    @DisplayName("应该对不同密钥产生不同 HMAC")
    void shouldProduceDifferentHmacForDifferentKey() {
        // Given
        String content = "Message";

        // When
        String hmac1 = sm3Utils.hmac("key1", content);
        String hmac2 = sm3Utils.hmac("key2", content);

        // Then
        assertThat(hmac1).isNotEqualTo(hmac2);
    }

    @Test
    @DisplayName("应该对空内容返回 null")
    void shouldReturnNullForEmptyContent() {
        // When
        String hmac = sm3Utils.hmac("key", "");

        // Then
        assertThat(hmac).isNull();
    }

    @Test
    @DisplayName("应该对 null 密钥使用普通哈希")
    void shouldUseRegularHashForNullKey() {
        // Given
        String content = "Message";

        // When
        String hmac = sm3Utils.hmac(null, content);
        String hash = sm3Utils.hash(content);

        // Then
        assertThat(hmac).isEqualTo(hash);
    }

    @Test
    @DisplayName("应该对空字符串密钥使用普通哈希")
    void shouldUseRegularHashForEmptyKey() {
        // Given
        String content = "Message";

        // When
        String hmac = sm3Utils.hmac("   ", content);
        String hash = sm3Utils.hash(content);

        // Then
        assertThat(hmac).isEqualTo(hash);
    }

    @Test
    @DisplayName("应该验证匹配的哈希值")
    void shouldVerifyMatchingHash() {
        // Given
        String hash = "abc123def456";

        // When
        boolean result = sm3Utils.verifyHash(hash, hash);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("应该验证不匹配的哈希值")
    void shouldNotVerifyMismatchedHash() {
        // Given
        String hash1 = "abc123";
        String hash2 = "def456";

        // When
        boolean result = sm3Utils.verifyHash(hash1, hash2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("应该对大小写不敏感验证哈希值")
    void shouldVerifyHashCaseInsensitive() {
        // Given
        String hash1 = "abc123def456";
        String hash2 = "ABC123DEF456";

        // When
        boolean result = sm3Utils.verifyHash(hash1, hash2);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("应该处理 null 哈希值验证")
    void shouldHandleNullHashVerification() {
        // When & Then
        assertThat(sm3Utils.verifyHash(null, null)).isTrue();
        assertThat(sm3Utils.verifyHash("abc", null)).isFalse();
        assertThat(sm3Utils.verifyHash(null, "def")).isFalse();
    }

    @Test
    @DisplayName("应该生成符合 SM3 标准的测试向量")
    void shouldGenerateStandardSM3TestVector() {
        // Given - SM3 标准测试向量
        String input = "abc";
        String expectedHash = "66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0";

        // When
        String actualHash = sm3Utils.hash(input);

        // Then
        assertThat(actualHash).isEqualToIgnoringCase(expectedHash);
    }

    @Test
    @DisplayName("应该对长文本正确计算哈希")
    void shouldCalculateHashForLongText() {
        // Given
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("NexusArchive");
        }

        // When
        String hash = sm3Utils.hash(longText.toString());

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
    }

    @Test
    @DisplayName("应该计算连续的哈希链")
    void shouldCalculateHashChain() {
        // Given - 模拟审计日志链
        String prevHash = "";
        String hash1 = sm3Utils.calculateLogHash("user1", "CREATE", "obj1", "2026-01-15T00:00:00", prevHash);
        String hash2 = sm3Utils.calculateLogHash("user2", "UPDATE", "obj2", "2026-01-15T00:01:00", hash1);
        String hash3 = sm3Utils.calculateLogHash("user3", "DELETE", "obj3", "2026-01-15T00:02:00", hash2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash2).isNotEqualTo(hash3);
        assertThat(hash3).isNotNull();
    }
}
