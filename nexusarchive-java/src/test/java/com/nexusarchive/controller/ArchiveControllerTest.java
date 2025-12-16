package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.config.RestAccessDeniedHandler;
import com.nexusarchive.config.RestAuthenticationEntryPoint;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.CustomUserDetailsService;
import com.nexusarchive.service.LicenseService;
import com.nexusarchive.service.TokenBlacklistService;
import com.nexusarchive.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(
    value = ArchiveController.class,
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = {com.nexusarchive.aspect.ArchivalAuditAspect.class}
    )
)
@org.springframework.context.annotation.Import(com.nexusarchive.config.SecurityConfig.class)
public class ArchiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private ArchiveService archiveService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private AuditLogService auditLogService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtUtil jwtUtil;

    // Mock for SecurityUserDetailsService which might be needed by SecurityConfig
    @org.springframework.boot.test.mock.mockito.MockBean
    private CustomUserDetailsService userDetailsService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private TokenBlacklistService tokenBlacklistService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private LicenseService licenseService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private UserMapper userMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.nexusarchive.config.ResilientFlywayRunner resilientFlywayRunner;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.nexusarchive.config.MigrationGatekeeperInterceptor migrationGatekeeperInterceptor;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.nexusarchive.config.WebMvcConfig webMvcConfig;

    private String token;

    @BeforeEach
    public void setup() {
        // Mock ResilientFlywayRunner to indicate system is ready
        org.mockito.Mockito.when(resilientFlywayRunner.isReady()).thenReturn(true);

        // 生成测试Token
        org.mockito.Mockito
                .when(jwtUtil.generateToken(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("mock-token");
        token = "mock-token";

        // Mock JWT parsing if filter uses it
        org.mockito.Mockito.when(jwtUtil.validateToken(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        org.mockito.Mockito.when(jwtUtil.extractUsername(org.mockito.ArgumentMatchers.anyString())).thenReturn("admin");

        // Mock blacklist
        org.mockito.Mockito.when(tokenBlacklistService.isBlacklisted(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        // Mock Claims
        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        org.mockito.Mockito.when(claims.getSubject()).thenReturn("admin");
        org.mockito.Mockito.when(claims.get("userId", String.class)).thenReturn("1");
        org.mockito.Mockito.when(jwtUtil.extractAllClaims(org.mockito.ArgumentMatchers.anyString())).thenReturn(claims);

        // Stub UserDetailsService
        org.springframework.security.core.userdetails.User user = new org.springframework.security.core.userdetails.User(
                "admin",
                "password",
                java.util.Arrays.asList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("archive:manage"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("nav:all")));
        org.mockito.Mockito.when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        org.mockito.Mockito.when(userDetailsService.loadUserById("1")).thenReturn(user);

        // Mock validateToken(token, username)
        org.mockito.Mockito.when(jwtUtil.validateToken(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
    }

    @Test
    public void testArchiveCrud() throws Exception {
        String archiveCode = "ARC_" + System.currentTimeMillis();
        String archiveId = UUID.randomUUID().toString();

        // 1. 创建档案 Mock
        Archive archive = new Archive();
        archive.setFondsNo("F001");
        archive.setArchiveCode(archiveCode);
        archive.setCategoryCode("C001");
        archive.setTitle("Test Archive");
        archive.setFiscalYear("2023");
        archive.setRetentionPeriod("10Y");
        archive.setOrgName("Test Org");

        Archive createdArchive = new Archive();
        org.springframework.beans.BeanUtils.copyProperties(archive, createdArchive);
        createdArchive.setId(archiveId);

        org.mockito.Mockito
                .when(archiveService.createArchive(org.mockito.ArgumentMatchers.any(Archive.class),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(createdArchive);

        mockMvc.perform(post("/archives")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(archive)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test Archive"));

        // 2. 获取档案列表 Mock
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Archive> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        page.setRecords(java.util.Collections.singletonList(createdArchive));
        page.setTotal(1);

        org.mockito.Mockito.when(archiveService.getArchives(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.eq(archiveCode),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(page);

        mockMvc.perform(get("/archives")
                .header("Authorization", "Bearer " + token)
                .param("search", archiveCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(archiveId));

        // 3. 更新档案 Mock
        archive.setTitle("Updated Archive Title");
        org.mockito.Mockito.doNothing().when(archiveService).updateArchive(org.mockito.ArgumentMatchers.eq(archiveId),
                org.mockito.ArgumentMatchers.any(Archive.class));

        mockMvc.perform(put("/archives/" + archiveId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(archive)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 4. 获取单个档案 Mock
        createdArchive.setTitle("Updated Archive Title");
        org.mockito.Mockito.when(archiveService.getArchiveById(archiveId)).thenReturn(createdArchive);

        mockMvc.perform(get("/archives/" + archiveId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Archive Title"));

        // 5. 删除档案 Mock
        org.mockito.Mockito.doNothing().when(archiveService).deleteArchive(archiveId);

        mockMvc.perform(delete("/archives/" + archiveId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
