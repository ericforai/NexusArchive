// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: RoleValidationService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

/**
 * 三员互斥校验服务
 * 
 * 根据等保2.0三级要求，系统管理员、安全管理员、审计管理员三种角色不能同时分配给同一用户
 */
@Service
@RequiredArgsConstructor
public class RoleValidationService {
    private final RoleMapper roleMapper;
    
    // 三员角色类别定义
    private static final Set<String> THREE_ADMIN_CATEGORIES = Set.of(
        "system_admin",     // 系统管理员
        "security_admin",   // 安全管理员
        "audit_admin"       // 审计管理员
    );

    /**
     * 校验用户的角色集合是否满足三员互斥规则
     * @param userId 当前用户ID（用于排除自身已有角色），可为null
     * @param roleIds 待分配的角色ID列表
     */
    public void validateThreeRoleExclusion(String userId, List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        
        // 查询待分配角色的详细信息
        List<com.nexusarchive.entity.Role> rolesToAssign = roleMapper.selectBatchIds(roleIds);
        
        // === 规则1：检查待分配的角色中是否包含多个三员角色 ===
        Set<String> threeAdminCategories = new HashSet<>();
        for (com.nexusarchive.entity.Role role : rolesToAssign) {
            String normalizedCategory = normalizeCategory(role.getRoleCategory());
            if (role.getIsExclusive() != null && role.getIsExclusive() 
                && normalizedCategory != null
                && THREE_ADMIN_CATEGORIES.contains(normalizedCategory)) {
                if (!threeAdminCategories.add(normalizedCategory)) {
                    throw new BusinessException("不能同时分配同一类别的多个排他角色: " + role.getRoleCategory());
                }
            }
        }
        
        // 如果待分配的三员角色超过1个，直接拒绝
        if (threeAdminCategories.size() > 1) {
            throw new BusinessException(
                "违反三员互斥原则：不能同时分配多个特权管理角色（系统管理员、安全管理员、审计管理员）"
            );
        }
        
        // === 规则2：如果是更新用户，检查用户已有的三员角色 ===
        if (userId != null && !threeAdminCategories.isEmpty()) {
            List<com.nexusarchive.entity.Role> existingRoles = roleMapper.findByUserId(userId);
            for (com.nexusarchive.entity.Role existingRole : existingRoles) {
                String existingCategory = normalizeCategory(existingRole.getRoleCategory());
                if (existingRole.getIsExclusive() != null && existingRole.getIsExclusive()
                    && existingCategory != null
                    && THREE_ADMIN_CATEGORIES.contains(existingCategory)) {
                    
                    // 检查新分配的三员角色是否与已有的冲突
                    for (String newCategory : threeAdminCategories) {
                        if (!newCategory.equals(existingCategory)) {
                            throw new BusinessException(
                                "违反三员互斥原则：用户已拥有 " + getCategoryDisplayName(existingCategory) + 
                                " 角色，不能再分配 " + getCategoryDisplayName(newCategory) + " 角色"
                            );
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取角色类别的显示名称
     */
    private String getCategoryDisplayName(String category) {
        return switch (category) {
            case "system_admin" -> "系统管理员";
            case "security_admin" -> "安全保密员";
            case "audit_admin" -> "安全审计员";
            default -> category;
        };
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }
}
