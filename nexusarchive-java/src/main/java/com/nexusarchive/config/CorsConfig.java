package com.nexusarchive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS跨域配置
 * 安全加固版本 - 2025-12-07
 * 
 * 配置说明：
 * - 开发环境：允许所有来源（CORS_ALLOWED_ORIGINS 为空时）
 * - 生产环境：必须明确指定允许的来源
 */
@Configuration
@Slf4j
public class CorsConfig {
    
    /**
     * 允许的跨域来源，多个用逗号分隔
     * 示例：http://localhost:3000,https://archive.example.com
     */
    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String allowedOrigins;
    
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 根据配置决定允许的来源
        if (StringUtils.hasText(allowedOrigins)) {
            // 生产环境：使用配置的来源列表
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            origins.forEach(origin -> {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    config.addAllowedOrigin(trimmed);
                }
            });
            log.info("✅ CORS 配置：允许来源 {}", origins);
        } else {
            // 开发环境：允许所有来源（带警告）
            config.addAllowedOriginPattern("*");
            log.warn("⚠️ CORS 配置：允许所有来源（仅限开发环境！生产环境请设置 CORS_ALLOWED_ORIGINS）");
        }
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许的请求方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("OPTIONS");
        
        // 允许携带凭证
        config.setAllowCredentials(true);
        
        // 暴露的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");
        
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return source;
    }
    
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}

