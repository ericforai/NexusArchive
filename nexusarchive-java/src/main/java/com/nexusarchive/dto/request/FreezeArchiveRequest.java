// Input: Jakarta Validation, Lombok
// Output: FreezeArchiveRequest 类
// Pos: 请求 DTO 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案冻结请求
 */
@Data
public class FreezeArchiveRequest {

    /**
     * 档案ID列表
     */
    @NotNull(message = "档案ID列表不能为空")
    private List<String> archiveIds;

    /**
     * 冻结原因
     */
    @NotBlank(message = "冻结原因不能为空")
    private String reason;

    /**
     * 冻结到期日期（可选，null 表示永久冻结）
     */
    private LocalDate expireDate;
}
