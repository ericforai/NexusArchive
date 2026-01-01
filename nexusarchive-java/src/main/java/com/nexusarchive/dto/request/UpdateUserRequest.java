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
}
