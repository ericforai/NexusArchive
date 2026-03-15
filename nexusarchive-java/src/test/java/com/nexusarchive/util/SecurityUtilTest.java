// Input: JUnit 5、Spring Test、SecurityUtil
// Output: SecurityUtilTest 类
// Pos: 测试模块

package com.nexusarchive.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SecurityUtil 单元测试
 *
 * 测试安全工具类的用户权限检查功能
 */
@Tag("unit")
@DisplayName("安全工具类测试")
class SecurityUtilTest {

    @BeforeEach
    void setUp() {
        // 清空安全上下文
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // 清空安全上下文
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("当用户是系统管理员时应该返回 true")
    void shouldReturnTrueForSystemAdmin() {
        // Given
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_system_admin")
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("当用户不是系统管理员时应该返回 false")
    void shouldReturnFalseForNonSystemAdmin() {
        // Given
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_user")
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("当没有认证信息时应该返回 false")
    void shouldReturnFalseWhenNoAuthentication() {
        // Given - 不设置任何认证信息

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("当认证为 null 时应该返回 false")
    void shouldReturnFalseWhenAuthenticationIsNull() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(null);

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("当用户有多个角色时应该正确判断")
    void shouldHandleMultipleRoles() {
        // Given - 用户有多个角色，包含 system_admin
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_user"),
                new SimpleGrantedAuthority("ROLE_system_admin"),
                new SimpleGrantedAuthority("ROLE_audit_admin")
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("multi-role-user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("当用户有多个角色但不含系统管理员时应该返回 false")
    void shouldReturnFalseForMultipleRolesWithoutSystemAdmin() {
        // Given
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_user"),
                new SimpleGrantedAuthority("ROLE_security_admin"),
                new SimpleGrantedAuthority("ROLE_audit_admin")
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("multi-role-user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("当权限列表为空时应该返回 false")
    void shouldReturnFalseForEmptyAuthorities() {
        // Given
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("应该严格匹配 ROLE_system_admin 角色名称")
    void shouldStrictlyMatchSystemAdminRole() {
        // Given - 相似但不同的角色名称
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority("ROLE_System_Admin"),
                new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                new SimpleGrantedAuthority("system_admin")  // 没有 ROLE_ 前缀
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        boolean result = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("应该处理没有权限的认证对象")
    void shouldHandleAuthenticationWithNullAuthorities() {
        // Given
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user", null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When & Then
        assertThatCode(() -> SecurityUtil.isSystemAdmin())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该正确识别三员分立角色")
    void shouldIdentifyThreeRoleSeparation() {
        // Given - 安全审计员
        List<SimpleGrantedAuthority> auditAuthorities = List.of(
                new SimpleGrantedAuthority("ROLE_audit_admin")
        );
        UsernamePasswordAuthenticationToken auditAuth =
                new UsernamePasswordAuthenticationToken("audit", null, auditAuthorities);
        SecurityContextHolder.getContext().setAuthentication(auditAuth);

        // When
        boolean isSystemAdmin = SecurityUtil.isSystemAdmin();

        // Then - 审计员不是系统管理员
        assertThat(isSystemAdmin).isFalse();
    }

    @Test
    @DisplayName("应该在连续调用时返回一致的结果")
    void shouldReturnConsistentResultsOnMultipleCalls() {
        // Given
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_system_admin")
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When - 多次调用
        boolean result1 = SecurityUtil.isSystemAdmin();
        boolean result2 = SecurityUtil.isSystemAdmin();
        boolean result3 = SecurityUtil.isSystemAdmin();

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        assertThat(result3).isTrue();
    }
}
