// Input: Lombok
// Output: OfdPreviewResourceResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

/**
 * OFD 预览资源决策结果
 */
@Data
public class OfdPreviewResourceResponse {

    /**
     * 优先预览模式: converted / liteofd
     */
    private String preferredMode;

    /**
     * 原始 OFD 文件ID
     */
    private String originalFileId;

    /**
     * 原始 OFD 下载地址
     */
    private String originalDownloadUrl;

    /**
     * 转换产物文件ID
     */
    private String convertedFileId;

    /**
     * 转换产物 MIME 类型
     */
    private String convertedMimeType;

    /**
     * 转换产物预览地址
     */
    private String convertedPreviewUrl;

    /**
     * 原始文件名
     */
    private String fileName;
}
