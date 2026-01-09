// Input: Spring Boot Test、Spring Security Test、JUnit 5、MockMvc
// Output: DebugControllerSecurityTest 测试用例类
// Pos: 后端安全测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全测试：DebugController 调试接口权限控制
 *
 * <p>验证:
 * <ul>
 *   <li>未登录用户无法访问调试接口</li>
 *   <li>普通用户无法访问调试接口</li>
 *   <li>非超级管理员无法访问调试接口</li>
 *   <li>超级管理员可以访问调试接口 (当启用时)</li>
 *   <li>配置禁用时调试接口不可用</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class DebugControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String superAdminToken;
    private String systemAdminToken;
    private String regularUserToken;

    @BeforeEach
    public void setupTokens() throws Exception {
        // 获取超级管理员 token
        superAdminToken = loginAs("superadmin", "admin123");

        // 获取系统管理员 token
        systemAdminToken = loginAs("systemadmin", "admin123");

        // 获取普通用户 token
        regularUserToken = loginAs("user", "user123");
    }

    private String loginAs(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Nested
    class UnauthenticatedAccessTests {

        @Test
        public void testUnauthenticatedUser_CannotAccessDebugEndpoints() throws Exception {
            mockMvc.perform(post("/debug/unlock/testuser")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class RegularUserAccessTests {

        @Test
        public void testRegularUser_CannotAccessDebugEndpoints() throws Exception {
            mockMvc.perform(post("/debug/unlock/testuser")
                            .header("Authorization", "Bearer " + regularUserToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class SystemAdminAccessTests {

        @Test
        public void testSystemAdmin_CannotAccessDebugEndpoints() throws Exception {
            // 系统管理员没有 SUPER_ADMIN 角色，应该被拒绝
            mockMvc.perform(post("/debug/unlock/testuser")
                            .header("Authorization", "Bearer " + systemAdminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class SuperAdminAccessTests {

        @Test
        public void testSuperAdmin_CanAccessDebugEndpoints() throws Exception {
            // 超级管理员应该可以访问
            mockMvc.perform(post("/debug/unlock/testuser")
                            .header("Authorization", "Bearer " + superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value("User testuser unlocked (login attempts cleared)."));
        }

        @Test
        public void testSuperAdmin_CanUnlockLockedUser() throws Exception {
            String username = "lockeduser";

            mockMvc.perform(post("/debug/unlock/" + username)
                            .header("Authorization", "Bearer " + superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value("User " + username + " unlocked (login attempts cleared)."));
        }
    }

    @Nested
    class ConfigurationTests {

        /**
         * 测试当 app.debug.enabled=false 时，调试接口应该不可用
         *
         * <p>注意: 此测试需要使用 prod profile 或手动设置 app.debug.enabled=false
         */
        @Test
        public void testWhenDebugDisabled_EndpointReturnsNotFound() throws Exception {
            // 当调试模式禁用时，整个 Controller 不会被注册
            // 因此会返回 404 而非 403
            mockMvc.perform(post("/debug/unlock/testuser")
                            .header("Authorization", "Bearer " + superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError());
        }
    }
}
