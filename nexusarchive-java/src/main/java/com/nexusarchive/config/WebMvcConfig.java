package com.nexusarchive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MigrationGatekeeperInterceptor migrationGatekeeperInterceptor;

    public WebMvcConfig(MigrationGatekeeperInterceptor migrationGatekeeperInterceptor) {
        this.migrationGatekeeperInterceptor = migrationGatekeeperInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
         registry.addInterceptor(migrationGatekeeperInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/health/**", "/api/health/**", "/error", "/swagger-ui/**", "/v3/api-docs/**"); // Allow health checks & swagger
    }
}
