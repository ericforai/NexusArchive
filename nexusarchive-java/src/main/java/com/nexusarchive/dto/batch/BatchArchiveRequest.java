// Input: Jakarta Validation、Lombok、Java 标准库
// Output: BatchArchiveRequest 类
// Pos: DTO 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量批次操作请求 DTO
 *
 * 用于批量审批或拒绝档案收集批次
 */
@Data
public class BatchArchiveRequest {

    /**
     * 待操作的批次ID列表 (Long类型)
     */
    @NotEmpty(message = "批次ID列表不能为空")
    @Size(max = 100, message = "单次批量操作最多支持100个批次")
    private List<@NotNull(message = "批次ID不能为空") Long> batchIds;

    /**
     * 操作人ID（可选，从认证上下文获取）
     */
    private String operatorId;

    /**
     * 操作人姓名（可选，从认证上下文获取）
     */
    private String operatorName;

    /**
     * 操作意见/备注
     */
    private String comment;

    /**
     * 跳过的批次ID列表（可选）
     * 用于部分失败场景，标记跳过的批次
     */
    private List<Long> skipBatchIds;
}
