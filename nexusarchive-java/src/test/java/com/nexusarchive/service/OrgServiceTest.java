// Input: MyBatis-Plus、org.junit、org.mockito、Spring Security、等
// Output: OrgServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Org;
import com.nexusarchive.mapper.OrgMapper;
import com.nexusarchive.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    @Mock
    private OrgMapper orgMapper;

    @InjectMocks
    private OrgService orgService;

    private Org rootOrg;
    private Org childOrg;

    @BeforeEach
    void setUp() {
        rootOrg = new Org();
        rootOrg.setId("org-001");
        rootOrg.setName("Root Company");
        rootOrg.setType("COMPANY");
        rootOrg.setOrderNum(1);
        rootOrg.setDeleted(0);

        childOrg = new Org();
        childOrg.setId("org-002");
        childOrg.setName("Child Dept");
        childOrg.setParentId("org-001");
        childOrg.setType("DEPARTMENT");
        childOrg.setOrderNum(1);
        childOrg.setDeleted(0);
    }

    @Test
    @DisplayName("Get Tree - Should build hierarchy correctly")
    void getTree_Success() {
        // Arrange
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(rootOrg, childOrg));

        // Act
        List<OrgService.OrgTreeNode> tree = orgService.getTree();

        // Assert
        assertEquals(1, tree.size());
        assertEquals("org-001", tree.get(0).getId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("org-002", tree.get(0).getChildren().get(0).getId());
    }

    @Test
    @DisplayName("Delete - Should succeed when user is in same org")
    void delete_Success_SameOrg() {
        // Arrange
        when(orgMapper.selectById("org-001")).thenReturn(rootOrg);
        when(orgMapper.updateById(any(Org.class))).thenReturn(1);

        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(user.getDepartmentId()).thenReturn("org-001"); // User is in the same org

        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Act
            orgService.delete("org-001");

            // Assert
            verify(orgMapper).updateById(any(Org.class));
            assertEquals(1, rootOrg.getDeleted());
        }
    }

    @Test
    @DisplayName("Delete - Should fail when user is in different org")
    void delete_Fail_DiffOrg() {
        // Arrange
        when(orgMapper.selectById("org-001")).thenReturn(rootOrg);

        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(user.getDepartmentId()).thenReturn("org-999"); // User is in different org
        // Ensure no system_admin role
        doReturn(Collections.emptyList()).when(auth).getAuthorities();

        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Act & Assert
            assertThrows(BusinessException.class, () -> orgService.delete("org-001"));
            verify(orgMapper, never()).updateById(any(Org.class));
        }
    }

    @Test
    @DisplayName("Delete - Should succeed when user is system_admin")
    void delete_Success_SystemAdmin() {
        // Arrange
        when(orgMapper.selectById("org-001")).thenReturn(rootOrg);
        when(orgMapper.updateById(any(Org.class))).thenReturn(1);

        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        
        // Add system_admin role
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_system_admin"));
        doReturn(authorities).when(auth).getAuthorities();

        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Act
            orgService.delete("org-001");

            // Assert
            verify(orgMapper).updateById(any(Org.class));
            assertEquals(1, rootOrg.getDeleted());
        }
    }
}
