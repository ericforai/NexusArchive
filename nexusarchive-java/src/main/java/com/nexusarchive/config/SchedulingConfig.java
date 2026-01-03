// Input: Spring Framework Scheduling
// Output: SchedulingConfig 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * 启用 Spring 定时任务功能，支持 @Scheduled 注解
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 配置类，仅用于启用定时任务功能
}


