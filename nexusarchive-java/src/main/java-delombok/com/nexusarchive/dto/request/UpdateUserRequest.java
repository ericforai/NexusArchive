// Input: Lombok、Java 标准库
// Output: UpdateUserRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 更新用户请求（不包括密码）
 */
@Data
public class UpdateUserRequest {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private String organizationId; // 组织ID（已替换 departmentId）
    private List<String> roleIds;

    // Manual Getters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatar() { return avatar; }
    public String getOrganizationId() { return organizationId; }
    public List<String> getRoleIds() { return roleIds; }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public void setRoleIds(List<String> roleIds) { this.roleIds = roleIds; }
}
