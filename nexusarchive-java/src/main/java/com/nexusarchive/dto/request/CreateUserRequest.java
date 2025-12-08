package com.nexusarchive.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 创建用户请求
 */
@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private String departmentId;
    private List<String> roleIds;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatar() { return avatar; }
    public String getDepartmentId() { return departmentId; }
    public List<String> getRoleIds() { return roleIds; }
}
