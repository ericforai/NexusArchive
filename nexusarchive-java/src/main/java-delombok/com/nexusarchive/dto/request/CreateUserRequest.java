// Input: Lombok、Java 标准库
// Output: CreateUserRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
    private String organizationId; // 组织ID（已替换 departmentId）
    private List<String> roleIds;
    private List<String> fondsCodes; // 可访问的全宗号列表

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatar() { return avatar; }
    public String getOrganizationId() { return organizationId; }
    public List<String> getRoleIds() { return roleIds; }
    public List<String> getFondsCodes() { return fondsCodes; }
}
