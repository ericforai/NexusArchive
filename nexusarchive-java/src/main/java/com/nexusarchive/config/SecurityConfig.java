package com.nexusarchive.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（使用JWT不需要CSRF保护）
                .csrf(csrf -> csrf.disable())
                
                // 禁用Session（使用JWT无状态认证）
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 配置权限规则
                .authorizeHttpRequests(auth -> auth
                        // 放行认证接口
                        .requestMatchers("/auth/**").permitAll()
                        // 放行静态资源和Swagger（如果有）
                        .requestMatchers("/static/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 管理员接口需要特定权限 (暂时放宽，后续细化)
                        .requestMatchers("/admin/**").authenticated() 
                        // 其他请求都需要认证
                        .anyRequest().authenticated()
                )
                
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
