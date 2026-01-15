// Input: Lombok、Java 标准库
// Output: BatchApprovalResponse 类
// Pos: DTO 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.approval;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量审批响应 DTO
 */
@Data
public class BatchApprovalResponse {

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failed;

    /**
     * 错误详情列表
     */
    private List<BatchErrorItem> errors = new ArrayList<>();

    /**
     * 批量错误项
     */
    @Data
    public static class BatchErrorItem {
        /**
         * 审批记录ID
         */
        private String id;

        /**
         * 失败原因
         */
        private String reason;

        public BatchErrorItem(String id, String reason) {
            this.id = id;
            this.reason = reason;
        }
    }

    /**
     * 添加错误项
     */
    public void addError(String id, String reason) {
        this.errors.add(new BatchErrorItem(id, reason));
        this.failed++;
    }

    /**
     * 增加成功计数
     */
    public void incrementSuccess() {
        this.successCount++;
    }
}
