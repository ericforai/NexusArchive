// Input: MyBatis、Spring Framework、Java 标准库
// Output: MybatisConfig 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.nexusarchive.mapper")
public class MybatisConfig {
}
