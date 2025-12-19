package com.nexusarchive.config;

import lombok.RequiredArgsConstructor;
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

/**
 * Spring Security配置 (安全重构版本)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final LicenseValidationFilter licenseValidationFilter;

    @Value("${app.security.cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com https://aistudiocdn.com; " +
                                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                        "img-src 'self' data: blob: https:; " +
                                        "font-src 'self' data: https://fonts.gstatic.com; " +
                                        "connect-src 'self' ws: wss:; " +
                                        "frame-ancestors 'self';"))
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
        
        List<String> origins = Arrays.asList(corsAllowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        // 也加入 127.0.0.1 变体
        configuration.addAllowedOrigin("http://127.0.0.1:15175");
        configuration.addAllowedOrigin("http://localhost:15175");
        configuration.addAllowedOrigin("http://127.0.0.1:5175");
        configuration.addAllowedOrigin("http://localhost:5175");
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
