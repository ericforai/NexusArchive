// Input: Jackson、Lombok、Java 标准库
// Output: SapAttachmentDto 类
// Pos: 数据传输对象 - SAP Attachment OData 响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.sap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SAP Attachment OData 响应 DTO
 * 代表 SAP S/4HANA 凭证附件
 *
 * @author Agent D (基础设施工程师)
 * @see <a href="https://help.sap.com/doc/">SAP S/4HANA API Attachment</a>
 */
@Data
public class SapAttachmentDto {

    /**
     * 附件文件名
     */
    @JsonProperty("FileName")
    private String fileName;

    /**
     * 文件大小 (字节)
     */
    @JsonProperty("FileSize")
    private String fileSize;

    /**
     * 文件 MIME 类型
     */
    @JsonProperty("MimeType")
    private String mimeType;

    /**
     * 下载 URL
     */
    @JsonProperty("URL")
    private String url;

    /**
     * 业务对象类型
     */
    @JsonProperty("BusinessObjectType")
    private String businessObjectType;

    /**
     * 文档 ID
     */
    @JsonProperty("DocumentInfoRecordDocType")
    private String documentInfoRecordDocType;
}
