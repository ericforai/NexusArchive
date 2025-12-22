// Input: Lombok、Spring Security、Java 标准库
// Output: CustomUserDetails 类
// Pos: 安全模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {

    private final String id;
    private final String fullName;
    private final String orgCode;
    private final String departmentId;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
                             String id, String fullName, String orgCode, String departmentId,
                             boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.fullName = fullName;
        this.orgCode = orgCode;
        this.departmentId = departmentId;
    }
}
