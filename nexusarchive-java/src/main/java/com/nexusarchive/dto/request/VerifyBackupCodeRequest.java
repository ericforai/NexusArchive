// Input: Lombok、Jakarta EE、Java 标准库
// Output: VerifyBackupCodeRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 备用码验证请求 DTO
 */
@Data
public class VerifyBackupCodeRequest {

    /**
     * 备用码
     */
    @NotBlank(message = "备用码不能为空")
    private String backupCode;
}
