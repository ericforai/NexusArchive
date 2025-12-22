// Input: Java 标准库
// Output: RoleCategory 枚举
// Pos: 后端模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.common.enums;

/**
 * 角色类别枚举 (GB/T 39784-2021 三员管理)
 * 
 * 三员分立要求：
 * - 系统管理员、安全保密员、安全审计员必须由不同人员担任
 * - 三员角色不能分配给同一用户
 */
public enum RoleCategory {
    
    /**
     * 系统管理员 - 负责系统运维和配置管理
     */
    SYSTEM_ADMIN("system_admin", "系统管理员", true),
    
    /**
     * 安全保密员 - 负责权限管理和密钥管理
     */
    SECURITY_ADMIN("security_admin", "安全保密员", true),
    
    /**
     * 安全审计员 - 负责查看和审计系统日志
     */
    AUDIT_ADMIN("audit_admin", "安全审计员", true),
    
    /**
     * 业务操作员 - 普通业务用户（档案员、会计等）
     */
    BUSINESS_USER("business_user", "业务操作员", false);
    
    private final String code;
    private final String label;
    private final boolean exclusive;
    
    RoleCategory(String code, String label, boolean exclusive) {
        this.code = code;
        this.label = label;
        this.exclusive = exclusive;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getLabel() {
        return label;
    }
    
    public boolean isExclusive() {
        return exclusive;
    }
    
    public static RoleCategory fromCode(String code) {
        for (RoleCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return BUSINESS_USER;
    }
}
