// Input: MyBatis、Spring Boot 3.1.6、Spring Framework、Java 标准库
// Output: NexusArchiveApplication 类
// Pos: 应用启动入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * NexusArchive 电子会计档案管理系统
 * 
 * 技术栈:
 * - Spring Boot 3.1.6
 * - MyBatis-Plus 3.5
 * - PostgreSQL / 达梦 / 人大金仓
 * 
 * 合规标准:
 * - DA/T 94-2022 电子会计档案管理规范
 * - GB/T 39784-2021 电子档案管理系统通用功能要求
 * 
 * @author NexusArchive Team
 * @version 2.0.0
 */
@Slf4j
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration.class
})
@EnableAsync
@MapperScan({
    "com.nexusarchive.mapper",
    "com.nexusarchive.modules.borrowing.infra.mapper",
    "com.nexusarchive.modules.document.infra.mapper",
    "com.nexusarchive.modules.signature.infra.mapper"
})
public class NexusArchiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusArchiveApplication.class, args);
        log.info("========================================");
        log.info("NexusArchive 启动成功!");
        log.info("API文档: http://localhost:8080/api/doc.html");
        log.info("========================================");
    }

}
