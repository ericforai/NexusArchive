package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.config.RestAccessDeniedHandler;
import com.nexusarchive.config.RestAuthenticationEntryPoint;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.service.CustomUserDetailsService;
import com.nexusarchive.service.DestructionService;
import com.nexusarchive.service.LicenseService;
import com.nexusarchive.service.TokenBlacklistService;
import com.nexusarchive.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DestructionController 集成测试
 * 
 * 测试覆盖:
 * - 创建销毁申请
 * - 查询销毁列表
 * - 审批销毁
 * - 执行销毁
 * 
 * @author Agent E - 质量保障工程师
 */
@WebMvcTest(
    value = DestructionController.class,
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = {com.nexusarchive.aspect.ArchivalAuditAspect.class}
    )
)
@Import(com.nexusarchive.config.SecurityConfig.class)
class DestructionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DestructionService destructionService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private LicenseService licenseService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @MockBean
    private com.nexusarchive.config.ResilientFlywayRunner resilientFlywayRunner;

    @MockBean
    private com.nexusarchive.config.MigrationGatekeeperInterceptor migrationGatekeeperInterceptor;

    @MockBean
    private com.nexusarchive.config.WebMvcConfig webMvcConfig;

    private String token = "mock-token";
    private Destruction testDestruction;

    @BeforeEach
    void setUp() throws Exception {
        // Mock ResilientFlywayRunner to indicate system is ready
        when(resilientFlywayRunner.isReady()).thenReturn(true);

        // Mock JWT
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.extractUsername(anyString())).thenReturn("admin");
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        // Mock Claims
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("admin");
        when(claims.get("userId", String.class)).thenReturn("user-001");
        when(jwtUtil.extractAllClaims(anyString())).thenReturn(claims);
        when(jwtUtil.validateToken(anyString(), anyString())).thenReturn(true);

        // Mock UserDetailsService
        User user = new User(
                "admin",
                "password",
                Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                        new SimpleGrantedAuthority("destruction:create"),
                        new SimpleGrantedAuthority("destruction:approve"),
                        new SimpleGrantedAuthority("destruction:execute"),
                        new SimpleGrantedAuthority("nav:all")
                )
        );
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        when(userDetailsService.loadUserById("user-001")).thenReturn(user);

        // 创建测试销毁数据
        testDestruction = new Destruction();
        testDestruction.setId("des-001");
        testDestruction.setApplicantId("user-001");
        testDestruction.setApplicantName("张三");
        testDestruction.setReason("保管期限已满");
        testDestruction.setArchiveCount(2);
        testDestruction.setArchiveIds("[\"arc-001\", \"arc-002\"]");
        testDestruction.setStatus("PENDING");
        testDestruction.setCreatedTime(LocalDateTime.now());
    }

    // ========== 创建销毁申请测试 ==========

    @Nested
    @DisplayName("创建销毁申请")
    class CreateDestructionTests {

        @Test
        @DisplayName("成功创建销毁申请")
        void createDestruction_Success() throws Exception {
            // Arrange
            Destruction request = new Destruction();
            request.setReason("保管期限已满");
            request.setArchiveIds("[\"arc-001\", \"arc-002\"]");
            request.setArchiveCount(2);

            when(destructionService.createDestruction(any(Destruction.class)))
                    .thenReturn(testDestruction);

            // Act & Assert
            mockMvc.perform(post("/destruction")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value("des-001"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }
    }

    // ========== 查询销毁列表测试 ==========

    @Nested
    @DisplayName("查询销毁列表")
    class GetDestructionsTests {

        @Test
        @DisplayName("成功查询销毁列表")
        void getDestructions_Success() throws Exception {
            // Arrange
            Page<Destruction> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(testDestruction));
            page.setTotal(1);

            when(destructionService.getDestructions(eq(1), eq(10), isNull()))
                    .thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/destruction")
                            .header("Authorization", "Bearer " + token)
                            .param("page", "1")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records[0].id").value("des-001"));
        }

        @Test
        @DisplayName("按状态筛选销毁列表")
        void getDestructions_WithStatus_Success() throws Exception {
            // Arrange
            Page<Destruction> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(testDestruction));
            page.setTotal(1);

            when(destructionService.getDestructions(eq(1), eq(10), eq("PENDING")))
                    .thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/destruction")
                            .header("Authorization", "Bearer " + token)
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records[0].status").value("PENDING"));
        }
    }

    // ========== 审批销毁测试 ==========

    @Nested
    @DisplayName("审批销毁")
    class ApproveDestructionTests {

        @Test
        @DisplayName("审批通过成功")
        void approveDestruction_Success() throws Exception {
            // Arrange
            doNothing().when(destructionService).approveDestruction(eq("des-001"), anyString(), anyString());

            // Act & Assert
            mockMvc.perform(post("/destruction/des-001/approve")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"同意销毁\""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(destructionService).approveDestruction(eq("des-001"), anyString(), anyString());
        }
    }

    // ========== 执行销毁测试 ==========

    @Nested
    @DisplayName("执行销毁")
    class ExecuteDestructionTests {

        @Test
        @DisplayName("执行成功")
        void executeDestruction_Success() throws Exception {
            // Arrange
            doNothing().when(destructionService).executeDestruction("des-001");

            // Act & Assert
            mockMvc.perform(post("/destruction/des-001/execute")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(destructionService).executeDestruction("des-001");
        }
    }
}
