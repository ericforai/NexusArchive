// Input: Lombok、Java 标准库
// Output: BatchDestructionApprovalResponse 类
// Pos: DTO层 - 销毁批量审批响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.destruction;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量审批结果响应
 */
@Data
public class BatchDestructionApprovalResponse {
    /**
     * 成功数量
     */
    private int success = 0;

    /**
     * 失败数量
     */
    private int failed = 0;

    /**
     * 失败详情列表
     */
    private List<ErrorDetail> errors = new ArrayList<>();

    /**
     * 失败详情
     */
    @Data
    public static class ErrorDetail {
        /**
         * 失败的ID
         */
        private String id;

        /**
         * 失败原因
         */
        private String reason;
    }

    /**
     * 增加成功计数
     */
    public void incrementSuccess() {
        this.success++;
    }

    /**
     * 增加失败计数并添加错误详情
     */
    public void addError(String id, String reason) {
        this.failed++;
        ErrorDetail detail = new ErrorDetail();
        detail.setId(id);
        detail.setReason(reason);
        this.errors.add(detail);
    }
}
