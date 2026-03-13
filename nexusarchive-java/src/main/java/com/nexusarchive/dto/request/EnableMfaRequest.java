// Input: Lombok、Jakarta EE、Java 标准库
// Output: EnableMfaRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 启用 MFA 请求 DTO
 */
@Data
public class EnableMfaRequest {

    /**
     * TOTP 验证码（6位数字）
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须为6位数字")
    private String code;
}
