// Input: Lombok、Java 标准库
// Output: BatchArchiveResponse 类
// Pos: DTO 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.batch;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量批次操作响应 DTO
 *
 * 返回批量操作的成功/失败统计及错误详情
 */
@Data
public class BatchArchiveResponse {

    /**
     * 成功数量
     */
    private int success;

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
         * 批次ID
         */
        private Long batchId;

        /**
         * 批次编号
         */
        private String batchNo;

        /**
         * 失败原因
         */
        private String reason;

        public BatchErrorItem(Long batchId, String batchNo, String reason) {
            this.batchId = batchId;
            this.batchNo = batchNo;
            this.reason = reason;
        }
    }

    /**
     * 添加错误项
     */
    public void addError(Long batchId, String batchNo, String reason) {
        this.errors.add(new BatchErrorItem(batchId, batchNo, reason));
        this.failed++;
    }

    /**
     * 增加成功计数
     */
    public void incrementSuccess() {
        this.success++;
    }
}
