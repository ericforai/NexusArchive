// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: DataScopeService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.enums.DataScopeType;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Borrowing;
import com.nexusarchive.entity.Org;
import com.nexusarchive.entity.Role;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.OrgMapper;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final RoleMapper roleMapper;
    private final OrgMapper orgMapper;
    private final UserMapper userMapper;
    private final ArchiveMapper archiveMapper;

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
        String departmentId = resolveDepartmentId(userDetails);
        Set<String> departmentIds = computeDepartmentSet(scopeType, departmentId);

        return new DataScopeContext(scopeType, userId, departmentIds, departmentId);
    }

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

        Set<String> deptIds = context.departmentIds();
        if (!deptIds.isEmpty()) {
            wrapper.in("department_id", deptIds);
            return;
        }

        if (StringUtils.hasText(context.departmentId())) {
            wrapper.eq("department_id", context.departmentId());
            return;
        }

        if (context.userId() != null) {
            wrapper.eq("created_by", context.userId());
        } else {
            wrapper.eq("1", "0");
        }
    }

    public void applyBorrowingScope(QueryWrapper<Borrowing> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq("user_id", context.userId());
            } else {
                wrapper.eq("1", "0");
            }
            return;
        }

        Set<String> deptIds = context.departmentIds();
        if (!deptIds.isEmpty()) {
            List<String> archiveIds = archiveMapper.selectIdsByDepartmentIds(deptIds);
            if (archiveIds.isEmpty()) {
                wrapper.eq("1", "0");
            } else {
                wrapper.in("archive_id", archiveIds);
            }
            return;
        }

        if (StringUtils.hasText(context.departmentId())) {
            List<String> archiveIds = archiveMapper.selectIdsByDepartmentIds(Collections.singleton(context.departmentId()));
            if (archiveIds.isEmpty()) {
                wrapper.eq("1", "0");
            } else {
                wrapper.in("archive_id", archiveIds);
            }
            return;
        }

        if (context.userId() != null) {
            wrapper.eq("user_id", context.userId());
        } else {
            wrapper.eq("1", "0");
        }
    }

    public boolean canAccessArchive(Archive archive, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return true;
        }
        if (context.isSelf()) {
            return context.userId() != null && context.userId().equals(archive.getCreatedBy());
        }
        String departmentId = archive.getDepartmentId();
        if (StringUtils.hasText(departmentId) && context.departmentIds().contains(departmentId)) {
            return true;
        }
        return context.userId() != null && context.userId().equals(archive.getCreatedBy());
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

    private String resolveDepartmentId(CustomUserDetails details) {
        if (StringUtils.hasText(details.getDepartmentId())) {
            return details.getDepartmentId();
        }
        User user = userMapper.selectById(details.getId());
        if (user != null && StringUtils.hasText(user.getDepartmentId())) {
            return user.getDepartmentId();
        }
        return details.getDepartmentId();
    }

    private Set<String> computeDepartmentSet(DataScopeType scopeType, String departmentId) {
        if (!StringUtils.hasText(departmentId)) {
            return Collections.emptySet();
        }
        if (scopeType == DataScopeType.DEPARTMENT_AND_CHILD) {
            return collectDepartmentIds(departmentId, true);
        }
        if (scopeType == DataScopeType.DEPARTMENT) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(Collections.singletonList(departmentId)));
        }
        return Collections.emptySet();
    }

    private Set<String> collectDepartmentIds(String rootId, boolean includeChildren) {
        if (!StringUtils.hasText(rootId)) {
            return Collections.emptySet();
        }
        List<Org> allOrgs = orgMapper.selectList(new LambdaQueryWrapper<Org>().eq(Org::getDeleted, 0));
        Map<String, List<Org>> parentMap = allOrgs.stream()
                .collect(Collectors.groupingBy(org -> org.getParentId()));
        Set<String> ids = new LinkedHashSet<>();
        ids.add(rootId);
        if (includeChildren) {
            collectChildren(rootId, parentMap, ids);
        }
        return Collections.unmodifiableSet(ids);
    }

    private void collectChildren(String parentId, Map<String, List<Org>> parentMap, Set<String> bucket) {
        List<Org> children = parentMap.get(parentId);
        if (children == null) {
            return;
        }
        for (Org child : children) {
            if (bucket.add(child.getId())) {
                collectChildren(child.getId(), parentMap, bucket);
            }
        }
    }

    public record DataScopeContext(DataScopeType type, String userId, Set<String> departmentIds, String departmentId) {
        public static DataScopeContext all() {
            return new DataScopeContext(DataScopeType.ALL, null, Collections.emptySet(), null);
        }

        public boolean isAll() {
            return type.isAll();
        }

        public boolean isSelf() {
            return type.isSelf();
        }
    }
}
