// Input: Spring Framework、Jackson、Java 8 Date/Time API
// Output: JacksonConfig 配置类
// Pos: 配置层 - Jackson 序列化配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置
 * <p>
 * 配置 ObjectMapper 以支持 Java 8 日期/时间类型的序列化
 * 使用 Jackson2ObjectMapperBuilderCustomizer 来自定义 Spring Boot 默认的 ObjectMapper
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.modulesToInstall(new JavaTimeModule());
    }
}
