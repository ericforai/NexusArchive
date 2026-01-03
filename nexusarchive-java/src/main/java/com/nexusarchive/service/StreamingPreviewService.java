// Input: FileStorageService, Archive Entity
// Output: StreamingPreviewService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.PreviewResponse;
import org.springframework.core.io.Resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 流式预览服务
 * 
 * 功能：
 * 1. 流式预览（支持 Range 请求）
 * 2. 预签名 URL 生成
 * 3. 服务端渲染水印（高敏模式）
 * 4. 水印元数据生成
 * 
 * PRD 来源: Section 2.2 - 流式预览与动态水印
 */
public interface StreamingPreviewService {
    
    /**
     * 流式预览
     * 
     * @param archiveId 档案ID
     * @param mode 预览模式: stream(流式), presigned(预签名), rendered(服务端渲染)
     * @param request HTTP 请求（用于 Range 支持）
     * @param response HTTP 响应
     * @return 预览响应（包含水印元数据）
     */
    PreviewResponse streamPreview(String archiveId, String mode, 
                                 HttpServletRequest request, HttpServletResponse response);
    
    /**
     * 生成预签名 URL
     * 
     * @param archiveId 档案ID
     * @param expiresInSeconds 过期时间（秒）
     * @return 预签名 URL 和水印元数据
     */
    PreviewResponse generatePresignedUrl(String archiveId, int expiresInSeconds);
    
    /**
     * 服务端渲染带水印的内容（高敏模式）
     * 
     * @param archiveId 档案ID
     * @param pageNumber 页码（从1开始）
     * @param request HTTP 请求
     * @param response HTTP 响应
     */
    void renderWithWatermark(String archiveId, int pageNumber, 
                            HttpServletRequest request, HttpServletResponse response);
}


