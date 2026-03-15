// Input: MyBatis-Plus、Spring Framework、Java 标准库
// Output: MyBatisPlusConfig 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置
 */
@Configuration
public class MyBatisPlusConfig {
    
    /**
     * MyBatis-Plus 插件配置
     * <p>插件顺序很重要：
     * <ul>
     *   <li>乐观锁插件 - 必须在分页插件之前</li>
     *   <li>分页插件 - 最后添加</li>
     * </ul>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件 (必须在分页插件之前)
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(1000L); // 最大分页限制

        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
