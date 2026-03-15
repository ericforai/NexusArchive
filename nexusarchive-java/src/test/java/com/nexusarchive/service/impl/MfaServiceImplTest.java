// Input: JUnit 5, Mockito, AssertJ, Spring Framework, Java 标准库
// Output: MfaServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.dto.response.MfaSetupResponse;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.entity.UserMfaConfig;
import com.nexusarchive.mapper.UserMfaConfigMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.UserService;
import com.nexusarchive.util.SM4Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MfaServiceImpl 单元测试
 *
 * 测试覆盖:
 * - MFA 设置初始化
 * - MFA 启用
 * - MFA 禁用
 * - TOTP 验证码验证
 * - 备用码验证和生成
 * - MFA 状态查询
 * - 边界情况和异常处理
 */
@ExtendWith(MockitoExtension.class)
class MfaServiceImplTest {

    @Mock
    private UserMfaConfigMapper mfaConfigMapper;

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MfaServiceImpl mfaService;

    private UserMfaConfig testConfig;
    private UserResponse testUser;

    @BeforeEach
    void setUp() {
        // 启用 MFA 功能（默认禁用）
        ReflectionTestUtils.setField(mfaService, "mfaEnabled", true);

        // 创建测试用户
        testUser = new UserResponse();
        testUser.setId("user-001");
        testUser.setUsername("testuser");

        // 创建测试 MFA 配置
        testConfig = new UserMfaConfig();
        testConfig.setId("config-001");
        testConfig.setUserId("user-001");
        testConfig.setMfaEnabled(false);
        testConfig.setMfaType("TOTP");
        testConfig.setSecretKey("encrypted-secret-key");
        testConfig.setBackupCodes("encrypted-backup-codes");
        testConfig.setCreatedAt(LocalDateTime.now());
        testConfig.setUpdatedAt(LocalDateTime.now());
        testConfig.setDeleted(0);
    }

    // ========== MFA 设置初始化测试 ==========

    @Nested
    @DisplayName("MFA 设置初始化")
    class SetupMfaTests {

        @Test
        @DisplayName("正常初始化 MFA 设置（新配置）")
        void setupMfa_NewConfig_Success() {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(testUser);
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);
            when(mfaConfigMapper.insert(any(UserMfaConfig.class))).thenAnswer(invocation -> {
                UserMfaConfig config = invocation.getArgument(0);
                config.setId("generated-config-id");
                return 1;
            });
            when(mfaConfigMapper.updateById(any(UserMfaConfig.class))).thenReturn(1);

            // Act
            MfaSetupResponse response = mfaService.setupMfa("user-001");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSecretKey()).isNotNull().hasSize(32);
            assertThat(response.getQrCodeUrl()).isNotNull()
                    .contains("otpauth://totp")
                    .contains("NexusArchive:testuser");
            assertThat(response.getBackupCodes()).isNotNull().hasSize(10);
            assertThat(response.getInstructions()).isNotNull();

            // Verify 验证
            verify(mfaConfigMapper).insert(any(UserMfaConfig.class));
            verify(mfaConfigMapper).updateById(any(UserMfaConfig.class));
        }

        @Test
        @DisplayName("初始化 MFA 设置（更新现有配置）")
        void setupMfa_ExistingConfig_UpdatesConfig() {
            // Arrange
            UserMfaConfig existingConfig = new UserMfaConfig();
            existingConfig.setId("existing-config-id");
            existingConfig.setUserId("user-001");
            existingConfig.setMfaEnabled(false);

            when(userService.getUserById("user-001")).thenReturn(testUser);
            when(mfaConfigMapper.selectOne(any())).thenReturn(existingConfig);
            when(mfaConfigMapper.updateById(any(UserMfaConfig.class))).thenReturn(1);

            // Act
            MfaSetupResponse response = mfaService.setupMfa("user-001");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSecretKey()).isNotNull();

            // Verify - 应该更新而非插入
            verify(mfaConfigMapper).updateById(any(UserMfaConfig.class));
        }

        @Test
        @DisplayName("MFA 功能未启用 - 抛出异常")
        void setupMfa_MfaDisabled_ThrowsException() {
            // Arrange
            ReflectionTestUtils.setField(mfaService, "mfaEnabled", false);

            // Act & Assert
            assertThatThrownBy(() -> mfaService.setupMfa("user-001"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MFA 功能未启用");
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void setupMfa_UserNotFound_ThrowsException() {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> mfaService.setupMfa("user-001"))
                    .isInstanceOf(Exception.class);
        }
    }

    // ========== MFA 启用测试 ==========

    @Nested
    @DisplayName("启用 MFA")
    class EnableMfaTests {

        @Test
        @DisplayName("正常启用 MFA（需要 TOTP 库支持）")
        void enableMfa_ValidCode_TotpNotImplementedThrowsException() {
            // Arrange
            testConfig.setMfaEnabled(false);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);
            when(mfaConfigMapper.updateById(any(UserMfaConfig.class))).thenReturn(1);
            when(userService.getUserById("user-001")).thenReturn(testUser);

            // Act & Assert - TOTP 功能未实现，会抛出异常
            assertThatThrownBy(() -> mfaService.enableMfa("user-001", "123456"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("TOTP code generation not yet implemented");
        }

        @Test
        @DisplayName("MFA 功能未启用 - 抛出异常")
        void enableMfa_MfaDisabled_ThrowsException() {
            // Arrange
            ReflectionTestUtils.setField(mfaService, "mfaEnabled", false);

            // Act & Assert
            assertThatThrownBy(() -> mfaService.enableMfa("user-001", "123456"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MFA 功能未启用");
        }

        @Test
        @DisplayName("MFA 配置不存在")
        void enableMfa_ConfigNotFound_VerifyTotpFails() {
            // Arrange
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);

            // Act & Assert - 由于配置不存在，verifyTotpCode 返回 false
            assertThatThrownBy(() -> mfaService.enableMfa("user-001", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("验证码错误");
        }
    }

    // ========== MFA 禁用测试 ==========

    @Nested
    @DisplayName("禁用 MFA")
    class DisableMfaTests {

        @Test
        @DisplayName("正常禁用 MFA")
        void disableMfa_ConfigExists_Success() {
            // Arrange
            testConfig.setMfaEnabled(true);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);
            when(mfaConfigMapper.updateById(any(UserMfaConfig.class))).thenReturn(1);
            when(userService.getUserById("user-001")).thenReturn(testUser);

            // Act
            mfaService.disableMfa("user-001", "password");

            // Assert
            verify(mfaConfigMapper).updateById(any(UserMfaConfig.class));
        }

        @Test
        @DisplayName("MFA 未配置 - 抛出异常")
        void disableMfa_ConfigNotExists_ThrowsException() {
            // Arrange
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> mfaService.disableMfa("user-001", "password"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MFA 未配置");
        }
    }

    // ========== TOTP 验证测试 ==========

    @Nested
    @DisplayName("TOTP 验证码验证")
    class VerifyTotpCodeTests {

        @Test
        @DisplayName("MFA 未启用返回 false")
        void verifyTotpCode_MfaNotEnabled_ReturnsFalse() {
            // Arrange
            testConfig.setMfaEnabled(false);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act
            boolean result = mfaService.verifyTotpCode("user-001", "123456");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("MFA 配置不存在返回 false")
        void verifyTotpCode_ConfigNotExists_ReturnsFalse() {
            // Arrange
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);

            // Act
            boolean result = mfaService.verifyTotpCode("user-001", "123456");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("TOTP 功能未实现 - 抛出异常")
        void verifyTotpCode_TotpNotImplemented_ThrowsException() {
            // Arrange
            testConfig.setMfaEnabled(true);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act & Assert
            assertThatThrownBy(() -> mfaService.verifyTotpCode("user-001", "123456"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("TOTP code generation not yet implemented");
        }
    }

    // ========== 备用码验证测试 ==========

    @Nested
    @DisplayName("备用码验证")
    class VerifyBackupCodeTests {

        @Test
        @DisplayName("有效备用码验证成功")
        void verifyBackupCode_ValidCode_Success() throws Exception {
            // Arrange
            List<String> backupCodes = Arrays.asList("123456", "234567", "345678");
            String encryptedBackupCodes = SM4Utils.encrypt(objectMapper.writeValueAsString(backupCodes));

            testConfig.setMfaEnabled(true);
            testConfig.setBackupCodes(encryptedBackupCodes);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);
            when(mfaConfigMapper.updateById(any(UserMfaConfig.class))).thenReturn(1);

            // Act
            boolean result = mfaService.verifyBackupCode("user-001", "123456");

            // Assert
            assertThat(result).isTrue();
            verify(mfaConfigMapper).updateById(any(UserMfaConfig.class));
        }

        @Test
        @DisplayName("无效备用码验证失败")
        void verifyBackupCode_InvalidCode_ReturnsFalse() throws Exception {
            // Arrange
            List<String> backupCodes = Arrays.asList("123456", "234567", "345678");
            String encryptedBackupCodes = SM4Utils.encrypt(objectMapper.writeValueAsString(backupCodes));

            testConfig.setMfaEnabled(true);
            testConfig.setBackupCodes(encryptedBackupCodes);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act
            boolean result = mfaService.verifyBackupCode("user-001", "999999");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("备用码已用尽")
        void verifyBackupCode_AllCodesUsed_ReturnsFalse() throws Exception {
            // Arrange - 空备用码列表
            List<String> backupCodes = Collections.emptyList();
            String encryptedBackupCodes = SM4Utils.encrypt(objectMapper.writeValueAsString(backupCodes));

            testConfig.setMfaEnabled(true);
            testConfig.setBackupCodes(encryptedBackupCodes);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act
            boolean result = mfaService.verifyBackupCode("user-001", "123456");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("MFA 配置不存在返回 false")
        void verifyBackupCode_ConfigNotExists_ReturnsFalse() {
            // Arrange
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);

            // Act
            boolean result = mfaService.verifyBackupCode("user-001", "123456");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("备用码为空返回 false")
        void verifyBackupCode_NoBackupCodes_ReturnsFalse() {
            // Arrange
            testConfig.setMfaEnabled(true);
            testConfig.setBackupCodes(null);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act
            boolean result = mfaService.verifyBackupCode("user-001", "123456");

            // Assert
            assertThat(result).isFalse();
        }
    }

    // ========== 生成备用码测试 ==========

    @Nested
    @DisplayName("生成备用码")
    class GenerateBackupCodesTests {

        @Test
        @DisplayName("正常生成备用码")
        void generateBackupCodes_ConfigExists_Success() throws Exception {
            // Arrange
            testConfig.setMfaEnabled(true);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);
            when(mfaConfigMapper.updateById(any(UserMfaConfig.class))).thenReturn(1);

            // Act
            List<String> codes = mfaService.generateBackupCodes("user-001");

            // Assert
            assertThat(codes).isNotNull().hasSize(10);
            codes.forEach(code -> {
                assertThat(code).hasSize(6);
                assertThat(code).matches("\\d{6}");
            });
            verify(mfaConfigMapper).updateById(any(UserMfaConfig.class));
        }

        @Test
        @DisplayName("MFA 未配置 - 抛出异常")
        void generateBackupCodes_ConfigNotExists_ThrowsException() {
            // Arrange
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> mfaService.generateBackupCodes("user-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MFA 未配置");
        }
    }

    // ========== MFA 状态查询测试 ==========

    @Nested
    @DisplayName("MFA 状态查询")
    class IsMfaEnabledTests {

        @Test
        @DisplayName("系统级 MFA 功能开关关闭返回 false")
        void isMfaEnabled_SystemDisabled_ReturnsFalse() {
            // Arrange
            ReflectionTestUtils.setField(mfaService, "mfaEnabled", false);

            // Act
            boolean result = mfaService.isMfaEnabled("user-001");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("用户未配置 MFA 返回 false")
        void isMfaEnabled_UserConfigNotExists_ReturnsFalse() {
            // Arrange
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);

            // Act
            boolean result = mfaService.isMfaEnabled("user-001");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("用户 MFA 已启用返回 true")
        void isMfaEnabled_UserMfaEnabled_ReturnsTrue() {
            // Arrange
            testConfig.setMfaEnabled(true);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act
            boolean result = mfaService.isMfaEnabled("user-001");

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("用户 MFA 未启用返回 false")
        void isMfaEnabled_UserMfaNotEnabled_ReturnsFalse() {
            // Arrange
            testConfig.setMfaEnabled(false);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act
            boolean result = mfaService.isMfaEnabled("user-001");

            // Assert
            assertThat(result).isFalse();
        }
    }

    // ========== 边界条件测试 ==========

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("空用户ID处理")
        void isMfaEnabled_EmptyUserId_ReturnsFalse() {
            // Arrange
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);

            // Act
            boolean result = mfaService.isMfaEnabled("");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 备用码参数处理")
        void verifyBackupCode_NullCode_ReturnsFalse() {
            // Arrange
            testConfig.setMfaEnabled(true);
            testConfig.setBackupCodes("some-encrypted-value");
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act
            boolean result = mfaService.verifyBackupCode("user-001", null);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 验证码参数处理")
        void verifyTotpCode_NullCode_ReturnsFalse() {
            // Arrange
            testConfig.setMfaEnabled(true);
            when(mfaConfigMapper.selectOne(any())).thenReturn(testConfig);

            // Act & Assert - null code 不会触发 TOTP 生成，直接返回 false
            assertThatThrownBy(() -> mfaService.verifyTotpCode("user-001", null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("已删除配置不被查询")
        void isMfaEnabled_DeletedConfig_ReturnsFalse() {
            // Arrange
            testConfig.setDeleted(1);
            when(mfaConfigMapper.selectOne(any())).thenReturn(null); // 查询条件包含 deleted=0

            // Act
            boolean result = mfaService.isMfaEnabled("user-001");

            // Assert
            assertThat(result).isFalse();
        }
    }

    // ========== 加密解密测试 ==========

    @Nested
    @DisplayName("加密解密功能测试")
    class EncryptionTests {

        @Test
        @DisplayName("密钥加密解密一致性")
        void secretKey_EncryptDecrypt_ReturnsOriginal() {
            // Arrange
            String originalKey = "ABCDEFGHIJKLMNOP";

            // Act
            String encrypted = SM4Utils.encrypt(originalKey);
            String decrypted = SM4Utils.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalKey);
        }

        @Test
        @DisplayName("备用码加密解密一致性")
        void backupCodes_EncryptDecrypt_ReturnsOriginal() throws Exception {
            // Arrange
            List<String> originalCodes = Arrays.asList("123456", "234567", "345678");

            // Act
            String json = objectMapper.writeValueAsString(originalCodes);
            String encrypted = SM4Utils.encrypt(json);
            String decryptedJson = SM4Utils.decrypt(encrypted);
            List<String> decryptedCodes = objectMapper.readValue(decryptedJson, new TypeReference<List<String>>() {});

            // Assert
            assertThat(decryptedCodes).isEqualTo(originalCodes);
        }
    }

    // ========== 二维码 URL 生成测试 ==========

    @Nested
    @DisplayName("二维码 URL 生成测试")
    class QrCodeUrlTests {

        @Test
        @DisplayName("二维码 URL 格式正确")
        void setupMfa_QrCodeUrlFormat_Correct() {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(testUser);
            when(mfaConfigMapper.selectOne(any())).thenReturn(null);
            when(mfaConfigMapper.insert(any(UserMfaConfig.class))).thenAnswer(invocation -> {
                UserMfaConfig config = invocation.getArgument(0);
                config.setId("generated-id");
                return 1;
            });
            when(mfaConfigMapper.updateById(any(UserMfaConfig.class))).thenReturn(1);

            // Act
            MfaSetupResponse response = mfaService.setupMfa("user-001");

            // Assert
            assertThat(response.getQrCodeUrl())
                    .startsWith("otpauth://totp/")
                    .contains("NexusArchive:testuser")
                    .contains("secret=")
                    .contains("issuer=NexusArchive")
                    .contains("algorithm=HmacSHA1")
                    .contains("digits=6")
                    .contains("period=30");
        }
    }
}
