// Input: 预览请求 (ArchiveID/FileID)
// Output: 流式文件内容 (支持 Range)
// Pos: NexusCore service
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.service;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface StreamingPreviewService {

    /**
     * 获取文件预览流
     * @param archiveId 档案ID (用于查找主文件)
     * @param fileId 文件ID (可选，指定特定文件)
     * @param mode 预览模式 (stream: 原文流 / rendered: 带水印渲染流)
     * @param rangeStart Range头起始字节 (nullable)
     * @param rangeEnd Range头结束字节 (nullable)
     * @param traceId 审计追踪ID (for watermark)
     * @param operator 操作人 (for watermark)
     * @return 用于构建响应的 ResponseEntity (含流与Headers)
     */
    ResponseEntity<Resource> preview(String archiveId,
                                     String fileId,
                                     String mode,
                                     Long rangeStart,
                                     Long rangeEnd,
                                     String traceId,
                                     String operator) throws IOException;
}
