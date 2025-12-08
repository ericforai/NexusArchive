package com.nexusarchive.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.nexusarchive.mapper")
public class MybatisConfig {
}
