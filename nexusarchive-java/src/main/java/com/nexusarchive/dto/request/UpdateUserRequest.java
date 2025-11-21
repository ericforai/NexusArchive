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
    private String departmentId;
    private List<String> roleIds;
}
