// Input: SpringDoc OpenAPI、Spring Security配置
// Output: OpenApiConfig 配置类
// Pos: 配置层 - Swagger/OpenAPI文档配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * OpenAPI/Swagger 配置类
 * <p>
 * 配置 API 文档的生成规则，包括安全认证、服务器信息、分组等。
 * 访问地址: http://localhost:19090/api/swagger-ui.html
 * </p>
 *
 * @see <a href="https://springdoc.org/">SpringDoc OpenAPI 官方文档</a>
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";
    public static final String SECURITY_SCHEME_BEARER = "bearer";

    /**
     * 配置 OpenAPI 文档基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NexusArchive API Documentation")
                        .description("""
                                ### 电子会计档案管理系统 API 文档

                                符合 **DA/T 94-2022**《电子会计档案管理规范》

                                **主要功能模块:**
                                - **档案管理**: 电子会计档案的创建、查询、更新、删除
                                - **归档审批**: 档案归档的审批流程管理
                                - **四性检测**: 真实性、完整性、可用性、安全性检测
                                - **ERP集成**: 用友 YonSuite 等第三方 ERP 系统数据同步
                                - **全宗管理**: 多全宗体系下的数据隔离与管理
                                - **三员分立**: 系统管理员、安全保密员、安全审计员权限分离
                                - **审计日志**: 基于 SM3 哈希链的防篡改审计日志
                                - **批量操作**: 归档审批、批次管理、销毁申请的批量处理

                                **认证方式:**
                                - 使用 JWT Bearer Token 进行认证
                                - 在 Swagger UI 右上角点击 "Authorize" 输入 token
                                - Token 格式: `Bearer <your-jwt-token>`

                                **分页参数:**
                                - `page`: 页码，从 1 开始
                                - `limit`: 每页条数，默认 10，最大 100

                                **响应格式:**
                                ```json
                                {
                                  "code": 200,
                                  "message": "success",
                                  "data": { ... }
                                }
                                ```
                                """)
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("NexusArchive Team")
                                .email("support@nexusarchive.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://nexusarchive.com/license")))
                .servers(List.of(
                        new Server().url("http://localhost:19090/api").description("本地开发环境"),
                        new Server().url("https://api-dev.nexusarchive.com/api").description("开发测试环境"),
                        new Server().url("https://api.nexusarchive.com/api").description("生产环境")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme(SECURITY_SCHEME_BEARER)
                                        .bearerFormat("JWT")
                                        .description("请输入 JWT Token，格式: Bearer {token}")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * 档案管理 API 分组
     */
    @Bean
    public GroupedOpenApi archiveApi() {
        return GroupedOpenApi.builder()
                .group("01-档案管理")
                .pathsToMatch("/archives/**", "/volumes/**", "/files/**")
                .build();
    }

    /**
     * 归档审批 API 分组
     */
    @Bean
    public GroupedOpenApi approvalApi() {
        return GroupedOpenApi.builder()
                .group("02-归档审批")
                .pathsToMatch("/approval/**", "/batch/**", "/ingest/**")
                .build();
    }

    /**
     * 四性检测 API 分组
     */
    @Bean
    public GroupedOpenApi complianceApi() {
        return GroupedOpenApi.builder()
                .group("03-四性检测")
                .pathsToMatch("/compliance/**", "/four-nature/**")
                .build();
    }

    /**
     * ERP 集成 API 分组
     */
    @Bean
    public GroupedOpenApi erpApi() {
        return GroupedOpenApi.builder()
                .group("04-ERP集成")
                .pathsToMatch("/erp/**", "/yonsuite/**", "/sync/**")
                .build();
    }

    /**
     * 全宗管理 API 分组
     */
    @Bean
    public GroupedOpenApi fondsApi() {
        return GroupedOpenApi.builder()
                .group("05-全宗管理")
                .pathsToMatch("/fonds/**", "/org/**", "/bas-fonds/**")
                .build();
    }

    /**
     * 用户权限 API 分组
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("06-用户权限")
                .pathsToMatch("/auth/**", "/users/**", "/roles/**", "/permissions/**")
                .build();
    }

    /**
     * 系统管理 API 分组
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("07-系统管理")
                .pathsToMatch("/admin/**", "/config/**", "/system/**", "/monitoring/**")
                .build();
    }

    /**
     * 审计日志 API 分组
     */
    @Bean
    public GroupedOpenApi auditApi() {
        return GroupedOpenApi.builder()
                .group("08-审计日志")
                .pathsToMatch("/audit/**", "/logs/**")
                .build();
    }

    /**
     * 搜索 API 分组
     */
    @Bean
    public GroupedOpenApi searchApi() {
        return GroupedOpenApi.builder()
                .group("09-搜索")
                .pathsToMatch("/search/**")
                .build();
    }

    /**
     * 异步任务 API 分组
     */
    @Bean
    public GroupedOpenApi asyncApi() {
        return GroupedOpenApi.builder()
                .group("10-异步任务")
                .pathsToMatch("/async/**", "/tasks/**")
                .build();
    }

    /**
     * 销毁管理 API 分组
     */
    @Bean
    public GroupedOpenApi destructionApi() {
        return GroupedOpenApi.builder()
                .group("11-销毁管理")
                .pathsToMatch("/destruction/**")
                .build();
    }

    /**
     * 扫描集成 API 分组
     */
    @Bean
    public GroupedOpenApi scanApi() {
        return GroupedOpenApi.builder()
                .group("12-扫描集成")
                .pathsToMatch("/scan/**")
                .build();
    }

    /**
     * 其他 API 分组
     */
    @Bean
    public GroupedOpenApi otherApi() {
        return GroupedOpenApi.builder()
                .group("13-其他接口")
                .pathsToMatch(
                        "/health/**",
                        "/license/**",
                        "/ticket/**",
                        "/notification/**",
                        "/relation/**",
                        "/reconciliation/**",
                        "/attachment/**",
                        "/warehouse/**",
                        "/position/**",
                        "/entity/**",
                        "/workflow/**",
                        "/certificate/**",
                        "/timestamp/**",
                        "/signature/**",
                        "/ofd/**",
                        "/export/**",
                        "/import/**",
                        "/stats/**",
                        "/pool/**",
                        "/preview/**",
                        "/nav/**",
                        "/ops/**",
                        "/debug/**")
                .build();
    }
}
