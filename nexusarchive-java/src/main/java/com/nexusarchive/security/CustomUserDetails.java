// Input: Lombok、Spring Security、Java 标准库
// Output: CustomUserDetails 类
// Pos: 安全模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails extends User {

    private final String id;
    private final String fullName;
    private final String orgCode;
    private final String departmentId; // 组织ID（用于用户归属，不参与数据隔离）
    private final List<String> allowedFonds; // 允许访问的全宗号列表（数据隔离键）

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
                             String id, String fullName, String orgCode, String departmentId,
                             List<String> allowedFonds,
                             boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.fullName = fullName;
        this.orgCode = orgCode;
        this.departmentId = departmentId;
        this.allowedFonds = allowedFonds != null ? allowedFonds : new ArrayList<>();
    }
}
