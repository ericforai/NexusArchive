// Input: Java 标准注解库
// Output: @ErpAdapter 注解
// Pos: integration.erp.annotation 包

package com.nexusarchive.integration.erp.annotation;

import java.lang.annotation.*;

/**
 * ERP 适配器元数据注解
 * <p>
 * 用于声明 ERP 适配器的元数据，支持运行时自动发现和注册
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ErpAdapter {

    /**
     * 适配器唯一标识
     * 例如: yonsuite, kingdee, generic
     */
    String identifier();

    /**
     * 适配器显示名称
     * 例如: 用友YonSuite, 金蝶云星空
     */
    String name();

    /**
     * 适配器描述
     */
    String description() default "";

    /**
     * 适配器版本
     */
    String version() default "1.0.0";

    /**
     * 支持的 ERP 系统类型
     */
    String erpType() default "custom";

    /**
     * 支持的业务场景
     * 例如: {"VOUCHER_SYNC", "ATTACHMENT_SYNC", "WEBHOOK"}
     */
    String[] supportedScenarios() default {};

    /**
     * 是否支持 Webhook
     */
    boolean supportsWebhook() default false;

    /**
     * 适配器优先级（数字越小优先级越高）
     * 用于多个适配器支持同一 ERP 类型时的选择
     */
    int priority() default 100;
}
