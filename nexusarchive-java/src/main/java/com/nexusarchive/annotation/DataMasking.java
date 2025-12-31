// Input: Java 标准库
// Output: DataMasking 注解
// Pos: 注解定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解
 * 
 * 用于标记需要脱敏的字段或方法返回值
 * 
 * PRD 来源: Section 2.1 - 高级检索与脱敏
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataMasking {
    
    /**
     * 脱敏类型
     */
    MaskingType type() default MaskingType.MIDDLE;
    
    /**
     * 保留前几位（默认保留前4位）
     */
    int prefixLength() default 4;
    
    /**
     * 保留后几位（默认保留后4位）
     */
    int suffixLength() default 4;
    
    /**
     * 脱敏占位符（默认8个星号）
     */
    String maskChar() default "********";
    
    /**
     * 脱敏类型枚举
     */
    enum MaskingType {
        /**
         * 中间脱敏（保留前后）
         */
        MIDDLE,
        /**
         * 全部脱敏
         */
        ALL,
        /**
         * 仅保留前几位
         */
        PREFIX_ONLY,
        /**
         * 仅保留后几位
         */
        SUFFIX_ONLY
    }
}

