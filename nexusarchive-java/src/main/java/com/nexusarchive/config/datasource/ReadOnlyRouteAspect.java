// Input: Spring AOP、Spring Framework、Java 标准库
// Output: ReadOnlyRouteAspect 类 - 只读路由切面
// Pos: 基础设施层 - 数据源路由
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

import com.nexusarchive.annotation.ReadOnly;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 只读路由切面
 * <p>拦截带有 {@link ReadOnly} 注解的方法，在执行前设置数据源为从库，
 * 执行后清理上下文</p>
 *
 * <p>执行顺序：</p>
 * <ol>
 *   <li>方法执行前：设置 {@link DataSourceContextHolder} 为 {@link DataSourceType#SLAVE}</li>
 *   <li>执行目标方法</li>
 *   <li>方法执行后：清理 {@link DataSourceContextHolder}</li>
 * </ol>
 *
 * @since 2.1.0
 */
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
@ConditionalOnProperty(prefix = "rw-split", name = "enabled", havingValue = "true")
public class ReadOnlyRouteAspect {

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyRouteAspect.class);

    /**
     * 拦截 @ReadOnly 注解的方法
     */
    @Around("@annotation(readOnly)")
    public Object routeToSlave(ProceedingJoinPoint joinPoint, ReadOnly readOnly) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("[RW_SPLIT] Routing method to SLAVE: {}", methodName);

        try {
            // 设置为从库
            DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);

            // 执行目标方法
            return joinPoint.proceed();

        } finally {
            // 清理上下文，避免影响后续方法
            DataSourceContextHolder.clearDataSourceType();
        }
    }

    /**
     * 拦截类级别 @ReadOnly 注解的所有方法
     */
    @Around("@within(com.nexusarchive.annotation.ReadOnly)")
    public Object routeClassToSlave(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("[RW_SPLIT] Routing class-level method to SLAVE: {}", methodName);

        try {
            DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
            return joinPoint.proceed();

        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
