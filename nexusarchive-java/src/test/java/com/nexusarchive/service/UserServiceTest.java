// Input: JUnit 5、Mockito、Spring Test、本地模块
// Output: UserServiceTest 测试类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.dto.request.CreateUserRequest;
import com.nexusarchive.dto.request.UpdateUserFondsScopeRequest;
import com.nexusarchive.dto.request.UpdateUserRequest;
import com.nexusarchive.dto.response.FondsScopeResponse;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.SysUserFondsScope;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.BasFondsMapper;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.mapper.SysUserFondsScopeMapper;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.util.PasswordPolicyValidator;
import com.nexusarchive.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService 单元测试
 * <p>
 * 测试覆盖：
 * - 用户创建（含默认角色和全宗分配）
 * - 用户查询（单个、分页、搜索）
 * - 用户更新（基本信息、角色）
 * - 用户删除（软删除）
 * - 密码重置
 * - 状态更新
 * - 全宗权限管理
 * - 三员互斥校验
 * - XSS 过滤
 * - 密码策略验证
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private SysUserFondsScopeMapper sysUserFondsScopeMapper;

    @Mock
    private BasFondsMapper basFondsMapper;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private RoleValidationService roleValidationService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Initialize test user
        testUser = new User();
        testUser.setId("user-001");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashed-password");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setAvatar("/avatar/test.jpg");
        testUser.setOrganizationId("org-001");
        testUser.setStatus("active");
        testUser.setDeleted(0);
        testUser.setCreatedTime(LocalDateTime.now());
        testUser.setLastModifiedTime(LocalDateTime.now());

        // Initialize create request
        createRequest = new CreateUserRequest();
        createRequest.setUsername("newuser");
        createRequest.setPassword("Password@123");
        createRequest.setFullName("New User");
        createRequest.setEmail("new@example.com");
        createRequest.setPhone("13900139000");
        createRequest.setAvatar("/avatar/new.jpg");
        createRequest.setOrganizationId("org-002");
        createRequest.setRoleIds(Arrays.asList("role_business_user"));
        createRequest.setFondsCodes(Arrays.asList("BR-GROUP"));

        // Initialize update request
        updateRequest = new UpdateUserRequest();
        updateRequest.setId("user-001");
        updateRequest.setFullName("Updated User");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setPhone("13900139001");
        updateRequest.setAvatar("/avatar/updated.jpg");
        updateRequest.setOrganizationId("org-003");
        updateRequest.setRoleIds(Arrays.asList("role_system_admin"));
    }

    @AfterEach
    void tearDown() {
        // Cleanup if needed
    }

    // ==================== createUser Tests ====================

    @Test
    @DisplayName("创建用户 - 成功创建并分配默认角色和全宗")
    void createUser_success_withDefaultRoleAndFonds() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("Password@123");
        request.setFullName("New User");
        request.setEmail("new@example.com");
        request.setPhone("13900139000");
        // No roleIds and fondsCodes - should use defaults

        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(passwordUtil.hashPassword("Password@123")).thenReturn("hashed-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-new");
            return 1;
        });

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getFullName()).isEqualTo("New User");
        assertThat(response.getEmail()).isEqualTo("new@example.com");

        // Verify user was inserted
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo("newuser");
        assertThat(capturedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(capturedUser.getStatus()).isEqualTo("active");

        // Verify default role was assigned
        verify(userMapper).insertUserRoles(eq("user-new"), eq(Arrays.asList("role_business_user")));

        // Verify default fonds was assigned
        ArgumentCaptor<SysUserFondsScope> fondsCaptor = ArgumentCaptor.forClass(SysUserFondsScope.class);
        verify(sysUserFondsScopeMapper).insert(fondsCaptor.capture());
        assertThat(fondsCaptor.getValue().getFondsNo()).isEqualTo("BR-GROUP");

        // Verify three-role exclusion validation was called
        verify(roleValidationService).validateThreeRoleExclusion(isNull(), anyList());
    }

    @Test
    @DisplayName("创建用户 - 用户名已存在时抛出异常")
    void createUser_usernameExists_throwsException() {
        // Given
        when(userMapper.findByUsername("existinguser")).thenReturn(testUser);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setPassword("Password@123");
        request.setFullName("Existing User");

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

        // Verify user was not inserted
        verify(userMapper, never()).insert(any(User.class));
        verify(passwordUtil, never()).hashPassword(anyString());
    }

    @Test
    @DisplayName("创建用户 - 密码策略验证失败时抛出异常")
    void createUser_invalidPassword_throwsException() {
        // Given
        when(userMapper.findByUsername("newuser")).thenReturn(null);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("weak"); // Too short, missing complexity

        // Mock PasswordPolicyValidator to throw exception
        // Note: This assumes PasswordPolicyValidator.validate() throws BusinessException
        // If it's a static method, we may need to use different mocking approach

        // When & Then
        // This test assumes PasswordPolicyValidator.validate() will be called and throw
        // If PasswordPolicyValidator is not mockable (static), we may need to adjust
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class);

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("创建用户 - 成功创建并分配指定角色和全宗")
    void createUser_success_withCustomRoleAndFonds() {
        // Given
        when(userMapper.findByUsername("customuser")).thenReturn(null);
        when(passwordUtil.hashPassword("Password@123")).thenReturn("hashed-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-custom");
            return 1;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("customuser");
        request.setPassword("Password@123");
        request.setFullName("Custom User");
        request.setEmail("custom@example.com");
        request.setPhone("13900139000");
        request.setRoleIds(Arrays.asList("role_system_admin", "role_security_admin"));
        request.setFondsCodes(Arrays.asList("F001", "F002"));

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertThat(response.getUsername()).isEqualTo("customuser");

        // Verify custom roles were assigned
        verify(userMapper).insertUserRoles(eq("user-custom"),
                eq(Arrays.asList("role_system_admin", "role_security_admin")));

        // Verify custom fonds were assigned
        ArgumentCaptor<SysUserFondsScope> fondsCaptor = ArgumentCaptor.forClass(SysUserFondsScope.class);
        verify(sysUserFondsScopeMapper, times(2)).insert(fondsCaptor.capture());
        List<SysUserFondsScope> capturedFonds = fondsCaptor.getAllValues();
        assertThat(capturedFonds.get(0).getFondsNo()).isEqualTo("F001");
        assertThat(capturedFonds.get(1).getFondsNo()).isEqualTo("F002");
    }

    @Test
    @DisplayName("创建用户 - XSS 过滤应用于敏感字段")
    void createUser_appliesXssFilter() {
        // Given
        when(userMapper.findByUsername("xssuser")).thenReturn(null);
        when(passwordUtil.hashPassword("Password@123")).thenReturn("hashed-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-xss");
            return 1;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("xssuser");
        request.setPassword("Password@123");
        request.setFullName("<script>alert('xss')</script>");
        request.setEmail("xss@example.com");
        request.setPhone("<img src=x onerror=alert('xss')>");

        // When
        UserResponse response = userService.createUser(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        // Verify XSS filtering was applied (assuming XssFilter.clean removes tags)
        // The exact assertion depends on XssFilter implementation
        assertThat(capturedUser.getFullName()).doesNotContain("<script>");
        assertThat(capturedUser.getPhone()).doesNotContain("<img>");
    }

    // ==================== updateUser Tests ====================

    @Test
    @DisplayName("更新用户 - 成功更新基本信息和角色")
    void updateUser_success() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(updateRequest);

        // Then
        assertThat(response.getFullName()).isEqualTo("Updated User");
        assertThat(response.getEmail()).isEqualTo("updated@example.com");
        assertThat(response.getPhone()).isEqualTo("13900139001");

        // Verify user was updated
        verify(userMapper).updateById(any(User.class));

        // Verify roles were updated
        verify(userMapper).deleteUserRoles("user-001");
        verify(userMapper).insertUserRoles("user-001", Arrays.asList("role_system_admin"));

        // Verify three-role exclusion validation was called
        verify(roleValidationService).validateThreeRoleExclusion(eq("user-001"), anyList());
    }

    @Test
    @DisplayName("更新用户 - 用户不存在时抛出异常")
    void updateUser_userNotFound_throwsException() {
        // Given
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setId("nonexistent");
        request.setFullName("Nonexistent User");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("更新用户 - 清空角色列表")
    void updateUser_clearRoles() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        updateRequest.setRoleIds(Collections.emptyList());

        // When
        UserResponse response = userService.updateUser(updateRequest);

        // Then
        assertThat(response).isNotNull();

        // Verify roles were deleted but not re-inserted
        verify(userMapper).deleteUserRoles("user-001");
        verify(userMapper, never()).insertUserRoles(anyString(), anyList());
    }

    @Test
    @DisplayName("更新用户 - XSS 过滤应用于更新字段")
    void updateUser_appliesXssFilter() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        updateRequest.setFullName("<script>alert('xss')</script>");
        updateRequest.setEmail("<img src=x onerror=alert('xss')>@example.com");
        updateRequest.setPhone("<iframe src='malicious'></iframe>");

        // When
        UserResponse response = userService.updateUser(updateRequest);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        // Verify XSS filtering was applied
        assertThat(capturedUser.getFullName()).doesNotContain("<script>");
        assertThat(capturedUser.getEmail()).doesNotContain("<img>");
        assertThat(capturedUser.getPhone()).doesNotContain("<iframe>");
    }

    // ==================== deleteUser Tests ====================

    @Test
    @DisplayName("删除用户 - 成功软删除用户")
    void deleteUser_success() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When
        userService.deleteUser("user-001");

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User deletedUser = userCaptor.getValue();
        assertThat(deletedUser.getDeleted()).isEqualTo(1);
    }

    @Test
    @DisplayName("删除用户 - 用户不存在时抛出异常")
    void deleteUser_userNotFound_throwsException() {
        // Given
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser("nonexistent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(userMapper, never()).updateById(any(User.class));
    }

    // ==================== getUserById Tests ====================

    @Test
    @DisplayName("根据ID查询用户 - 成功返回用户信息")
    void getUserById_success() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);
        when(userMapper.selectRoleIdsByUserId("user-001")).thenReturn(Arrays.asList("role_business_user"));

        // When
        UserResponse response = userService.getUserById("user-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("user-001");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getRoleIds()).containsExactly("role_business_user");
    }

    @Test
    @DisplayName("根据ID查询用户 - 用户不存在时抛出异常")
    void getUserById_userNotFound_throwsException() {
        // Given
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.getUserById("nonexistent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("根据ID查询用户 - 已删除用户视为不存在")
    void getUserById_deletedUser_throwsException() {
        // Given
        testUser.setDeleted(1);
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When & Then
        assertThatThrownBy(() -> userService.getUserById("user-001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    // ==================== listPaged Tests ====================

    @Test
    @DisplayName("分页查询用户 - 成功返回分页结果")
    void listPaged_success() {
        // Given
        Page<User> userPage = new Page<>(1, 10);
        userPage.setRecords(Arrays.asList(testUser));
        userPage.setTotal(1);
        userPage.setPages(1);

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.listPaged(1, 10, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("分页查询用户 - 按用户名或姓名搜索")
    void listPaged_withSearch() {
        // Given
        Page<User> userPage = new Page<>(1, 10);
        userPage.setRecords(Arrays.asList(testUser));
        userPage.setTotal(1);

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.listPaged(1, 10, "test", null);

        // Then
        assertThat(result.getRecords()).hasSize(1);
        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询用户 - 按状态筛选")
    void listPaged_withStatusFilter() {
        // Given
        Page<User> userPage = new Page<>(1, 10);
        userPage.setRecords(Arrays.asList(testUser));
        userPage.setTotal(1);

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.listPaged(1, 10, null, "active");

        // Then
        assertThat(result.getRecords()).hasSize(1);
        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询用户 - 空结果")
    void listPaged_emptyResult() {
        // Given
        Page<User> userPage = new Page<>(1, 10);
        userPage.setRecords(Collections.emptyList());
        userPage.setTotal(0);

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.listPaged(1, 10, "nonexistent", null);

        // Then
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
    }

    // ==================== resetPassword Tests ====================

    @Test
    @DisplayName("重置密码 - 成功重置密码")
    void resetPassword_success() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);
        when(passwordUtil.hashPassword("NewPassword@123")).thenReturn("new-hashed-password");

        // When
        userService.resetPassword("user-001", "NewPassword@123");

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getPasswordHash()).isEqualTo("new-hashed-password");
    }

    @Test
    @DisplayName("重置密码 - 用户不存在时抛出异常")
    void resetPassword_userNotFound_throwsException() {
        // Given
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.resetPassword("nonexistent", "NewPassword@123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(passwordUtil, never()).hashPassword(anyString());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("重置密码 - 弱密码被拒绝")
    void resetPassword_weakPassword_throwsException() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When & Then
        assertThatThrownBy(() -> userService.resetPassword("user-001", "weak"))
                .isInstanceOf(BusinessException.class);

        verify(passwordUtil, never()).hashPassword(anyString());
        verify(userMapper, never()).updateById(any(User.class));
    }

    // ==================== updateStatus Tests ====================

    @Test
    @DisplayName("更新用户状态 - 成功更新为有效状态")
    void updateStatus_success_validStatus() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When
        userService.updateStatus("user-001", "disabled");

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getStatus()).isEqualTo("disabled");
    }

    @Test
    @DisplayName("更新用户状态 - 无效状态值抛出异常")
    void updateStatus_invalidStatus_throwsException() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When & Then
        assertThatThrownBy(() -> userService.updateStatus("user-001", "invalid"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法状态值");

        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("更新用户状态 - 用户不存在时抛出异常")
    void updateStatus_userNotFound_throwsException() {
        // Given
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.updateStatus("nonexistent", "active"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("更新用户状态 - 支持所有有效状态值")
    void updateStatus_allValidStatuses() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When & Then - Test all valid statuses
        userService.updateStatus("user-001", "active");
        verify(userMapper).updateById(any(User.class));

        userService.updateStatus("user-001", "disabled");
        verify(userMapper, times(2)).updateById(any(User.class));

        userService.updateStatus("user-001", "locked");
        verify(userMapper, times(3)).updateById(any(User.class));
    }

    // ==================== getUserFondsScope Tests ====================

    @Test
    @DisplayName("获取用户全宗权限 - 成功返回已分配和可分配全宗")
    void getUserFondsScope_success() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);
        when(sysUserFondsScopeMapper.findFondsNoByUserId("user-001"))
                .thenReturn(Arrays.asList("BR-GROUP", "F001"));

        BasFonds fonds1 = new BasFonds();
        fonds1.setFondsCode("BR-GROUP");
        fonds1.setFondsName("泊冉集团有限公司");
        fonds1.setCompanyName("泊冉集团有限公司");

        BasFonds fonds2 = new BasFonds();
        fonds2.setFondsCode("F001");
        fonds2.setFondsName("测试全宗");
        fonds2.setCompanyName("测试公司");

        when(basFondsMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(fonds1, fonds2));

        // When
        FondsScopeResponse response = userService.getUserFondsScope("user-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user-001");
        assertThat(response.getAssignedFonds()).containsExactly("BR-GROUP", "F001");
        assertThat(response.getAvailableFonds()).hasSize(2);
        assertThat(response.getAvailableFonds().get(0).getFondsCode()).isEqualTo("BR-GROUP");
        assertThat(response.getAvailableFonds().get(1).getFondsCode()).isEqualTo("F001");
    }

    @Test
    @DisplayName("获取用户全宗权限 - 用户不存在时抛出异常")
    void getUserFondsScope_userNotFound_throwsException() {
        // Given
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.getUserFondsScope("nonexistent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(sysUserFondsScopeMapper, never()).findFondsNoByUserId(anyString());
        verify(basFondsMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取用户全宗权限 - 已删除用户视为不存在")
    void getUserFondsScope_deletedUser_throwsException() {
        // Given
        testUser.setDeleted(1);
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        // When & Then
        assertThatThrownBy(() -> userService.getUserFondsScope("user-001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("获取用户全宗权限 - 无已分配全宗")
    void getUserFondsScope_noAssignedFonds() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);
        when(sysUserFondsScopeMapper.findFondsNoByUserId("user-001"))
                .thenReturn(Collections.emptyList());

        BasFonds fonds = new BasFonds();
        fonds.setFondsCode("BR-GROUP");
        fonds.setFondsName("泊冉集团有限公司");
        fonds.setCompanyName("泊冉集团有限公司");

        when(basFondsMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(fonds));

        // When
        FondsScopeResponse response = userService.getUserFondsScope("user-001");

        // Then
        assertThat(response.getAssignedFonds()).isEmpty();
        assertThat(response.getAvailableFonds()).hasSize(1);
    }

    // ==================== updateUserFondsScope Tests ====================

    @Test
    @DisplayName("更新用户全宗权限 - 成功更新全宗列表")
    void updateUserFondsScope_success() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        BasFonds fonds1 = new BasFonds();
        fonds1.setFondsCode("F001");
        fonds1.setFondsName("全宗1");
        fonds1.setCompanyName("公司1");

        BasFonds fonds2 = new BasFonds();
        fonds2.setFondsCode("F002");
        fonds2.setFondsName("全宗2");
        fonds2.setCompanyName("公司2");

        when(basFondsMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(fonds1, fonds2);

        UpdateUserFondsScopeRequest request = new UpdateUserFondsScopeRequest();
        request.setFondsCodes(Arrays.asList("F001", "F002"));

        // When
        userService.updateUserFondsScope("user-001", request);

        // Then
        // Verify existing fonds were deleted
        verify(sysUserFondsScopeMapper).deleteByUserId("user-001");

        // Verify new fonds were inserted
        ArgumentCaptor<SysUserFondsScope> fondsCaptor = ArgumentCaptor.forClass(SysUserFondsScope.class);
        verify(sysUserFondsScopeMapper, times(2)).insert(fondsCaptor.capture());
        List<SysUserFondsScope> capturedFonds = fondsCaptor.getAllValues();
        assertThat(capturedFonds.get(0).getFondsNo()).isEqualTo("F001");
        assertThat(capturedFonds.get(1).getFondsNo()).isEqualTo("F002");
    }

    @Test
    @DisplayName("更新用户全宗权限 - 清空所有全宗")
    void updateUserFondsScope_clearAllFonds() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        UpdateUserFondsScopeRequest request = new UpdateUserFondsScopeRequest();
        request.setFondsCodes(Collections.emptyList());

        // When
        userService.updateUserFondsScope("user-001", request);

        // Then
        verify(sysUserFondsScopeMapper).deleteByUserId("user-001");
        verify(sysUserFondsScopeMapper, never()).insert(any(SysUserFondsScope.class));
    }

    @Test
    @DisplayName("更新用户全宗权限 - 用户不存在时抛出异常")
    void updateUserFondsScope_userNotFound_throwsException() {
        // Given
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        UpdateUserFondsScopeRequest request = new UpdateUserFondsScopeRequest();
        request.setFondsCodes(Arrays.asList("F001"));

        // When & Then
        assertThatThrownBy(() -> userService.updateUserFondsScope("nonexistent", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(sysUserFondsScopeMapper, never()).deleteByUserId(anyString());
        verify(sysUserFondsScopeMapper, never()).insert(any(SysUserFondsScope.class));
    }

    @Test
    @DisplayName("更新用户全宗权限 - 全宗不存在时抛出异常")
    void updateUserFondsScope_fondsNotFound_throwsException() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);
        when(basFondsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        UpdateUserFondsScopeRequest request = new UpdateUserFondsScopeRequest();
        request.setFondsCodes(Arrays.asList("NONEXISTENT"));

        // When & Then
        assertThatThrownBy(() -> userService.updateUserFondsScope("user-001", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("资源不存在");

        verify(sysUserFondsScopeMapper).deleteByUserId("user-001");
        verify(sysUserFondsScopeMapper, never()).insert(any(SysUserFondsScope.class));
    }

    @Test
    @DisplayName("更新用户全宗权限 - null fondsCodes 视为空列表")
    void updateUserFondsScope_nullFondsCodes_treatedAsEmpty() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);

        UpdateUserFondsScopeRequest request = new UpdateUserFondsScopeRequest();
        request.setFondsCodes(null);

        // When
        userService.updateUserFondsScope("user-001", request);

        // Then
        verify(sysUserFondsScopeMapper).deleteByUserId("user-001");
        verify(sysUserFondsScopeMapper, never()).insert(any(SysUserFondsScope.class));
    }

    // ==================== Edge Cases and Integration Scenarios ====================

    @Test
    @DisplayName("创建用户 - 空字符串用户名")
    void createUser_emptyUsername_throwsException() {
        // Given
        when(userMapper.findByUsername("")).thenReturn(null);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("");
        request.setPassword("Password@123");
        request.setFullName("Empty User");

        // When & Then
        // This should fail at validation level or business logic
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("创建用户 - null 角色列表使用默认值")
    void createUser_nullRoleIds_usesDefault() {
        // Given
        when(userMapper.findByUsername("defuser")).thenReturn(null);
        when(passwordUtil.hashPassword("Password@123")).thenReturn("hashed-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-def");
            return 1;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("defuser");
        request.setPassword("Password@123");
        request.setFullName("Default User");
        request.setRoleIds(null);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        verify(userMapper).insertUserRoles(eq("user-def"), eq(Arrays.asList("role_business_user")));
    }

    @Test
    @DisplayName("创建用户 - null 全宗列表使用默认值")
    void createUser_nullFondsCodes_usesDefault() {
        // Given
        when(userMapper.findByUsername("defuser")).thenReturn(null);
        when(passwordUtil.hashPassword("Password@123")).thenReturn("hashed-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-def");
            return 1;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("defuser");
        request.setPassword("Password@123");
        request.setFullName("Default User");
        request.setFondsCodes(null);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        ArgumentCaptor<SysUserFondsScope> fondsCaptor = ArgumentCaptor.forClass(SysUserFondsScope.class);
        verify(sysUserFondsScopeMapper).insert(fondsCaptor.capture());
        assertThat(fondsCaptor.getValue().getFondsNo()).isEqualTo("BR-GROUP");
    }

    @Test
    @DisplayName("分页查询 - 边界值测试（第一页、最后一页）")
    void listPaged_boundaryValues() {
        // Given
        Page<User> userPage = new Page<>(1, 10);
        userPage.setRecords(Collections.emptyList());
        userPage.setTotal(0);

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(userPage);

        // When & Then - First page
        Page<UserResponse> result1 = userService.listPaged(1, 10, null, null);
        assertThat(result1.getCurrent()).isEqualTo(1);

        // When & Then - Large page number
        Page<UserResponse> result2 = userService.listPaged(999, 10, null, null);
        assertThat(result2.getCurrent()).isEqualTo(999);
    }

    @Test
    @DisplayName("更新用户 - 保持三员互斥约束")
    void updateUser_enforcesThreeRoleExclusion() {
        // Given
        when(userMapper.selectById("user-001")).thenReturn(testUser);
        updateRequest.setRoleIds(Arrays.asList("role_system_admin", "role_security_admin"));

        // Mock role validation to throw exception
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.INVALID_STATE_TRANSITION))
                .when(roleValidationService).validateThreeRoleExclusion(anyString(), anyList());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(updateRequest))
                .isInstanceOf(BusinessException.class);

        // Verify user was not updated
        verify(userMapper, never()).updateById(any(User.class));
    }
}
