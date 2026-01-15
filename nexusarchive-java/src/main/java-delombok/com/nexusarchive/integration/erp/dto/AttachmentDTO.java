// Input: Lombok、Java 标准库
// Output: AttachmentDTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ERP 附件 DTO
 * 
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDTO {

    /**
     * 附件ID
     */
    private String attachmentId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型 (PDF, JPG, etc.)
     */
    private String fileType;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * 下载 URL
     */
    private String downloadUrl;

    /**
     * 文件内容 (Base64，用于小文件直接传输)
     */
    private String contentBase64;
}
