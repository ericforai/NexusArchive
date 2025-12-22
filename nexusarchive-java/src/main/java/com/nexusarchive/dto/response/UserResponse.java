// Input: Lombok、Java 标准库
// Output: UserResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
