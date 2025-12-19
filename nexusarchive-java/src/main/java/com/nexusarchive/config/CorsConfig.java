package com.nexusarchive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * CORS跨域配置
 * 使用 SimpleCorsFilter 完全控制 CORS 头设置
 */
/*
@Configuration
@Slf4j
public class CorsConfig {
    
    @Bean
    public FilterRegistrationBean<SimpleCorsFilter> simpleCorsFilterRegistration() {
        log.info("✅ 注册 SimpleCorsFilter");
        FilterRegistrationBean<SimpleCorsFilter> bean = 
            new FilterRegistrationBean<>(new SimpleCorsFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.addUrlPatterns("/*");
        bean.setName("simpleCorsFilter");
        return bean;
    }
}
*/
