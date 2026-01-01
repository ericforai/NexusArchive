// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: DataScopeService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.enums.DataScopeType;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Role;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final RoleMapper roleMapper;
    /**
     * 解析数据权限上下文
     * 
     * 数据隔离策略：
     * 1. 系统管理员/拥有 nav:all 权限的用户：可以访问所有数据
     * 2. 普通用户：只能访问 allowed_fonds 列表中的全宗数据
     * 
     * 注意：数据隔离基于 fonds_no（全宗号），而非 organization_id（组织ID）
     */
    public DataScopeContext resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return DataScopeContext.all();
        }

        if (isSystemAdmin(auth) || hasNavAll(auth)) {
            return DataScopeContext.all();
        }

        String userId = userDetails.getId();
        DataScopeType scopeType = determineScope(userId);
        // 获取用户允许访问的全宗列表（数据隔离键）
        List<String> allowedFonds = userDetails.getAllowedFonds();
        
        return new DataScopeContext(scopeType, userId, allowedFonds != null ? new LinkedHashSet<>(allowedFonds) : Collections.emptySet());
    }

    /**
     * 应用档案数据权限过滤
     * 
     * 数据隔离基于 fonds_no（全宗号）：
     * 1. 如果 context 为 all，不添加过滤条件（可访问所有数据）
     * 2. 如果 context 为 self，只返回当前用户创建的数据
     * 3. 否则，只返回 allowedFonds 列表中的全宗数据
     * 
     * 注意：不再使用 department_id 进行数据隔离（违反档案法规要求）
     */
    public void applyArchiveScope(QueryWrapper<Archive> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq("created_by", context.userId());
            } else {
                wrapper.eq("1", "0");
            }
            return;
        }

        // 基于 fonds_no（全宗号）进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            wrapper.in("fonds_no", allowedFonds);
        } else {
            // 如果没有允许访问的全宗，则不允许访问任何数据
            wrapper.eq("1", "0");
        }
    }

    /**
     * 检查是否可以访问指定档案
     * 
     * 数据隔离基于 fonds_no（全宗号）：
     * 1. 如果 context 为 all，可以访问
     * 2. 如果 context 为 self，只能访问自己创建的数据
     * 3. 否则，检查档案的 fonds_no 是否在 allowedFonds 列表中
     */
    public boolean canAccessArchive(Archive archive, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return true;
        }
        if (context.isSelf()) {
            return context.userId() != null && context.userId().equals(archive.getCreatedBy());
        }
        
        // 基于 fonds_no（全宗号）进行数据隔离
        String fondsNo = archive.getFondsNo();
        if (StringUtils.hasText(fondsNo) && context.allowedFonds().contains(fondsNo)) {
            return true;
        }
        
        // 如果没有匹配的全宗，则不允许访问
        return false;
    }

    private boolean isSystemAdmin(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_SYSTEM_ADMIN".equals(authority.getAuthority()) || "ROLE_system_admin".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNavAll(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("nav:all".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private DataScopeType determineScope(String userId) {
        if (userId == null) {
            return DataScopeType.SELF;
        }
        List<Role> roles = roleMapper.findByUserId(userId);
        DataScopeType max = DataScopeType.SELF;
        for (Role role : roles) {
            DataScopeType candidate = DataScopeType.from(role.getDataScope());
            max = DataScopeType.max(max, candidate);
            if (max == DataScopeType.ALL) {
                break;
            }
        }
        return max;
    }

    /**
     * 数据权限上下文
     * 
     * 数据隔离基于 fonds_no（全宗号），而非 department_id（部门ID）
     * 
     * @param type 数据权限类型（ALL/SELF/DEPARTMENT等）
     * @param userId 用户ID
     * @param allowedFonds 允许访问的全宗号列表（数据隔离键）
     */
    public record DataScopeContext(DataScopeType type, String userId, Set<String> allowedFonds) {
        public static DataScopeContext all() {
            return new DataScopeContext(DataScopeType.ALL, null, Collections.emptySet());
        }

        public boolean isAll() {
            return type.isAll();
        }

        public boolean isSelf() {
            return type.isSelf();
        }
    }
}
