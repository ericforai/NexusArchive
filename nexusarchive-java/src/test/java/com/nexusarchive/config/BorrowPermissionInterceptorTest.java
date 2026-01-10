// Input: Spring Web、Security、BorrowingFacade、org.junit
// Output: BorrowPermissionInterceptorTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.nexusarchive.modules.borrowing.app.BorrowingFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowPermissionInterceptorTest {

    @Mock
    private BorrowingFacade borrowingFacade;

    @InjectMocks
    private BorrowPermissionInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setupQueryUser(String userId) {
        when(authentication.getPrincipal()).thenReturn(userId);
        when(authentication.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("query_user")));
    }

    private void setupAdminUser() {
        when(authentication.getPrincipal()).thenReturn("admin-001");
        when(authentication.getAuthorities()).thenAnswer(invocation ->
                Arrays.asList(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
    }

    @Nested
    @DisplayName("角色检查")
    class RoleCheckTests {

        @Test
        @DisplayName("非 query_user 角色直接放行")
        void nonQueryUser_ShouldPass() throws Exception {
            setupAdminUser();
            request.setRequestURI("/api/archives/arc-001");

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            verify(borrowingFacade, never()).checkAccess(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("query_user 角色需要检查权限")
        void queryUser_ShouldCheckPermission() throws Exception {
            setupQueryUser("user-001");
            request.setRequestURI("/api/archives/arc-001");
            when(borrowingFacade.checkAccess("user-001", "arc-001", "VIEW")).thenReturn(true);

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            verify(borrowingFacade).checkAccess("user-001", "arc-001", "VIEW");
        }
    }

    @Nested
    @DisplayName("权限检查")
    class PermissionCheckTests {

        @Test
        @DisplayName("有借阅权限 - 放行")
        void hasBorrowingPermission_ShouldPass() throws Exception {
            setupQueryUser("user-001");
            request.setRequestURI("/api/archives/arc-001");
            when(borrowingFacade.checkAccess("user-001", "arc-001", "VIEW")).thenReturn(true);

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            assertThat(response.getStatus()).isNotEqualTo(403);
        }

        @Test
        @DisplayName("无借阅权限 - 拒绝")
        void noBorrowingPermission_ShouldReject() throws Exception {
            setupQueryUser("user-001");
            request.setRequestURI("/api/archives/arc-001");
            when(borrowingFacade.checkAccess("user-001", "arc-001", "VIEW")).thenReturn(false);

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentAsString()).contains("无权访问该档案");
        }
    }

    @Nested
    @DisplayName("路径匹配")
    class PathMatchingTests {

        @Test
        @DisplayName("非受保护路径 - 放行")
        void unprotectedPath_ShouldPass() throws Exception {
            setupQueryUser("user-001");
            request.setRequestURI("/api/other/path");

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            verify(borrowingFacade, never()).checkAccess(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("从路径正确提取档案ID")
        void extractArchiveId_FromPath() throws Exception {
            setupQueryUser("user-001");
            request.setRequestURI("/api/archives/test-archive-123");
            when(borrowingFacade.checkAccess("user-001", "test-archive-123", "VIEW")).thenReturn(true);

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            verify(borrowingFacade).checkAccess("user-001", "test-archive-123", "VIEW");
        }
    }
}
