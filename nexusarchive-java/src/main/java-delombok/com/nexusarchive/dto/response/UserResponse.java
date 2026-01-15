// Input: Lombok、Java 标准库、Swagger
// Output: UserResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import com.nexusarchive.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息响应 DTO
 * <p>
 * 从 User Entity 转换，隐藏以下敏感字段：
 * - passwordHash: 密码哈希（绝对不返回）
 * - deleted: 逻辑删除标记
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应")
public class UserResponse {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1234567890")
    private String id;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    /**
     * M84 机构人员名称 (DA/T 94)
     */
    @Schema(description = "姓名", example = "张三")
    private String fullName;

    /**
     * M85 组织机构代码 (DA/T 94)
     */
    @Schema(description = "组织机构代码", example = "91110000XXXXXXXX")
    private String orgCode;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL", example = "/avatar/user123.jpg")
    private String avatar;

    /**
     * 组织ID（已替换 departmentId）
     */
    @Schema(description = "组织ID", example = "org001")
    private String organizationId;

    /**
     * 状态 (active/disabled/locked)
     */
    @Schema(description = "状态", example = "active")
    private String status;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间", example = "2024-01-01T10:00:00")
    private LocalDateTime lastLoginAt;

    /**
     * 工号
     */
    @Schema(description = "工号", example = "E001")
    private String employeeId;

    /**
     * 职位
     */
    @Schema(description = "职位", example = "档案员")
    private String jobTitle;

    /**
     * 入职日期
     */
    @Schema(description = "入职日期", example = "2020-01-01")
    private String joinDate;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间", example = "2024-01-01T10:00:00")
    private LocalDateTime lastModifiedTime;

    /**
     * 角色ID列表
     */
    @Schema(description = "角色ID列表", example = "[\"role1\", \"role2\"]")
    private List<String> roleIds;

    /**
     * 全宗ID列表
     */
    @Schema(description = "全宗ID列表", example = "[\"fonds1\", \"fonds2\"]")
    private List<String> fondsIds;

    /**
     * 从 Entity 转换为 DTO
     * 隐藏字段: passwordHash, deleted
     */
    public static UserResponse fromEntity(User entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .fullName(entity.getFullName())
                .orgCode(entity.getOrgCode())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .avatar(entity.getAvatar())
                .organizationId(entity.getOrganizationId())
                .status(entity.getStatus())
                .lastLoginAt(entity.getLastLoginAt())
                .employeeId(entity.getEmployeeId())
                .jobTitle(entity.getJobTitle())
                .joinDate(entity.getJoinDate())
                .createdTime(entity.getCreatedTime())
                .lastModifiedTime(entity.getLastModifiedTime())
                .build();
    }

    /**
     * 从 Entity 转换为 DTO（带角色）
     */
    public static UserResponse fromEntity(User entity, List<String> roleIds) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .fullName(entity.getFullName())
                .orgCode(entity.getOrgCode())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .avatar(entity.getAvatar())
                .organizationId(entity.getOrganizationId())
                .status(entity.getStatus())
                .lastLoginAt(entity.getLastLoginAt())
                .employeeId(entity.getEmployeeId())
                .jobTitle(entity.getJobTitle())
                .joinDate(entity.getJoinDate())
                .createdTime(entity.getCreatedTime())
                .lastModifiedTime(entity.getLastModifiedTime())
                .roleIds(roleIds)
                .build();
    }

    /**
     * 从 Entity 转换为 DTO（带角色和全宗）
     */
    /**
     * 从 Entity 转换为 DTO（带角色和全宗）
     */
    public static UserResponse fromEntityWithFonds(User entity, List<String> roleIds, List<String> fondsIds) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .fullName(entity.getFullName())
                .orgCode(entity.getOrgCode())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .avatar(entity.getAvatar())
                .organizationId(entity.getOrganizationId())
                .status(entity.getStatus())
                .lastLoginAt(entity.getLastLoginAt())
                .employeeId(entity.getEmployeeId())
                .jobTitle(entity.getJobTitle())
                .joinDate(entity.getJoinDate())
                .createdTime(entity.getCreatedTime())
                .lastModifiedTime(entity.getLastModifiedTime())
                .roleIds(roleIds)
                .fondsIds(fondsIds)
                .build();
    }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public void setStatus(String status) { this.status = status; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }
    public void setRoleIds(List<String> roleIds) { this.roleIds = roleIds; }
    public void setFondsIds(List<String> fondsIds) { this.fondsIds = fondsIds; }

    // Manual Builder
    public static UserResponseBuilder builder() { return new UserResponseBuilder(); }

    public static class UserResponseBuilder {
        private UserResponse dto = new UserResponse();
        public UserResponseBuilder id(String id) { dto.setId(id); return this; }
        public UserResponseBuilder username(String username) { dto.setUsername(username); return this; }
        public UserResponseBuilder fullName(String fullName) { dto.setFullName(fullName); return this; }
        public UserResponseBuilder orgCode(String orgCode) { dto.setOrgCode(orgCode); return this; }
        public UserResponseBuilder email(String email) { dto.setEmail(email); return this; }
        public UserResponseBuilder phone(String phone) { dto.setPhone(phone); return this; }
        public UserResponseBuilder avatar(String avatar) { dto.setAvatar(avatar); return this; }
        public UserResponseBuilder organizationId(String organizationId) { dto.setOrganizationId(organizationId); return this; }
        public UserResponseBuilder status(String status) { dto.setStatus(status); return this; }
        public UserResponseBuilder lastLoginAt(LocalDateTime lastLoginAt) { dto.setLastLoginAt(lastLoginAt); return this; }
        public UserResponseBuilder employeeId(String employeeId) { dto.setEmployeeId(employeeId); return this; }
        public UserResponseBuilder jobTitle(String jobTitle) { dto.setJobTitle(jobTitle); return this; }
        public UserResponseBuilder joinDate(String joinDate) { dto.setJoinDate(joinDate); return this; }
        public UserResponseBuilder createdTime(LocalDateTime createdTime) { dto.setCreatedTime(createdTime); return this; }
        public UserResponseBuilder lastModifiedTime(LocalDateTime lastModifiedTime) { dto.setLastModifiedTime(lastModifiedTime); return this; }
        public UserResponseBuilder roleIds(List<String> roleIds) { dto.setRoleIds(roleIds); return this; }
        public UserResponseBuilder fondsIds(List<String> fondsIds) { dto.setFondsIds(fondsIds); return this; }
        public UserResponse build() { return dto; }
    }
}
