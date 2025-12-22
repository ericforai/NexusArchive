// Input: Java 标准库
// Output: ArchivalAudit 接口
// Pos: 后端模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 档案审计注解
 * 
 * 用于标记需要审计的方法
 * 自动记录操作日志到 sys_audit_log 表
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArchivalAudit {
    
    /**
     * 操作类型: CAPTURE, ARCHIVE, MODIFY_META, DESTROY, PRINT, DOWNLOAD
     */
    String operationType();
    
    /**
     * 资源类型: ARCHIVE, USER, ROLE, etc.
     */
    String resourceType() default "ARCHIVE";
    
    /**
     * 操作描述
     */
    String description() default "";
}
