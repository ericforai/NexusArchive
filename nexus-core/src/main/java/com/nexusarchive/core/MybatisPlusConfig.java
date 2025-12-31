// Input: MyBatis-Plus
// Output: Fonds isolation + SQL 审计守卫拦截器装配（可配置）
// Pos: NexusCore configuration
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(SqlAuditRulesResolver rulesResolver,
                                                         SqlAuditGuardProperties properties) {
        SqlAuditRules rules = rulesResolver.resolve();
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new FondsIsolationInterceptor(rules));
        if (properties.isEnabled()) {
            interceptor.addInnerInterceptor(new SqlAuditGuardInterceptor(new SqlAuditGuard(rules)));
        }
        return interceptor;
    }
}
