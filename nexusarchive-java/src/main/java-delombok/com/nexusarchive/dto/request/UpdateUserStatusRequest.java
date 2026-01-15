// Input: Lombok、Jakarta EE、Java 标准库
// Output: UpdateUserStatusRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UpdateUserStatusRequest {
    @NotBlank
    private String status; // active / disabled / locked
}
