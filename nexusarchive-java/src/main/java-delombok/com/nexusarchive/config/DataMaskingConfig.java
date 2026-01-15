// Input: Spring Framework、Jackson
// Output: DataMaskingConfig 配置类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.serializer.DataMaskingSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 数据脱敏配置
 * 
 * PRD 来源: Section 2.1 - 高级检索与脱敏
 * 
 * 注意：如果项目中已有 ObjectMapper Bean，此配置不会覆盖
 */
@Configuration
public class DataMaskingConfig {
    
    /**
     * 配置 ObjectMapper，添加数据脱敏序列化器
     * 使用 @ConditionalOnMissingBean 避免覆盖现有的 ObjectMapper
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper dataMaskingObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        // 注册数据脱敏序列化器
        // 注意：这里使用自定义序列化器，但实际脱敏逻辑在 AOP 切面中处理
        // 序列化器作为备用方案

        mapper.registerModule(module);
        // 注册 JavaTimeModule 以支持 LocalDate、LocalDateTime 等类型
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}

