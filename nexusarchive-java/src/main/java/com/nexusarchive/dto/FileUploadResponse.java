package com.nexusarchive.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传响应
 */
@Data
@Builder
public class FileUploadResponse {
    
    /**
     * 生成的唯一流水号
     */
    private String code;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 存储路径
     */
    private String storagePath;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 来源系统
     */
    private String source;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 文件哈希值
     */
    private String fileHash;
    
    /**
     * 哈希算法
     */
    private String hashAlgorithm;
}
