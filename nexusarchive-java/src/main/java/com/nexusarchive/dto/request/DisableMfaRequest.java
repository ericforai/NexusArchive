// Input: Lombok、Jakarta EE、Java 标准库
// Output: DisableMfaRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 禁用 MFA 请求 DTO
 */
@Data
public class DisableMfaRequest {

    /**
     * 用户密码（用于确认身份）
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
