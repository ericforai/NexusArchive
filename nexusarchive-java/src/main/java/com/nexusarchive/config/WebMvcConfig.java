// Input: Spring Framework、Java 标准库
// Output: WebMvcConfig 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
