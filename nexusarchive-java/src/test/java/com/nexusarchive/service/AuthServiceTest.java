package com.nexusarchive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.entity.Role;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.util.JwtUtil;
import com.nexusarchive.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 
 * 测试覆盖:
 * - 用户登录
 * - Token刷新
 * - 用户登出
 * - Token验证
 * - 当前用户获取
 * 
 * @author Agent E - 质量保障工程师
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private LicenseService licenseService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId("user-001");
        testUser.setUsername("admin");
        testUser.setPasswordHash("hashed_password");
        testUser.setFullName("管理员");
        testUser.setEmail("admin@example.com");
        testUser.setStatus("active");
        testUser.setDepartmentId("dept-001");

        // 创建登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        // 创建测试角色
        testRole = new Role();
        testRole.setId("role-001");
        testRole.setCode("ADMIN");
        testRole.setName("管理员");
        testRole.setPermissions("[\"archive:read\", \"archive:write\", \"user:manage\"]");
    }

    // ========== 登录测试 ==========

    @Nested
    @DisplayName("用户登录")
    class LoginTests {

        @Test
        @DisplayName("正确凭证登录成功")
        void login_ValidCredentials_Success() {
            // Arrange
            when(loginAttemptService.isLocked("admin")).thenReturn(false);
            when(userMapper.findByUsername("admin")).thenReturn(testUser);
            when(passwordUtil.verifyPassword("hashed_password", "admin123")).thenReturn(true);
            when(jwtUtil.generateToken("admin", "user-001")).thenReturn("jwt-token");
            when(roleMapper.findByUserId("user-001")).thenReturn(Collections.singletonList(testRole));
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getUsername()).isEqualTo("admin");
            assertThat(response.getUser().getRoles()).contains("ADMIN");
            assertThat(response.getUser().getPermissions()).contains("archive:read");

            verify(loginAttemptService).recordSuccess("admin");
            verify(userMapper).updateById(any(User.class)); // 更新最后登录时间
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void login_UserNotFound_ThrowsException() {
            // Arrange
            when(loginAttemptService.isLocked("admin")).thenReturn(false);
            when(userMapper.findByUsername("admin")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");

            verify(loginAttemptService).recordFailure("admin");
        }

        @Test
        @DisplayName("密码错误 - 抛出异常")
        void login_WrongPassword_ThrowsException() {
            // Arrange
            when(loginAttemptService.isLocked("admin")).thenReturn(false);
            when(userMapper.findByUsername("admin")).thenReturn(testUser);
            when(passwordUtil.verifyPassword("hashed_password", "admin123")).thenReturn(false);
            when(loginAttemptService.getRemainingAttempts("admin")).thenReturn(3);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");

            verify(loginAttemptService).recordFailure("admin");
        }

        @Test
        @DisplayName("账户锁定 - 抛出异常")
        void login_AccountLocked_ThrowsException() {
            // Arrange
            when(loginAttemptService.isLocked("admin")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("账户已锁定");

            verify(userMapper, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("用户已禁用 - 抛出异常")
        void login_UserDisabled_ThrowsException() {
            // Arrange
            testUser.setStatus("disabled");
            when(loginAttemptService.isLocked("admin")).thenReturn(false);
            when(userMapper.findByUsername("admin")).thenReturn(testUser);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户已被禁用或锁定");
        }

        @Test
        @DisplayName("密码错误次数过多导致锁定 - 抛出异常")
        void login_TooManyFailures_AccountLocked() {
            // Arrange
            when(loginAttemptService.isLocked("admin")).thenReturn(false);
            when(userMapper.findByUsername("admin")).thenReturn(testUser);
            when(passwordUtil.verifyPassword("hashed_password", "admin123")).thenReturn(false);
            when(loginAttemptService.getRemainingAttempts("admin")).thenReturn(0);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("密码错误次数过多，账号已锁定");
        }
    }

    // ========== Token刷新测试 ==========

    @Nested
    @DisplayName("Token刷新")
    class RefreshTokenTests {

        @Test
        @DisplayName("有效Token刷新成功")
        void refreshToken_Valid_Success() {
            // Arrange
            String oldToken = "old-jwt-token";
            when(tokenBlacklistService.isBlacklisted(oldToken)).thenReturn(false);
            when(jwtUtil.validateToken(oldToken)).thenReturn(true);
            when(jwtUtil.extractUsername(oldToken)).thenReturn("admin");
            when(jwtUtil.extractUserId(oldToken)).thenReturn("user-001");
            when(jwtUtil.generateToken("admin", "user-001")).thenReturn("new-jwt-token");
            when(userMapper.countActiveUsers()).thenReturn(1);
            doNothing().when(licenseService).assertValid(anyInt());

            // Act
            String newToken = authService.refreshToken(oldToken);

            // Assert
            assertThat(newToken).isEqualTo("new-jwt-token");
        }

        @Test
        @DisplayName("黑名单Token刷新失败 - 抛出异常")
        void refreshToken_Blacklisted_ThrowsException() {
            // Arrange
            String blacklistedToken = "blacklisted-token";
            when(tokenBlacklistService.isBlacklisted(blacklistedToken)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(blacklistedToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Token 已失效");
        }

        @Test
        @DisplayName("无效Token刷新失败 - 抛出异常")
        void refreshToken_Invalid_ThrowsException() {
            // Arrange
            String invalidToken = "invalid-token";
            when(tokenBlacklistService.isBlacklisted(invalidToken)).thenReturn(false);
            when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Token 无效或已过期");
        }
    }

    // ========== 登出测试 ==========

    @Nested
    @DisplayName("用户登出")
    class LogoutTests {

        @Test
        @DisplayName("正常登出成功")
        void logout_Success() {
            // Arrange
            String token = "valid-token";
            Date expiration = new Date(System.currentTimeMillis() + 3600000);
            when(jwtUtil.extractExpiration(token)).thenReturn(expiration);
            doNothing().when(tokenBlacklistService).blacklist(anyString(), anyLong());

            // Act
            authService.logout(token);

            // Assert
            verify(tokenBlacklistService).blacklist(eq(token), eq(expiration.getTime()));
        }

        @Test
        @DisplayName("损坏Token登出 - 不抛出异常")
        void logout_CorruptedToken_NoException() {
            // Arrange
            String corruptedToken = "corrupted-token";
            when(jwtUtil.extractExpiration(corruptedToken)).thenThrow(new RuntimeException("Parse error"));

            // Act - 不应抛出异常
            authService.logout(corruptedToken);

            // Assert
            verify(tokenBlacklistService, never()).blacklist(anyString(), anyLong());
        }
    }

    // ========== Token验证测试 ==========

    @Nested
    @DisplayName("Token验证")
    class ValidateTokenTests {

        @Test
        @DisplayName("有效Token验证通过")
        void validateToken_Valid_ReturnsTrue() {
            // Arrange
            when(jwtUtil.validateToken("valid-token")).thenReturn(true);

            // Act
            boolean result = authService.validateToken("valid-token");

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("无效Token验证失败")
        void validateToken_Invalid_ReturnsFalse() {
            // Arrange
            when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

            // Act
            boolean result = authService.validateToken("invalid-token");

            // Assert
            assertThat(result).isFalse();
        }
    }

    // ========== 获取当前用户测试 ==========

    @Nested
    @DisplayName("获取当前用户")
    class GetCurrentUserTests {

        @Test
        @DisplayName("有效Token获取用户成功")
        void getCurrentUser_ValidToken_ReturnsUser() {
            // Arrange
            when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
            when(jwtUtil.extractUserId("valid-token")).thenReturn("user-001");
            when(userMapper.selectById("user-001")).thenReturn(testUser);
            when(roleMapper.findByUserId("user-001")).thenReturn(Collections.singletonList(testRole));

            // Act
            LoginResponse.UserInfo userInfo = authService.getCurrentUser("valid-token");

            // Assert
            assertThat(userInfo).isNotNull();
            assertThat(userInfo.getUsername()).isEqualTo("admin");
            assertThat(userInfo.getFullName()).isEqualTo("管理员");
            assertThat(userInfo.getRoles()).contains("ADMIN");
        }

        @Test
        @DisplayName("黑名单Token - 抛出异常")
        void getCurrentUser_BlacklistedToken_ThrowsException() {
            // Arrange
            when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.getCurrentUser("blacklisted-token"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Token 已失效");
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void getCurrentUser_UserNotFound_ThrowsException() {
            // Arrange
            when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
            when(jwtUtil.extractUserId("valid-token")).thenReturn("deleted-user");
            when(userMapper.selectById("deleted-user")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> authService.getCurrentUser("valid-token"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }
}
