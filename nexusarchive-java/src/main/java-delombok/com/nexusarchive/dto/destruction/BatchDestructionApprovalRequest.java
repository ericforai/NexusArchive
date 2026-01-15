// Input: Jakarta Validation、Java 标准库
// Output: BatchDestructionApprovalRequest 类
// Pos: DTO层 - 销毁批量审批请求
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.destruction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量销毁审批请求
 */
public record BatchDestructionApprovalRequest(
        /**
         * 待审批的销毁申请ID列表
         */
        @NotEmpty(message = "审批ID列表不能为空")
        @Size(max = 100, message = "单次最多审批100条记录")
        List<String> ids,

        /**
         * 统一审批意见（可选）
         */
        @Size(max = 500, message = "审批意见不能超过500字符")
        String comment,

        /**
         * 审批类型：first（第一审批）、second（第二审批复核）
         * 注意：当前后端使用单审批模型，此字段保留用于未来扩展
         */
        String approvalType
) {
    /**
     * 获取审批意见，如果为null则返回空字符串
     */
    public String comment() {
        return comment == null ? "" : comment;
    }

    /**
     * 获取审批类型，如果为null则返回默认值"first"
     */
    public String approvalType() {
        return approvalType == null ? "first" : approvalType;
    }
}
