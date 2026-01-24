// Input: Lombok、Java 标准库
// Output: VoucherDataDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;
import java.util.List;

/**
 * 凭证分录数据 DTO
 */
@Data
public class VoucherDataDto {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 源数据
     */
    private String sourceData;

    /**
     * 凭证字
     */
    private String voucherWord;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 文档日期
     */
    private String docDate;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 关联附件列表（来自原始凭证）
     */
    private List<AttachmentInfo> attachments;

    /**
     * 附件信息（用于展示关联的原始凭证附件）
     */
    @Data
    public static class AttachmentInfo {
        /**
         * 文件ID（用于下载）
         */
        private String id;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 文件类型
         */
        private String fileType;

        /**
         * 文件大小（字节）
         */
        private Long fileSize;

        /**
         * 关联的原始凭证ID
         */
        private String originalVoucherId;

        public AttachmentInfo() {}

        public AttachmentInfo(String id, String fileName, String fileType, Long fileSize, String originalVoucherId) {
            this.id = id;
            this.fileName = fileName;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.originalVoucherId = originalVoucherId;
        }
    }
}
