// Input: 预览请求参数
// Output: DTO
// Pos: NexusCore controller dto
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.controller;

import lombok.Data;

@Data
public class ArchivePreviewRequest {
    private String archiveId;
    private String fileId;
    private String mode; // stream | presigned | rendered
    // client info could be in headers, but if in body:
    private String clientRangeSupported;
}
