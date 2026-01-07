// Input: Jakarta Validation、Lombok、Java 标准库
// Output: BatchApprovalRequest 类
// Pos: DTO 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量审批请求 DTO
 */
@Data
public class BatchApprovalRequest {

    /**
     * 待审批的档案ID列表
     */
    @NotEmpty(message = "审批ID列表不能为空")
    @Size(max = 100, message = "单次批量审批最多支持100条记录")
    private List<@NotBlank(message = "审批ID不能为空") String> ids;

    /**
     * 审批人ID（可选，从认证上下文获取）
     */
    private String approverId;

    /**
     * 审批人姓名（可选，从认证上下文获取）
     */
    private String approverName;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 跳过的记录ID列表（可选）
     * 用于部分失败场景，标记跳过的记录
     */
    private List<String> skipIds;
}
