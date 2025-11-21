package com.nexusarchive.service;

import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 三员互斥校验服务
 */
@Service
@RequiredArgsConstructor
public class RoleValidationService {
    private final RoleMapper roleMapper;

    /**
     * 校验用户的角色集合是否满足三员互斥规则
     * @param userId 当前用户ID（用于排除自身已有角色），可为null
     * @param roleIds 待分配的角色ID列表
     */
    public void validateThreeRoleExclusion(String userId, List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        // 查询角色的category信息
        List<String> categories = roleMapper.selectCategoriesByIds(roleIds);
        // 统计每个category出现的次数
        Set<String> exclusiveCategories = categories.stream()
                .filter(cat -> {
                    // 判断该category是否标记为排他（is_exclusive = true）
                    return roleMapper.isCategoryExclusive(cat);
                })
                .collect(Collectors.toSet());
        // 如果同一category出现多次，则违反排他规则
        if (exclusiveCategories.size() < categories.size()) {
            throw new BusinessException("同一类角色只能分配一个，违反三员互斥规则");
        }
        // 进一步检查已有角色（排除自身）
        if (userId != null) {
            List<String> existingCategories = roleMapper.selectCategoriesByUserId(userId);
            for (String cat : existingCategories) {
                if (exclusiveCategories.contains(cat)) {
                    throw new BusinessException("用户已拥有同类排他角色，无法再分配");
                }
            }
        }
    }
}
