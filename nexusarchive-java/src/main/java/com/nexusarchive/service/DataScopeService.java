// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: DataScopeService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.enums.DataScopeType;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.Role;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.security.FondsContext;
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
     * 1. 系统管理员（ROLE_SYSTEM_ADMIN）：可以访问所有数据
     * 2. 普通用户：只能访问 allowed_fonds 列表中的全宗数据
     *
     * 注意：导航权限（如 nav:all）仅控制 UI 访问，不应影响数据隔离
     * 数据隔离基于 fonds_no（全宗号），而非 organization_id（组织ID）
     */
    public DataScopeContext resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return DataScopeContext.all();
        }

        // 只有系统管理员角色才能绕过数据隔离，nav:all 权限仅用于 UI 导航
        if (isSystemAdmin(auth)) {
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
     * 数据隔离优先级（按优先级从高到低）：
     * 1. 全宗号过滤（FondsContext）：这是系统的主要数据隔离机制，必须优先应用
     * 2. allowedFonds 列表过滤：后备的基于全宗号的过滤
     * 3. 创建者过滤（self scope）：仅在上述两者都不适用时使用
     *
     * 重要：fonds_no 过滤必须优先于 created_by 过滤，因为：
     * - 多用户可以共享同一全宗的数据
     * - 业务用户需要查看全宗内所有数据，而不仅仅是自己创建的数据
     * - data_scope=self 的含义在档案系统中应该仅作为后备方案
     *
     * @param wrapper MyBatis-Plus 查询条件
     * @param context 数据权限上下文
     */
    public void applyArchiveScope(QueryWrapper<Archive> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        // 优先级1：优先使用当前选中的全宗（从 FondsContext 获取）
        // 这是系统的核心数据隔离机制，必须优先检查
        String currentFondsNo = FondsContext.getCurrentFondsNo();
        if (StringUtils.hasText(currentFondsNo)) {
            // 用户已切换到特定全宗，只返回该全宗的数据
            wrapper.eq("fonds_no", currentFondsNo);
            return;
        }

        // 优先级2：后备方案 - 基于 allowedFonds 列表进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            wrapper.in("fonds_no", allowedFonds);
            return;
        }

        // 优先级3：仅在没有任何全宗权限时，才使用 created_by 过滤
        // 这是为了确保用户至少能看到自己创建的数据
        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq("created_by", context.userId());
            } else {
                wrapper.eq("1", "0");
            }
            return;
        }

        // 如果没有任何权限，不允许访问任何数据
        wrapper.eq("1", "0");
    }

    /**
     * 应用档案数据权限过滤 (LambdaQueryWrapper 版本)
     *
     * @param wrapper MyBatis-Plus Lambda查询条件
     * @param context 数据权限上下文
     */
    public void applyArchiveScope(LambdaQueryWrapper<Archive> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        // 优先级1：优先使用当前选中的全宗（从 FondsContext 获取）
        String currentFondsNo = FondsContext.getCurrentFondsNo();
        if (StringUtils.hasText(currentFondsNo)) {
            wrapper.eq(Archive::getFondsNo, currentFondsNo);
            return;
        }

        // 优先级2：后备方案 - 基于 allowedFonds 列表进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            wrapper.in(Archive::getFondsNo, allowedFonds);
            return;
        }

        // 优先级3：仅在没有任何全宗权限时，才使用 created_by 过滤
        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq(Archive::getCreatedBy, context.userId());
            } else {
                wrapper.apply("1=0");
            }
            return;
        }

        // 如果没有任何权限，不允许访问任何数据
        wrapper.apply("1=0");
    }

    /**
     * 应用原始凭证数据权限过滤 (LambdaQueryWrapper 版本)
     *
     * @param wrapper MyBatis-Plus Lambda查询条件
     * @param context 数据权限上下文
     */
    public void applyOriginalVoucherScope(LambdaQueryWrapper<OriginalVoucher> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        // 优先级1：优先使用当前选中的全宗（从 FondsContext 获取）
        String currentFondsNo = FondsContext.getCurrentFondsNo();
        if (StringUtils.hasText(currentFondsNo)) {
            wrapper.eq(OriginalVoucher::getFondsCode, currentFondsNo);
            return;
        }

        // 优先级2：后备方案 - 基于 allowedFonds 列表进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            wrapper.in(OriginalVoucher::getFondsCode, allowedFonds);
            return;
        }

        // 优先级3：仅在没有任何全宗权限时，才使用 created_by 过滤
        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq(OriginalVoucher::getCreatedBy, context.userId());
            } else {
                wrapper.eq(OriginalVoucher::getId, "never-match");
            }
            return;
        }

        // 如果没有任何权限，不允许访问任何数据
        wrapper.eq(OriginalVoucher::getId, "never-match");
    }

    /**
     * 应用原始凭证数据权限过滤 (QueryWrapper 版本)
     *
     * @param wrapper MyBatis-Plus 查询条件
     * @param context 数据权限上下文
     */
    public void applyOriginalVoucherScope(QueryWrapper<OriginalVoucher> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        // 优先级1：优先使用当前选中的全宗（从 FondsContext 获取）
        String currentFondsNo = FondsContext.getCurrentFondsNo();
        if (StringUtils.hasText(currentFondsNo)) {
            wrapper.eq("fonds_code", currentFondsNo);
            return;
        }

        // 优先级2：后备方案 - 基于 allowedFonds 列表进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            wrapper.in("fonds_code", allowedFonds);
            return;
        }

        // 优先级3：仅在没有任何全宗权限时，才使用 created_by 过滤
        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq("created_by", context.userId());
            } else {
                wrapper.eq("1", "0");
            }
            return;
        }

        // 如果没有任何权限，不允许访问任何数据
        wrapper.eq("1", "0");
    }

    /**
     * 检查是否可以访问指定原始凭证
     */
    public boolean canAccessOriginalVoucher(OriginalVoucher voucher, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return true;
        }

        String voucherFondsCode = voucher.getFondsCode();

        // 优先级1：优先使用当前选中的全宗（从 FondsContext 获取）
        String currentFondsNo = FondsContext.getCurrentFondsNo();
        if (StringUtils.hasText(currentFondsNo)) {
            return currentFondsNo.equals(voucherFondsCode);
        }

        // 优先级2：后备方案 - 基于 allowedFonds 列表进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            return StringUtils.hasText(voucherFondsCode) && allowedFonds.contains(voucherFondsCode);
        }

        // 优先级3：仅在没有任何全宗权限时，才使用 created_by 过滤
        if (context.isSelf()) {
            return context.userId() != null && context.userId().equals(voucher.getCreatedBy());
        }

        return false;
    }

    /**
     * 检查是否可以访问指定档案
     *
     * 数据隔离优先级（与 applyArchiveScope 保持一致）：
     * 1. 全宗号过滤（FondsContext）：优先级最高
     * 2. allowedFonds 列表过滤：后备方案
     * 3. 创建者过滤（self scope）：仅在没有全宗权限时使用
     *
     * 重要：必须与 applyArchiveScope() 的优先级顺序完全一致，否则会出现不一致的访问控制
     */
    public boolean canAccessArchive(Archive archive, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return true;
        }

        String archiveFondsNo = archive.getFondsNo();

        // 优先级1：优先使用当前选中的全宗（从 FondsContext 获取）
        // 这是系统的核心数据隔离机制，必须优先检查
        String currentFondsNo = FondsContext.getCurrentFondsNo();
        if (StringUtils.hasText(currentFondsNo)) {
            // 用户已切换到特定全宗，只能访问该全宗的数据
            return currentFondsNo.equals(archiveFondsNo);
        }

        // 优先级2：后备方案 - 基于 allowedFonds 列表进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            return StringUtils.hasText(archiveFondsNo) && allowedFonds.contains(archiveFondsNo);
        }

        // 优先级3：仅在没有任何全宗权限时，才使用 created_by 过滤
        // 这是为了确保用户至少能访问自己创建的数据
        if (context.isSelf()) {
            return context.userId() != null && context.userId().equals(archive.getCreatedBy());
        }

        // 如果没有任何权限，不允许访问
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
