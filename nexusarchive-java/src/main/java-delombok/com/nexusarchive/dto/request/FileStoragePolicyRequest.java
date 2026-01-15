// Input: Java 标准库
// Output: FileStoragePolicyRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 文件存储策略请求 DTO
 */
@Data
public class FileStoragePolicyRequest {
    
    /**
     * 全宗号
     */
    private String fondsNo;
    
    /**
     * 策略类型: IMMUTABLE, RETENTION
     */
    private String policyType;
    
    /**
     * 保留天数（NULL表示永久保留）
     */
    private Integer retentionDays;
    
    /**
     * 不可变截止日期
     */
    private LocalDate immutableUntil;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
}





