// Input: JUnit 5、Spring Framework、Java 标准库
// Output: RoleValidationServiceTest 测试类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Role;
import com.nexusarchive.mapper.RoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 三员互斥校验服务单元测试
 *
 * 验证 RoleValidationService 的三员互斥规则：
 * 1. 同一用户不能同时拥有多个三员角色
 * 2. 大小写不敏感比较（SYSTEM_ADMIN = system_admin）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("三员互斥校验服务测试")
@Tag("unit")
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleValidationServiceTest {

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleValidationService roleValidationService;

    private Role systemAdminRole;
    private Role securityAdminRole;
    private Role auditAdminRole;
    private Role businessUserRole;

    @BeforeEach
    void setUp() {
        // 模拟数据库中的角色（使用大写的 role_category，与实际数据库一致）
        systemAdminRole = createRole("role_system_admin", "system_admin", "SYSTEM_ADMIN", true);
        securityAdminRole = createRole("role_security_admin", "security_admin", "SECURITY_ADMIN", true);
        auditAdminRole = createRole("role_audit_admin", "audit_admin", "AUDIT_ADMIN", true);
        businessUserRole = createRole("role_business_user", "business_user", "BUSINESS_USER", false);
    }

    private Role createRole(String id, String code, String category, Boolean exclusive) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setRoleCategory(category);
        role.setIsExclusive(exclusive);
        return role;
    }

    @Nested
    @DisplayName("规则1：待分配角色中不能包含多个三员角色")
    class TestNewUserValidation {

        @Test
        @DisplayName("分配单个系统管理员 - 应该成功")
        void validateSingleSystemAdmin_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(systemAdminRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_system_admin"))
            );
        }

        @Test
        @DisplayName("分配单个安全保密员 - 应该成功")
        void validateSingleSecurityAdmin_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(securityAdminRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_security_admin"))
            );
        }

        @Test
        @DisplayName("分配单个安全审计员 - 应该成功")
        void validateSingleAuditAdmin_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(auditAdminRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_audit_admin"))
            );
        }

        @Test
        @DisplayName("分配系统管理员+安全保密员 - 应该抛出异常")
        void validateSystemAndSecurityAdmin_shouldThrow() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(systemAdminRole, securityAdminRole));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion(null,
                    Arrays.asList("role_system_admin", "role_security_admin"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
        }

        @Test
        @DisplayName("分配系统管理员+安全审计员 - 应该抛出异常")
        void validateSystemAndAuditAdmin_shouldThrow() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(systemAdminRole, auditAdminRole));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion(null,
                    Arrays.asList("role_system_admin", "role_audit_admin"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
        }

        @Test
        @DisplayName("分配安全保密员+安全审计员 - 应该抛出异常")
        void validateSecurityAndAuditAdmin_shouldThrow() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(securityAdminRole, auditAdminRole));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion(null,
                    Arrays.asList("role_security_admin", "role_audit_admin"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
        }

        @Test
        @DisplayName("分配所有三员角色 - 应该抛出异常")
        void validateAllThreeAdminRoles_shouldThrow() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(
                Arrays.asList(systemAdminRole, securityAdminRole, auditAdminRole));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion(null,
                    Arrays.asList("role_system_admin", "role_security_admin", "role_audit_admin"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
        }

        @Test
        @DisplayName("分配三员角色+业务角色 - 应该抛出异常")
        void validateThreeAdminWithBusiness_shouldThrow() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(
                Arrays.asList(systemAdminRole, securityAdminRole, businessUserRole));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion(null,
                    Arrays.asList("role_system_admin", "role_security_admin", "role_business_user"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
        }

        @Test
        @DisplayName("分配单个业务角色 - 应该成功")
        void validateSingleBusinessRole_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(businessUserRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_business_user"))
            );
        }

        @Test
        @DisplayName("分配业务角色+系统管理员 - 应该成功")
        void validateBusinessWithSystemAdmin_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(businessUserRole, systemAdminRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null,
                    Arrays.asList("role_business_user", "role_system_admin"))
            );
        }

        @Test
        @DisplayName("分配空角色列表 - 应该成功")
        void validateEmptyRoles_shouldSucceed() {
            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Collections.emptyList())
            );
        }

        @Test
        @DisplayName("分配null角色列表 - 应该成功")
        void validateNullRoles_shouldSucceed() {
            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, null)
            );
        }
    }

    @Nested
    @DisplayName("规则2：更新用户时检查已有角色冲突")
    class TestUpdateUserValidation {

        @Test
        @DisplayName("用户已有系统管理员，分配安全保密员 - 应该抛出异常")
        void updateUserWithSystemAdminToSecurity_shouldThrow() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(securityAdminRole));
            when(roleMapper.findByUserId("user123")).thenReturn(Arrays.asList(systemAdminRole));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion("user123", Arrays.asList("role_security_admin"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
            assertTrue(ex.getMessage().contains("系统管理员"));
            assertTrue(ex.getMessage().contains("安全保密员"));
        }

        @Test
        @DisplayName("用户已有系统管理员，更新为相同角色 - 应该成功")
        void updateUserWithSameRole_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(systemAdminRole));
            when(roleMapper.findByUserId("user123")).thenReturn(Arrays.asList(systemAdminRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion("user123", Arrays.asList("role_system_admin"))
            );
        }

        @Test
        @DisplayName("用户已有系统管理员，分配业务角色 - 应该成功")
        void updateUserWithSystemAdminToBusiness_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(businessUserRole));
            when(roleMapper.findByUserId("user123")).thenReturn(Arrays.asList(systemAdminRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion("user123", Arrays.asList("role_business_user"))
            );
        }

        @Test
        @DisplayName("用户已有业务角色，分配系统管理员 - 应该成功")
        void updateUserWithBusinessToSystemAdmin_shouldSucceed() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(systemAdminRole));
            when(roleMapper.findByUserId("user123")).thenReturn(Arrays.asList(businessUserRole));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion("user123", Arrays.asList("role_system_admin"))
            );
        }

        @Test
        @DisplayName("用户已有安全审计员，分配系统管理员+安全保密员 - 应该抛出异常")
        void updateUserWithAuditToMultiple_shouldThrow() {
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(systemAdminRole, securityAdminRole));
            when(roleMapper.findByUserId("user123")).thenReturn(Arrays.asList(auditAdminRole));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion("user123",
                    Arrays.asList("role_system_admin", "role_security_admin"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
        }
    }

    @Nested
    @DisplayName("大小写不敏感测试")
    class TestCaseInsensitivity {

        @Test
        @DisplayName("大写 SYSTEM_ADMIN 应被正确识别")
        void validateUppercaseCategory_shouldRecognize() {
            // 使用大写 role_category（与数据库实际存储一致）
            Role roleWithUppercase = createRole("role_system_admin", "system_admin", "SYSTEM_ADMIN", true);
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(roleWithUppercase));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_system_admin"))
            );
        }

        @Test
        @DisplayName("小写 system_admin 应被正确识别")
        void validateLowercaseCategory_shouldRecognize() {
            // 使用小写 role_category
            Role roleWithLowercase = createRole("role_system_admin", "system_admin", "system_admin", true);
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(roleWithLowercase));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_system_admin"))
            );
        }

        @Test
        @DisplayName("混合大小写 SysTem_AdMin 应被正确识别")
        void validateMixedCaseCategory_shouldRecognize() {
            // 使用混合大小写 role_category
            Role roleWithMixedCase = createRole("role_system_admin", "system_admin", "SysTem_AdMin", true);
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(roleWithMixedCase));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_system_admin"))
            );
        }

        @Test
        @DisplayName("大写 SYSTEM_ADMIN + 大写 SECURITY_ADMIN - 应该抛出异常")
        void validateUppercaseMixedRoles_shouldThrow() {
            Role sysAdmin = createRole("role_system_admin", "system_admin", "SYSTEM_ADMIN", true);
            Role secAdmin = createRole("role_security_admin", "security_admin", "SECURITY_ADMIN", true);
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(sysAdmin, secAdmin));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                roleValidationService.validateThreeRoleExclusion(null,
                    Arrays.asList("role_system_admin", "role_security_admin"))
            );

            assertTrue(ex.getMessage().contains("违反三员互斥原则"));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class TestEdgeCases {

        @Test
        @DisplayName("is_exclusive 为 null 的角色不应触发互斥检查")
        void validateNullExclusiveFlag_shouldSkip() {
            Role roleWithNullExclusive = createRole("role_custom", "custom", "system_admin", null);
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(roleWithNullExclusive));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_custom"))
            );
        }

        @Test
        @DisplayName("is_exclusive 为 false 的三员角色不应触发互斥检查")
        void validateFalseExclusiveFlag_shouldSkip() {
            Role nonExclusiveSysAdmin = createRole("role_system_admin", "system_admin", "SYSTEM_ADMIN", false);
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(nonExclusiveSysAdmin));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_system_admin"))
            );
        }

        @Test
        @DisplayName("role_category 为 null 的角色不应触发互斥检查")
        void validateNullCategory_shouldSkip() {
            Role roleWithNullCategory = createRole("role_custom", "custom", null, true);
            when(roleMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(roleWithNullCategory));

            assertDoesNotThrow(() ->
                roleValidationService.validateThreeRoleExclusion(null, Arrays.asList("role_custom"))
            );
        }
    }
}
