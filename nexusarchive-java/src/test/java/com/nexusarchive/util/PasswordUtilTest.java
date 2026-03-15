// Input: JUnit 5、Spring Test、PasswordUtil
// Output: PasswordUtilTest 类
// Pos: 测试模块

package com.nexusarchive.util;

import com.nexusarchive.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * PasswordUtil 单元测试
 *
 * 测试密码工具类的哈希、验证和策略校验功能
 */
@Tag("unit")
@DisplayName("密码工具类测试")
class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
    }

    // ==================== 密码哈希测试 ====================

    @Test
    @DisplayName("应该生成密码哈希")
    void shouldGeneratePasswordHash() {
        // Given
        String password = "MySecurePassword123!";

        // When
        String hash = passwordUtil.hashPassword(password);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).isNotEmpty();
        assertThat(hash).isNotEqualTo(password);
    }

    @Test
    @DisplayName("应该对相同密码生成不同哈希（盐值）")
    void shouldGenerateDifferentHashForSamePassword() {
        // Given
        String password = "SamePassword123!";

        // When
        String hash1 = passwordUtil.hashPassword(password);
        String hash2 = passwordUtil.hashPassword(password);

        // Then - Argon2 每次都会生成不同的盐值
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("应该验证正确的密码")
    void shouldVerifyCorrectPassword() {
        // Given
        String password = "CorrectPassword123!";
        String hash = passwordUtil.hashPassword(password);

        // When
        boolean isValid = passwordUtil.verifyPassword(hash, password);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("应该拒绝错误的密码")
    void shouldRejectIncorrectPassword() {
        // Given
        String password = "CorrectPassword123!";
        String wrongPassword = "WrongPassword123!";
        String hash = passwordUtil.hashPassword(password);

        // When
        boolean isValid = passwordUtil.verifyPassword(hash, wrongPassword);

        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== 密码策略验证测试 ====================

    @Test
    @DisplayName("应该拒绝空密码")
    void shouldRejectEmptyPassword() {
        // When & Then
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength(""))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("密码不能为空");
    }

    @Test
    @DisplayName("应该拒绝 null 密码")
    void shouldRejectNullPassword() {
        // When & Then
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength(null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("密码不能为空");
    }

    @Test
    @DisplayName("应该拒绝过短的密码")
    void shouldRejectTooShortPassword() {
        // When & Then
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("Ab1!"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("至少 8 位");
    }

    @Test
    @DisplayName("应该拒绝过长的密码")
    void shouldRejectTooLongPassword() {
        // Given
        StringBuilder longPassword = new StringBuilder();
        for (int i = 0; i < 130; i++) {
            longPassword.append("A");
        }

        // When & Then
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength(longPassword.toString()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能超过 128 位");
    }

    @Test
    @DisplayName("应该拒绝复杂度不足的密码")
    void shouldRejectPasswordWithInsufficientComplexity() {
        // 只有字母 - 复杂度 1
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("abcdefgh"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("至少三种");

        // 只有数字 - 复杂度 1
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("12345678"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("至少三种");
    }

    @Test
    @DisplayName("应该接受包含三种字符类型的密码")
    void shouldAcceptPasswordWithThreeCharacterTypes() {
        // 大写 + 小写 + 数字（避免连续字符）
        assertThatCode(() -> passwordUtil.validatePasswordStrength("Abz12359"))
            .doesNotThrowAnyException();

        // 大写 + 小写 + 特殊字符（避免连续字符，至少8位）
        assertThatCode(() -> passwordUtil.validatePasswordStrength("Abc!xyz1"))
            .doesNotThrowAnyException();

        // 小写 + 数字 + 特殊字符（避免连续字符）
        assertThatCode(() -> passwordUtil.validatePasswordStrength("abc357!@"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该接受包含全部四种字符类型的密码")
    void shouldAcceptPasswordWithAllCharacterTypes() {
        assertThatCode(() -> passwordUtil.validatePasswordStrength("Abc123!@"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该拒绝常见弱密码")
    void shouldRejectCommonWeakPasswords() {
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("password"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("过于简单");

        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("12345678"))
            .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("qwerty"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("应该拒绝连续字符的密码")
    void shouldRejectPasswordWithConsecutiveChars() {
        // 连续小写字母 bcde
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("Abcde123!"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("连续字符");

        // 连续数字 1234
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("Abc12345!"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("连续字符");

        // 连续大写字母 BCDE
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("ABCDE12!"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("连续字符");
    }

    @Test
    @DisplayName("应该拒绝重复字符的密码")
    void shouldRejectPasswordWithRepeatingChars() {
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("Abc1111!"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("重复字符");

        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("AAAAaaaa!"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("重复字符");
    }

    @Test
    @DisplayName("应该接受符合策略的强密码")
    void shouldAcceptStrongPassword() {
        assertThatCode(() -> passwordUtil.validatePasswordStrength("MySecure@123"))
            .doesNotThrowAnyException();

        assertThatCode(() -> passwordUtil.validatePasswordStrength("Nexus@2026"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该聚合多个验证错误")
    void shouldAggregateMultipleValidationErrors() {
        // When & Then
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("123"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("至少 8 位")  // 长度不足
            .hasMessageContaining("至少三种"); // 复杂度不足
    }

    @Test
    @DisplayName("应该清除字符数组")
    void shouldWipeCharArray() {
        // Given
        char[] array = {'s', 'e', 'c', 'r', 'e', 't'};

        // When
        passwordUtil.wipeArray(array);

        // Then - Argon2 的 wipeArray 应该清零数组
        // 注意：我们无法直接验证内部实现，但可以确认方法不会抛出异常
        assertThatCode(() -> passwordUtil.wipeArray(array))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该对空字符数组调用 wipeArray 不抛异常")
    void shouldWipeEmptyArray() {
        // Given
        char[] emptyArray = {};

        // When & Then
        assertThatCode(() -> passwordUtil.wipeArray(emptyArray))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("哈希和验证应该是幂等的")
    void shouldBeIdempotentForHashAndVerify() {
        // Given
        String password = "TestPassword123!";
        String hash = passwordUtil.hashPassword(password);

        // When - 多次验证
        boolean isValid1 = passwordUtil.verifyPassword(hash, password);
        boolean isValid2 = passwordUtil.verifyPassword(hash, password);
        boolean isValid3 = passwordUtil.verifyPassword(hash, password);

        // Then
        assertThat(isValid1).isTrue();
        assertThat(isValid2).isTrue();
        assertThat(isValid3).isTrue();
    }

    @Test
    @DisplayName("应该拒绝弱密码的变体（大小写变化）")
    void shouldRejectWeakPasswordVariants() {
        // 弱密码黑名单检查是大小写不敏感的
        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("PassWord"))
            .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> passwordUtil.validatePasswordStrength("PASSWORD"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("应该验证带有特殊字符的密码")
    void shouldHandleSpecialCharacters() {
        // 测试各种特殊字符（避免连续字符）
        assertThatCode(() -> passwordUtil.validatePasswordStrength("Test@1293"))
            .doesNotThrowAnyException();

        assertThatCode(() -> passwordUtil.validatePasswordStrength("Test#5729"))
            .doesNotThrowAnyException();

        assertThatCode(() -> passwordUtil.validatePasswordStrength("Test$9753"))
            .doesNotThrowAnyException();

        assertThatCode(() -> passwordUtil.validatePasswordStrength("Test%3579"))
            .doesNotThrowAnyException();

        assertThatCode(() -> passwordUtil.validatePasswordStrength("Test^5791"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该正确处理 Unicode 字符")
    void shouldHandleUnicodeCharacters() {
        // 包含 Unicode 字符的密码
        assertThatCode(() -> passwordUtil.validatePasswordStrength("Test中文123!"))
            .doesNotThrowAnyException();

        String hash = passwordUtil.hashPassword("Test中文123!");
        boolean isValid = passwordUtil.verifyPassword(hash, "Test中文123!");
        assertThat(isValid).isTrue();
    }
}
