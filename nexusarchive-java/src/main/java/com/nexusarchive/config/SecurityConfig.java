// Input: Lombok、Spring Framework、Spring Security、Java 标准库
// Output: SecurityConfig 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security配置 (安全重构版本)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final LicenseValidationFilter licenseValidationFilter;

    @Value("${app.security.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${app.security.csp.allow-unsafe-inline:false}")
    private boolean cspAllowUnsafeInline;

    @Value("${app.security.csp.allow-unsafe-eval:false}")
    private boolean cspAllowUnsafeEval;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String cspDirectives = buildCspDirectives();
        http
                // 启用并配置集中的CORS策略
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 禁用CSRF（使用JWT不需要CSRF保护）
                .csrf(csrf -> csrf.disable())

                // 禁用Session（使用JWT无状态认证）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置权限规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout",
                                "/license/**", // License 导入需要在未授权时可访问
                                "/health",
                                "/health/**",
                                "/actuator/health",
                                "/ws/**",
                                "/error")
                        .permitAll()
                        // 允许所有 OPTIONS 请求 (CORS 预检)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-resources/**")
                        .permitAll()
                        .anyRequest().authenticated())

                // 异常处理
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // 添加自定义过滤器
                // LicenseFilter 先于 JWT 过滤器
                .addFilterBefore(licenseValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, LicenseValidationFilter.class)


                // 安全响应头配置 (安全加固)
                .headers(headers -> headers
                        // 防止点击劫持：仅允许同源 iframe 嵌入
                        .frameOptions(frame -> frame.sameOrigin())
                        // [CRITICAL] 实施严格的内容安全策略 (CSP)，禁止内联脚本和eval
                        .contentSecurityPolicy(csp -> csp.policyDirectives(cspDirectives))
                        // 防止 MIME 类型嗅探
                        .contentTypeOptions(cto -> {})
                        // 引用策略
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // 权限策略（禁用不需要的浏览器功能）
                        .permissionsPolicy(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=(), payment=()")));

        return http.build();
    }

    /**
     * 集中的CORS配置源
     * 从配置文件加载 Allowed Origins，支持逗号分隔
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        
        // [FIXED P1-6] 禁止通配符，抛出异常阻止启动
        if (origins.contains("*")) {
            throw new IllegalStateException(
                "CORS 配置错误: 不允许使用通配符 '*'。" +
                "请在 application.yml 中配置具体的允许域名，例如: " +
                "app.security.cors.allowed-origins=http://localhost:5173,http://localhost:3000"
            );
        }
        
        if (origins.isEmpty()) {
            log.warn("CORS 配置为空，将拒绝所有跨域请求");
        }
        
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private String buildCspDirectives() {
        String scriptSrc = "script-src 'self' https://cdn.tailwindcss.com https://aistudiocdn.com";
        if (cspAllowUnsafeInline) {
            scriptSrc += " 'unsafe-inline'";
        }
        if (cspAllowUnsafeEval) {
            scriptSrc += " 'unsafe-eval'";
        }

        String styleSrc = "style-src 'self' https://fonts.googleapis.com";
        if (cspAllowUnsafeInline) {
            styleSrc += " 'unsafe-inline'";
        }

        return String.join(" ",
                "default-src 'self';",
                scriptSrc + ";",
                styleSrc + ";",
                "img-src 'self' data: blob: https:;",
                "font-src 'self' data: https://fonts.gstatic.com;",
                "connect-src 'self' ws: wss:;",
                "frame-ancestors 'self';");
    }
}
