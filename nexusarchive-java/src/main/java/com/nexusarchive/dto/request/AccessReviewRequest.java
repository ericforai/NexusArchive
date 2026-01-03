// Input: Java 标准库
// Output: AccessReviewRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 访问权限复核请求 DTO
 */
@Data
public class AccessReviewRequest {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 复核类型: PERIODIC, AD_HOC, ON_DEMAND
     */
    private String reviewType;
    
    /**
     * 复核日期
     */
    private LocalDate reviewDate;
    
    /**
     * 复核人ID
     */
    private String reviewerId;
}


