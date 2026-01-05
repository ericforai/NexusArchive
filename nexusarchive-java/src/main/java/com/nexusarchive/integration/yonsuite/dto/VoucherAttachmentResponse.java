// Input: Lombok、Jackson
// Output: VoucherAttachmentResponse 类
// Pos: YonSuite 集成 - DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 凭证附件查询响应
 * 对应 YonSuite API: /yonbip/EFI/rest/v1/openapi/queryBusinessFiles
 */
@Data
public class VoucherAttachmentResponse {

    /**
     * 响应码，200为正常
     */
    @JsonProperty("code")
    private String code;

    /**
     * 错误提示信息
     */
    @JsonProperty("message")
    private String message;

    /**
     * 附件数据
     * key 为凭证 ID，value 为附件列表
     */
    @JsonProperty("data")
    private Map<String, List<VoucherAttachment>> data;

    /**
     * 凭证附件
     */
    @Data
    public static class VoucherAttachment {

        /**
         * 文件 ID
         */
        @JsonProperty("fileId")
        private String fileId;

        /**
         * 文件访问路径
         */
        @JsonProperty("filePath")
        private String filePath;

        /**
         * 创建时间
         */
        @JsonProperty("ctime")
        private Long ctime;

        /**
         * 更新时间
         */
        @JsonProperty("utime")
        private Long utime;

        /**
         * 文件扩展名
         */
        @JsonProperty("fileExtension")
        private String fileExtension;

        /**
         * 文件大小（字节）
         */
        @JsonProperty("fileSize")
        private Long fileSize;

        /**
         * 文件名称
         */
        @JsonProperty("fileName")
        private String fileName;

        /**
         * 文件显示名称（含扩展名）
         */
        @JsonProperty("name")
        private String displayName;

        /**
         * 用户 ID
         */
        @JsonProperty("yhtUserId")
        private String yhtUserId;

        /**
         * 租户 ID
         */
        @JsonProperty("tenantId")
        private String tenantId;

        /**
         * 业务对象 ID（凭证 ID）
         */
        @JsonProperty("objectId")
        private String objectId;

        /**
         * 对象名称
         */
        @JsonProperty("objectName")
        private String objectName;
    }
}
