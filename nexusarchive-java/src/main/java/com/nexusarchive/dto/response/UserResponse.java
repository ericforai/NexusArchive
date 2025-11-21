package com.nexusarchive.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 用户信息响应
 */
@Data
public class UserResponse {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private String departmentId;
    private String status;
    private List<String> roleIds;
}
