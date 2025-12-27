// Input: Lombok
// Output: OnboardingSummary DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初始化扫描摘要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSummary {
    
    // 科目统计
    private int totalAccounts;
    private int matchedAccounts;
    private int unmatchedAccounts;
    private double accountMatchRate;
    
    // 单据类型统计
    private int totalDocTypes;
    private int matchedDocTypes;
    private int unmatchedDocTypes;
    private double docTypeMatchRate;
    
    // 状态
    private String status;  // PENDING / COMPLETED
}
