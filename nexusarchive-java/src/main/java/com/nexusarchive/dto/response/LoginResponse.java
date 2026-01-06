// Input: Lombok、Java 标准库
// Output: LoginResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String username;
        private String fullName;
        private String email;
        private String avatar;
        private String departmentId;
        private String status;
        private List<String> roles;
        private List<String> permissions;

        // 新增字段：个人资料展示
        private String phone;
        private String employeeId;
        private String jobTitle;
        private String orgCode;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdTime;
        private List<String> roleNames;
    }
}
