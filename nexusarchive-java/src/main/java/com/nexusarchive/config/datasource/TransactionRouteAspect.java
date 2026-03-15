// Input: Spring AOP、Spring Framework、Java 标准库
// Output: TransactionRouteAspect 类 - 事务写操作路由切面
// Pos: 基础设施层 - 数据源路由
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务写操作路由切面
 * <p>拦截带有 {@link Transactional} 注解的方法，根据 readOnly 参数决定路由</p>
 *
 * <p>路由规则：</p>
 * <ul>
 *   <li>{@code @Transactional(readOnly=false)} 或 {@code @Transactional} → 主库</li>
 *   <li>{@code @Transactional(readOnly=true)} → 从库</li>
 * </ul>
 *
 * @since 2.1.0
 */
@Aspect
@Component
@Order(2) // 在 ReadOnlyRouteAspect 之后执行
@ConditionalOnProperty(prefix = "rw-split", name = "enabled", havingValue = "true")
public class TransactionRouteAspect {

    private static final Logger log = LoggerFactory.getLogger(TransactionRouteAspect.class);

    /**
     * 拦截 @Transactional 注解的方法
     * <p>当事务不是只读时，路由到主库</p>
     */
    @Around("@annotation(transactional)")
    public Object routeTransaction(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        // 只读事务路由到从库（由默认行为处理，这里不需要特殊处理）
        // 非只读事务路由到主库
        if (!transactional.readOnly()) {
            log.debug("[RW_SPLIT] Routing transactional method to MASTER: {}", methodName);

            try {
                DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
                return joinPoint.proceed();

            } finally {
                DataSourceContextHolder.clearDataSourceType();
            }
        }

        // 只读事务，使用默认行为（从库），但需要确保在方法结束后清理
        log.debug("[RW_SPLIT] Transactional method is read-only, using default SLAVE: {}", methodName);

        try {
            return joinPoint.proceed();
        } finally {
            // 确保上下文被清理，避免 ThreadLocal 内存泄漏
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
