// Input: Java 标准库
// Output: PreviewResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预览响应 DTO
 */
@Data
public class PreviewResponse {
    
    /**
     * 预览模式: stream, presigned, rendered
     */
    private String mode;
    
    /**
     * 预签名 URL（presigned 模式）
     */
    private String presignedUrl;
    
    /**
     * 预签名 URL 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 追踪ID
     */
    private String traceId;
    
    /**
     * 水印元数据
     */
    private WatermarkMetadata watermark;
    
    /**
     * 水印元数据
     */
    @Data
    public static class WatermarkMetadata {
        /**
         * 水印文本（用户名 + 时间戳 + TraceID）
         */
        private String text;
        
        /**
         * 水印副文本（TraceID + FondsNo）
         */
        private String subtext;
        
        /**
         * 透明度（0.0 - 1.0）
         */
        private Double opacity;
        
        /**
         * 旋转角度（度）
         */
        private Integer rotate;
    }
}





