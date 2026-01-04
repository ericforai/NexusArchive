// Input: Java 标准库
// Output: SamplingCriteria DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 抽检条件
 */
@Data
public class SamplingCriteria {
    
    /**
     * 用户ID（可选）
     */
    private String userId;
    
    /**
     * 操作类型（可选）
     */
    private String action;
    
    /**
     * 资源类型（可选）
     */
    private String resourceType;
    
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
    
    /**
     * 全宗号（可选）
     */
    private String fondsNo;
}



