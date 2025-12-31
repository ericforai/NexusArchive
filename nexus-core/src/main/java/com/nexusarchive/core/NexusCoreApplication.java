// Input: Spring Boot
// Output: Sprint 0 应用启动入口
// Pos: NexusCore 应用层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NexusCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusCoreApplication.class, args);
    }
}
