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
}
