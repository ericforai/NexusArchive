// Input: Lombok、Java 标准库
// Output: FileAttachmentDTO 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 核心统一文档附件 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachmentDTO {
    /**
     * 附件ID
     */
    private String id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型 (后缀名)
     */
    private String fileType;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * 下载地址 (URL 或 本地路径)
     */
    private String downloadUrl;

    /**
     * 附件原始内容 (可选)
     */
    private byte[] content;
}
