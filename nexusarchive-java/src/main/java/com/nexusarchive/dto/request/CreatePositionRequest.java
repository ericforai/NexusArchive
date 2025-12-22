// Input: Jakarta EE、Lombok、Java 标准库
// Output: CreatePositionRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePositionRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String code;
    private String departmentId;
    private String description;
    private String status;
}
