// Input: Jakarta Validation, Lombok
// Output: UnfreezeArchiveRequest 类
// Pos: 请求 DTO 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解除档案冻结请求
 */
@Data
public class UnfreezeArchiveRequest {

    /**
     * 解除原因
     */
    @NotBlank(message = "解除原因不能为空")
    private String reason;
}
